import { useParams, Link } from "react-router";
import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Textarea } from "../ui/textarea";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Separator } from "../ui/separator";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../ui/dialog";
import { ArrowLeft, Star, Loader2, Send } from "lucide-react";
import { toast } from "sonner";
import { projectApi, proposalApi } from "../../../api/apiService";
import type { ProjectDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const STATUS_LABELS: Record<string, string> = {
  OPEN: "모집중",
  CLOSED: "마감",
  IN_PROGRESS: "진행중",
  COMPLETED: "완료",
  CANCELLED: "중단",
};

export function ProjectDetail() {
  const { id } = useParams<{ id: string }>();
  const { isLoggedIn, userRole } = useAuth();

  const [project, setProject] = useState<ProjectDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showProposalModal, setShowProposalModal] = useState(false);
  const [proposalForm, setProposalForm] = useState({
    coverLetter: "",
    proposedBudget: "",
    proposedDuration: "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!id) return;
    setIsLoading(true);
    projectApi
      .getById(Number(id))
      .then((res) => setProject(res.data.data))
      .catch(() => setProject(null))
      .finally(() => setIsLoading(false));
  }, [id]);

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
                <div className="flex items-center gap-3 mb-3">
                  <Badge variant="secondary">{project.category}</Badge>
                  <Badge variant="outline">{STATUS_LABELS[project.status]}</Badge>
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
                    <p className="text-gray-600 leading-relaxed">{project.description}</p>
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
                      <p className="font-medium text-blue-600">
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
                    {project.clientRating?.toFixed(1) ?? "없음"}
                  </div>
                </div>
                {isLoggedIn && userRole === "DEVELOPER" && project.status === "OPEN" && (
                  <Button className="w-full" onClick={() => setShowProposalModal(true)}>
                    <Send className="w-4 h-4 mr-2" />
                    제안서 보내기
                  </Button>
                )}
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
                onChange={(e) => setProposalForm({ ...proposalForm, coverLetter: e.target.value })}
                required
              />
            </div>
            <div className="space-y-2">
              <Label>제안 예산 *</Label>
              <Input
                placeholder="예: 300만원 ~ 400만원"
                value={proposalForm.proposedBudget}
                onChange={(e) => setProposalForm({ ...proposalForm, proposedBudget: e.target.value })}
                required
              />
            </div>
            <div className="space-y-2">
              <Label>제안 기간 *</Label>
              <Input
                placeholder="예: 45일"
                value={proposalForm.proposedDuration}
                onChange={(e) => setProposalForm({ ...proposalForm, proposedDuration: e.target.value })}
                required
              />
            </div>
            <div className="flex gap-3 pt-2">
              <Button type="submit" className="flex-1" disabled={isSubmitting}>
                {isSubmitting ? "제출 중..." : "제안서 제출"}
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
