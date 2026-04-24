/**
 * DeveloperProfile.tsx (리뷰 CRUD 추가판)
 * - 리뷰 작성 모달 (CLIENT: 완료/취소된 프로젝트 기반)
 * - 내 리뷰 수정 (CLIENT/DEVELOPER 본인 리뷰)
 * - 내 리뷰 삭제
 * - 포트폴리오 탭 유지
 */
import { useParams, Link } from "react-router";
import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Separator } from "../ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Textarea } from "../ui/textarea";
import { Label } from "../ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../ui/dialog";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import {
  ArrowLeft,
  Star,
  Clock,
  Briefcase,
  Calendar,
  Loader2,
  ExternalLink,
  Image as ImageIcon,
  Pencil,
  Trash2,
  Plus,
  User,
} from "lucide-react";
import { toast } from "sonner";
import { developerApi, reviewApi, portfolioApi, clientApi } from "../../../api/apiService";
import type { DeveloperDto, ReviewDto, PortfolioDto, ProjectDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";

export function DeveloperProfile() {
  const { id } = useParams<{ id: string }>();
  const { isLoggedIn, userRole, userId } = useAuth();
  const [developer, setDeveloper] = useState<DeveloperDto | null>(null);
  const [reviews, setReviews] = useState<ReviewDto[]>([]);
  const [portfolios, setPortfolios] = useState<PortfolioDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 리뷰 작성 모달
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [completedProjects, setCompletedProjects] = useState<ProjectDto[]>([]);
  const [reviewForm, setReviewForm] = useState({
    projectId: "",
    rating: "5",
    comment: "",
  });
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);

  // 리뷰 수정
  const [editReview, setEditReview] = useState<ReviewDto | null>(null);
  const [editForm, setEditForm] = useState({ rating: "5", comment: "" });

  // 리뷰 삭제
  const [deleteReview, setDeleteReview] = useState<ReviewDto | null>(null);

  // 내 targetUserId (개발자의 User ID) 찾기
  const [developerUserId, setDeveloperUserId] = useState<number | null>(null);

  const loadData = async () => {
    if (!id) return;
    setIsLoading(true);
    try {
      const promises: Promise<any>[] = [
        developerApi.getById(Number(id)),
        portfolioApi.getByDeveloper(Number(id), undefined, 1, 10),
      ];

      // 로그인한 경우에만 리뷰 데이터 요청
      if (isLoggedIn) {
        promises.push(reviewApi.getByDeveloper(Number(id), { page: 1, size: 20 }));
      }

      const results = await Promise.allSettled(promises);

      if (results[0].status === "fulfilled") {
        setDeveloper(results[0].value.data.data);
      }
      if (results[1].status === "fulfilled") {
        setPortfolios(results[1].value.data.data.content);
      }
      if (isLoggedIn && results[2] && results[2].status === "fulfilled") {
        setReviews(results[2].value.data.data.content);
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadData(); }, [id]);

  // CLIENT가 리뷰 작성 모달을 열 때: 완료/취소된 프로젝트 목록 로드
  const openReviewModal = async () => {
    if (userRole !== "CLIENT") return;
    try {
      const res = await clientApi.getMyProjects(1, 50);
      const myProjects: ProjectDto[] = res.data.data.content || [];
      const eligible = myProjects.filter(
        (p) => p.status === "COMPLETED" || p.status === "CANCELLED"
      );
      setCompletedProjects(eligible);
      setReviewForm({ projectId: eligible[0]?.id?.toString() || "", rating: "5", comment: "" });
      setShowReviewModal(true);
    } catch {
      toast.error("프로젝트 목록을 불러오는 데 실패했습니다.");
    }
  };

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reviewForm.projectId) { toast.error("프로젝트를 선택해주세요."); return; }
    if (!reviewForm.comment.trim()) { toast.error("리뷰 내용을 입력해주세요."); return; }
    setIsSubmittingReview(true);
    try {
      // targetUserId: 개발자의 User ID가 필요. 여기서는 developer id를 통해 user id를 조회하거나,
      // 백엔드의 /v1/reviews?targetUserId= 파라미터에 developer user id를 전달해야 함.
      // 프론트에서 사용 가능한 방법: /v1/developers/{id} 응답에 userId가 없으므로
      // 임시로 developer.id 대신 userId를 찾는 방식을 사용
      // 실제로는 백엔드에서 developerId 기반으로 처리해주어야 함.
      // 현재 백엔드 ReviewController: @RequestParam Long targetUserId (유저 ID)
      // DeveloperDto에 userId 필드가 없으므로, reviewApi.create에서 developer.id를 전달하면
      // 백엔드가 userId와 다를 수 있음. 이 부분은 백엔드 API 개선이 필요하지만
      // 우선 DeveloperDto의 id를 전달하되, 백엔드에서 처리하는 방식에 따라 조정 필요.
      if (!developer?.userId) {
        toast.error("개발자 사용자 정보를 찾을 수 없습니다.");
        return;
      }

      await reviewApi.create(developer.userId, {
        projectId: Number(reviewForm.projectId),
        rating: Number(reviewForm.rating),
        comment: reviewForm.comment,
      });
      toast.success("리뷰가 작성되었습니다.");
      setShowReviewModal(false);
      // 리뷰 목록 새로고침
      const res = await reviewApi.getByDeveloper(Number(id), { page: 1, size: 20 });
      setReviews(res.data.data.content);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "리뷰 작성에 실패했습니다.");
    } finally {
      setIsSubmittingReview(false);
    }
  };

  const openEditReview = (review: ReviewDto) => {
    setEditReview(review);
    setEditForm({ rating: review.rating.toString(), comment: review.comment });
  };

  const handleEditReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editReview) return;
    try {
      await reviewApi.update(editReview.id, {
        rating: Number(editForm.rating),
        comment: editForm.comment || undefined,
      });
      toast.success("리뷰가 수정되었습니다.");
      setEditReview(null);
      const res = await reviewApi.getByDeveloper(Number(id), { page: 1, size: 20 });
      setReviews(res.data.data.content);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "리뷰 수정에 실패했습니다.");
    }
  };

  const handleDeleteReview = async () => {
    if (!deleteReview) return;
    try {
      await reviewApi.delete(deleteReview.id);
      toast.success("리뷰가 삭제되었습니다.");
      setDeleteReview(null);
      const res = await reviewApi.getByDeveloper(Number(id), { page: 1, size: 20 });
      setReviews(res.data.data.content);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "리뷰 삭제에 실패했습니다.");
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!developer) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <h1 className="text-2xl mb-4">개발자를 찾을 수 없습니다</h1>
        <Link to="/developers"><Button>개발자 목록으로</Button></Link>
      </div>
    );
  }

  // 내 리뷰인지 판단 (clientId or developerId 기반)
  const isMyReview = (review: ReviewDto) => {
    if (userRole === "CLIENT") return review.clientId !== undefined; // 내가 작성한 클라이언트 리뷰
    if (userRole === "DEVELOPER") return review.developerId !== undefined;
    return false;
  };

  const StarRating = ({ value, onChange }: { value: string; onChange: (v: string) => void }) => (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          onClick={() => onChange(n.toString())}
          className={`text-2xl transition-colors ${Number(value) >= n ? "text-yellow-400" : "text-gray-300"}`}
        >
          ★
        </button>
      ))}
      <span className="ml-2 text-sm text-gray-500">{value}점</span>
    </div>
  );

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-6xl">
        <Link to="/developers">
          <Button variant="ghost" className="mb-8 text-muted-foreground hover:text-foreground font-bold hover:bg-secondary rounded-xl">
            <ArrowLeft className="w-4 h-4 mr-2" />개발자 목록으로
          </Button>
        </Link>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* 사이드바 */}
          <div className="space-y-6">
            <Card className="bg-card border-border shadow-xl rounded-[32px] overflow-hidden">
              <div className="h-24 bg-gradient-to-r from-primary/80 to-primary" />
              <CardContent className="p-8 pt-0 text-center relative">
                <div className="w-32 h-32 rounded-[32px] bg-card border-4 border-card flex items-center justify-center mx-auto -mt-16 mb-4 text-5xl font-black text-primary shadow-2xl">
                  {developer.name[0]}
                </div>
                <h1 className="text-3xl font-black text-foreground mb-1 tracking-tight">{developer.name}</h1>
                <p className="text-primary font-bold mb-4">{developer.title}</p>
                
                <div className="flex items-center justify-center gap-3 mb-6 bg-secondary/30 py-2 rounded-2xl border border-border/50">
                  <div className="flex items-center gap-1.5">
                    <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                    <span className="text-xl font-black text-foreground">{developer.rating?.toFixed(1) ?? "0.0"}</span>
                  </div>
                  <div className="w-px h-4 bg-border" />
                  <span className="text-muted-foreground font-bold text-sm">{developer.reviewCount}개의 평가</span>
                </div>

                <div className="space-y-3 mb-6 text-sm">
                  {developer.responseTime && (
                    <div className="flex items-center justify-between text-muted-foreground bg-secondary/20 p-3 rounded-xl">
                      <div className="flex items-center gap-2"><Clock className="w-4 h-4" /><span>평균 응답</span></div>
                      <span className="font-bold text-foreground">{developer.responseTime}</span>
                    </div>
                  )}
                  <div className="flex items-center justify-between text-muted-foreground bg-secondary/20 p-3 rounded-xl">
                    <div className="flex items-center gap-2"><Briefcase className="w-4 h-4" /><span>완료 프로젝트</span></div>
                    <span className="font-bold text-foreground">{developer.completedProjects}건</span>
                  </div>
                  <div className="pt-2">
                    <Badge variant={developer.availableForWork ? "default" : "secondary"} className={`w-full justify-center py-2.5 rounded-xl font-black text-xs tracking-wider ${developer.availableForWork ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20" : "bg-secondary text-muted-foreground"}`}>
                      {developer.availableForWork ? "● 프로젝트 가능" : "○ 현재 작업 중"}
                    </Badge>
                  </div>
                </div>

                {isLoggedIn && userRole === "CLIENT" && (
                  <Button className="w-full bg-foreground text-background hover:bg-foreground/90 font-black h-12 rounded-xl shadow-lg" onClick={openReviewModal}>
                    <Plus className="w-4 h-4 mr-2" />리뷰 남기기
                  </Button>
                )}
              </CardContent>
            </Card>

            <Card className="bg-card border-border shadow-md rounded-3xl">
              <CardHeader className="pb-2"><CardTitle className="text-lg font-black uppercase tracking-widest text-muted-foreground">희망 시급 (KRW)</CardTitle></CardHeader>
              <CardContent>
                <div className="text-3xl text-primary font-black mb-1">
                  {(developer.minHourlyPay ?? 0).toLocaleString()} <span className="text-sm text-muted-foreground font-bold">~</span> {(developer.maxHourlyPay ?? 0).toLocaleString()}원
                </div>
                <p className="text-xs text-muted-foreground font-medium">실제 프로젝트 협의 후 조정될 수 있습니다.</p>
              </CardContent>
            </Card>

            <Card className="bg-card border-border shadow-md rounded-3xl">
              <CardHeader className="pb-2"><CardTitle className="text-lg font-black uppercase tracking-widest text-muted-foreground">핵심 기술</CardTitle></CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2">
                  {(developer.skills ?? []).map((skill) => (
                    <Badge key={skill} variant="secondary" className="bg-secondary text-foreground border-none font-bold px-3 py-1 rounded-lg">{skill}</Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 메인 */}
          <div className="lg:col-span-2 space-y-8">
            <Card className="bg-card border-border shadow-xl rounded-[32px] overflow-hidden">
              <CardHeader className="bg-secondary/10 border-b border-border/50 pb-6 pt-8 px-8">
                <CardTitle className="text-2xl font-black text-foreground flex items-center gap-3">
                  <User className="w-6 h-6 text-primary" /> 개발자 소개
                </CardTitle>
              </CardHeader>
              <CardContent className="p-8">
                <p className="text-foreground leading-relaxed mb-10 whitespace-pre-wrap font-medium text-lg italic opacity-90">
                  "{developer.description ?? "소개가 없습니다."}"
                </p>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="text-center p-5 bg-secondary/30 rounded-2xl border border-border/50">
                    <div className="text-3xl mb-1 font-black text-primary">{developer.completedProjects}</div>
                    <div className="text-[10px] font-black text-muted-foreground uppercase tracking-widest">Projects</div>
                  </div>
                  <div className="text-center p-5 bg-secondary/30 rounded-2xl border border-border/50">
                    <div className="text-3xl mb-1 font-black text-primary">{developer.reviewCount}</div>
                    <div className="text-[10px] font-black text-muted-foreground uppercase tracking-widest">Reviews</div>
                  </div>
                  <div className="text-center p-5 bg-secondary/30 rounded-2xl border border-border/50">
                    <div className="text-3xl mb-1 font-black text-primary">{developer.rating?.toFixed(1) ?? "0.0"}</div>
                    <div className="text-[10px] font-black text-muted-foreground uppercase tracking-widest">Rating</div>
                  </div>
                  <div className="text-center p-5 bg-secondary/30 rounded-2xl border border-border/50">
                    <div className="text-xl mb-1 font-black text-primary mt-2">
                      {developer.participateType?.toUpperCase() === "INDIVIDUAL" ? "개인" : 
                       developer.participateType?.toUpperCase() === "COMPANY" ? "업체" : "미지정"}
                    </div>
                    <div className="text-[10px] font-black text-muted-foreground uppercase tracking-widest">Type</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Tabs defaultValue="reviews" className="w-full">
              <TabsList className="grid w-full grid-cols-2 mb-8 bg-secondary/50 p-1 rounded-[20px] border border-border h-16">
                <TabsTrigger value="reviews" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-black text-base">리뷰 ({reviews?.length || 0})</TabsTrigger>
                <TabsTrigger value="portfolio" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-black text-base">포트폴리오 ({portfolios?.length || 0})</TabsTrigger>
              </TabsList>

              {/* 리뷰 탭 */}
              <TabsContent value="reviews">
                <Card className="bg-card border-border shadow-md rounded-[32px]">
                  <CardHeader className="flex flex-row items-center justify-between px-8 pt-8">
                    <CardTitle className="text-xl font-black">클라이언트 피드백</CardTitle>
                    {isLoggedIn && userRole === "CLIENT" && (
                      <Button size="sm" variant="outline" onClick={openReviewModal} className="font-bold rounded-xl border-border hover:bg-secondary">
                        <Plus className="w-4 h-4 mr-1" />리뷰 작성
                      </Button>
                    )}
                  </CardHeader>
                  <CardContent className="px-8 pb-8">
                    {!isLoggedIn ? (
                      <div className="text-center py-20 bg-secondary/10 rounded-2xl border border-dashed border-border">
                        <Star className="w-16 h-16 mx-auto mb-4 opacity-10 text-primary" />
                        <p className="font-bold text-muted-foreground">상세 리뷰는 로그인 후 확인할 수 있습니다.</p>
                        <Link to="/login">
                          <Button className="mt-6 bg-primary font-bold rounded-xl px-8">로그인하기</Button>
                        </Link>
                      </div>
                    ) : (reviews?.length || 0) > 0 ? (
                      <div className="space-y-6">
                        {reviews.map((review) => (
                          <div key={review.id} className="p-6 bg-secondary/10 rounded-2xl border border-border/30 hover:bg-secondary/20 transition-colors">
                            <div className="flex items-start justify-between mb-4">
                              <div>
                                <div className="flex items-center gap-3 mb-1">
                                  <span className="font-black text-lg text-foreground">{review.clientName}</span>
                                  <div className="flex gap-0.5">
                                    {[...Array(5)].map((_, i) => (
                                      <Star key={i} className={`w-3.5 h-3.5 ${i < (review.rating || 0) ? "fill-yellow-400 text-yellow-400" : "text-muted-foreground/30"}`} />
                                    ))}
                                  </div>
                                </div>
                                <div className="flex items-center gap-2 text-sm font-bold text-primary">
                                  <Briefcase className="w-3.5 h-3.5" />
                                  {review.projectTitle}
                                </div>
                              </div>
                              <div className="flex items-center gap-3">
                                <span className="text-[10px] font-bold text-muted-foreground bg-background px-2 py-0.5 rounded-full border border-border">
                                  {new Date(review.createdAt).toLocaleDateString("ko-KR")}
                                </span>
                                {isLoggedIn && userRole === "CLIENT" && (
                                  <div className="flex gap-1">
                                    <button onClick={() => openEditReview(review)} className="text-muted-foreground hover:text-primary p-1.5 bg-background rounded-lg border border-border hover:border-primary/30 transition-all">
                                      <Pencil className="w-3.5 h-3.5" />
                                    </button>
                                    <button onClick={() => setDeleteReview(review)} className="text-muted-foreground hover:text-destructive p-1.5 bg-background rounded-lg border border-border hover:border-destructive/30 transition-all">
                                      <Trash2 className="w-3.5 h-3.5" />
                                    </button>
                                  </div>
                                )}
                              </div>
                            </div>
                            <p className="text-foreground/80 leading-relaxed font-medium">{review.comment}</p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-20 bg-secondary/10 rounded-2xl border border-dashed border-border">
                        <Star className="w-16 h-16 mx-auto mb-4 opacity-10 text-primary" />
                        <p className="font-bold text-muted-foreground">아직 작성된 리뷰가 없습니다.</p>
                        {isLoggedIn && userRole === "CLIENT" && (
                          <Button variant="outline" className="mt-6 font-bold rounded-xl" onClick={openReviewModal}>
                            첫 번째 리뷰 작성하기
                          </Button>
                        )}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              {/* 포트폴리오 탭 */}
              <TabsContent value="portfolio">
                <Card className="bg-card border-border shadow-md rounded-[32px]">
                  <CardHeader className="px-8 pt-8"><CardTitle className="text-xl font-black text-foreground">포트폴리오</CardTitle></CardHeader>
                  <CardContent className="px-8 pb-8">
                    {(portfolios?.length || 0) > 0 ? (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {portfolios.map((portfolio) => (
                          <div key={portfolio.id} className="group border border-border/50 rounded-2xl overflow-hidden bg-secondary/5 hover:border-primary/50 transition-all hover:shadow-lg hover:shadow-primary/5">
                            {portfolio.imageUrl ? (
                              <div className="aspect-video overflow-hidden">
                                <img src={portfolio.imageUrl} alt={portfolio.title}
                                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                  onError={(e) => { (e.target as HTMLImageElement).parentElement!.style.display = "none"; }} />
                              </div>
                            ) : (
                              <div className="aspect-video bg-gradient-to-br from-primary/5 to-primary/10 flex items-center justify-center">
                                <ImageIcon className="w-10 h-10 text-primary/20" />
                              </div>
                            )}
                            <div className="p-5">
                              <div className="flex justify-between items-start mb-3">
                                <h3 className="font-black text-lg text-foreground group-hover:text-primary transition-colors truncate">{portfolio.title}</h3>
                                {portfolio.projectUrl && (
                                  <a href={portfolio.projectUrl} target="_blank" rel="noopener noreferrer"
                                    className="text-primary hover:bg-primary/10 p-1.5 rounded-lg transition-colors">
                                    <ExternalLink className="w-4 h-4" />
                                  </a>
                                )}
                              </div>
                              <p className="text-muted-foreground text-sm mb-5 line-clamp-2 font-medium leading-relaxed h-10">{portfolio.description}</p>
                              <div className="flex flex-wrap gap-1.5 mb-4 min-h-[24px]">
                                {(portfolio.skills ?? []).map((skill) => (
                                  <Badge key={skill} variant="outline" className="text-[10px] bg-background border-border text-muted-foreground px-2 py-0 font-bold">{skill}</Badge>
                                ))}
                              </div>
                              <p className="text-[10px] text-muted-foreground/50 font-black uppercase tracking-wider">
                                Registered {new Date(portfolio.createdAt).toLocaleDateString("ko-KR")}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-20 bg-secondary/10 rounded-2xl border border-dashed border-border">
                        <Briefcase className="w-16 h-16 mx-auto mb-4 opacity-10 text-primary" />
                        <p className="font-bold text-muted-foreground">등록된 포트폴리오가 없습니다.</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </div>

      {/* 리뷰 작성 모달 */}
      <Dialog open={showReviewModal} onOpenChange={setShowReviewModal}>
        <DialogContent className="max-w-md bg-card border-border rounded-3xl">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black text-foreground">"{developer.name}"님 리뷰 작성</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmitReview} className="space-y-6 mt-6">
            <div className="space-y-2">
              <Label className="text-sm font-bold text-muted-foreground ml-1">진행한 프로젝트 선택 *</Label>
              {completedProjects.length === 0 ? (
                <div className="text-sm text-destructive bg-destructive/5 p-4 rounded-xl border border-destructive/20 font-medium">
                  아직 완료된 프로젝트가 없어 리뷰를 작성할 수 없습니다.
                </div>
              ) : (
                <Select value={reviewForm.projectId} onValueChange={(v) => setReviewForm({ ...reviewForm, projectId: v })}>
                  <SelectTrigger className="h-12 bg-secondary/30 border-border rounded-xl"><SelectValue placeholder="프로젝트를 선택하세요" /></SelectTrigger>
                  <SelectContent className="bg-card border-border">
                    {completedProjects.map((p) => (
                      <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
            <div className="space-y-3">
              <Label className="text-sm font-bold text-muted-foreground ml-1">협업 만족도 *</Label>
              <div className="flex gap-2 items-center bg-secondary/20 p-4 rounded-2xl justify-center">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button key={n} type="button"
                    onClick={() => setReviewForm({ ...reviewForm, rating: n.toString() })}
                    className="focus:outline-none transition-transform active:scale-90">
                    <Star className={`w-9 h-9 ${Number(reviewForm.rating) >= n ? "fill-yellow-400 text-yellow-400 shadow-yellow-400/20" : "text-border"}`} />
                  </button>
                ))}
                <span className="ml-4 text-xl font-black text-foreground w-12">{reviewForm.rating}.0</span>
              </div>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-bold text-muted-foreground ml-1">상세 협업 경험 (최대 100자) *</Label>
              <Textarea
                value={reviewForm.comment}
                onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value.slice(0, 100) })}
                placeholder="개발자와의 협업 경험이 어떠셨나요? (소통, 전문성, 일정 준수 등)"
                rows={4}
                maxLength={100}
                className="bg-secondary/30 border-border rounded-xl resize-none p-4"
                required
              />
              <p className="text-[10px] text-muted-foreground/50 text-right font-bold tracking-widest">{reviewForm.comment.length} / 100 CHARS</p>
            </div>
            <div className="flex gap-3 pt-2">
              <Button type="submit" className="flex-1 h-12 bg-primary text-primary-foreground font-black rounded-xl shadow-lg shadow-primary/20" disabled={isSubmittingReview || completedProjects.length === 0}>
                {isSubmittingReview ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null}
                리뷰 등록 완료
              </Button>
              <Button type="button" variant="outline" onClick={() => setShowReviewModal(false)} className="h-12 border-border text-foreground hover:bg-secondary font-bold rounded-xl px-6">취소</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 리뷰 수정 모달 */}
      <Dialog open={!!editReview} onOpenChange={(o) => !o && setEditReview(null)}>
        <DialogContent className="max-w-md bg-card border-border rounded-3xl">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">리뷰 수정</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleEditReview} className="space-y-6 mt-6">
            <div className="space-y-3">
              <Label className="text-sm font-bold text-muted-foreground ml-1">평점 수정</Label>
              <div className="flex gap-2 bg-secondary/20 p-4 rounded-2xl justify-center">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button key={n} type="button"
                    onClick={() => setEditForm({ ...editForm, rating: n.toString() })}
                    className="focus:outline-none transition-transform active:scale-90">
                    <Star className={`w-8 h-8 ${Number(editForm.rating) >= n ? "fill-yellow-400 text-yellow-400" : "text-border"}`} />
                  </button>
                ))}
              </div>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-bold text-muted-foreground ml-1">리뷰 내용 수정</Label>
              <Textarea
                value={editForm.comment}
                onChange={(e) => setEditForm({ ...editForm, comment: e.target.value })}
                rows={4}
                className="bg-secondary/30 border-border rounded-xl resize-none p-4"
              />
            </div>
            <div className="flex gap-3">
              <Button type="submit" className="flex-1 h-12 bg-primary text-primary-foreground font-black rounded-xl shadow-lg">수정 저장</Button>
              <Button type="button" variant="outline" onClick={() => setEditReview(null)} className="h-12 border-border font-bold rounded-xl px-6">취소</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 리뷰 삭제 확인 */}
      <AlertDialog open={!!deleteReview} onOpenChange={(o) => !o && setDeleteReview(null)}>
        <AlertDialogContent className="bg-card border-border rounded-3xl">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-2xl font-black">리뷰를 삭제하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription className="font-medium text-muted-foreground">
              삭제된 리뷰는 복구할 수 없습니다. 정말로 진행하시겠습니까?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="gap-3 mt-4">
            <AlertDialogCancel className="h-11 border-border font-bold rounded-xl px-6">취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteReview} className="h-11 bg-destructive text-white hover:bg-destructive/90 font-black rounded-xl px-8 shadow-lg shadow-destructive/20">삭제 확정</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
