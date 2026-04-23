/**
 * ChatPage.tsx - 일반 채팅 & CS 상담 통합 채팅 페이지
 */
import { useState, useEffect, useRef, useCallback } from "react";
import { Link, useSearchParams } from "react-router";
import { Client as StompClient } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Card, CardContent } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../ui/alert-dialog";
import {
  Send,
  Loader2,
  MessageCircle,
  RefreshCw,
  Pencil,
  Trash2,
  ChevronLeft,
} from "lucide-react";
import { toast } from "sonner";
import { chatApi, csApi } from "../../../api/apiService";
import { apiClient } from "../../../api/client";
import type { ChatRoomDto, MessageResponseDto, CsChatRoomDto, CsMessageDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const getWsUrl = () => {
  const apiBase = import.meta.env.VITE_API_BASE_URL || "/api";
  // Vercel(HTTPS) 환경에서 상대경로가 http로 해석되는 것을 방지하기 위해 origin을 붙임
  const base = apiBase.startsWith("http") ? apiBase : window.location.origin + apiBase;
  return base.replace(/\/api$/, "") + "/api/ws";
};

const WS_URL = getWsUrl();

export function ChatPage() {
  const { isLoggedIn, userId } = useAuth();
  const [searchParams] = useSearchParams();
  const csRoomId = searchParams.get("csRoomId");

  const [rooms, setRooms] = useState<ChatRoomDto[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoomDto | null>(null);
  const [csRoom, setCsRoom] = useState<CsChatRoomDto | null>(null);
  const [messages, setMessages] = useState<MessageResponseDto[]>([]);
  const [inputText, setInputText] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isLoadingRooms, setIsLoadingRooms] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);

  const [deleteRoomTarget, setDeleteRoomTarget] = useState<ChatRoomDto | null>(null);
  const [isDeletingRoom, setIsDeletingRoom] = useState(false);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<MessageResponseDto | null>(null);

  const stompRef = useRef<StompClient | null>(null);
  const subscriptionRef = useRef<any>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const isNearBottomRef = useRef(true);
  const pendingRoomIdRef = useRef<{ id: number; isCs: boolean } | null>(null);

  const isCsMode = !!csRoomId;

  // 스크롤 제어
  const scrollToBottom = useCallback((smooth = true) => {
    if (scrollContainerRef.current) {
      const { scrollHeight } = scrollContainerRef.current;
      scrollContainerRef.current.scrollTo({ top: scrollHeight, behavior: smooth ? "smooth" : "auto" });
    }
  }, []);

  const handleScroll = () => {
    if (!scrollContainerRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = scrollContainerRef.current;
    isNearBottomRef.current = scrollHeight - scrollTop - clientHeight < 150;
  };

  // CS 데이터를 일반 메시지 형식으로 변환
  const mapCsToMsg = useCallback((csMsg: CsMessageDto): MessageResponseDto => ({
    id: csMsg.id,
    senderId: csMsg.senderId,
    senderName: csMsg.senderName,
    content: csMsg.content,
    eventType: csMsg.eventType,
    isRead: csMsg.isRead,
    isSystem: csMsg.eventType === "ENTER" || csMsg.eventType === "LEAVE",
    sentAt: csMsg.createdAt,
    chatRoomId: Number(csRoomId)
  }), [csRoomId]);

  // STOMP 구독 분기 처리
  const subscribeToRoom = useCallback((id: number, isCs: boolean) => {

    console.log(`[STOMP DEBUG] subscribeToRoom 호출`, {
      roomId: id,
      stompConnected: stompRef.current?.connected,
      stompExists: !!stompRef.current,
    });

    const c = stompRef.current;

    // ✅ connected 체크를 더 엄격하게
    if (!c || !c.connected) {
      console.warn(`[STOMP] subscribeToRoom 호출 시 연결 안됨. room: ${id}`);
      return;
    }

    // 이전 구독 해제
    subscriptionRef.current?.unsubscribe();
    subscriptionRef.current = null;

    const dest = isCs ? `/receive/chat/cs/${id}` : `/receive/chat/rooms/${id}`;

    const accessToken = localStorage.getItem("accessToken");
    const headers = { Authorization: `Bearer ${accessToken}` };

    subscriptionRef.current = c.subscribe(dest, (frame) => {
      const msgData = JSON.parse(frame.body);
      const msg = isCs ? mapCsToMsg(msgData) : (msgData as MessageResponseDto);

      const type = String(msg.eventType).toUpperCase();
      setMessages((prev) => {
        if (type === "EDIT") return prev.map((m) => m.id === msg.id ? { ...m, content: msg.content } : m);
        if (type === "DELETE") return prev.filter((m) => m.id !== msg.id);
        if (prev.some(m => m.id === msg.id)) return prev;
        return [...prev, msg];
      });

      if (type === "SEND" || type === "ENTER") {
        setTimeout(() => scrollToBottom(true), 50);
      }
    }, headers);

    // ✅ subscribe 후 약간 딜레이 두고 enter 발행 (서버 처리 시간 확보)
    const enterDest = isCs ? `/send/chat/cs/${id}/enter` : `/send/chat/rooms/${id}/enter`;
    setTimeout(() => {
      if (stompRef.current?.connected) {
        stompRef.current.publish({ 
          destination: enterDest, 
          body: "",
          headers: headers
        });
      }
    }, 100);

  }, [mapCsToMsg, scrollToBottom]);

  const connectStomp = useCallback(() => {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken || stompRef.current?.connected) return;

    setIsConnecting(true);

    const client = new StompClient({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      onConnect: () => {
        // ✅ onConnect 진입 시점에 stompRef를 먼저 확정
        stompRef.current = client;
        setIsConnected(true);
        setIsConnecting(false);

        if (pendingRoomIdRef.current) {
          const { id, isCs } = pendingRoomIdRef.current;
          pendingRoomIdRef.current = null; // ✅ null 처리 먼저
          subscribeToRoom(id, isCs);       // ✅ client 파라미터 불필요 (stompRef 이미 세팅됨)
        }
      },
      onDisconnect: () => {
        setIsConnected(false);
        setIsConnecting(false);
      },
      onStompError: () => {
        setIsConnected(false);
        setIsConnecting(false);
        toast.error("채팅 서버 연결에 실패했습니다.");
      },
      reconnectDelay: 5000,
    });

    // ✅ activate 전에 stompRef 선점 (onConnect보다 먼저 assign)
    stompRef.current = client;
    client.activate();

  }, [subscribeToRoom]);

  const disconnectStomp = useCallback(() => {
    subscriptionRef.current?.unsubscribe();
    subscriptionRef.current = null;
    stompRef.current?.deactivate();
    stompRef.current = null;
    setIsConnected(false);
  }, []);

  useEffect(() => {
    if (isLoggedIn) connectStomp();
    return () => disconnectStomp();
  }, [isLoggedIn, connectStomp, disconnectStomp]);

  // 데이터 로딩 로직
  const loadInitialData = useCallback(async () => {
    if (!isLoggedIn) return;
    setIsLoadingRooms(true);
    try {
      if (isCsMode) {
        setIsLoadingMessages(true);
        // CS 모드: 방 정보 + 메시지 동시 로드
        const [roomRes, msgRes] = await Promise.all([
          apiClient.get(`/v1/cs/rooms/${csRoomId}`),
          apiClient.get(`/v1/cs/rooms/${csRoomId}/messages`)
        ]);
        setCsRoom(roomRes.data.data);
        setMessages((msgRes.data.data as CsMessageDto[]).map(mapCsToMsg));
        setTimeout(() => scrollToBottom(false), 50);
        
        if (stompRef.current?.connected) subscribeToRoom(Number(csRoomId), true);
        else pendingRoomIdRef.current = { id: Number(csRoomId), isCs: true };
      } else {
        // 일반 채팅 모드: 방 목록만 로드
        const res = await chatApi.getMyRooms(0, 50);
        setRooms(res.data.data.content);
      }
    } catch {
      toast.error("데이터를 불러오는데 실패했습니다.");
    } finally {
      setIsLoadingRooms(false);
      setIsLoadingMessages(false);
    }
  }, [isLoggedIn, isCsMode, csRoomId, mapCsToMsg, subscribeToRoom]);

  useEffect(() => { loadInitialData(); }, [loadInitialData]);

  // 방 선택 핸들러 (일반 채팅용)
  const handleSelectRoom = async (room: ChatRoomDto) => {
    setSelectedRoom(room);
    setMessages([]);
    setIsLoadingMessages(true);

    try {
      const res = await chatApi.getMessages(room.id);
      setMessages([...res.data.data.messages].reverse());
      setTimeout(() => scrollToBottom(false), 50);

      // ✅ pendingRoomIdRef를 먼저 설정해두고
      pendingRoomIdRef.current = { id: room.id, isCs: false };

      if (stompRef.current?.connected) {
        // 연결됨 → 즉시 구독
        pendingRoomIdRef.current = null;
        subscribeToRoom(room.id, false);
      }
      // 연결 안됨 → onConnect에서 처리됨 (pending이 이미 설정됨)

    } catch {
      toast.error("메시지 로드 실패");
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const handleSend = () => {
    if (!inputText.trim() || !stompRef.current?.connected) return;
    const dest = isCsMode ? `/send/chat/cs/${csRoomId}` : `/send/chat/rooms/${selectedRoom?.id}`;
    
    const accessToken = localStorage.getItem("accessToken");
    
    stompRef.current.publish({
      destination: dest,
      body: JSON.stringify({ content: inputText.trim() }),
      headers: { Authorization: `Bearer ${accessToken}` }
    });
    setInputText("");
    isNearBottomRef.current = true;
  };

  const handleDeleteRoom = async () => {
    if (!deleteRoomTarget) return;
    setIsDeletingRoom(true);
    try {
      await chatApi.deleteRoom(deleteRoomTarget.id);
      toast.success("채팅방이 삭제되었습니다.");
      setRooms(prev => prev.filter(r => r.id !== deleteRoomTarget.id));
      if (selectedRoom?.id === deleteRoomTarget.id) {
        setSelectedRoom(null);
        setMessages([]);
        subscriptionRef.current?.unsubscribe();
      }
      setDeleteRoomTarget(null);
    } catch {
      toast.error("채팅방 삭제 실패");
    } finally {
      setIsDeletingRoom(false);
    }
  };

  const handleUpdateMessage = async (id: number, content: string) => {
    try {
      if (isCsMode) {
        await csApi.updateMessage(id, content);
      } else {
        await chatApi.updateMessage(id, content);
      }
      setMessages(prev => prev.map(m => m.id === id ? { ...m, content } : m));
      setEditingId(null);
      toast.success("메시지가 수정되었습니다.");
    } catch {
      toast.error("메시지 수정 실패");
    }
  };

  const handleDeleteMessage = async (id: number) => {
    if (!confirm("메시지를 삭제하시겠습니까?")) return;
    try {
      if (isCsMode) {
        await csApi.deleteMessage(id);
      } else {
        await chatApi.deleteMessage(id);
      }
      setMessages(prev => prev.filter(m => m.id !== id));
      toast.success("메시지가 삭제되었습니다.");
    } catch {
      toast.error("메시지 삭제 실패");
    }
  };

  if (!isLoggedIn) return <div className="flex items-center justify-center h-full py-20 text-gray-500">로그인이 필요합니다.</div>;

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-white overflow-hidden">
      <div className="px-4 py-3 border-b flex items-center justify-between shrink-0 bg-white z-20">
        <div className="flex items-center gap-3">
          {(selectedRoom || isCsMode) && (
            <Button variant="ghost" size="icon" onClick={() => isCsMode ? navigate(-1) : setSelectedRoom(null)} className="lg:hidden -ml-2">
              <ChevronLeft className="w-6 h-6" />
            </Button>
          )}
          <h1 className="text-lg font-extrabold text-gray-900">
            {isCsMode ? "고객센터 상담" : selectedRoom ? "대화 중" : "메시지"}
          </h1>
          <Badge className={`text-[10px] h-5 ${isConnected ? "bg-green-50 text-green-600" : "bg-gray-50 text-gray-400"}`} variant="outline">
            {isConnected ? "● 실시간" : "○ 연결끊김"}
          </Badge>
        </div>
        <Button size="icon" variant="ghost" onClick={loadInitialData} className="w-8 h-8 text-gray-400">
          <RefreshCw className="w-4 h-4" />
        </Button>
      </div>

      <div className="flex-1 flex min-h-0">
        {/* ── 사이드바 ── */}
        {!isCsMode && (
          <div className={`${selectedRoom ? "hidden lg:flex" : "flex"} w-full lg:w-80 flex-col border-r bg-gray-50/30`}>
            <div className="flex-1 overflow-y-auto">
              {isLoadingRooms ? <div className="flex justify-center py-10"><Loader2 className="animate-spin" /></div> :
               rooms.length === 0 ? <div className="p-10 text-center text-gray-400 text-sm">대화가 없습니다.</div> :
               rooms.map(room => (
                <div key={room.id} className={`group p-4 border-b cursor-pointer relative ${selectedRoom?.id === room.id ? "bg-white border-l-4 border-l-blue-600" : "hover:bg-gray-100"}`}>
                  <div onClick={() => handleSelectRoom(room)}>
                    <p className="font-bold text-sm truncate pr-6">{room.projectTitle}</p>
                    <p className="text-[11px] text-gray-500">{room.clientName} & {room.developerName}</p>
                  </div>
                  <button 
                    onClick={(e) => { e.stopPropagation(); setDeleteRoomTarget(room); }}
                    className="absolute right-2 top-1/2 -translate-y-1/2 p-2 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ── 채팅창 ── */}
        <div className={`${(selectedRoom || isCsMode) ? "flex" : "hidden lg:flex"} flex-1 flex-col bg-white`}>
          {(selectedRoom || isCsMode) ? (
            <>
              <div className="px-4 py-2 border-b bg-gray-50/50 hidden lg:block">
                <p className="text-xs font-bold text-gray-400 uppercase">Chatting with</p>
                <p className="text-sm font-black text-gray-700">{isCsMode ? csRoom?.title : selectedRoom?.projectTitle}</p>
              </div>

              <div ref={scrollContainerRef} onScroll={handleScroll} className="flex-1 overflow-y-auto p-4 space-y-4 bg-white">
                {isLoadingMessages ? <div className="flex justify-center py-10"><Loader2 className="animate-spin" /></div> :
                 messages.length === 0 ? <div className="flex flex-col items-center justify-center h-full text-gray-300">대화의 시작을 열어보세요!</div> :
                 messages.map(msg => {
                   const isMine = msg.senderId === userId;
                   const isEditing = editingId === msg.id;

                   if (msg.isSystem) return <div key={msg.id} className="flex justify-center my-4"><span className="bg-gray-100 text-gray-400 text-[10px] px-4 py-1 rounded-full">{msg.content}</span></div>;
                   
                   return (
                     <div key={msg.id} className={`flex ${isMine ? "justify-end" : "justify-start"} group`}>
                       <div className={`flex flex-col ${isMine ? "items-end" : "items-start"} max-w-[85%]`}>
                         {!isMine && <span className="text-[11px] font-bold text-gray-400 mb-1">{msg.senderName}</span>}
                         
                         <div className="flex items-end gap-2 max-w-full">
                           {isMine && !isEditing && (
                             <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity mb-1">
                               <button onClick={() => { setEditingId(msg.id); setEditContent(msg.content); }} className="p-1 text-gray-300 hover:text-blue-500"><Pencil className="w-3 h-3" /></button>
                               <button onClick={() => handleDeleteMessage(msg.id)} className="p-1 text-gray-300 hover:text-red-500"><Trash2 className="w-3 h-3" /></button>
                             </div>
                           )}

                           <div className={`relative px-4 py-2.5 rounded-2xl text-sm shadow-sm ${isMine ? "bg-blue-600 text-white rounded-tr-none" : "bg-gray-100 text-gray-800 rounded-tl-none"}`}>
                             {isEditing ? (
                               <div className="flex flex-col gap-2 min-w-[200px]">
                                 <textarea 
                                   value={editContent} 
                                   onChange={e => setEditContent(e.target.value)}
                                   className="bg-transparent border-none text-white focus:ring-0 p-0 resize-none text-sm w-full"
                                   rows={2}
                                   autoFocus
                                 />
                                 <div className="flex justify-end gap-2">
                                   <button onClick={() => setEditingId(null)} className="text-[10px] text-blue-200 hover:text-white">취소</button>
                                   <button onClick={() => handleUpdateMessage(msg.id, editContent)} className="text-[10px] font-bold text-white bg-blue-500 px-2 py-0.5 rounded shadow-sm">저장</button>
                                 </div>
                               </div>
                             ) : (
                               <p className="whitespace-pre-wrap break-all">{msg.content}</p>
                             )}
                           </div>
                         </div>
                         
                         <span className="text-[9px] text-gray-300 mt-1">
                           {new Date(msg.sentAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                           {msg.eventType === "EDIT" && <span className="ml-1 text-blue-300">(수정됨)</span>}
                         </span>
                       </div>
                     </div>
                   );
                 })}
              </div>

              <div className="p-4 border-t bg-white">
                <div className="flex gap-2 items-center bg-gray-100 p-1.5 rounded-2xl border transition-all focus-within:bg-white focus-within:border-blue-200">
                  <Input 
                    placeholder="메시지를 입력하세요..." 
                    value={inputText} 
                    onChange={e => setInputText(e.target.value)} 
                    className="border-none bg-transparent focus-visible:ring-0 h-10 flex-1"
                    onKeyDown={e => e.key === "Enter" && !e.shiftKey && (e.preventDefault(), handleSend())}
                  />
                  <Button onClick={handleSend} disabled={!inputText.trim()} size="icon" className="rounded-xl w-10 h-10 bg-blue-600"><Send className="w-4 h-4 text-white" /></Button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-gray-200 bg-gray-50/20">
              <MessageCircle className="w-12 h-12 mb-4 opacity-10" />
              <p className="text-sm font-bold text-gray-400">대화할 채팅방을 선택해 주세요</p>
            </div>
          )}
        </div>
      </div>

      {/* 채팅방 삭제 확인 */}
      <AlertDialog open={!!deleteRoomTarget} onOpenChange={(o) => !o && setDeleteRoomTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>채팅방 나가기</AlertDialogTitle>
            <AlertDialogDescription>
              이 채팅방을 나가시겠습니까? 대화 내용이 모두 삭제되며 상대방과의 연결이 끊어집니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteRoom} disabled={isDeletingRoom} className="bg-red-600 hover:bg-red-700">
              {isDeletingRoom ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null}
              나가기
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
