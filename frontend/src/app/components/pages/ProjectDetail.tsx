/**
 * ProjectDetail.tsx 개선 사항
 *
 * [버그 수정]
 * - CLIENT가 자신의 프로젝트에서 제안서 목록 조회 가능하도록 추가
 * - 제안서 상태 변경(ACCEPTED/REJECTED) 기능 추가
 *
 * [UX 개선]
 * - 로딩 상태 개선
 * - 로그인 안 된 상태에서 제안 버튼 클릭 시 로그인 페이지로 이동
 * - 프로젝트 상태가 OPEN이 아닐 때 제안 버튼 비활성화 + 안내 메시지
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
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../ui/dialog";
import { ArrowLeft, Star, Loader2, Send, CheckCircle, XCircle } from "lucide-react";
import { toast } from "sonner";
import { projectApi, proposalApi } from "../../../api/apiService";
import type { ProjectDto, ProposalDto } from "../../../api/types";
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

const PROPOSAL_STATUS_LABELS: Record<string, string> = {
  pending: "검토 중",
  accepted: "수락됨",
  rejected: "거절됨",
  withdrawn: "철회됨",
};

export function ProjectDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isLoggedIn, userRole } = useAuth();

  const [project, setProject] = useState<ProjectDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 제안서 제출 모달
  const [showProposalModal, setShowProposalModal] = useState(false);
  const [proposalForm, setProposalForm] = useState({
    coverLetter: "",
    proposedBudget: "",
    proposedDuration: "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // CLIENT용: 내 프로젝트의 제안서 목록
  const [proposals, setProposals] = useState<ProposalDto[]>([]);
  const [isLoadingProposals, setIsLoadingProposals] = useState(false);

  useEffect(() => {
    if (!id) return;
    setIsLoading(true);
    projectApi
      .getById(Number(id))
      .then((res) => setProject(res.data.data))
      .catch(() => setProject(null))
      .finally(() => setIsLoading(false));
  }, [id]);

  // CLIENT라면 제안서 목록도 조회
  useEffect(() => {
    if (!id || !isLoggedIn || userRole !== "CLIENT") return;
    setIsLoadingProposals(true);
    proposalApi
      .getByProject(Number(id), 0, 20)
      .then((res) => setProposals(res.data.data.content))
      .catch(() => {}) // 본인 프로젝트 아니면 403 → 조용히 처리
      .finally(() => setIsLoadingProposals(false));
  }, [id, isLoggedIn, userRole]);

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
      toast.success("제안서가 성공적으로 제출되었습니다!");
      setShowProposalModal(false);
      setProposalForm({ coverLetter: "", proposedBudget: "", proposedDuration: "" });
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "제안서 제출에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleProposalStatusChange = async (
    proposalId: number,
    status: "ACCEPTED" | "REJECTED"
  ) => {
    const action = status === "ACCEPTED" ? "수락" : "거절";
    if (!confirm(`이 제안서를 ${action}하시겠습니까?`)) return;
    try {
      await proposalApi.updateStatus(proposalId, status);
      toast.success(`제안서를 ${action}했습니다.`);
      // 목록 갱신
      const res = await proposalApi.getByProject(Number(id), 0, 20);
      setProposals(res.data.data.content);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || `${action}에 실패했습니다.`);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!project) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <h1 className="text-2xl mb-4">프로젝트를 찾을 수 없습니다</h1>
        <Link to="/projects">
          <Button>프로젝트 목록으로</Button>
        </Link>
      </div>
    );
  }

  const isOpen = project.status === "OPEN";

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <Link to="/projects">
          <Button variant="ghost" className="mb-6">
            <ArrowLeft className="w-4 h-4 mr-2" />
            프로젝트 목록
          </Button>
        </Link>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 메인 내용 */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <div className="flex items-center gap-3 mb-3 flex-wrap">
                  <Badge variant="secondary">{project.category}</Badge>
                  <span
                    className={`text-xs px-2 py-1 rounded-full font-medium ${
                      STATUS_COLORS[project.status] ?? "bg-gray-100 text-gray-600"
                    }`}
                  >
                    {STATUS_LABELS[project.status] ?? project.status}
                  </span>
                  <span className="text-sm text-gray-500">
                    {new Date(project.createdAt).toLocaleDateString("ko-KR")}
                  </span>
                </div>
                <CardTitle className="text-3xl">{project.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div>
                    <h3 className="text-lg mb-2">프로젝트 설명</h3>
                    <p className="text-gray-600 leading-relaxed whitespace-pre-wrap">
                      {project.description}
                    </p>
                  </div>
                  <Separator />
                  <div>
                    <h3 className="text-lg mb-3">필요 기술</h3>
                    <div className="flex flex-wrap gap-2">
                      {project.skills.map((skill) => (
                        <Badge key={skill} variant="outline" className="text-sm px-3 py-1">
                          {skill}
                        </Badge>
                      ))}
                    </div>
                  </div>
                  <Separator />
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-gray-500">예산</span>
                      <p className="font-medium text-blue-600 text-lg">
                        {project.minBudget.toLocaleString()}~{project.maxBudget.toLocaleString()}원
                      </p>
                    </div>
                    <div>
                      <span className="text-gray-500">기간</span>
                      <p className="font-medium">{project.duration}일</p>
                    </div>
                    <div>
                      <span className="text-gray-500">제안 현황</span>
                      <p className="font-medium">
                        {project.currentProposalCount} / {project.maxProposalCount}개
                      </p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* CLIENT용: 받은 제안서 목록 */}
            {isLoggedIn && userRole === "CLIENT" && (
              <Card>
                <CardHeader>
                  <CardTitle>받은 제안서</CardTitle>
                </CardHeader>
                <CardContent>
                  {isLoadingProposals ? (
                    <div className="flex justify-center py-6">
                      <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
                    </div>
                  ) : proposals.length === 0 ? (
                    <p className="text-gray-500 text-sm text-center py-4">
                      아직 제안서가 없습니다.
                    </p>
                  ) : (
                    <div className="space-y-4">
                      {proposals.map((proposal) => (
                        <div key={proposal.id} className="border rounded-lg p-4">
                          <div className="flex justify-between items-start mb-2">
                            <div>
                              <p className="font-medium">{proposal.developerName}</p>
                              <p className="text-sm text-gray-500">
                                예산: {proposal.proposedBudget} / 기간: {proposal.proposedDuration}
                              </p>
                            </div>
                            <Badge
                              variant={
                                proposal.status === "accepted"
                                  ? "default"
                                  : proposal.status === "rejected"
                                  ? "destructive"
                                  : "secondary"
                              }
                            >
                              {PROPOSAL_STATUS_LABELS[proposal.status] ?? proposal.status}
                            </Badge>
                          </div>
                          <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                            {proposal.coverLetter}
                          </p>
                          {proposal.status === "pending" && (
                            <div className="flex gap-2">
                              <Button
                                size="sm"
                                onClick={() =>
                                  handleProposalStatusChange(proposal.id, "ACCEPTED")
                                }
                              >
                                <CheckCircle className="w-4 h-4 mr-1" />
                                수락
                              </Button>
                              <Button
                                size="sm"
                                variant="destructive"
                                onClick={() =>
                                  handleProposalStatusChange(proposal.id, "REJECTED")
                                }
                              >
                                <XCircle className="w-4 h-4 mr-1" />
                                거절
                              </Button>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            )}
          </div>

          {/* 사이드바 */}
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>클라이언트 정보</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="mb-4">
                  <p className="font-medium text-lg">{project.clientName}</p>
                  <div className="flex items-center gap-1 text-sm text-gray-600 mt-1">
                    <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                    {project.clientRating != null
                      ? project.clientRating.toFixed(1)
                      : "평점 없음"}
                  </div>
                </div>

                {/* 개발자: 제안서 제출 버튼 */}
                {isLoggedIn && userRole === "DEVELOPER" && (
                  <>
                    {isOpen ? (
                      <Button className="w-full" onClick={() => setShowProposalModal(true)}>
                        <Send className="w-4 h-4 mr-2" />
                        제안서 보내기
                      </Button>
                    ) : (
                      <div className="text-center">
                        <p className="text-sm text-gray-500 mb-2">
                          현재 모집 중인 프로젝트가 아닙니다.
                        </p>
                        <Badge variant="secondary">{STATUS_LABELS[project.status]}</Badge>
                      </div>
                    )}
                  </>
                )}

                {/* 비로그인: 로그인 유도 */}
                {!isLoggedIn && (
                  <Link to="/login">
                    <Button className="w-full" variant="outline">
                      로그인 후 제안하기
                    </Button>
                  </Link>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* 제안서 제출 모달 */}
      <Dialog open={showProposalModal} onOpenChange={setShowProposalModal}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>제안서 제출</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleProposalSubmit} className="space-y-4 mt-2">
            <div className="space-y-2">
              <Label>커버 레터 *</Label>
              <Textarea
                placeholder="프로젝트에 지원하는 이유와 본인의 강점을 작성해주세요."
                rows={5}
                value={proposalForm.coverLetter}
                onChange={(e) =>
                  setProposalForm({ ...proposalForm, coverLetter: e.target.value })
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label>제안 예산 *</Label>
              <Input
                placeholder="예: 300만원 ~ 400만원"
                value={proposalForm.proposedBudget}
                onChange={(e) =>
                  setProposalForm({ ...proposalForm, proposedBudget: e.target.value })
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label>제안 기간 *</Label>
              <Input
                placeholder="예: 45일"
                value={proposalForm.proposedDuration}
                onChange={(e) =>
                  setProposalForm({ ...proposalForm, proposedDuration: e.target.value })
                }
                required
              />
            </div>
            <div className="flex gap-3 pt-2">
              <Button type="submit" className="flex-1" disabled={isSubmitting}>
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    제출 중...
                  </>
                ) : (
                  "제안서 제출"
                )}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => setShowProposalModal(false)}
              >
                취소
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
