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
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <Link to="/developers">
          <Button variant="ghost" className="mb-6">
            <ArrowLeft className="w-4 h-4 mr-2" />개발자 목록
          </Button>
        </Link>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 사이드바 */}
          <div className="space-y-6">
            <Card>
              <CardContent className="p-6 text-center">
                <div className="w-32 h-32 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-4 text-5xl">
                  {developer.name[0]}
                </div>
                <h1 className="text-2xl mb-2">{developer.name}</h1>
                <p className="text-gray-600 mb-3">{developer.title}</p>
                <div className="flex items-center justify-center gap-2 mb-4">
                  <div className="flex items-center gap-1">
                    <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                    <span className="text-xl">{developer.rating?.toFixed(1) ?? "0.0"}</span>
                  </div>
                  <span className="text-gray-500">({developer.reviewCount}개 리뷰)</span>
                </div>
                <div className="space-y-2 mb-4 text-sm">
                  {developer.responseTime && (
                    <div className="flex items-center justify-center gap-2 text-gray-600">
                      <Clock className="w-4 h-4" />
                      <span>응답 시간: {developer.responseTime}</span>
                    </div>
                  )}
                  <div className="flex items-center justify-center gap-2 text-gray-600">
                    <Briefcase className="w-4 h-4" />
                    <span>{developer.completedProjects}개 프로젝트 완료</span>
                  </div>
                  <div className="flex items-center justify-center gap-2">
                    <Badge variant={developer.availableForWork ? "default" : "secondary"}>
                      {developer.availableForWork ? "✅ 작업 가능" : "🔴 작업 중"}
                    </Badge>
                  </div>
                </div>
                {/* CLIENT: 리뷰 작성 버튼 */}
                {isLoggedIn && userRole === "CLIENT" && (
                  <Button variant="outline" size="sm" className="w-full" onClick={openReviewModal}>
                    <Plus className="w-4 h-4 mr-1" />리뷰 작성
                  </Button>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader><CardTitle>시간당 요금</CardTitle></CardHeader>
              <CardContent>
                <div className="text-2xl text-blue-600 mb-2">
                  {(developer.minHourlyPay ?? 0).toLocaleString()}~{(developer.maxHourlyPay ?? 0).toLocaleString()}원/시간
                </div>
                <p className="text-sm text-gray-600">프로젝트 규모에 따라 협의 가능합니다</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader><CardTitle>보유 기술</CardTitle></CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2">
                  {(developer.skills ?? []).map((skill) => (
                    <Badge key={skill} variant="secondary">{skill}</Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 메인 */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader><CardTitle>소개</CardTitle></CardHeader>
              <CardContent>
                <p className="text-gray-700 leading-relaxed mb-6 whitespace-pre-wrap">
                  {developer.description ?? "소개가 없습니다."}
                </p>
                <Separator className="my-6" />
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">{developer.completedProjects}</div>
                    <div className="text-sm text-gray-600">완료 프로젝트</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">{developer.reviewCount}</div>
                    <div className="text-sm text-gray-600">총 리뷰</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">{developer.rating?.toFixed(1) ?? "0.0"}★</div>
                    <div className="text-sm text-gray-600">평균 평점</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">
                      {developer.participateType === "INDIVIDUAL" ? "개인" : developer.participateType === "COMPANY" ? "회사" : "미지정"}
                    </div>
                    <div className="text-sm text-gray-600">유형</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Tabs defaultValue="reviews" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="reviews">리뷰 ({reviews?.length || 0})</TabsTrigger>
                <TabsTrigger value="portfolio">포트폴리오 ({portfolios?.length || 0})</TabsTrigger>
              </TabsList>

              {/* 리뷰 탭 */}
              <TabsContent value="reviews">
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>고객 리뷰</CardTitle>
                    {isLoggedIn && userRole === "CLIENT" && (
                      <Button size="sm" variant="outline" onClick={openReviewModal}>
                        <Plus className="w-4 h-4 mr-1" />리뷰 작성
                      </Button>
                    )}
                  </CardHeader>
                  <CardContent>
                    {!isLoggedIn ? (
                      <div className="text-center py-12 text-gray-500">
                        <Star className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>리뷰 목록은 로그인 후 확인할 수 있습니다.</p>
                        <Link to="/login">
                          <Button size="sm" variant="outline" className="mt-4">로그인하기</Button>
                        </Link>
                      </div>
                    ) : (reviews?.length || 0) > 0 ? (
                      <div className="space-y-6">
                        {reviews.map((review) => (
                          <div key={review.id} className="border-b pb-6 last:border-0">
                            <div className="flex items-start justify-between mb-3">
                              <div>
                                <div className="flex items-center gap-2 mb-1">
                                  <span className="font-medium">{review.clientName}</span>
                                  <div className="flex">
                                    {Array.from({ length: review.rating || 0 }).map((_, i) => (
                                      <Star key={i} className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                                    ))}
                                  </div>
                                </div>
                                <p className="text-sm text-gray-600">{review.projectTitle}</p>
                              </div>
                              <div className="flex items-center gap-2">
                                <div className="flex items-center gap-1 text-sm text-gray-500">
                                  <Calendar className="w-4 h-4" />
                                  {new Date(review.createdAt).toLocaleDateString("ko-KR")}
                                </div>
                                {/* 내 리뷰: 수정/삭제 */}
                                {isLoggedIn && userRole === "CLIENT" && (
                                  <div className="flex gap-1">
                                    <button onClick={() => openEditReview(review)} className="text-blue-500 hover:text-blue-700 p-1">
                                      <Pencil className="w-3 h-3" />
                                    </button>
                                    <button onClick={() => setDeleteReview(review)} className="text-red-400 hover:text-red-600 p-1">
                                      <Trash2 className="w-3 h-3" />
                                    </button>
                                  </div>
                                )}
                              </div>
                            </div>
                            <p className="text-gray-700">{review.comment}</p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-gray-500">
                        <Star className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>아직 리뷰가 없습니다</p>
                        {isLoggedIn && userRole === "CLIENT" && (
                          <Button size="sm" variant="outline" className="mt-4" onClick={openReviewModal}>
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
                <Card>
                  <CardHeader><CardTitle>포트폴리오</CardTitle></CardHeader>
                  <CardContent>
                    {(portfolios?.length || 0) > 0 ? (
                      <div className="space-y-6">
                        {portfolios.map((portfolio) => (
                          <div key={portfolio.id} className="border rounded-lg p-4">
                            {portfolio.imageUrl ? (
                              <img src={portfolio.imageUrl} alt={portfolio.title}
                                className="w-full h-48 object-cover rounded-md mb-4"
                                onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }} />
                            ) : (
                              <div className="w-full h-32 bg-gray-100 rounded-md mb-4 flex items-center justify-center text-gray-400">
                                <ImageIcon className="w-8 h-8" />
                              </div>
                            )}
                            <div className="flex justify-between items-start mb-2">
                              <h3 className="font-medium text-lg">{portfolio.title}</h3>
                              {portfolio.projectUrl && (
                                <a href={portfolio.projectUrl} target="_blank" rel="noopener noreferrer"
                                  className="text-blue-600 hover:underline flex items-center gap-1 text-sm">
                                  <ExternalLink className="w-4 h-4" />링크
                                </a>
                              )}
                            </div>
                            <p className="text-gray-600 text-sm mb-3">{portfolio.description}</p>
                            <div className="flex flex-wrap gap-1">
                              {(portfolio.skills ?? []).map((skill) => (
                                <Badge key={skill} variant="outline" className="text-xs">{skill}</Badge>
                              ))}
                            </div>
                            <p className="text-xs text-gray-400 mt-3">
                              {new Date(portfolio.createdAt).toLocaleDateString("ko-KR")}
                            </p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-gray-500">
                        <Briefcase className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>아직 포트폴리오가 없습니다</p>
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
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{developer.name}님에게 리뷰 작성</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmitReview} className="space-y-4 mt-2">
            <div className="space-y-2">
              <Label>프로젝트 선택 *</Label>
              {completedProjects.length === 0 ? (
                <p className="text-sm text-amber-600 bg-amber-50 p-3 rounded">
                  완료되거나 취소된 프로젝트가 없습니다. 프로젝트 완료 후 리뷰를 작성할 수 있습니다.
                </p>
              ) : (
                <Select value={reviewForm.projectId} onValueChange={(v) => setReviewForm({ ...reviewForm, projectId: v })}>
                  <SelectTrigger><SelectValue placeholder="프로젝트를 선택하세요" /></SelectTrigger>
                  <SelectContent>
                    {completedProjects.map((p) => (
                      <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
            <div className="space-y-2">
              <Label>평점 *</Label>
              <div className="flex gap-1">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button key={n} type="button"
                    onClick={() => setReviewForm({ ...reviewForm, rating: n.toString() })}
                    className={`text-2xl transition-colors ${Number(reviewForm.rating) >= n ? "text-yellow-400" : "text-gray-300"}`}>
                    ★
                  </button>
                ))}
                <span className="ml-2 text-sm text-gray-500 self-center">{reviewForm.rating}점</span>
              </div>
            </div>
            <div className="space-y-2">
              <Label>리뷰 내용 * (최대 100자)</Label>
              <Textarea
                value={reviewForm.comment}
                onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value.slice(0, 100) })}
                placeholder="개발자와의 협업 경험을 공유해주세요."
                rows={4}
                maxLength={100}
                required
              />
              <p className="text-xs text-gray-400 text-right">{reviewForm.comment.length}/100</p>
            </div>
            <div className="flex gap-3 pt-2">
              <Button type="submit" className="flex-1" disabled={isSubmittingReview || completedProjects.length === 0}>
                {isSubmittingReview ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null}
                리뷰 작성
              </Button>
              <Button type="button" variant="outline" onClick={() => setShowReviewModal(false)}>취소</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 리뷰 수정 모달 */}
      <Dialog open={!!editReview} onOpenChange={(o) => !o && setEditReview(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>리뷰 수정</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleEditReview} className="space-y-4 mt-2">
            <div className="space-y-2">
              <Label>평점</Label>
              <div className="flex gap-1">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button key={n} type="button"
                    onClick={() => setEditForm({ ...editForm, rating: n.toString() })}
                    className={`text-2xl transition-colors ${Number(editForm.rating) >= n ? "text-yellow-400" : "text-gray-300"}`}>
                    ★
                  </button>
                ))}
              </div>
            </div>
            <div className="space-y-2">
              <Label>리뷰 내용</Label>
              <Textarea
                value={editForm.comment}
                onChange={(e) => setEditForm({ ...editForm, comment: e.target.value })}
                rows={4}
              />
            </div>
            <div className="flex gap-3">
              <Button type="submit" className="flex-1">수정 완료</Button>
              <Button type="button" variant="outline" onClick={() => setEditReview(null)}>취소</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 리뷰 삭제 확인 */}
      <AlertDialog open={!!deleteReview} onOpenChange={(o) => !o && setDeleteReview(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>리뷰 삭제</AlertDialogTitle>
            <AlertDialogDescription>이 리뷰를 삭제하시겠습니까? 되돌릴 수 없습니다.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteReview} className="bg-red-600 hover:bg-red-700">삭제</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
