/**
 * ChatPage.tsx
 * - 내 채팅방 목록 조회
 * - STOMP WebSocket으로 실시간 메시지 송수신
 * - 커서 기반 이전 메시지 페이징
 *
 * 의존성 설치 필요:
 *   npm install @stomp/stompjs sockjs-client
 *   npm install -D @types/sockjs-client
 */
import { useState, useEffect, useRef, useCallback } from "react";
import { Link } from "react-router";
import { Client as StompClient } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import { Send, Loader2, ArrowLeft, MessageCircle } from "lucide-react";
import { toast } from "sonner";
import { chatApi } from "../../../api/apiService";
import type { ChatRoomDto, MessageResponseDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const WS_URL = API_BASE.replace("/api", "") + "/api/ws";

export function ChatPage() {
  const { isLoggedIn, userEmail } = useAuth();

  const [rooms, setRooms] = useState<ChatRoomDto[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoomDto | null>(null);
  const [messages, setMessages] = useState<MessageResponseDto[]>([]);
  const [inputText, setInputText] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [isLoadingRooms, setIsLoadingRooms] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [nextCursor, setNextCursor] = useState<number | null>(null);

  const stompRef = useRef<StompClient | null>(null);
  const subscriptionRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // ── 채팅방 목록 로드 ──────────────────────────────────────
  useEffect(() => {
    if (!isLoggedIn) return;
    chatApi.getMyRooms().then((res) => {
      setRooms(res.data.data.content);
    }).finally(() => setIsLoadingRooms(false));
  }, [isLoggedIn]);

  // ── STOMP 연결 ──────────────────────────────────────────
  const connectStomp = useCallback(() => {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) return;

    const client = new StompClient({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      onConnect: () => setIsConnected(true),
      onDisconnect: () => setIsConnected(false),
      reconnectDelay: 3000,
    });
    client.activate();
    stompRef.current = client;
  }, []);

  const disconnectStomp = useCallback(() => {
    subscriptionRef.current?.unsubscribe();
    stompRef.current?.deactivate();
    stompRef.current = null;
    setIsConnected(false);
  }, []);

  useEffect(() => {
    if (isLoggedIn) connectStomp();
    return () => disconnectStomp();
  }, [isLoggedIn]);

  // ── 채팅방 선택 ──────────────────────────────────────────
  const handleSelectRoom = useCallback(async (room: ChatRoomDto) => {
    // 이전 방 구독 해제
    subscriptionRef.current?.unsubscribe();
    setSelectedRoom(room);
    setMessages([]);
    setNextCursor(null);

    // 이전 메시지 로드
    setIsLoadingMessages(true);
    try {
      const res = await chatApi.getMessages(room.id);
      const data = res.data.data;
      // 최신→오래된 순으로 오므로 역순 정렬
      setMessages([...data.messages].reverse());
      setHasNext(data.hasNext);
      setNextCursor(data.nextCursor);
    } finally {
      setIsLoadingMessages(false);
    }

    // STOMP 구독 시작
    if (stompRef.current?.connected) {
      subscriptionRef.current = stompRef.current.subscribe(
        `/receive/chat/rooms/${room.id}`,
        (frame) => {
          const msg: MessageResponseDto = JSON.parse(frame.body);
          setMessages((prev) => [...prev, msg]);
        }
      );

      // 입장 메시지 전송
      stompRef.current.publish({
        destination: `/send/chat/rooms/${room.id}/enter`,
        body: "",
      });
    }
  }, []);

  // ── 메시지 전송 ──────────────────────────────────────────
  const handleSend = useCallback(() => {
    if (!inputText.trim() || !selectedRoom || !stompRef.current?.connected) return;

    stompRef.current.publish({
      destination: `/send/chat/rooms/${selectedRoom.id}`,
      body: JSON.stringify({ content: inputText.trim() }),
    });
    setInputText("");
  }, [inputText, selectedRoom]);

  // ── 이전 메시지 더보기 ────────────────────────────────────
  const loadMoreMessages = async () => {
    if (!selectedRoom || !hasNext || !nextCursor) return;
    const res = await chatApi.getMessages(selectedRoom.id, nextCursor);
    const data = res.data.data;
    setMessages((prev) => [...[...data.messages].reverse(), ...prev]);
    setHasNext(data.hasNext);
    setNextCursor(data.nextCursor);
  };

  // 새 메시지 도착 시 스크롤
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card>
          <CardContent className="p-8 text-center">
            <MessageCircle className="w-12 h-12 mx-auto mb-4 text-gray-400" />
            <p className="mb-4">채팅을 이용하려면 로그인이 필요합니다.</p>
            <Link to="/login"><Button>로그인하기</Button></Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="flex items-center gap-4 mb-6">
          <Link to="/"><Button variant="ghost" size="sm"><ArrowLeft className="w-4 h-4 mr-1" />홈</Button></Link>
          <h1 className="text-2xl">채팅</h1>
          <Badge variant={isConnected ? "default" : "secondary"}>
            {isConnected ? "● 연결됨" : "○ 연결 중..."}
          </Badge>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[600px]">
          {/* 채팅방 목록 */}
          <Card className="overflow-hidden">
            <CardHeader className="p-4 border-b">
              <CardTitle className="text-base">채팅방</CardTitle>
            </CardHeader>
            <div className="overflow-y-auto h-full">
              {isLoadingRooms ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
                </div>
              ) : rooms.length === 0 ? (
                <div className="p-6 text-center text-gray-500 text-sm">
                  채팅방이 없습니다.<br />
                  <span className="text-xs">승인된 제안서의 프로젝트에서<br />채팅방을 생성할 수 있습니다.</span>
                </div>
              ) : (
                rooms.map((room) => (
                  <div
                    key={room.id}
                    onClick={() => handleSelectRoom(room)}
                    className={`p-4 border-b cursor-pointer hover:bg-gray-50 transition-colors ${
                      selectedRoom?.id === room.id ? "bg-blue-50 border-l-2 border-l-blue-600" : ""
                    }`}
                  >
                    <p className="font-medium text-sm truncate">{room.projectTitle}</p>
                    <p className="text-xs text-gray-500 mt-1">
                      {room.clientName} ↔ {room.developerName}
                    </p>
                    {room.unreadCount > 0 && (
                      <Badge className="mt-1 text-xs">{room.unreadCount}</Badge>
                    )}
                  </div>
                ))
              )}
            </div>
          </Card>

          {/* 채팅창 */}
          <Card className="lg:col-span-2 flex flex-col overflow-hidden">
            {selectedRoom ? (
              <>
                <CardHeader className="p-4 border-b">
                  <CardTitle className="text-base">{selectedRoom.projectTitle}</CardTitle>
                  <p className="text-xs text-gray-500">
                    {selectedRoom.clientName} ↔ {selectedRoom.developerName}
                  </p>
                </CardHeader>

                {/* 메시지 목록 */}
                <div className="flex-1 overflow-y-auto p-4 space-y-3">
                  {hasNext && (
                    <div className="text-center">
                      <Button variant="ghost" size="sm" onClick={loadMoreMessages}>
                        이전 메시지 불러오기
                      </Button>
                    </div>
                  )}
                  {isLoadingMessages ? (
                    <div className="flex justify-center py-8">
                      <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isMine = msg.senderName === (userEmail?.split("@")[0] ?? "");
                      if (msg.isSystem) {
                        return (
                          <div key={msg.id} className="text-center text-xs text-gray-400 py-1">
                            {msg.content}
                          </div>
                        );
                      }
                      return (
                        <div
                          key={msg.id}
                          className={`flex ${isMine ? "justify-end" : "justify-start"}`}
                        >
                          <div
                            className={`max-w-[70%] px-3 py-2 rounded-2xl text-sm ${
                              isMine
                                ? "bg-blue-600 text-white rounded-br-none"
                                : "bg-gray-200 text-gray-800 rounded-bl-none"
                            }`}
                          >
                            {!isMine && (
                              <p className="text-xs font-medium mb-1 opacity-70">{msg.senderName}</p>
                            )}
                            <p>{msg.content}</p>
                            <p className={`text-xs mt-1 ${isMine ? "opacity-70" : "text-gray-500"}`}>
                              {new Date(msg.sentAt).toLocaleTimeString("ko-KR", {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* 입력창 */}
                <div className="p-4 border-t flex gap-2">
                  <Input
                    placeholder="메시지를 입력하세요..."
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
                    disabled={!isConnected}
                  />
                  <Button onClick={handleSend} disabled={!isConnected || !inputText.trim()}>
                    <Send className="w-4 h-4" />
                  </Button>
                </div>
              </>
            ) : (
              <div className="flex-1 flex items-center justify-center text-gray-400">
                <div className="text-center">
                  <MessageCircle className="w-12 h-12 mx-auto mb-3 opacity-40" />
                  <p>채팅방을 선택하세요</p>
                </div>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
