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
  PENDING:   { label: "검토 중",  variant: "default" },
  ACCEPTED:  { label: "수락됨",   variant: "default" },
  REJECTED:  { label: "거절됨",   variant: "destructive" },
  WITHDRAWN: { label: "철회됨",   variant: "secondary" },
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
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-3xl">
        <div className="flex items-center gap-3 mb-8">
          <div className="bg-primary p-2 rounded-xl">
            <FileText className="w-6 h-6 text-primary-foreground" />
          </div>
          <h1 className="text-3xl font-bold text-foreground">내 제안서 목록</h1>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        ) : !proposals || proposals.length === 0 ? (
          <Card className="bg-card border-border border-dashed rounded-3xl">
            <CardContent className="p-16 text-center text-muted-foreground">
              <FileText className="w-16 h-16 mx-auto mb-4 opacity-20" />
              <p className="text-xl font-bold mb-4">제출한 제안서가 없습니다.</p>
              <Link to="/projects">
                <Button variant="outline" className="rounded-xl border-border hover:bg-secondary font-bold px-6">프로젝트 찾기</Button>
              </Link>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-4">
            {proposals.map((p) => {
              const statusInfo = STATUS_LABELS[p.status] ?? { label: p.status, variant: "outline" as const };
              return (
                <Card key={p.id} className="bg-card border-border hover:shadow-md transition-all rounded-2xl group">
                  <CardContent className="p-6">
                    <div className="flex items-start justify-between mb-4">
                      <div>
                        <Link to={`/projects/${p.projectId}`}>
                          <h3 className="font-bold text-xl text-foreground group-hover:text-primary transition-colors">{p.projectTitle}</h3>
                        </Link>
                        <p className="text-xs text-muted-foreground mt-2 font-medium">
                          {new Date(p.createdAt).toLocaleDateString("ko-KR")} 제출
                        </p>
                      </div>
                      <Badge variant={statusInfo.variant} className={`font-bold px-3 py-1 rounded-lg ${
                        p.status === "PENDING" ? "bg-primary text-primary-foreground" :
                        p.status === "ACCEPTED" ? "bg-green-500 text-white" :
                        p.status === "REJECTED" ? "bg-destructive text-destructive-foreground" : "bg-secondary text-muted-foreground"
                      }`}>
                        {statusInfo.label}
                      </Badge>
                    </div>

                    <div className="bg-secondary/20 p-4 rounded-xl mb-4 border border-border/50">
                      <p className="text-muted-foreground text-sm line-clamp-2 leading-relaxed">{p.coverLetter}</p>
                    </div>

                    <div className="flex gap-6 text-sm mb-5 ml-1">
                      <div className="flex flex-col">
                        <span className="text-xs text-muted-foreground font-bold uppercase mb-1">제안 예산</span>
                        <span className="text-foreground font-bold">{Number(p.proposedBudget).toLocaleString()}원</span>
                      </div>
                      <div className="flex flex-col">
                        <span className="text-xs text-muted-foreground font-bold uppercase mb-1">제안 기간</span>
                        <span className="text-foreground font-bold">{p.proposedDuration}일</span>
                      </div>
                    </div>

                    {p.status === "PENDING" && (
                      <Button
                        variant="outline"
                        size="sm"
                        className="rounded-xl border-destructive/20 text-destructive hover:bg-destructive/10 font-bold h-10 px-5"
                        onClick={() => handleWithdraw(p.id)}
                      >
                        제안서 철회하기
                      </Button>
                    )}
                  </CardContent>
                </Card>
              );
            })}

            {totalPages > 1 && (
              <div className="flex justify-center gap-3 mt-16">
                <Button 
                  variant="outline" 
                  className="rounded-xl h-10 px-6 font-bold border-border text-foreground hover:bg-secondary"
                  disabled={currentPage === 0} 
                  onClick={() => setCurrentPage((p) => p - 1)}
                >
                  이전
                </Button>
                <div className="flex items-center gap-2">
                  {[...Array(totalPages)].map((_, i) => (
                    <button
                      key={i}
                      onClick={() => setCurrentPage(i)}
                      className={`w-10 h-10 rounded-xl font-bold transition-all ${
                        currentPage === i 
                          ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20 scale-110" 
                          : "bg-card text-muted-foreground hover:bg-secondary border border-border"
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
                <Button 
                  variant="outline" 
                  className="rounded-xl h-10 px-6 font-bold border-border text-foreground hover:bg-secondary"
                  disabled={currentPage >= totalPages - 1} 
                  onClick={() => setCurrentPage((p) => p + 1)}
                >
                  다음
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
