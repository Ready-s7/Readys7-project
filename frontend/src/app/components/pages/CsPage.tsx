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
      <div className="flex justify-center py-32 bg-background min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-3xl">
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="text-foreground">
              <ArrowLeft className="w-5 h-5" />
            </Button>
            <div>
              <h1 className="text-3xl font-bold text-foreground">고객센터</h1>
              <p className="text-muted-foreground text-sm mt-1">관리자와의 1:1 상담 내역입니다.</p>
            </div>
          </div>

          <Dialog open={showCsModal} onOpenChange={setShowCsModal}>
            <DialogTrigger asChild>
              <Button className="bg-primary hover:bg-primary/90 text-primary-foreground shadow-lg font-bold">
                <Plus className="w-4 h-4 mr-2" />
                새 문의하기
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md bg-card border-border text-foreground">
              <DialogHeader>
                <DialogTitle className="text-xl font-bold">문의 내용 작성</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleCreateCs} className="space-y-5 mt-4">
                <div className="space-y-2">
                  <Label className="text-sm font-semibold text-muted-foreground">문의 제목</Label>
                  <Input 
                    value={csTitle} 
                    onChange={(e) => setCsTitle(e.target.value)} 
                    placeholder="상담받으실 내용을 간단히 요약해주세요."
                    className="h-11 bg-secondary/30 border-border text-foreground"
                    required 
                  />
                </div>
                <div className="bg-secondary/50 p-4 rounded-lg border border-border">
                  <p className="text-xs text-primary leading-relaxed">
                    • 문의 접수 후 관리자가 확인하여 상담을 시작합니다.<br/>
                    • 답변 알림은 별도로 제공되지 않으니 이 페이지에서 확인 부탁드립니다.
                  </p>
                </div>
                <div className="flex gap-3 pt-2">
                  <Button type="submit" className="flex-1 h-11 bg-primary hover:bg-primary/90 text-primary-foreground font-bold" disabled={isCreatingCs}>
                    {isCreatingCs ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null} 문의 접수하기
                  </Button>
                  <Button type="button" variant="outline" className="h-11 border-border text-foreground hover:bg-secondary" onClick={() => setShowCsModal(false)}>취소</Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        <div className="space-y-4">
          <div className="flex items-center gap-2 mb-2 px-1">
            <MessageSquare className="w-5 h-5 text-primary" />
            <h2 className="text-xl font-black text-foreground">상담 내역 <span className="text-primary/50 text-sm ml-1 font-bold">({csRooms.length})</span></h2>
          </div>
          
          {csRooms.length === 0 ? (
            <Card className="border-border bg-card shadow-sm rounded-3xl border-dashed">
              <CardContent className="p-20 text-center text-muted-foreground">
                <div className="bg-secondary/30 w-20 h-20 rounded-[24px] flex items-center justify-center mx-auto mb-6 border border-border">
                  <MessageSquare className="w-10 h-10 opacity-20 text-primary" />
                </div>
                <p className="text-xl font-black text-foreground mb-2">접수된 문의가 없습니다.</p>
                <p className="text-sm font-medium mb-8">서비스 이용 중 궁금하신 점은 언제든 물어보세요.</p>
                <Button onClick={() => setShowCsModal(true)} className="bg-primary hover:bg-primary/90 text-primary-foreground font-black px-8 rounded-xl shadow-lg shadow-primary/20 h-12">
                  <Plus className="w-5 h-5 mr-2" /> 지금 첫 문의하기
                </Button>
              </CardContent>
            </Card>
          ) : (
            <div className="grid gap-4">
              {csRooms.map((room) => (
                <Card 
                  key={room.id} 
                  className="border-border bg-card shadow-sm hover:shadow-md hover:border-primary/30 transition-all cursor-pointer group rounded-2xl overflow-hidden"
                  onClick={() => navigate(`/chat?csRoomId=${room.id}`)}
                >
                  <CardContent className="p-6">
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-3 mb-3">
                          <Badge className={`px-2.5 py-0.5 text-[10px] font-black uppercase tracking-widest border-none ${
                            room.status === "WAITING" ? "bg-destructive text-white" :
                            room.status === "IN_PROGRESS" ? "bg-primary text-white shadow-sm shadow-primary/20" : 
                            "bg-secondary text-muted-foreground"
                          }`}>
                            {room.status === "WAITING" ? "대기중" :
                             room.status === "IN_PROGRESS" ? "상담중" : "상담종료"}
                          </Badge>
                          <span className="text-[11px] font-bold text-muted-foreground bg-secondary/50 px-2 py-0.5 rounded-md">
                            No. {room.id.toString().padStart(4, '0')}
                          </span>
                        </div>
                        <h4 className="font-black text-xl text-foreground truncate group-hover:text-primary transition-colors mb-1">
                          {room.title}
                        </h4>
                        <p className="text-xs text-muted-foreground font-medium">
                          {new Date(room.createdAt).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })} 접수됨
                        </p>
                      </div>
                      <div className="shrink-0 flex items-center gap-3">
                        <div className="w-px h-10 bg-border hidden md:block" />
                        <Button variant="ghost" className="text-primary font-black text-sm gap-2 hover:bg-primary/5 rounded-xl px-4">
                          상담실 입장
                          <ExternalLink className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
