import { useState, useEffect } from "react";
import { useParams, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog";
import { Label } from "../ui/label";
import { Textarea } from "../ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { 
  AlertDialog, 
  AlertDialogAction, 
  AlertDialogCancel, 
  AlertDialogContent, 
  AlertDialogDescription, 
  AlertDialogFooter, 
  AlertDialogHeader, 
  AlertDialogTitle 
} from "../ui/alert-dialog";
import { Star, Briefcase, MessageSquare, Loader2, Calendar, PlusCircle, Pencil, Trash2 } from "lucide-react";
import { clientApi, reviewApi, developerApi } from "../../../api/apiService";
import type { ClientDto, ProjectDto, ReviewDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";
import { toast } from "sonner";

export function ClientDetail() {
  const { id } = useParams();
  const { isLoggedIn, userRole, userId } = useAuth();
  
  const [client, setClient] = useState<ClientDto | null>(null);
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [reviews, setReviews] = useState<ReviewDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 리뷰 작성 관련 상태
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [reviewForm, setReviewForm] = useState({
    projectId: "",
    rating: 5,
    comment: ""
  });

  // 리뷰 수정 관련 상태
  const [editReview, setEditReview] = useState<ReviewDto | null>(null);
  const [editForm, setEditForm] = useState({ rating: 5, comment: "" });

  // 리뷰 삭제 관련 상태
  const [deleteReview, setDeleteReview] = useState<ReviewDto | null>(null);

  useEffect(() => {
    if (id) {
      loadData();
    }
  }, [id]);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [clientRes, reviewRes] = await Promise.allSettled([
        clientApi.getById(Number(id)),
        reviewApi.getByClient(Number(id), { page: 1, size: 20 })
      ]);

      if (clientRes.status === "fulfilled") {
        const clientData = clientRes.value.data.data;
        setClient(clientData);
        
        // 개발자라면 이 클라이언트와 관련된 프로젝트 목록을 가져옴 (리뷰 작성용)
        if (userRole === "DEVELOPER") {
          const projectRes = await developerApi.getMyProjects(0, 100);
          // 이 클라이언트의 프로젝트이면서, 상태가 COMPLETED인 프로젝트만 필터링
          const myProjectsWithThisClient = projectRes.data.data.content.filter(
            p => Number(p.clientId) === Number(clientData.id) && p.status === "COMPLETED"
          );
          setProjects(myProjectsWithThisClient);
        }
      }
      if (reviewRes.status === "fulfilled") {
        setReviews(reviewRes.value.data.data.content);
      }
    } catch (e) {
      console.error(e);
      toast.error("데이터를 불러오는데 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleReviewSubmit = async () => {
    if (!reviewForm.projectId) return toast.error("프로젝트를 선택해주세요.");
    if (!reviewForm.comment.trim()) return toast.error("내용을 입력해주세요.");
    if (!client) return;

    setIsSubmitting(true);
    try {
      await reviewApi.create(client.userId, {
        projectId: Number(reviewForm.projectId),
        rating: reviewForm.rating,
        comment: reviewForm.comment
      });
      toast.success("리뷰가 등록되었습니다.");
      setIsReviewModalOpen(false);
      setReviewForm({ projectId: "", rating: 5, comment: "" });
      // 리뷰 목록 및 클라이언트 정보 새로고침
      refreshData();
    } catch (e: any) {
      toast.error(e.response?.data?.message || "리뷰 등록에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEditReview = async () => {
    if (!editReview) return;
    try {
      await reviewApi.update(editReview.id, {
        rating: editForm.rating,
        comment: editForm.comment
      });
      toast.success("리뷰가 수정되었습니다.");
      setEditReview(null);
      refreshData();
    } catch (e: any) {
      toast.error(e.response?.data?.message || "리뷰 수정에 실패했습니다.");
    }
  };

  const handleDeleteReview = async () => {
    if (!deleteReview) return;
    try {
      await reviewApi.delete(deleteReview.id);
      toast.success("리뷰가 삭제되었습니다.");
      setDeleteReview(null);
      refreshData();
    } catch (e: any) {
      toast.error(e.response?.data?.message || "리뷰 삭제에 실패했습니다.");
    }
  };

  const refreshData = async () => {
    const [clientRes, reviewRes] = await Promise.allSettled([
      clientApi.getById(Number(id)),
      reviewApi.getByClient(Number(id), { page: 1, size: 20 })
    ]);
    if (clientRes.status === "fulfilled") setClient(clientRes.value.data.data);
    if (reviewRes.status === "fulfilled") setReviews(reviewRes.value.data.data.content);
  };

  const isMyReview = (review: ReviewDto) => {
    if (!isLoggedIn || !userId) return false;
    // 이 페이지는 CLIENT의 상세 페이지이므로, 작성자는 DEVELOPER임.
    return userRole === "DEVELOPER" && Number(review.developerUserId) === Number(userId);
  };

  if (isLoading) return <div className="flex justify-center py-32"><Loader2 className="animate-spin text-primary w-10 h-10" /></div>;
  if (!client) return <div className="text-center py-32 text-muted-foreground font-bold">클라이언트를 찾을 수 없습니다.</div>;

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-5xl">
        
        {/* Profile Header */}
        <Card className="border-border bg-card shadow-md mb-8 overflow-hidden rounded-3xl">
          <div className="h-32 bg-gradient-to-r from-primary/80 to-primary" />
          <CardContent className="relative pt-0 px-8 pb-8">
            <div className="flex flex-col md:flex-row md:items-end gap-6 -mt-12">
              <div className="w-32 h-32 rounded-3xl bg-card border-4 border-card shadow-xl flex items-center justify-center text-4xl font-black text-primary">
                {client.name[0]}
              </div>
              <div className="flex-1 mb-2">
                <div className="flex items-center gap-3 mb-2">
                  <h1 className="text-3xl font-bold text-foreground">{client.name}</h1>
                  <Badge variant="outline" className="bg-primary/10 text-primary border-primary/20 font-bold px-3 py-1">
                    {client.participateType === "COMPANY" ? "🏢 기업" : "🧑 개인"}
                  </Badge>
                </div>
                <p className="text-muted-foreground font-medium text-lg">{client.title || "Ready's7 클라이언트"}</p>
              </div>
              <div className="flex gap-3 mb-2">
                {isLoggedIn && userRole === "DEVELOPER" && (
                  <Button 
                    variant="outline" 
                    className="rounded-xl border-border text-foreground hover:bg-secondary font-bold h-11 px-6"
                    onClick={() => setIsReviewModalOpen(true)}
                  >
                    <PlusCircle className="w-4 h-4 mr-2" />
                    리뷰 작성
                  </Button>
                )}
                <Button className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl h-11 px-8 shadow-lg shadow-primary/20">채팅하기</Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Stats Row */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <Card className="border-border bg-card shadow-sm hover:shadow-md transition-shadow rounded-2xl">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="bg-yellow-400/10 p-3 rounded-xl"><Star className="text-yellow-400 w-6 h-6 fill-current" /></div>
              <div>
                <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">평점</p>
                <p className="text-2xl font-black text-foreground">{Number(client.rating ?? 0).toFixed(1)} <span className="text-sm text-muted-foreground font-normal">/ 5.0</span></p>
              </div>
            </CardContent>
          </Card>
          <Card className="border-border bg-card shadow-sm hover:shadow-md transition-shadow rounded-2xl">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="bg-primary/10 p-3 rounded-xl"><Briefcase className="text-primary w-6 h-6" /></div>
              <div>
                <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">완료 프로젝트</p>
                <p className="text-2xl font-black text-foreground">{client.completedProject}건</p>
              </div>
            </CardContent>
          </Card>
          <Card className="border-border bg-card shadow-sm hover:shadow-md transition-shadow rounded-2xl">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="bg-green-500/10 p-3 rounded-xl"><MessageSquare className="text-green-500 w-6 h-6" /></div>
              <div>
                <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">리뷰</p>
                <p className="text-2xl font-black text-foreground">{client.reviewCount || 0}건</p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Main Content */}
        <Tabs defaultValue="reviews" className="w-full">
          <TabsList className="grid w-full grid-cols-2 mb-8 bg-secondary/50 p-1 rounded-2xl border border-border h-14">
            <TabsTrigger value="info" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-bold">소개</TabsTrigger>
            <TabsTrigger value="reviews" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-bold">리뷰 ({client.reviewCount || 0})</TabsTrigger>
          </TabsList>

          <TabsContent value="info">
            <Card className="border-border bg-card shadow-sm rounded-2xl">
              <CardHeader><CardTitle className="text-foreground">클라이언트 소개</CardTitle></CardHeader>
              <CardContent className="text-muted-foreground leading-relaxed whitespace-pre-wrap">
                {client.description || "등록된 소개글이 없습니다."}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="reviews">
            <div className="space-y-4">
              {reviews.length === 0 ? (
                <Card className="border-border bg-card shadow-sm rounded-2xl"><CardContent className="p-12 text-center text-muted-foreground font-medium">아직 작성된 리뷰가 없습니다.</CardContent></Card>
              ) : (
                reviews.map((r) => (
                  <Card key={r.id} className="border-border bg-card shadow-sm hover:shadow-md transition-all rounded-2xl">
                    <CardContent className="p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl bg-secondary flex items-center justify-center font-bold text-primary">
                            {r.developerName[0]}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <p className="font-bold text-foreground">{r.developerName}</p>
                              {isMyReview(r) && <Badge variant="secondary" className="text-[10px] py-0 bg-primary/10 text-primary border-none">내 리뷰</Badge>}
                            </div>
                            <div className="flex items-center gap-1 text-yellow-400">
                              {[...Array(5)].map((_, i) => (
                                <Star key={i} className={`w-3.5 h-3.5 ${i < r.rating ? "fill-current" : "text-secondary"}`} />
                              ))}
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-xs text-muted-foreground">{new Date(r.createdAt).toLocaleDateString()}</span>
                          {isMyReview(r) && (
                            <div className="flex gap-1">
                              <button 
                                onClick={() => {
                                  setEditReview(r);
                                  setEditForm({ rating: r.rating, comment: r.comment });
                                }}
                                className="p-1.5 text-primary hover:bg-primary/10 rounded-lg transition-colors"
                              >
                                <Pencil className="w-4 h-4" />
                              </button>
                              <button 
                                onClick={() => setDeleteReview(r)}
                                className="p-1.5 text-destructive hover:bg-destructive/10 rounded-lg transition-colors"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                      <p className="text-foreground text-sm leading-relaxed mb-4">{r.comment}</p>
                      <div className="bg-secondary/30 p-3 rounded-xl flex items-center gap-2 border border-border/50">
                        <Briefcase className="w-3.5 h-3.5 text-muted-foreground" />
                        <span className="text-xs text-muted-foreground font-medium">{r.projectTitle}</span>
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </TabsContent>
        </Tabs>

        {/* Review 작성 Modal */}
        <Dialog open={isReviewModalOpen} onOpenChange={setIsReviewModalOpen}>
          <DialogContent className="sm:max-w-md bg-card border-border">
            <DialogHeader>
              <DialogTitle className="text-foreground">클라이언트 리뷰 작성</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label className="text-muted-foreground">프로젝트 선택</Label>
                <Select 
                  onValueChange={(val) => setReviewForm(prev => ({ ...prev, projectId: val }))}
                  value={reviewForm.projectId}
                >
                  <SelectTrigger className="bg-secondary/30 border-border text-foreground h-11 rounded-xl">
                    <SelectValue placeholder="리뷰를 작성할 프로젝트를 선택하세요" />
                  </SelectTrigger>
                  <SelectContent className="bg-card border-border">
                    {projects.length === 0 ? (
                      <div className="p-2 text-sm text-muted-foreground text-center">진행한 프로젝트가 없습니다.</div>
                    ) : (
                      projects.map(p => (
                        <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>
                      ))
                    )}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label className="text-muted-foreground">평점</Label>
                <div className="flex gap-2">
                  {[1, 2, 3, 4, 5].map((num) => (
                    <button
                      key={num}
                      type="button"
                      onClick={() => setReviewForm(prev => ({ ...prev, rating: num }))}
                      className="focus:outline-none transition-transform active:scale-90"
                    >
                      <Star 
                        className={`w-9 h-9 ${num <= reviewForm.rating ? "text-yellow-400 fill-current shadow-yellow-400/20" : "text-secondary"}`} 
                      />
                    </button>
                  ))}
                </div>
              </div>
              <div className="space-y-2">
                <Label className="text-muted-foreground">리뷰 내용</Label>
                <Textarea 
                  placeholder="클라이언트와의 작업 경험을 공유해주세요."
                  className="h-32 bg-secondary/30 border-border text-foreground rounded-xl resize-none"
                  value={reviewForm.comment}
                  onChange={(e) => setReviewForm(prev => ({ ...prev, comment: e.target.value }))}
                />
              </div>
            </div>
            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={() => setIsReviewModalOpen(false)} className="rounded-xl border-border text-foreground hover:bg-secondary">취소</Button>
              <Button 
                className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl px-6" 
                onClick={handleReviewSubmit}
                disabled={isSubmitting || projects.length === 0}
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
                리뷰 등록
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 리뷰 수정 모달 */}
        <Dialog open={!!editReview} onOpenChange={(o) => !o && setEditReview(null)}>
          <DialogContent className="max-w-md bg-card border-border">
            <DialogHeader>
              <DialogTitle className="text-foreground">리뷰 수정</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label className="text-muted-foreground">평점</Label>
                <div className="flex gap-2">
                  {[1, 2, 3, 4, 5].map((num) => (
                    <button
                      key={num}
                      type="button"
                      onClick={() => setEditForm(prev => ({ ...prev, rating: num }))}
                      className="focus:outline-none"
                    >
                      <Star 
                        className={`w-9 h-9 ${num <= editForm.rating ? "text-yellow-400 fill-current" : "text-secondary"}`} 
                      />
                    </button>
                  ))}
                </div>
              </div>
              <div className="space-y-2">
                <Label className="text-muted-foreground">리뷰 내용</Label>
                <Textarea
                  value={editForm.comment}
                  onChange={(e) => setEditForm(prev => ({ ...prev, comment: e.target.value }))}
                  className="h-32 bg-secondary/30 border-border text-foreground rounded-xl resize-none"
                />
              </div>
            </div>
            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={() => setEditReview(null)} className="rounded-xl border-border text-foreground hover:bg-secondary">취소</Button>
              <Button className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl px-6" onClick={handleEditReview}>수정 완료</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 리뷰 삭제 확인 */}
        <AlertDialog open={!!deleteReview} onOpenChange={(o) => !o && setDeleteReview(null)}>
          <AlertDialogContent className="bg-card border-border">
            <AlertDialogHeader>
              <AlertDialogTitle className="text-foreground">리뷰 삭제</AlertDialogTitle>
              <AlertDialogDescription className="text-muted-foreground">이 리뷰를 삭제하시겠습니까? 되돌릴 수 없습니다.</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter className="gap-2">
              <AlertDialogCancel className="rounded-xl border-border text-foreground hover:bg-secondary">취소</AlertDialogCancel>
              <AlertDialogAction onClick={handleDeleteReview} className="bg-destructive hover:bg-destructive/90 text-white font-bold rounded-xl px-6">삭제</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

      </div>
    </div>
  );
}
