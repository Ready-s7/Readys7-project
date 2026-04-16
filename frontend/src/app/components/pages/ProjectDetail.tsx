/**
 * ProjectDetail.tsx - 프로젝트 상세 및 리뷰 작성 기능 통합
 */
import { useParams, Link, useNavigate } from "react-router";
import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Textarea } from "../ui/textarea";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Separator } from "../ui/separator";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../ui/dialog";
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
  Loader2,
  Send,
  CheckCircle,
  XCircle,
  MessageSquarePlus,
  MessageCircle,
  Settings,
  Edit3,
} from "lucide-react";
import { toast } from "sonner";
import { projectApi, proposalApi, chatApi, reviewApi } from "../../../api/apiService";
import { apiClient } from "../../../api/client";
import type { ProjectDto, ProposalDto, ChatRoomDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const STATUS_LABELS: Record<string, string> = {
  OPEN: "모집중",
  CLOSED: "마감",
  IN_PROGRESS: "진행중",
  COMPLETED: "완료",
  CANCELLED: "중단",
};

const STATUS_COLORS: Record<string, string> = {
  OPEN: "bg-green-100 text-green-700",
  CLOSED: "bg-gray-100 text-gray-600",
  IN_PROGRESS: "bg-blue-100 text-blue-700",
  COMPLETED: "bg-purple-100 text-purple-700",
  CANCELLED: "bg-red-100 text-red-700",
};

const PROPOSAL_STATUS_LABELS: Record<string, { label: string; color: string }> = {
  PENDING: { label: "검토 중", color: "bg-yellow-100 text-yellow-700" },
  ACCEPTED: { label: "수락됨", color: "bg-green-100 text-green-700" },
  REJECTED: { label: "거절됨", color: "bg-red-100 text-red-700" },
  WITHDRAWN: { label: "철회됨", color: "bg-gray-100 text-gray-600" },
};

const NEXT_STATUS_OPTIONS: Record<string, string[]> = {
  OPEN: ["IN_PROGRESS", "CANCELLED"],
  CLOSED: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};

export function ProjectDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isLoggedIn, userRole, userId } = useAuth();

  const [project, setProject] = useState<ProjectDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [currentClientId, setCurrentClientId] = useState<number | null>(null);

  // 제안서 관련
  const [showProposalModal, setShowProposalModal] = useState(false);
  const [proposalForm, setProposalForm] = useState({ coverLetter: "", proposedBudget: "", proposedDuration: "" });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [proposals, setProposals] = useState<ProposalDto[]>([]);
  const [isLoadingProposals, setIsLoadingProposals] = useState(false);
  const [myProposal, setMyProposal] = useState<ProposalDto | null>(null);

  // 채팅방 관련
  const [creatingChatRoomFor, setCreatingChatRoomFor] = useState<number | null>(null);
  const [existingChatRooms, setExistingChatRooms] = useState<Record<number, number>>({});

  // 프로젝트 상태 관련
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [newStatus, setNewStatus] = useState("");
  const [isChangingStatus, setIsChangingStatus] = useState(false);

  // 리뷰 관련
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewForm, setReviewForm] = useState({ rating: 5, comment: "" });
  const [isReviewSubmitting, setIsReviewSubmitting] = useState(false);

  useEffect(() => {
    if (!id) return;
    loadProject();
  }, [id]);

  const loadProject = async () => {
    setIsLoading(true);
    try {
      const res = await projectApi.getById(Number(id));
      setProject(res.data.data);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!id || !isLoggedIn) return;
    
    // 현재 사용자가 클라이언트라면 본인의 clientId를 찾아야 함
    if (userRole === "CLIENT") {
      apiClient.get("/v1/users/me").then(res => {
        const myUserId = res.data.data.id;
        apiClient.get("/v1/clients", { params: { page: 1, size: 100 } }).then(allRes => {
          const myClient = allRes.data.data.content.find((c: any) => Number(c.userId) === Number(myUserId));
          if (myClient) setCurrentClientId(myClient.id);
        });
      });
    }

    if (userRole === "DEVELOPER") {
      proposalApi.getMyProposals(0, 100).then((res) => {
        const found = res.data.data.content.find(p => p.projectId === Number(id));
        if (found) setMyProposal(found);
      });
    }
    chatApi.getMyRooms(0, 100).then((res) => {
      const map: Record<number, number> = {};
      res.data.data.content.forEach((r) => { if (r.projectId === Number(id)) map[r.developerId] = r.id; });
      setExistingChatRooms(map);
    });
  }, [id, isLoggedIn, userRole]);

  // currentClientId나 project가 변경될 때 제안서 목록 로딩
  useEffect(() => {
    if (userRole === "CLIENT" && project && currentClientId !== null && Number(project.clientId) === Number(currentClientId)) {
      setIsLoadingProposals(true);
      proposalApi.getByProject(Number(id), 0, 50)
        .then((res) => setProposals(res.data.data.content))
        .finally(() => setIsLoadingProposals(false));
    }
  }, [id, userRole, project, currentClientId]);

  const handleProposalSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!project) return;
    setIsSubmitting(true);
    try {
      await proposalApi.create({
        projectId: project.id,
        coverLetter: proposalForm.coverLetter,
        proposedBudget: proposalForm.proposedBudget,
        proposedDuration: proposalForm.proposedDuration,
      });
      toast.success("제안서가 제출되었습니다!");
      setShowProposalModal(false);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "제안서 제출 실패");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleProposalStatusChange = async (proposalId: number, status: "ACCEPTED" | "REJECTED") => {
    if (!confirm(`이 제안서를 ${status === "ACCEPTED" ? "수락" : "거절"}하시겠습니까?`)) return;
    try {
      await proposalApi.updateStatus(proposalId, status);
      toast.success("상태가 업데이트되었습니다.");
      loadProject();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "업데이트 실패");
    }
  };

  const handleCreateChatRoom = async (proposal: ProposalDto) => {
    if (!project) return;
    setCreatingChatRoomFor(proposal.id);
    try {
      const res = await chatApi.createRoom(project.id, proposal.developerId);
      setExistingChatRooms(prev => ({ ...prev, [proposal.developerId]: res.data.data.id }));
      toast.success("채팅방이 생성되었습니다!");
    } finally {
      setCreatingChatRoomFor(null);
    }
  };

  const handleStatusChange = async () => {
    if (!project || !newStatus) return;
    setIsChangingStatus(true);
    try {
      await projectApi.changeStatus(project.id, newStatus);
      toast.success("프로젝트 상태가 변경되었습니다.");
      setShowStatusModal(false);
      loadProject();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "상태 변경 실패");
    } finally {
      setIsChangingStatus(false);
    }
  };

  const handleReviewSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!project) return;
    setIsReviewSubmitting(true);
    try {
      // 프로젝트 상태가 COMPLETED일 때 리뷰 작성
      // 클라이언트라면 수락된 제안서의 개발자에게, 개발자라면 프로젝트 클라이언트에게 작성
      const targetUserId = userRole === "CLIENT" 
        ? proposals.find(p => p.status === "ACCEPTED")?.developerId // 실제로는 developer의 userId가 필요함
        : project.clientId; // 이것도 실제로는 client의 userId가 필요함
      
      // 임시로 targetUserId를 백엔드 스펙에 맞춰 전달 (추후 백엔드 필드 확인 필요)
      await reviewApi.create({
        projectId: project.id,
        targetUserId: Number(targetUserId), 
        rating: reviewForm.rating,
        comment: reviewForm.comment,
      });
      toast.success("리뷰가 등록되었습니다!");
      setShowReviewModal(false);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "리뷰 등록 실패");
    } finally {
      setIsReviewSubmitting(false);
    }
  };

  if (!project) return <div className="text-center py-32">프로젝트를 찾을 수 없습니다.</div>;

  const isOwner = isLoggedIn && userRole === "CLIENT" && currentClientId !== null && Number(project.clientId) === Number(currentClientId);
  const isOpen = project.status === "OPEN";
  const isCompleted = project.status === "COMPLETED";

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-6xl">
        <Button variant="ghost" onClick={() => navigate(-1)} className="mb-6"><ArrowLeft className="w-4 h-4 mr-2" /> 뒤로가기</Button>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            <Card className="border-none shadow-sm">
              <CardHeader>
                <div className="flex items-center gap-3 mb-4">
                  <Badge variant="secondary" className="px-3 py-1">{project.category}</Badge>
                  <Badge className={`${STATUS_COLORS[project.status]} border-none font-bold`}>{STATUS_LABELS[project.status]}</Badge>
                </div>
                <CardTitle className="text-3xl font-black text-gray-900">{project.title}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-8">
                <div className="bg-gray-50 p-6 rounded-2xl">
                  <h3 className="font-bold text-gray-900 mb-3 flex items-center gap-2"><Edit3 className="w-4 h-4" /> 프로젝트 상세 내용</h3>
                  <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">{project.description}</p>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                  <div><p className="text-xs text-gray-400 font-bold uppercase mb-1">예산</p><p className="font-black text-blue-600">{project.minBudget.toLocaleString()}원 ~</p></div>
                  <div><p className="text-xs text-gray-400 font-bold uppercase mb-1">기간</p><p className="font-bold">{project.duration}일</p></div>
                  <div><p className="text-xs text-gray-400 font-bold uppercase mb-1">제안수</p><p className="font-bold">{project.currentProposalCount} / {project.maxProposalCount}</p></div>
                  <div><p className="text-xs text-gray-400 font-bold uppercase mb-1">등록일</p><p className="font-bold text-gray-500">{new Date(project.createdAt).toLocaleDateString()}</p></div>
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 mb-3">요구 기술 스택</h3>
                  <div className="flex flex-wrap gap-2">
                    {project.skills.map(s => <Badge key={s} variant="outline" className="bg-white px-3 py-1 font-medium">{s}</Badge>)}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 제안서 목록 (CLIENT용 - 본인 프로젝트인 경우에만) */}
            {isOwner && (
              <Card className="border-none shadow-sm">
                <CardHeader><CardTitle>지원한 개발자 목록</CardTitle></CardHeader>
                <CardContent>
                  {isLoadingProposals ? <Loader2 className="animate-spin mx-auto" /> : 
                   proposals.length === 0 ? <p className="text-center text-gray-400 py-10">아직 제안서가 없습니다.</p> :
                   <div className="space-y-4">
                     {proposals.map(p => (
                       <div key={p.id} className="border rounded-xl p-5 hover:bg-gray-50 transition-colors">
                         <div className="flex justify-between items-start mb-3">
                           <Link to={`/developers/${p.developerId}`} className="font-bold text-lg hover:text-blue-600">{p.developerName}</Link>
                           <Badge className={PROPOSAL_STATUS_LABELS[p.status]?.color || ""}>{PROPOSAL_STATUS_LABELS[p.status]?.label}</Badge>
                         </div>
                         <p className="text-sm text-gray-600 mb-4 line-clamp-2">{p.coverLetter}</p>
                         <div className="flex gap-2">
                           {p.status === "PENDING" && (
                             <>
                               <Button size="sm" onClick={() => handleProposalStatusChange(p.id, "ACCEPTED")}>수락</Button>
                               <Button size="sm" variant="destructive" onClick={() => handleProposalStatusChange(p.id, "REJECTED")}>거절</Button>
                             </>
                           )}
                           {p.status === "ACCEPTED" && (
                             existingChatRooms[p.developerId] ? 
                             <Button size="sm" variant="outline" onClick={() => navigate("/chat")}>채팅하기</Button> :
                             <Button size="sm" onClick={() => handleCreateChatRoom(p)} disabled={creatingChatRoomFor === p.id}>채팅방 생성</Button>
                           )}
                         </div>
                       </div>
                     ))}
                   </div>
                  }
                </CardContent>
              </Card>
            )}
          </div>

          {/* 사이드바 */}
          <div className="space-y-6">
            <Card className="border-none shadow-sm">
              <CardHeader><CardTitle>클라이언트</CardTitle></CardHeader>
              <CardContent>
                <Link to={`/clients/${project.clientId}`} className="block group">
                  <p className="font-bold text-xl group-hover:text-blue-600 transition-colors">{project.clientName}</p>
                  <div className="flex items-center gap-1 text-sm text-yellow-500 mt-1">
                    <Star className="w-4 h-4 fill-current" />
                    <span className="font-bold">{project.clientRating?.toFixed(1) || "0.0"}</span>
                  </div>
                </Link>
                <Separator className="my-6" />
                
                {isLoggedIn && userRole === "DEVELOPER" && (
                  myProposal ? (
                    <div className="space-y-4">
                      <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 text-center">
                        <p className="text-blue-700 font-bold mb-1 flex items-center justify-center gap-2">
                          <CheckCircle className="w-4 h-4" /> 제안서 제출 완료
                        </p>
                        <p className="text-[11px] text-blue-500">상태: {PROPOSAL_STATUS_LABELS[myProposal.status]?.label}</p>
                      </div>
                      {isCompleted && myProposal.status === "ACCEPTED" && (
                        <Button className="w-full h-12 font-bold bg-green-600 hover:bg-green-700" onClick={() => setShowReviewModal(true)}>클라이언트 리뷰하기</Button>
                      )}
                    </div>
                  ) : isOpen && (
                    <Button className="w-full h-12 text-lg font-bold bg-blue-600" onClick={() => setShowProposalModal(true)}>제안서 보내기</Button>
                  )
                )}
                
                {isOwner && !isCompleted && (
                  <Button variant="outline" className="w-full h-12 font-bold" onClick={() => { setNewStatus(""); setShowStatusModal(true); }}>프로젝트 상태 변경</Button>
                )}

                {isOwner && isCompleted && (
                  <Button className="w-full h-12 font-bold bg-green-600 hover:bg-green-700" onClick={() => setShowReviewModal(true)}>개발자 리뷰하기</Button>
                )}

                {!isLoggedIn && (
                  <Button className="w-full h-12 font-bold" variant="outline" onClick={() => navigate("/login")}>로그인 후 지원하기</Button>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* 프로젝트 상태 변경 모달 */}
      <Dialog open={showStatusModal} onOpenChange={setShowStatusModal}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>프로젝트 상태 변경</DialogTitle></DialogHeader>
          <div className="space-y-4 mt-4">
            <div className="space-y-2">
              <Label>변경할 상태 선택</Label>
              <Select value={newStatus} onValueChange={setNewStatus}>
                <SelectTrigger><SelectValue placeholder="상태를 선택하세요" /></SelectTrigger>
                <SelectContent>
                  {project && NEXT_STATUS_OPTIONS[project.status]?.map(s => (
                    <SelectItem key={s} value={s}>{STATUS_LABELS[s]}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <p className="text-xs text-gray-500 bg-gray-50 p-3 rounded-lg">
              * 한 번 변경된 상태는 되돌리기 어려울 수 있습니다. 신중하게 선택해 주세요.
            </p>
            <Button className="w-full h-12 font-bold" onClick={handleStatusChange} disabled={!newStatus || isChangingStatus}>
              {isChangingStatus ? <Loader2 className="animate-spin mr-2" /> : null}
              상태 변경 적용
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* 제안서 모달 */}
      <Dialog open={showProposalModal} onOpenChange={setShowProposalModal}>
        <DialogContent className="max-w-xl">
          <DialogHeader><DialogTitle>프로젝트 제안서 제출</DialogTitle></DialogHeader>
          <form onSubmit={handleProposalSubmit} className="space-y-5 mt-4">
            <div className="space-y-2"><Label>지원 동기 및 강점 *</Label><Textarea rows={6} value={proposalForm.coverLetter} onChange={e => setProposalForm({...proposalForm, coverLetter: e.target.value})} required /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>제안 예산 (원) *</Label><Input type="number" value={proposalForm.proposedBudget} onChange={e => setProposalForm({...proposalForm, proposedBudget: e.target.value})} required /></div>
              <div className="space-y-2"><Label>제안 기간 (일) *</Label><Input type="number" value={proposalForm.proposedDuration} onChange={e => setProposalForm({...proposalForm, proposedDuration: e.target.value})} required /></div>
            </div>
            <Button type="submit" className="w-full h-12 font-bold bg-blue-600" disabled={isSubmitting}>제안서 제출하기</Button>
          </form>
        </DialogContent>
      </Dialog>

      {/* 리뷰 모달 */}
      <Dialog open={showReviewModal} onOpenChange={setShowReviewModal}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>리뷰 작성</DialogTitle></DialogHeader>
          <form onSubmit={handleReviewSubmit} className="space-y-5 mt-4">
            <div className="space-y-2">
              <Label>평점</Label>
              <div className="flex gap-2">
                {[1,2,3,4,5].map(v => <button type="button" key={v} onClick={() => setReviewForm({...reviewForm, rating: v})} className={`w-10 h-10 rounded-lg border font-bold ${reviewForm.rating === v ? "bg-yellow-400 text-white border-yellow-400" : "bg-white text-gray-400"}`}>{v}</button>)}
              </div>
            </div>
            <div className="space-y-2"><Label>상세 후기</Label><Textarea rows={4} value={reviewForm.comment} onChange={e => setReviewForm({...reviewForm, comment: e.target.value})} placeholder="상대방과의 협업 경험을 들려주세요." required /></div>
            <Button type="submit" className="w-full h-12 font-bold bg-green-600" disabled={isReviewSubmitting}>리뷰 등록 완료</Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
