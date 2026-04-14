/**
 * MyProposals.tsx
 * - DEVELOPER: 내가 제출한 제안서 목록 + 철회 기능
 * - CLIENT: 내 프로젝트별 수신 제안서는 ProjectDetail에서 확인
 */
import { useState, useEffect } from "react";
import { Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Loader2, FileText } from "lucide-react";
import { toast } from "sonner";
import { proposalApi } from "../../../api/apiService";
import type { ProposalDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const STATUS_LABELS: Record<string, { label: string; variant: "default" | "secondary" | "outline" | "destructive" }> = {
  pending:   { label: "검토 중",  variant: "default" },
  accepted:  { label: "수락됨",   variant: "default" },
  rejected:  { label: "거절됨",   variant: "destructive" },
  withdrawn: { label: "철회됨",   variant: "secondary" },
};

export function MyProposals() {
  const { isLoggedIn, userRole } = useAuth();
  const [proposals, setProposals] = useState<ProposalDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchProposals = async (page = 0) => {
    setIsLoading(true);
    try {
      const res = await proposalApi.getMyProposals(page, 10);
      setProposals(res.data.data.content);
      setTotalPages(res.data.data.totalPages);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isLoggedIn && userRole === "DEVELOPER") fetchProposals(currentPage);
  }, [isLoggedIn, userRole, currentPage]);

  const handleWithdraw = async (proposalId: number) => {
    if (!confirm("제안서를 철회하시겠습니까?")) return;
    try {
      await proposalApi.updateStatus(proposalId, "WITHDRAWN");
      toast.success("제안서가 철회되었습니다.");
      fetchProposals(currentPage);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "철회에 실패했습니다.");
    }
  };

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card><CardContent className="p-8 text-center">
          <p>로그인이 필요합니다.</p>
          <Link to="/login"><Button className="mt-4">로그인</Button></Link>
        </CardContent></Card>
      </div>
    );
  }

  if (userRole !== "DEVELOPER") {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card><CardContent className="p-8 text-center">
          <p>개발자 계정만 이용 가능합니다.</p>
        </CardContent></Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-3xl">
        <h1 className="text-3xl mb-6">내 제안서 목록</h1>

        {isLoading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          </div>
        ) : proposals.length === 0 ? (
          <Card>
            <CardContent className="p-12 text-center text-gray-500">
              <FileText className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p>제출한 제안서가 없습니다.</p>
              <Link to="/projects" className="mt-4 inline-block">
                <Button variant="outline">프로젝트 찾기</Button>
              </Link>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-4">
            {proposals.map((p) => {
              const statusInfo = STATUS_LABELS[p.status] ?? { label: p.status, variant: "outline" as const };
              return (
                <Card key={p.id}>
                  <CardContent className="p-6">
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <Link to={`/projects/${p.projectId}`}>
                          <h3 className="font-medium text-lg hover:text-blue-600">{p.projectTitle}</h3>
                        </Link>
                        <p className="text-sm text-gray-500 mt-1">
                          {new Date(p.createdAt).toLocaleDateString("ko-KR")} 제출
                        </p>
                      </div>
                      <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                    </div>

                    <p className="text-gray-600 text-sm mb-4 line-clamp-2">{p.coverLetter}</p>

                    <div className="flex gap-4 text-sm text-gray-600 mb-4">
                      <span>제안 예산: {p.proposedBudget}</span>
                      <span>제안 기간: {p.proposedDuration}</span>
                    </div>

                    {p.status === "pending" && (
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-red-600 hover:bg-red-50"
                        onClick={() => handleWithdraw(p.id)}
                      >
                        제안서 철회
                      </Button>
                    )}
                  </CardContent>
                </Card>
              );
            })}

            {totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-6">
                <Button variant="outline" size="sm" disabled={currentPage === 0}
                  onClick={() => setCurrentPage((p) => p - 1)}>이전</Button>
                <span className="flex items-center px-4 text-sm">{currentPage + 1} / {totalPages}</span>
                <Button variant="outline" size="sm" disabled={currentPage >= totalPages - 1}
                  onClick={() => setCurrentPage((p) => p + 1)}>다음</Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
