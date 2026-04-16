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
import { Star, Briefcase, MessageSquare, Loader2, MapPin, Calendar, ExternalLink, PlusCircle } from "lucide-react";
import { clientApi, reviewApi, developerApi } from "../../../api/apiService";
import type { ClientDto, ProjectDto, ReviewDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";
import { toast } from "sonner";

export function ClientDetail() {
  const { id } = useParams();
  const { isLoggedIn, userRole } = useAuth();
  
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
          const projectRes = await developerApi.getMyProjects(1, 100);
          // 이 클라이언트의 프로젝트만 필터링
          const myProjectsWithThisClient = projectRes.data.data.content.filter(
            p => p.clientId === clientData.id
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
      // 리뷰 목록 새로고침
      const reviewRes = await reviewApi.getByClient(Number(id), { page: 1, size: 20 });
      setReviews(reviewRes.data.data.content);
    } catch (e: any) {
      toast.error(e.response?.data?.message || "리뷰 등록에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) return <div className="flex justify-center py-32"><Loader2 className="animate-spin text-blue-600 w-10 h-10" /></div>;
  if (!client) return <div className="text-center py-32 text-gray-500">클라이언트를 찾을 수 없습니다.</div>;

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-5xl">
        
        {/* Profile Header */}
        <Card className="border-none shadow-sm mb-8 overflow-hidden">
          <div className="h-32 bg-gradient-to-r from-blue-600 to-indigo-600" />
          <CardContent className="relative pt-0 px-8 pb-8">
            <div className="flex flex-col md:flex-row md:items-end gap-6 -mt-12">
              <div className="w-32 h-32 rounded-2xl bg-white border-4 border-white shadow-md flex items-center justify-center text-4xl font-bold text-blue-600">
                {client.name[0]}
              </div>
              <div className="flex-1 mb-2">
                <div className="flex items-center gap-3 mb-2">
                  <h1 className="text-3xl font-bold text-gray-900">{client.name}</h1>
                  <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-100 uppercase text-[10px] py-0">
                    {client.participateType === "COMPANY" ? "🏢 기업" : "🧑 개인"}
                  </Badge>
                </div>
                <p className="text-gray-600 font-medium text-lg">{client.title || "Ready's7 클라이언트"}</p>
              </div>
              <div className="flex gap-3 mb-2">
                {isLoggedIn && userRole === "DEVELOPER" && (
                  <Button 
                    variant="outline" 
                    className="border-blue-600 text-blue-600 hover:bg-blue-50"
                    onClick={() => setIsReviewModalOpen(true)}
                  >
                    <PlusCircle className="w-4 h-4 mr-2" />
                    리뷰 작성
                  </Button>
                )}
                <Button className="bg-blue-600 hover:bg-blue-700">채팅하기</Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Stats Row */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <Card className="border-none shadow-sm">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="bg-yellow-50 p-3 rounded-xl"><Star className="text-yellow-500 w-6 h-6 fill-current" /></div>
              <div>
                <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">평점</p>
                <p className="text-2xl font-black text-gray-900">{client.rating.toFixed(1)} / 5.0</p>
              </div>
            </CardContent>
          </Card>
          <Card className="border-none shadow-sm">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="bg-blue-50 p-3 rounded-xl"><Briefcase className="text-blue-600 w-6 h-6" /></div>
              <div>
                <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">완료 프로젝트</p>
                <p className="text-2xl font-black text-gray-900">{client.completedProject}건</p>
              </div>
            </CardContent>
          </Card>
          <Card className="border-none shadow-sm">
            <CardContent className="p-6 flex items-center gap-4">
              <div className="bg-green-50 p-3 rounded-xl"><MessageSquare className="text-green-600 w-6 h-6" /></div>
              <div>
                <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">리뷰</p>
                <p className="text-2xl font-black text-gray-900">{reviews.length}건</p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Main Content */}
        <Tabs defaultValue="reviews" className="w-full">
          <TabsList className="grid w-full grid-cols-2 mb-8 bg-white p-1 rounded-xl shadow-sm border h-12">
            <TabsTrigger value="info" className="rounded-lg">소개</TabsTrigger>
            <TabsTrigger value="reviews" className="rounded-lg">리뷰 ({reviews.length})</TabsTrigger>
          </TabsList>

          <TabsContent value="info">
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle>클라이언트 소개</CardTitle></CardHeader>
              <CardContent className="text-gray-700 leading-relaxed whitespace-pre-wrap">
                {client.description || "등록된 소개글이 없습니다."}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="reviews">
            <div className="space-y-4">
              {reviews.length === 0 ? (
                <Card className="border-none shadow-sm"><CardContent className="p-12 text-center text-gray-400">아직 작성된 리뷰가 없습니다.</CardContent></Card>
              ) : (
                reviews.map((r) => (
                  <Card key={r.id} className="border-none shadow-sm hover:shadow-md transition-shadow">
                    <CardContent className="p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center font-bold text-gray-500">
                            {r.developerName[0]}
                          </div>
                          <div>
                            <p className="font-bold text-gray-900">{r.developerName}</p>
                            <div className="flex items-center gap-1 text-yellow-500">
                              {[...Array(5)].map((_, i) => (
                                <Star key={i} className={`w-3 h-3 ${i < r.rating ? "fill-current" : "text-gray-200"}`} />
                              ))}
                            </div>
                          </div>
                        </div>
                        <span className="text-xs text-gray-400">{new Date(r.createdAt).toLocaleDateString()}</span>
                      </div>
                      <p className="text-gray-700 text-sm leading-relaxed mb-3">{r.comment}</p>
                      <div className="bg-gray-50 p-3 rounded-lg flex items-center gap-2">
                        <Briefcase className="w-3 h-3 text-gray-400" />
                        <span className="text-xs text-gray-500 font-medium">{r.projectTitle}</span>
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </TabsContent>
        </Tabs>

        {/* Review Modal */}
        <Dialog open={isReviewModalOpen} onOpenChange={setIsReviewModalOpen}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>클라이언트 리뷰 작성</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label>프로젝트 선택</Label>
                <Select 
                  onValueChange={(val) => setReviewForm(prev => ({ ...prev, projectId: val }))}
                  value={reviewForm.projectId}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="리뷰를 작성할 프로젝트를 선택하세요" />
                  </SelectTrigger>
                  <SelectContent>
                    {projects.length === 0 ? (
                      <div className="p-2 text-sm text-gray-500 text-center">진행한 프로젝트가 없습니다.</div>
                    ) : (
                      projects.map(p => (
                        <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>
                      ))
                    )}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>평점</Label>
                <div className="flex gap-1">
                  {[1, 2, 3, 4, 5].map((num) => (
                    <button
                      key={num}
                      type="button"
                      onClick={() => setReviewForm(prev => ({ ...prev, rating: num }))}
                      className="focus:outline-none"
                    >
                      <Star 
                        className={`w-8 h-8 ${num <= reviewForm.rating ? "text-yellow-500 fill-current" : "text-gray-200"}`} 
                      />
                    </button>
                  ))}
                </div>
              </div>
              <div className="space-y-2">
                <Label>리뷰 내용</Label>
                <Textarea 
                  placeholder="클라이언트와의 작업 경험을 공유해주세요."
                  className="h-32"
                  value={reviewForm.comment}
                  onChange={(e) => setReviewForm(prev => ({ ...prev, comment: e.target.value }))}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsReviewModalOpen(false)}>취소</Button>
              <Button 
                className="bg-blue-600 hover:bg-blue-700" 
                onClick={handleReviewSubmit}
                disabled={isSubmitting || projects.length === 0}
              >
                {isSubmitting ? "등록 중..." : "리뷰 등록"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}
