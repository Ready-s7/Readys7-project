import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Badge } from "../ui/badge";
import { 
  Loader2, MessageSquare, Plus, ExternalLink, ArrowLeft 
} from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../../../context/AuthContext";
import { csApi } from "../../../api/apiService";
import type { CsChatRoomDto } from "../../../api/types";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "../ui/dialog";

export function CsPage() {
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();

  const [csRooms, setCsRooms] = useState<CsChatRoomDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showCsModal, setShowCsModal] = useState(false);
  const [csTitle, setCsTitle] = useState("");
  const [isCreatingCs, setIsCreatingCs] = useState(false);

  useEffect(() => {
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    fetchCsRooms();
  }, [isLoggedIn]);

  const fetchCsRooms = async () => {
    setIsLoading(true);
    try {
      const res = await csApi.getMyRooms(1, 20);
      setCsRooms(res.data.data.content || []);
    } catch (e) {
      console.error("CS 내역 로드 실패:", e);
      toast.error("문의 내역을 불러오는데 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateCs = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!csTitle.trim()) { toast.error("문의 제목을 입력해주세요."); return; }
    setIsCreatingCs(true);
    try {
      await csApi.createRoom({ title: csTitle });
      toast.success("문의가 정상적으로 접수되었습니다.");
      setShowCsModal(false);
      setCsTitle("");
      fetchCsRooms();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "문의 접수에 실패했습니다.");
    } finally {
      setIsCreatingCs(false);
    }
  };

  if (isLoading && csRooms.length === 0) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-3xl">
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
              <ArrowLeft className="w-5 h-5" />
            </Button>
            <div>
              <h1 className="text-3xl font-bold">고객센터</h1>
              <p className="text-gray-500 text-sm mt-1">관리자와의 1:1 상담 내역입니다.</p>
            </div>
          </div>

          <Dialog open={showCsModal} onOpenChange={setShowCsModal}>
            <DialogTrigger asChild>
              <Button className="bg-blue-600 hover:bg-blue-700 shadow-lg">
                <Plus className="w-4 h-4 mr-2" />
                새 문의하기
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle className="text-xl font-bold">문의 내용 작성</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleCreateCs} className="space-y-5 mt-4">
                <div className="space-y-2">
                  <Label className="text-sm font-semibold text-gray-700">문의 제목</Label>
                  <Input 
                    value={csTitle} 
                    onChange={(e) => setCsTitle(e.target.value)} 
                    placeholder="상담받으실 내용을 간단히 요약해주세요."
                    className="h-11"
                    required 
                  />
                </div>
                <div className="bg-blue-50 p-4 rounded-lg">
                  <p className="text-xs text-blue-700 leading-relaxed">
                    • 문의 접수 후 관리자가 확인하여 상담을 시작합니다.<br/>
                    • 답변 알림은 별도로 제공되지 않으니 이 페이지에서 확인 부탁드립니다.
                  </p>
                </div>
                <div className="flex gap-3 pt-2">
                  <Button type="submit" className="flex-1 h-11 bg-blue-600 hover:bg-blue-700" disabled={isCreatingCs}>
                    {isCreatingCs ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null} 문의 접수하기
                  </Button>
                  <Button type="button" variant="outline" className="h-11" onClick={() => setShowCsModal(false)}>취소</Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        <Card className="border-none shadow-xl">
          <CardHeader className="border-b bg-white rounded-t-xl">
            <CardTitle className="flex items-center gap-2 text-lg">
              <MessageSquare className="w-5 h-5 text-blue-600" />
              내 문의 내역 ({csRooms.length})
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {csRooms.length === 0 ? (
              <div className="text-center py-20 text-gray-500">
                <div className="bg-gray-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                  <MessageSquare className="w-8 h-8 opacity-20" />
                </div>
                <p className="text-lg font-medium">아직 문의하신 내역이 없습니다.</p>
                <p className="text-sm mt-1">궁금하신 점이 있다면 새 문의를 남겨주세요.</p>
                <Button variant="outline" onClick={() => setShowCsModal(true)} className="mt-6">
                  지금 문의하기
                </Button>
              </div>
            ) : (
              <div className="divide-y">
                {csRooms.map((room) => (
                  <div key={room.id} className="flex items-center justify-between p-6 hover:bg-gray-50 transition-colors cursor-pointer" onClick={() => navigate(`/chat?csRoomId=${room.id}`)}>
                    <div className="flex-1 min-w-0 pr-6">
                      <div className="flex items-center gap-3 mb-2">
                        <Badge variant={
                          room.status === "WAITING" ? "destructive" :
                          room.status === "IN_PROGRESS" ? "default" : "secondary"
                        } className="px-2 py-0.5 text-[11px] font-bold uppercase tracking-wider">
                          {room.status === "WAITING" ? "대기중" :
                           room.status === "IN_PROGRESS" ? "처리중" : "완료"}
                        </Badge>
                        <span className="text-xs text-gray-400">
                          {new Date(room.createdAt).toLocaleString()} 접수
                        </span>
                      </div>
                      <h4 className="font-bold text-lg text-gray-900 truncate group-hover:text-blue-600 transition-colors">
                        {room.title}
                      </h4>
                    </div>
                    <div className="shrink-0 flex items-center gap-2 text-blue-600 font-semibold text-sm">
                      상담하기
                      <ExternalLink className="w-4 h-4" />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
