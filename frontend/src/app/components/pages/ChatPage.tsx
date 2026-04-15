/**
 * ChatPage.tsx - 채팅 페이지 (모바일 레이아웃 대응 및 실시간 수정/삭제 반영 완벽화)
 */
import { useState, useEffect, useRef, useCallback } from "react";
import { Link } from "react-router";
import { Client as StompClient } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
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
  ArrowLeft,
  MessageCircle,
  RefreshCw,
  Pencil,
  Trash2,
  Check,
  X,
  ChevronLeft,
} from "lucide-react";
import { toast } from "sonner";
import { chatApi } from "../../../api/apiService";
import type { ChatRoomDto, MessageResponseDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const getWsUrl = () => {
  const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
  return apiBase.replace(/\/api$/, "") + "/api/ws";
};

const WS_URL = getWsUrl();

export function ChatPage() {
  const { isLoggedIn, userId } = useAuth();

  const [rooms, setRooms] = useState<ChatRoomDto[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoomDto | null>(null);
  const [messages, setMessages] = useState<MessageResponseDto[]>([]);
  const [inputText, setInputText] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isLoadingRooms, setIsLoadingRooms] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [nextCursor, setNextCursor] = useState<number | null>(null);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<MessageResponseDto | null>(null);

  const stompRef = useRef<StompClient | null>(null);
  const subscriptionRef = useRef<any>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const isNearBottomRef = useRef(true);

  // 스크롤 제어
  const scrollToBottom = useCallback((smooth = true) => {
    if (scrollContainerRef.current) {
      const { scrollHeight } = scrollContainerRef.current;
      scrollContainerRef.current.scrollTo({
        top: scrollHeight,
        behavior: smooth ? "smooth" : "auto",
      });
    }
  }, []);

  const handleScroll = () => {
    if (!scrollContainerRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = scrollContainerRef.current;
    isNearBottomRef.current = scrollHeight - scrollTop - clientHeight < 150;
  };

  const loadRooms = useCallback(async () => {
    if (!isLoggedIn) return;
    setIsLoadingRooms(true);
    try {
      const res = await chatApi.getMyRooms(0, 50);
      setRooms(res.data.data.content);
    } finally {
      setIsLoadingRooms(false);
    }
  }, [isLoggedIn]);

  useEffect(() => { loadRooms(); }, [loadRooms]);

  // STOMP 구독 (수정/삭제 실시간 반영 로직 대폭 강화)
  const subscribeToRoom = useCallback((room: ChatRoomDto, client?: StompClient) => {
    const c = client || stompRef.current;
    if (!c?.connected) return;
    
    subscriptionRef.current?.unsubscribe();
    subscriptionRef.current = c.subscribe(`/receive/chat/rooms/${room.id}`, (frame) => {
      const msg: MessageResponseDto = JSON.parse(frame.body);
      const type = String(msg.eventType).toUpperCase(); // 안전한 타입 비교

      setMessages((prev) => {
        // 1. 수정(EDIT) 처리
        if (type === "EDIT") {
          return prev.map((m) => m.id === msg.id ? { ...m, content: msg.content } : m);
        }
        // 2. 삭제(DELETE) 처리
        if (type === "DELETE") {
          return prev.filter((m) => m.id !== msg.id);
        }
        // 3. 중복 수신 방지
        if (prev.some(m => m.id === msg.id)) return prev;
        // 4. 일반 메시지 추가
        return [...prev, msg];
      });

      // 스크롤: 새 메시지일 때만 처리
      if (type === "SEND" || type === "ENTER") {
        const isMine = msg.senderId === userId;
        setTimeout(() => {
          if (isMine || isNearBottomRef.current) scrollToBottom(true);
        }, 50);
      }
    });

    c.publish({ destination: `/send/chat/rooms/${room.id}/enter`, body: "" });
  }, [userId, scrollToBottom]);

  const connectStomp = useCallback(() => {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken || stompRef.current?.connected) return;
    setIsConnecting(true);
    const client = new StompClient({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      onConnect: () => {
        setIsConnected(true);
        setIsConnecting(false);
        if (pendingRoomRef.current) {
          subscribeToRoom(pendingRoomRef.current, client);
          pendingRoomRef.current = null;
        }
      },
      onDisconnect: () => { setIsConnected(false); setIsConnecting(false); },
      onStompError: () => {
        setIsConnected(false);
        setIsConnecting(false);
        toast.error("채팅 서버 연결에 실패했습니다.");
      },
      reconnectDelay: 5000,
    });
    client.activate();
    stompRef.current = client;
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

  const handleSelectRoom = useCallback(async (room: ChatRoomDto) => {
    setSelectedRoom(room);
    setMessages([]);
    setEditingId(null);
    setIsLoadingMessages(true);
    try {
      const res = await chatApi.getMessages(room.id);
      const data = res.data.data;
      setMessages([...data.messages].reverse());
      setHasNext(data.hasNext);
      setNextCursor(data.nextCursor);
      setTimeout(() => scrollToBottom(false), 50);
    } catch {
      toast.error("메시지를 불러오는 데 실패했습니다.");
    } finally {
      setIsLoadingMessages(false);
    }
    if (stompRef.current?.connected) {
      subscribeToRoom(room);
    } else {
      pendingRoomRef.current = room;
      if (!isConnecting) connectStomp();
    }
  }, [subscribeToRoom, connectStomp, isConnecting, scrollToBottom]);

  const handleSend = useCallback(() => {
    if (!inputText.trim() || !selectedRoom || !stompRef.current?.connected) return;
    stompRef.current.publish({
      destination: `/send/chat/rooms/${selectedRoom.id}`,
      body: JSON.stringify({ content: inputText.trim() }),
    });
    setInputText("");
    isNearBottomRef.current = true;
  }, [inputText, selectedRoom]);

  const submitEdit = async (msgId: number) => {
    if (!editContent.trim()) return;
    try {
      await chatApi.updateMessage(msgId, editContent);
      setEditingId(null);
      setEditContent("");
      // STOMP 이벤트를 기다리지만, 사용자 경험을 위해 즉시 반영할 수도 있음 (여기선 STOMP 신뢰)
    } catch {
      toast.error("수정에 실패했습니다.");
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await chatApi.deleteMessage(deleteTarget.id);
      setDeleteTarget(null);
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  if (!isLoggedIn) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-64px)] bg-white">
        <Card className="p-8 text-center border-none shadow-none">
          <MessageCircle className="w-12 h-12 mx-auto mb-4 text-blue-500 opacity-20" />
          <p className="mb-4 text-gray-500">로그인이 필요한 서비스입니다.</p>
          <Link to="/login"><Button className="bg-blue-600 rounded-xl px-8">로그인하기</Button></Link>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-white overflow-hidden">
      {/* 상단 헤더 (전체) */}
      <div className="px-4 py-3 border-b flex items-center justify-between shrink-0 bg-white z-20">
        <div className="flex items-center gap-3">
          {/* 모바일에서만 보이는 뒤로가기 (채팅방 선택 시) */}
          {selectedRoom && (
            <Button variant="ghost" size="icon" onClick={() => setSelectedRoom(null)} className="lg:hidden -ml-2 text-gray-500">
              <ChevronLeft className="w-6 h-6" />
            </Button>
          )}
          <h1 className="text-lg font-extrabold tracking-tight text-gray-900">
            {selectedRoom ? "대화 중" : "메시지"}
          </h1>
          <Badge className={`text-[10px] h-5 ${isConnected ? "bg-green-50 text-green-600 border-green-100" : "bg-gray-50 text-gray-400"}`} variant="outline">
            {isConnected ? "● 실시간" : "○ 연결끊김"}
          </Badge>
        </div>
        <div className="flex items-center gap-2">
          {!isConnected && !isConnecting && (
            <Button size="sm" variant="ghost" onClick={connectStomp} className="text-blue-600 h-8 text-xs">재연결</Button>
          )}
          <Button size="icon" variant="ghost" onClick={loadRooms} className="w-8 h-8 text-gray-400">
            <RefreshCw className="w-4 h-4" />
          </Button>
        </div>
      </div>

      <div className="flex-1 flex min-h-0 relative">
        {/* ── 채팅방 목록 ── */}
        <div className={`
          ${selectedRoom ? "hidden lg:flex" : "flex"} 
          w-full lg:w-80 flex-col border-r shrink-0 bg-gray-50/30
        `}>
          <div className="flex-1 overflow-y-auto custom-scrollbar">
            {isLoadingRooms ? (
              <div className="flex justify-center py-12"><Loader2 className="w-6 h-6 animate-spin text-blue-600" /></div>
            ) : !rooms || rooms.length === 0 ? (
              <div className="p-12 text-center text-gray-400 text-sm">참여 중인 대화가 없습니다.</div>
            ) : (
              rooms.map((room) => (
                <div
                  key={room.id}
                  onClick={() => handleSelectRoom(room)}
                  className={`p-4 border-b cursor-pointer transition-all ${
                    selectedRoom?.id === room.id ? "bg-white border-l-4 border-l-blue-600 shadow-sm" : "hover:bg-gray-100/50"
                  }`}
                >
                  <div className="flex justify-between items-start mb-1">
                    <p className="font-bold text-sm truncate text-gray-800 flex-1">{room.projectTitle}</p>
                    {room.unreadCount > 0 && (
                      <Badge className="ml-2 bg-red-500 text-[10px] h-4 min-w-[16px] px-1 justify-center border-none">{room.unreadCount}</Badge>
                    )}
                  </div>
                  <p className="text-[11px] text-gray-500">{room.clientName} & {room.developerName}</p>
                </div>
              ))
            )}
          </div>
        </div>

        {/* ── 채팅창 ── */}
        <div className={`
          ${selectedRoom ? "flex" : "hidden lg:flex"} 
          flex-1 flex-col min-w-0 bg-white
        `}>
          {selectedRoom ? (
            <>
              {/* 채팅방 정보 헤더 (선택된 방 제목) */}
              <div className="px-4 py-2 border-b bg-gray-50/50 shrink-0 hidden lg:block">
                <p className="text-xs font-bold text-gray-400 uppercase tracking-widest">Chatting with</p>
                <p className="text-sm font-black text-gray-700 truncate">{selectedRoom.projectTitle}</p>
              </div>

              {/* 메시지 리스트 */}
              <div 
                ref={scrollContainerRef} 
                onScroll={handleScroll}
                className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar bg-white"
              >
                {hasNext && (
                  <div className="flex justify-center pb-2">
                    <Button variant="ghost" size="sm" onClick={() => {}} className="text-[10px] h-7 text-gray-400 rounded-full hover:bg-gray-100">이전 대화 불러오기</Button>
                  </div>
                )}
                {isLoadingMessages ? (
                  <div className="flex justify-center py-12"><Loader2 className="w-6 h-6 animate-spin text-blue-600" /></div>
                ) : messages.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-gray-300 gap-2">
                    <MessageCircle className="w-10 h-10 opacity-10" />
                    <p className="text-xs font-medium">대화의 시작을 열어보세요!</p>
                  </div>
                ) : (
                  messages.map((msg) => {
                    const isMine = userId !== null && msg.senderId === userId;
                    if (msg.isSystem) {
                      return (
                        <div key={msg.id} className="flex justify-center my-4">
                          <span className="bg-gray-100 text-gray-400 text-[10px] px-4 py-1 rounded-full">{msg.content}</span>
                        </div>
                      );
                    }
                    return (
                      <div key={msg.id} className={`flex ${isMine ? "justify-end" : "justify-start"} group`}>
                        <div className={`flex flex-col ${isMine ? "items-end" : "items-start"} max-w-[85%]`}>
                          {!isMine && <span className="text-[11px] font-bold text-gray-400 ml-1 mb-1">{msg.senderName}</span>}
                          
                          <div className="flex items-end gap-2 group/bubble">
                            {isMine && (
                              <div className="flex flex-col items-end gap-1 mb-0.5">
                                <span className="text-[9px] text-gray-300 font-medium whitespace-nowrap">
                                  {new Date(msg.sentAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                                </span>
                                {editingId !== msg.id && (
                                  <div className="flex gap-1.5 opacity-0 group-hover/bubble:opacity-100 transition-opacity">
                                    <button onClick={() => { setEditingId(msg.id); setEditContent(msg.content); }} className="text-gray-300 hover:text-blue-500"><Pencil className="w-3 h-3" /></button>
                                    <button onClick={() => setDeleteTarget(msg)} className="text-gray-300 hover:text-red-500"><Trash2 className="w-3 h-3" /></button>
                                  </div>
                                )}
                              </div>
                            )}

                            {editingId === msg.id ? (
                              <div className="flex flex-col gap-2 p-3 bg-white border-2 border-blue-500 rounded-2xl shadow-xl w-64 z-10">
                                <textarea 
                                  value={editContent} 
                                  onChange={(e) => setEditContent(e.target.value)} 
                                  className="text-sm w-full outline-none resize-none min-h-[50px]" 
                                  autoFocus 
                                />
                                <div className="flex justify-end gap-2 pt-2 border-t">
                                  <Button size="xs" variant="ghost" onClick={() => setEditingId(null)} className="h-6 text-[10px]">취소</Button>
                                  <Button size="xs" onClick={() => submitEdit(msg.id)} className="h-6 text-[10px] bg-blue-600 px-3">저장</Button>
                                </div>
                              </div>
                            ) : (
                              <div
                                className={`px-4 py-2.5 rounded-2xl text-[14px] leading-relaxed shadow-sm break-all ${
                                  isMine
                                    ? "bg-blue-600 text-white rounded-tr-none"
                                    : "bg-gray-100 text-gray-800 rounded-tl-none"
                                }`}
                              >
                                {msg.content}
                              </div>
                            )}

                            {!isMine && (
                              <span className="text-[9px] text-gray-300 font-medium mb-0.5 whitespace-nowrap">
                                {new Date(msg.sentAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>

              {/* 입력창 */}
              <div className="p-4 border-t bg-white shrink-0">
                {!isConnected ? (
                  <div className="w-full text-center text-xs text-amber-600 bg-amber-50 py-3 rounded-xl border border-amber-100">
                    서버와 연결이 끊겼습니다. <button onClick={connectStomp} className="underline font-bold ml-1">재연결</button>
                  </div>
                ) : (
                  <div className="flex gap-2 items-center bg-gray-100 p-1.5 rounded-2xl border border-transparent focus-within:bg-white focus-within:border-blue-200 transition-all">
                    <Input
                      placeholder="메시지를 입력하세요..."
                      value={inputText}
                      onChange={(e) => setInputText(e.target.value)}
                      className="border-none bg-transparent focus-visible:ring-0 h-10 text-sm flex-1"
                      onKeyDown={(e) => {
                        if (e.key === "Enter" && !e.shiftKey) {
                          e.preventDefault();
                          handleSend();
                        }
                      }}
                    />
                    <Button onClick={handleSend} disabled={!inputText.trim()} size="icon" className="rounded-xl w-10 h-10 bg-blue-600 hover:bg-blue-700 shadow-md shadow-blue-100 shrink-0">
                      <Send className="w-4 h-4 text-white" />
                    </Button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-gray-200 bg-gray-50/20">
              <div className="w-24 h-24 bg-white rounded-[40px] shadow-sm flex items-center justify-center mb-6">
                <MessageCircle className="w-12 h-12 text-gray-100" />
              </div>
              <p className="text-sm font-bold text-gray-400">대화할 채팅방을 선택해 주세요</p>
            </div>
          )}
        </div>
      </div>

      <AlertDialog open={!!deleteTarget} onOpenChange={(o) => !o && setDeleteTarget(null)}>
        <AlertDialogContent className="rounded-3xl max-w-[320px] border-none shadow-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-center font-bold">메시지 삭제</AlertDialogTitle>
            <AlertDialogDescription className="text-center text-xs text-gray-500">이 메시지를 정말 삭제하시겠습니까?</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="flex-col sm:flex-col gap-2 mt-4">
            <AlertDialogAction onClick={handleDelete} className="w-full bg-red-600 hover:bg-red-700 h-12 rounded-2xl font-bold">삭제</AlertDialogAction>
            <AlertDialogCancel className="w-full border-none bg-gray-100 h-12 rounded-2xl font-bold">취소</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
