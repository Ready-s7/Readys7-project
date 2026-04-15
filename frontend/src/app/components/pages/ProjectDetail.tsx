/**
 * ProjectDetail.tsx 수정 사항
 *
 * [버그 수정 & 기능 추가]
 * 1. CLIENT가 제안서 수락 후 해당 개발자와 채팅방 생성 버튼 추가
 *    → ACCEPTED 상태 제안서에 "채팅방 생성" 버튼 표시
 *    → 이미 채팅방이 있으면 "채팅하러 가기" 링크 표시
 * 2. 프로젝트 상태 변경 기능 추가 (CLIENT 전용)
 *    → 현재 상태에서 전환 가능한 상태 목록 표시
 * 3. 제안서 목록 UI 개선 (개발자명 클릭 시 프로필 이동)
 *
 * [기존 동작 유지]
 * - DEVELOPER: 제안서 제출 모달
 * - CLIENT: 제안서 수락/거절
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
} from "lucide-react";
import { toast } from "sonner";
import { projectApi, proposalApi, chatApi } from "../../../api/apiService";
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

const PROPOSAL_STATUS_LABELS: Record<
  string,
  { label: string; color: string }
> = {
  pending: { label: "검토 중", color: "bg-yellow-100 text-yellow-700" },
  accepted: { label: "수락됨", color: "bg-green-100 text-green-700" },
  rejected: { label: "거절됨", color: "bg-red-100 text-red-700" },
  withdrawn: { label: "철회됨", color: "bg-gray-100 text-gray-600" },
};

// 현재 상태에서 전환 가능한 상태
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

  // CLIENT용: 제안서 목록
  const [proposals, setProposals] = useState<ProposalDto[]>([]);
  const [isLoadingProposals, setIsLoadingProposals] = useState(false);

  // 채팅방 생성 중 상태 관리 (proposalId → loading)
  const [creatingChatRoomFor, setCreatingChatRoomFor] = useState<number | null>(null);
  // 이미 생성된 채팅방 맵 (developerId → chatRoomId)
  const [existingChatRooms, setExistingChatRooms] = useState<
    Record<number, number>
  >({});

  // 상태 변경
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [newStatus, setNewStatus] = useState("");
  const [isChangingStatus, setIsChangingStatus] = useState(false);

  // 프로젝트 로드
  useEffect(() => {
    if (!id) return;
    setIsLoading(true);
    projectApi
      .getById(Number(id))
      .then((res) => setProject(res.data.data))
      .catch(() => setProject(null))
      .finally(() => setIsLoading(false));
  }, [id]);

  // CLIENT용: 제안서 목록 + 기존 채팅방 조회
  useEffect(() => {
    if (!id || !isLoggedIn || userRole !== "CLIENT") return;

    // 제안서 목록 조회
    setIsLoadingProposals(true);
    proposalApi
      .getByProject(Number(id), 0, 20)
      .then((res) => setProposals(res.data.data.content))
      .catch(() => {})
      .finally(() => setIsLoadingProposals(false));

    // 기존 채팅방 목록 조회 (채팅방 생성 여부 확인용)
    chatApi
      .getMyRooms(0, 50)
      .then((res) => {
        const rooms = res.data.data.content;
        const map: Record<number, number> = {};
        rooms.forEach((room) => {
          // 현재 프로젝트의 채팅방이면 developerId → roomId 매핑
          if (room.projectId === Number(id)) {
            map[room.developerId] = room.id;
          }
        });
        setExistingChatRooms(map);
      })
      .catch(() => {});
  }, [id, isLoggedIn, userRole]);

  // 제안서 제출
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
      setProposalForm({
        coverLetter: "",
        proposedBudget: "",
        proposedDuration: "",
      });
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message || "제안서 제출에 실패했습니다."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // 제안서 상태 변경 (수락/거절)
  const handleProposalStatusChange = async (
    proposalId: number,
    status: "ACCEPTED" | "REJECTED"
  ) => {
    const action = status === "ACCEPTED" ? "수락" : "거절";
    if (!confirm(`이 제안서를 ${action}하시겠습니까?`)) return;
    try {
      await proposalApi.updateStatus(proposalId, status);
      toast.success(`제안서를 ${action}했습니다.`);
      const res = await proposalApi.getByProject(Number(id), 0, 20);
      setProposals(res.data.data.content);
      // 프로젝트 정보 갱신 (상태 변경 반영)
      const projRes = await projectApi.getById(Number(id));
      setProject(projRes.data.data);
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message || `${action}에 실패했습니다.`
      );
    }
  };

  // ★ 채팅방 생성
  const handleCreateChatRoom = async (
    proposal: ProposalDto
  ) => {
    if (!project) return;
    setCreatingChatRoomFor(proposal.id);
    try {
      const res = await chatApi.createRoom(project.id, proposal.developerId);
      const newRoom: ChatRoomDto = res.data.data;
      setExistingChatRooms((prev) => ({
        ...prev,
        [proposal.developerId]: newRoom.id,
      }));
      toast.success(`${proposal.developerName}님과의 채팅방이 생성되었습니다!`);
    } catch (err: any) {
      const msg = err?.response?.data?.message;
      if (msg?.includes("이미 존재")) {
        toast.info("이미 채팅방이 존재합니다.");
        // 채팅방 목록 재조회
        chatApi.getMyRooms(0, 50).then((res) => {
          const rooms = res.data.data.content;
          const map: Record<number, number> = {};
          rooms.forEach((room) => {
            if (room.projectId === project.id) {
              map[room.developerId] = room.id;
            }
          });
          setExistingChatRooms(map);
        });
      } else {
        toast.error(msg || "채팅방 생성에 실패했습니다.");
      }
    } finally {
      setCreatingChatRoomFor(null);
    }
  };

  // 프로젝트 상태 변경
  const handleStatusChange = async () => {
    if (!project || !newStatus) return;
    setIsChangingStatus(true);
    try {
      const res = await projectApi.changeStatus(project.id, newStatus);
      setProject(res.data.data);
      toast.success(`프로젝트 상태가 '${STATUS_LABELS[newStatus]}'(으)로 변경되었습니다.`);
      setShowStatusModal(false);
      setNewStatus("");
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message || "상태 변경에 실패했습니다."
      );
    } finally {
      setIsChangingStatus(false);
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
  const nextOptions = NEXT_STATUS_OPTIONS[project.status] ?? [];

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
          {/* ── 메인 내용 ── */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <div className="flex items-center gap-3 mb-3 flex-wrap">
                  <Badge variant="secondary">{project.category}</Badge>
                  <span
                    className={`text-xs px-2 py-1 rounded-full font-medium ${
                      STATUS_COLORS[project.status] ??
                      "bg-gray-100 text-gray-600"
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
                        <Badge
                          key={skill}
                          variant="outline"
                          className="text-sm px-3 py-1"
                        >
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
                        {project.minBudget.toLocaleString()}~
                        {project.maxBudget.toLocaleString()}원
                      </p>
                    </div>
                    <div>
                      <span className="text-gray-500">기간</span>
                      <p className="font-medium">{project.duration}일</p>
                    </div>
                    <div>
                      <span className="text-gray-500">제안 현황</span>
                      <p className="font-medium">
                        {project.currentProposalCount} /{" "}
                        {project.maxProposalCount}개
                      </p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* ── CLIENT용: 받은 제안서 목록 ── */}
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
                      {proposals.map((proposal) => {
                        const statusInfo =
                          PROPOSAL_STATUS_LABELS[proposal.status] ?? {
                            label: proposal.status,
                            color: "bg-gray-100 text-gray-600",
                          };
                        const existingRoomId =
                          existingChatRooms[proposal.developerId];
                        const isCreating =
                          creatingChatRoomFor === proposal.id;

                        return (
                          <div
                            key={proposal.id}
                            className="border rounded-lg p-4"
                          >
                            <div className="flex justify-between items-start mb-2">
                              <div>
                                <Link
                                  to={`/developers/${proposal.developerId}`}
                                  className="font-medium hover:text-blue-600"
                                >
                                  {proposal.developerName}
                                </Link>
                                <p className="text-sm text-gray-500 mt-0.5">
                                  예산: {proposal.proposedBudget} / 기간:{" "}
                                  {proposal.proposedDuration}일
                                </p>
                              </div>
                              <span
                                className={`text-xs px-2 py-1 rounded-full font-medium ${statusInfo.color}`}
                              >
                                {statusInfo.label}
                              </span>
                            </div>
                            <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                              {proposal.coverLetter}
                            </p>

                            <div className="flex gap-2 flex-wrap">
                              {/* 수락/거절 버튼 (PENDING 상태일 때만) */}
                              {proposal.status === "pending" && (
                                <>
                                  <Button
                                    size="sm"
                                    onClick={() =>
                                      handleProposalStatusChange(
                                        proposal.id,
                                        "ACCEPTED"
                                      )
                                    }
                                  >
                                    <CheckCircle className="w-4 h-4 mr-1" />
                                    수락
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="destructive"
                                    onClick={() =>
                                      handleProposalStatusChange(
                                        proposal.id,
                                        "REJECTED"
                                      )
                                    }
                                  >
                                    <XCircle className="w-4 h-4 mr-1" />
                                    거절
                                  </Button>
                                </>
                              )}

                              {/* ★ 채팅방 생성/이동 버튼 (ACCEPTED 상태일 때만) */}
                              {proposal.status === "accepted" && (
                                <>
                                  {existingRoomId ? (
                                    <Link to="/chat">
                                      <Button
                                        size="sm"
                                        variant="outline"
                                        className="text-blue-600 border-blue-600"
                                      >
                                        <MessageCircle className="w-4 h-4 mr-1" />
                                        채팅 보러가기
                                      </Button>
                                    </Link>
                                  ) : (
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      className="text-green-600 border-green-600"
                                      onClick={() =>
                                        handleCreateChatRoom(proposal)
                                      }
                                      disabled={isCreating}
                                    >
                                      {isCreating ? (
                                        <Loader2 className="w-4 h-4 mr-1 animate-spin" />
                                      ) : (
                                        <MessageSquarePlus className="w-4 h-4 mr-1" />
                                      )}
                                      채팅방 생성
                                    </Button>
                                  )}
                                </>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </CardContent>
              </Card>
            )}
          </div>

          {/* ── 사이드바 ── */}
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
                      <Button
                        className="w-full"
                        onClick={() => setShowProposalModal(true)}
                      >
                        <Send className="w-4 h-4 mr-2" />
                        제안서 보내기
                      </Button>
                    ) : (
                      <div className="text-center">
                        <p className="text-sm text-gray-500 mb-2">
                          현재 모집 중인 프로젝트가 아닙니다.
                        </p>
                        <Badge variant="secondary">
                          {STATUS_LABELS[project.status]}
                        </Badge>
                      </div>
                    )}
                  </>
                )}

                {/* CLIENT: 프로젝트 상태 변경 버튼 */}
                {isLoggedIn && userRole === "CLIENT" && nextOptions.length > 0 && (
                  <Button
                    variant="outline"
                    className="w-full mt-3"
                    onClick={() => setShowStatusModal(true)}
                  >
                    <Settings className="w-4 h-4 mr-2" />
                    프로젝트 상태 변경
                  </Button>
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

            {/* 프로젝트 정보 요약 카드 */}
            <Card>
              <CardContent className="p-4 space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">상태</span>
                  <span
                    className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                      STATUS_COLORS[project.status]
                    }`}
                  >
                    {STATUS_LABELS[project.status]}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">예산</span>
                  <span className="font-medium text-blue-600">
                    {project.minBudget.toLocaleString()}~
                    {project.maxBudget.toLocaleString()}원
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">기간</span>
                  <span>{project.duration}일</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">제안 현황</span>
                  <span>
                    {project.currentProposalCount}/{project.maxProposalCount}개
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">카테고리</span>
                  <span>{project.category}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* ── 제안서 제출 모달 ── */}
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
                  setProposalForm({
                    ...proposalForm,
                    coverLetter: e.target.value,
                  })
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
                  setProposalForm({
                    ...proposalForm,
                    proposedBudget: e.target.value,
                  })
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label>제안 기간 * (일 단위)</Label>
              <Input
                type="number"
                placeholder="예: 45"
                min={1}
                value={proposalForm.proposedDuration}
                onChange={(e) =>
                  setProposalForm({
                    ...proposalForm,
                    proposedDuration: e.target.value,
                  })
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

      {/* ── 프로젝트 상태 변경 모달 ── */}
      <Dialog open={showStatusModal} onOpenChange={setShowStatusModal}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>프로젝트 상태 변경</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 mt-2">
            <p className="text-sm text-gray-600">
              현재 상태:{" "}
              <strong>{STATUS_LABELS[project.status]}</strong>
            </p>
            <div className="space-y-2">
              <Label>변경할 상태 선택</Label>
              <Select
                value={newStatus}
                onValueChange={setNewStatus}
              >
                <SelectTrigger>
                  <SelectValue placeholder="상태 선택" />
                </SelectTrigger>
                <SelectContent>
                  {nextOptions.map((s) => (
                    <SelectItem key={s} value={s}>
                      {STATUS_LABELS[s]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-3">
              <Button
                className="flex-1"
                onClick={handleStatusChange}
                disabled={!newStatus || isChangingStatus}
              >
                {isChangingStatus ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  "변경"
                )}
              </Button>
              <Button
                variant="outline"
                onClick={() => {
                  setShowStatusModal(false);
                  setNewStatus("");
                }}
              >
                취소
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
