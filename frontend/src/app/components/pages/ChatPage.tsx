/**
 * ChatPage.tsx - 일반 채팅 & CS 상담 통합 채팅 페이지 (UI 통합 버전)
 */
import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router";
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
  User,
  Clock,
} from "lucide-react";
import { toast } from "sonner";
import { chatApi, csApi } from "../../../api/apiService";
import { apiClient } from "../../../api/client";
import type { ChatRoomDto, MessageResponseDto, CsChatRoomDto, CsMessageDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const getWsUrl = () => {
  const apiBase = import.meta.env.VITE_API_BASE_URL || "/api";
  const base = apiBase.startsWith("http") ? apiBase : window.location.origin + apiBase;
  return base.replace(/\/api$/, "") + "/api/ws";
};

const WS_URL = getWsUrl();

export function ChatPage() {
  const navigate = useNavigate();
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

  const stompRef = useRef<StompClient | null>(null);
  const subscriptionRef = useRef<any>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const isNearBottomRef = useRef(true);
  const pendingRoomIdRef = useRef<{ id: number; isCs: boolean } | null>(null);

  const isCsMode = !!csRoomId;

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

  const subscribeToRoom = useCallback((id: number, isCs: boolean) => {
    const c = stompRef.current;
    if (!c || !c.connected) return;

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

    const enterDest = isCs ? `/send/chat/cs/${id}/enter` : `/send/chat/rooms/${id}/enter`;
    setTimeout(() => {
      if (stompRef.current?.connected) {
        stompRef.current.publish({ destination: enterDest, body: "", headers: headers });
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
        stompRef.current = client;
        setIsConnected(true);
        setIsConnecting(false);
        if (pendingRoomIdRef.current) {
          const { id, isCs } = pendingRoomIdRef.current;
          pendingRoomIdRef.current = null;
          subscribeToRoom(id, isCs);
        }
      },
      onDisconnect: () => { setIsConnected(false); setIsConnecting(false); },
      onStompError: () => { setIsConnected(false); setIsConnecting(false); toast.error("채팅 서버 연결 실패"); },
      reconnectDelay: 5000,
    });
    stompRef.current = client;
    client.activate();
  }, [subscribeToRoom]);

  const disconnectStomp = useCallback(() => {
    subscriptionRef.current?.unsubscribe();
    stompRef.current?.deactivate();
    stompRef.current = null;
    setIsConnected(false);
  }, []);

  useEffect(() => {
    if (isLoggedIn) connectStomp();
    return () => disconnectStomp();
  }, [isLoggedIn, connectStomp, disconnectStomp]);

  const loadInitialData = useCallback(async () => {
    if (!isLoggedIn) return;
    setIsLoadingRooms(true);
    try {
      if (isCsMode) {
        setIsLoadingMessages(true);
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
        const res = await chatApi.getMyRooms(0, 50);
        setRooms(res.data.data.content);
      }
    } catch { toast.error("데이터 로드 실패"); } finally {
      setIsLoadingRooms(false);
      setIsLoadingMessages(false);
    }
  }, [isLoggedIn, isCsMode, csRoomId, mapCsToMsg, subscribeToRoom]);

  useEffect(() => { loadInitialData(); }, [loadInitialData]);

  const handleSelectRoom = async (room: ChatRoomDto) => {
    setSelectedRoom(room);
    setMessages([]);
    setIsLoadingMessages(true);
    try {
      const res = await chatApi.getMessages(room.id);
      setMessages([...res.data.data.messages].reverse());
      setTimeout(() => scrollToBottom(false), 50);
      pendingRoomIdRef.current = { id: room.id, isCs: false };
      if (stompRef.current?.connected) {
        pendingRoomIdRef.current = null;
        subscribeToRoom(room.id, false);
      }
    } catch { toast.error("메시지 로드 실패"); } finally { setIsLoadingMessages(false); }
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
      toast.success("삭제되었습니다.");
      setRooms(prev => prev.filter(r => r.id !== deleteRoomTarget.id));
      if (selectedRoom?.id === deleteRoomTarget.id) setSelectedRoom(null);
      setDeleteRoomTarget(null);
    } catch { toast.error("삭제 실패"); } finally { setIsDeletingRoom(false); }
  };

  const handleUpdateMessage = async (id: number, content: string) => {
    try {
      if (isCsMode) await csApi.updateMessage(id, content);
      else await chatApi.updateMessage(id, content);
      setMessages(prev => prev.map(m => m.id === id ? { ...m, content } : m));
      setEditingId(null);
    } catch { toast.error("수정 실패"); }
  };

  const handleDeleteMessage = async (id: number) => {
    if (!confirm("메시지를 삭제하시겠습니까?")) return;
    try {
      if (isCsMode) await csApi.deleteMessage(id);
      else await chatApi.deleteMessage(id);
      setMessages(prev => prev.filter(m => m.id !== id));
    } catch { toast.error("삭제 실패"); }
  };

  if (!isLoggedIn) return <div className="flex items-center justify-center h-full py-20 text-muted-foreground bg-background">로그인이 필요합니다.</div>;

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-background overflow-hidden">
      <div className="px-6 py-4 border-b border-border flex items-center justify-between shrink-0 bg-card/80 backdrop-blur-md z-20">
        <div className="flex items-center gap-4">
          {(selectedRoom || isCsMode) && (
            <Button variant="ghost" size="icon" onClick={() => isCsMode ? navigate(-1) : setSelectedRoom(null)} className="lg:hidden -ml-2 text-foreground hover:bg-secondary rounded-full">
              <ChevronLeft className="w-6 h-6" />
            </Button>
          )}
          <div className="flex flex-col">
            <h1 className="text-xl font-black text-foreground tracking-tight flex items-center gap-2">
              {isCsMode ? "고객 지원 센터" : selectedRoom ? "프로젝트 상담실" : "나의 메시지"}
              <div className={`w-2 h-2 rounded-full ${isConnected ? "bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)] animate-pulse" : "bg-muted"}`} />
            </h1>
            <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
              {isConnected ? "Real-time Connection Active" : "Connection Terminated"}
            </p>
          </div>
        </div>
        <Button size="icon" variant="ghost" onClick={loadInitialData} className="w-10 h-10 text-muted-foreground hover:text-primary hover:bg-primary/10 rounded-xl">
          <RefreshCw className="w-5 h-5" />
        </Button>
      </div>

      <div className="flex-1 flex min-h-0">
        {!isCsMode && (
          <div className={`${selectedRoom ? "hidden lg:flex" : "flex"} w-full lg:w-96 flex-col border-r border-border bg-card/30`}>
            <div className="flex-1 overflow-y-auto p-3 space-y-2">
              {isLoadingRooms ? <div className="flex justify-center py-20"><Loader2 className="animate-spin text-primary w-8 h-8" /></div> :
               rooms.length === 0 ? <div className="p-10 text-center text-muted-foreground text-sm font-bold">대화가 없습니다.</div> :
               rooms.map(room => (
                <div 
                  key={room.id} 
                  className={`group p-4 rounded-[20px] transition-all cursor-pointer relative border ${selectedRoom?.id === room.id ? "bg-primary text-primary-foreground border-primary shadow-lg shadow-primary/20 scale-[1.02]" : "hover:bg-secondary/50 bg-card/50 border-border"}`}
                  onClick={() => handleSelectRoom(room)}
                >
                  <div className="flex items-start gap-4">
                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center font-black text-lg ${selectedRoom?.id === room.id ? "bg-primary-foreground/20" : "bg-primary/10 text-primary"}`}>
                      {room.projectTitle[0]}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className={`font-black text-sm truncate mb-0.5 ${selectedRoom?.id === room.id ? "text-primary-foreground" : "text-foreground"}`}>{room.projectTitle}</p>
                      <p className={`text-[11px] font-bold opacity-70 ${selectedRoom?.id === room.id ? "text-primary-foreground" : "text-muted-foreground"}`}>{room.clientName} & {room.developerName}</p>
                    </div>
                  </div>
                  <button onClick={(e) => { e.stopPropagation(); setDeleteRoomTarget(room); }} className={`absolute right-3 top-3 p-1.5 rounded-lg opacity-0 group-hover:opacity-100 ${selectedRoom?.id === room.id ? "text-primary-foreground/50 hover:bg-primary-foreground/20" : "text-muted-foreground hover:bg-destructive/10 hover:text-destructive"}`}>
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className={`${(selectedRoom || isCsMode) ? "flex" : "hidden lg:flex"} flex-1 flex-col bg-background relative`}>
          {(selectedRoom || isCsMode) ? (
            <>
              <div className="px-6 py-3 border-b border-border bg-card/20 flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-secondary flex items-center justify-center"><User className="w-4 h-4 text-primary" /></div>
                <div>
                  <p className="text-[10px] font-black text-muted-foreground uppercase tracking-widest leading-none mb-1">Subject</p>
                  <p className="text-sm font-black text-foreground tracking-tight">{isCsMode ? csRoom?.title : selectedRoom?.projectTitle}</p>
                </div>
              </div>

              <div ref={scrollContainerRef} onScroll={handleScroll} className="flex-1 overflow-y-auto p-6 space-y-6 bg-background">
                {isLoadingMessages ? <div className="flex justify-center py-20"><Loader2 className="animate-spin text-primary w-10 h-10" /></div> :
                 messages.length === 0 ? <div className="flex flex-col items-center justify-center h-full opacity-20"><Send className="w-8 h-8 text-primary mb-4" /><p className="font-black text-sm tracking-widest uppercase">Start conversation</p></div> :
                 messages.map(msg => {
                   const isMine = msg.senderId === userId;
                   const isEditing = editingId === msg.id;
                   if (msg.isSystem) return <div key={msg.id} className="flex justify-center"><span className="bg-secondary/50 text-muted-foreground text-[10px] font-black px-6 py-1.5 rounded-full border border-border/50 uppercase">{msg.content}</span></div>;
                   return (
                     <div key={msg.id} className={`flex ${isMine ? "justify-end" : "justify-start"} group animate-in fade-in slide-in-from-bottom-2`}>
                       <div className={`flex flex-col ${isMine ? "items-end" : "items-start"} max-w-[80%]`}>
                         {!isMine && <span className="text-[11px] font-black text-muted-foreground mb-1.5 ml-1 uppercase tracking-widest">{msg.senderName}</span>}
                         <div className="flex items-end gap-2">
                           {isMine && !isEditing && (
                             <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-all scale-75">
                               <button onClick={() => { setEditingId(msg.id); setEditContent(msg.content); }} className="p-2 text-muted-foreground hover:text-primary bg-card rounded-xl border border-border"><Pencil className="w-4 h-4" /></button>
                               <button onClick={() => handleDeleteMessage(msg.id)} className="p-2 text-muted-foreground hover:text-destructive bg-card rounded-xl border border-border"><Trash2 className="w-4 h-4" /></button>
                             </div>
                           )}
                           <div className={`relative px-5 py-3 rounded-[24px] text-[15px] font-medium shadow-md ${isMine ? "bg-primary text-primary-foreground rounded-tr-none" : "bg-card text-foreground border border-border rounded-tl-none"}`}>
                             {isEditing ? (
                               <div className="flex flex-col gap-3 min-w-[240px]">
                                 <textarea value={editContent} onChange={e => setEditContent(e.target.value)} className="bg-primary-foreground/10 border-none text-primary-foreground p-3 rounded-xl resize-none text-sm w-full font-bold" rows={3} autoFocus />
                                 <div className="flex justify-end gap-2">
                                   <button onClick={() => setEditingId(null)} className="text-[10px] font-black uppercase text-primary-foreground/60 hover:text-primary-foreground">Cancel</button>
                                   <button onClick={() => handleUpdateMessage(msg.id, editContent)} className="text-[10px] font-black uppercase text-primary bg-primary-foreground px-4 py-1.5 rounded-lg shadow-xl">Save</button>
                                 </div>
                               </div>
                             ) : <p className="whitespace-pre-wrap break-all leading-relaxed">{msg.content}</p>}
                           </div>
                         </div>
                         <div className="flex items-center gap-1.5 mt-1.5 px-1">
                           <span className="text-[9px] font-black text-muted-foreground/40 uppercase">{new Date(msg.sentAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit', hour12: false})}</span>
                           {msg.eventType === "EDIT" && <Badge variant="secondary" className="text-[8px] h-3.5 px-1 bg-primary/5 text-primary/50 border-none font-black">EDITED</Badge>}
                         </div>
                       </div>
                     </div>
                   );
                 })}
              </div>

              <div className="p-6 pt-2 border-t border-border bg-card/10">
                <div className="flex gap-3 items-center bg-card p-2 rounded-[28px] border border-border shadow-xl focus-within:border-primary/50 transition-all">
                  <Input placeholder="Type your message..." value={inputText} onChange={e => setInputText(e.target.value)} className="border-none bg-transparent focus-visible:ring-0 h-12 flex-1 text-foreground font-bold px-6 text-lg" onKeyDown={e => e.key === "Enter" && !e.shiftKey && (e.preventDefault(), handleSend())} />
                  <Button onClick={handleSend} disabled={!inputText.trim() || !isConnected} size="icon" className="rounded-full w-12 h-12 bg-primary hover:bg-primary/90 shadow-lg"><Send className="w-5 h-5 text-primary-foreground" /></Button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center p-10">
              <div className="w-32 h-32 bg-secondary/30 rounded-[40px] flex items-center justify-center mb-6 relative"><div className="absolute inset-0 bg-primary/10 rounded-[40px] animate-ping" /><MessageCircle className="w-16 h-16 text-primary opacity-20" /></div>
              <h3 className="text-2xl font-black text-foreground mb-2">대화방 선택</h3>
              <p className="text-muted-foreground font-bold text-sm tracking-widest uppercase opacity-40">Select a conversation to begin</p>
            </div>
          )}
        </div>
      </div>

      <AlertDialog open={!!deleteRoomTarget} onOpenChange={(o) => !o && setDeleteRoomTarget(null)}>
        <AlertDialogContent className="bg-card border-border rounded-[32px] shadow-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-2xl font-black text-foreground">채팅방을 나갈까요?</AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground font-medium pt-2">대화 기록이 모두 삭제되며 복구할 수 없습니다.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="gap-3 mt-6">
            <AlertDialogCancel className="h-12 border-border font-bold rounded-xl px-6">유지</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteRoom} disabled={isDeletingRoom} className="h-12 bg-destructive text-white font-black rounded-xl px-10 shadow-lg">삭제</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
