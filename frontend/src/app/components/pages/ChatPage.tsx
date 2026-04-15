import { useState, useEffect, useRef, useCallback } from "react";
import { Link } from "react-router";
import { Client as StompClient } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import {
  Send,
  Loader2,
  ArrowLeft,
  MessageCircle,
  RefreshCw,
} from "lucide-react";
import { toast } from "sonner";
import { chatApi } from "../../../api/apiService";
import type { ChatRoomDto, MessageResponseDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

// WebSocket URL 계산
const getWsUrl = () => {
  const apiBase =
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
  const wsBase = apiBase.replace(/\/api$/, "");
  return `${wsBase}/api/ws`;
};

const WS_URL = getWsUrl();

export function ChatPage() {
  const { isLoggedIn, userRole, userId } = useAuth(); // userId 추가

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

  const stompRef = useRef<StompClient | null>(null);
  const subscriptionRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const pendingRoomRef = useRef<ChatRoomDto | null>(null);

  // 채팅방 목록 로드
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

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  // STOMP 연결
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
      onDisconnect: () => {
        setIsConnected(false);
        setIsConnecting(false);
      },
      onStompError: (frame) => {
        console.error("STOMP error:", frame);
        setIsConnected(false);
        setIsConnecting(false);
        toast.error("채팅 서버 연결에 실패했습니다.");
      },
      reconnectDelay: 5000,
    });

    client.activate();
    stompRef.current = client;
  }, []);

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
  }, [isLoggedIn]);

  // 채팅방 STOMP 구독 함수
  const subscribeToRoom = useCallback(
    (room: ChatRoomDto, client?: StompClient) => {
      const c = client || stompRef.current;
      if (!c?.connected) return;

      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = c.subscribe(
        `/receive/chat/rooms/${room.id}`,
        (frame) => {
          try {
            const msg: MessageResponseDto = JSON.parse(frame.body);
            setMessages((prev) => [...prev, msg]);
          } catch (e) {
            console.error("메시지 파싱 오류:", e);
          }
        }
      );

      // 입장 알림 전송
      c.publish({
        destination: `/send/chat/rooms/${room.id}/enter`,
        body: "",
      });
    },
    []
  );

  // 채팅방 선택
  const handleSelectRoom = useCallback(
    async (room: ChatRoomDto) => {
      setSelectedRoom(room);
      setMessages([]);
      setNextCursor(null);

      setIsLoadingMessages(true);
      try {
        const res = await chatApi.getMessages(room.id);
        const data = res.data.data;
        setMessages([...data.messages].reverse());
        setHasNext(data.hasNext);
        setNextCursor(data.nextCursor);
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
    },
    [subscribeToRoom, connectStomp, isConnecting]
  );

  // 메시지 전송
  const handleSend = useCallback(() => {
    if (
      !inputText.trim() ||
      !selectedRoom ||
      !stompRef.current?.connected
    )
      return;

    stompRef.current.publish({
      destination: `/send/chat/rooms/${selectedRoom.id}`,
      body: JSON.stringify({ content: inputText.trim() }),
    });
    setInputText("");
  }, [inputText, selectedRoom]);

  // 이전 메시지 더보기
  const loadMoreMessages = async () => {
    if (!selectedRoom || !hasNext || !nextCursor) return;
    try {
      const res = await chatApi.getMessages(selectedRoom.id, nextCursor);
      const data = res.data.data;
      setMessages((prev) => [...[...data.messages].reverse(), ...prev]);
      setHasNext(data.hasNext);
      setNextCursor(data.nextCursor);
    } catch {
      toast.error("이전 메시지를 불러오는 데 실패했습니다.");
    }
  };

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
            <Link to="/login">
              <Button>로그인하기</Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="flex items-center gap-4 mb-6">
          <Link to="/">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="w-4 h-4 mr-1" />홈
            </Button>
          </Link>
          <h1 className="text-2xl">채팅</h1>
          <Badge variant={isConnected ? "default" : "secondary"}>
            {isConnecting
              ? "연결 중..."
              : isConnected
              ? "● 연결됨"
              : "○ 연결 끊김"}
          </Badge>
          {!isConnected && !isConnecting && (
            <Button size="sm" variant="outline" onClick={connectStomp}>
              <RefreshCw className="w-4 h-4 mr-1" />
              재연결
            </Button>
          )}
          <Button
            size="sm"
            variant="ghost"
            onClick={loadRooms}
            title="채팅방 목록 새로고침"
          >
            <RefreshCw className="w-4 h-4" />
          </Button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[600px]">
          {/* 채팅방 목록 */}
          <Card className="overflow-hidden flex flex-col">
            <CardHeader className="p-4 border-b shrink-0">
              <CardTitle className="text-base">
                채팅방 ({rooms.length})
              </CardTitle>
            </CardHeader>
            <div className="overflow-y-auto flex-1">
              {isLoadingRooms ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
                </div>
              ) : rooms.length === 0 ? (
                <div className="p-6 text-center text-gray-500 text-sm">
                  <MessageCircle className="w-8 h-8 mx-auto mb-2 opacity-40" />
                  <p className="mb-2">채팅방이 없습니다.</p>
                  {userRole === "CLIENT" ? (
                    <p className="text-xs">
                      프로젝트 상세 페이지에서<br />
                      수락된 제안서의 개발자와<br />
                      채팅방을 생성할 수 있습니다.
                    </p>
                  ) : (
                    <p className="text-xs">
                      제안서가 수락된 후<br />
                      클라이언트가 채팅방을 생성합니다.
                    </p>
                  )}
                  {userRole === "CLIENT" && (
                    <Link to="/projects">
                      <Button size="sm" variant="outline" className="mt-3">
                        프로젝트 보기
                      </Button>
                    </Link>
                  )}
                </div>
              ) : (
                rooms.map((room) => (
                  <div
                    key={room.id}
                    onClick={() => handleSelectRoom(room)}
                    className={`p-4 border-b cursor-pointer hover:bg-gray-50 transition-colors ${
                      selectedRoom?.id === room.id
                        ? "bg-blue-50 border-l-2 border-l-blue-600"
                        : ""
                    }`}
                  >
                    <p className="font-medium text-sm truncate">
                      {room.projectTitle}
                    </p>
                    <p className="text-xs text-gray-500 mt-1">
                      {room.clientName} ↔ {room.developerName}
                    </p>
                    {room.unreadCount > 0 && (
                      <Badge className="mt-1 text-xs">
                        {room.unreadCount}
                      </Badge>
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
                <CardHeader className="p-4 border-b shrink-0">
                  <CardTitle className="text-base">
                    {selectedRoom.projectTitle}
                  </CardTitle>
                  <p className="text-xs text-gray-500">
                    {selectedRoom.clientName} ↔ {selectedRoom.developerName}
                  </p>
                </CardHeader>

                {/* 메시지 목록 */}
                <div className="flex-1 overflow-y-auto p-4 space-y-3">
                  {hasNext && (
                    <div className="text-center">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={loadMoreMessages}
                      >
                        이전 메시지 불러오기
                      </Button>
                    </div>
                  )}
                  {isLoadingMessages ? (
                    <div className="flex justify-center py-8">
                      <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
                    </div>
                  ) : messages.length === 0 ? (
                    <div className="flex items-center justify-center h-full text-gray-400 text-sm">
                      아직 메시지가 없습니다. 첫 메시지를 보내보세요!
                    </div>
                  ) : (
                    messages.map((msg) => {
                      // ★ AuthContext에서 가져온 userId와 직접 비교
                      const isMine = userId !== null && msg.senderId === userId;

                      if (msg.isSystem) {
                        return (
                          <div
                            key={msg.id}
                            className="text-center text-xs text-gray-400 py-1"
                          >
                            {msg.content}
                          </div>
                        );
                      }
                      return (
                        <div
                          key={msg.id}
                          className={`flex ${
                            isMine ? "justify-end" : "justify-start"
                          }`}
                        >
                          <div
                            className={`max-w-[70%] px-3 py-2 rounded-2xl text-sm ${
                              isMine
                                ? "bg-blue-600 text-white rounded-br-none"
                                : "bg-gray-200 text-gray-800 rounded-bl-none"
                            }`}
                          >
                            {!isMine && (
                              <p className="text-xs font-medium mb-1 opacity-70">
                                {msg.senderName}
                              </p>
                            )}
                            <p>{msg.content}</p>
                            <p
                              className={`text-xs mt-1 ${
                                isMine ? "opacity-70" : "text-gray-500"
                              }`}
                            >
                              {new Date(msg.sentAt).toLocaleTimeString(
                                "ko-KR",
                                { hour: "2-digit", minute: "2-digit" }
                              )}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* 입력창 */}
                <div className="p-4 border-t shrink-0 flex gap-2">
                  {!isConnected && (
                    <div className="w-full text-center text-sm text-amber-600 bg-amber-50 py-2 rounded mb-2">
                      서버 연결이 끊겼습니다.{" "}
                      <button
                        onClick={connectStomp}
                        className="underline font-medium"
                      >
                        재연결
                      </button>
                    </div>
                  )}
                  {isConnected && (
                    <>
                      <Input
                        placeholder="메시지를 입력하세요..."
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" && !e.shiftKey) {
                            e.preventDefault();
                            handleSend();
                          }
                        }}
                      />
                      <Button
                        onClick={handleSend}
                        disabled={!inputText.trim()}
                      >
                        <Send className="w-4 h-4" />
                      </Button>
                    </>
                  )}
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
