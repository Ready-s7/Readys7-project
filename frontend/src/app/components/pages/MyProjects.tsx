/**
 * MyProjects.tsx - 내 프로젝트 목록 (CLIENT: 등록한 프로젝트 / DEVELOPER: 참여 중인 프로젝트)
 * - CLIENT: GET /v1/clients/my-projects
 * - DEVELOPER: GET /v1/developers/me/my-projects
 */
import { useState, useEffect } from "react";
import { Link } from "react-router";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Loader2, Briefcase, Plus, FolderOpen } from "lucide-react";
import { clientApi, developerApi } from "../../../api/apiService";
import type { ProjectDto } from "../../../api/types";
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

export function MyProjects() {
  const { isLoggedIn, userRole } = useAuth();
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPage, setTotalPage] = useState(0);

  const fetchProjects = async (page: number) => {
    setIsLoading(true);
    try {
      if (userRole === "CLIENT") {
        const res = await clientApi.getMyProjects(page, 10);
        const data = res.data.data;
        setProjects(data.content);
        setTotalPage(data.totalPage);
      } else if (userRole === "DEVELOPER") {
        const res = await developerApi.getMyProjects(page - 1, 10);
        const data = res.data.data;
        setProjects(data.content || []);
        setTotalPage(data.totalPage);
      }
    } catch {
      setProjects([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isLoggedIn && (userRole === "CLIENT" || userRole === "DEVELOPER")) {
      fetchProjects(currentPage);
    }
  }, [isLoggedIn, userRole, currentPage]);

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card>
          <CardContent className="p-8 text-center">
            <p className="mb-4">로그인이 필요합니다.</p>
            <Link to="/login">
              <Button>로그인</Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    );
  }

  const pageTitle =
    userRole === "CLIENT" ? "등록한 프로젝트" : "참여 중인 프로젝트";

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-4xl">
        <div className="flex items-center justify-between mb-8 gap-4">
          <div className="flex items-center gap-3">
            <div className="bg-primary p-2 rounded-xl">
              <FolderOpen className="w-6 h-6 text-primary-foreground" />
            </div>
            <h1 className="text-3xl font-bold text-foreground">{pageTitle}</h1>
          </div>
          {userRole === "CLIENT" && (
            <Link to="/projects/new">
              <Button className="rounded-xl bg-primary hover:bg-primary/90 text-primary-foreground font-bold h-11 px-6 shadow-lg shadow-primary/20">
                <Plus className="w-4 h-4 mr-2" />
                새 프로젝트 등록
              </Button>
            </Link>
          )}
        </div>

        {isLoading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        ) : !projects || projects.length === 0 ? (
          <Card className="bg-card border-border border-dashed rounded-3xl">
            <CardContent className="p-16 text-center text-muted-foreground">
              <Briefcase className="w-16 h-16 mx-auto mb-4 opacity-20" />
              <p className="text-xl font-bold mb-6">
                {userRole === "CLIENT"
                  ? "아직 등록한 프로젝트가 없습니다."
                  : "참여 중인 프로젝트가 없습니다."}
              </p>
              {userRole === "CLIENT" && (
                <Link to="/projects/new">
                  <Button className="rounded-xl bg-primary hover:bg-primary/90 text-primary-foreground font-bold px-8 h-12">
                    <Plus className="w-4 h-4 mr-2" />
                    첫 프로젝트 등록하기
                  </Button>
                </Link>
              )}
              {userRole === "DEVELOPER" && (
                <Link to="/projects">
                  <Button variant="outline" className="rounded-xl border-border hover:bg-secondary font-bold px-8 h-12">
                    프로젝트 찾기
                  </Button>
                </Link>
              )}
            </CardContent>
          </Card>
        ) : (
          <>
            <div className="space-y-4">
              {projects.map((project) => (
                <Link key={project.id} to={`/projects/${project.id}`}>
                  <Card className="bg-card border-border hover:shadow-lg hover:shadow-primary/5 transition-all duration-300 rounded-2xl group overflow-hidden">
                    <CardContent className="p-6">
                      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-3 flex-wrap">
                            <Badge className={`${
                              STATUS_COLORS[project.status] ?? "bg-secondary text-muted-foreground"
                            } border-none font-bold px-2.5 py-0.5 rounded-lg text-[11px]`}>
                              {STATUS_LABELS[project.status] ?? project.status}
                            </Badge>
                            <Badge variant="secondary" className="bg-secondary/50 text-muted-foreground border-none px-2.5 py-0.5 rounded-lg text-[11px]">
                              {project.category}
                            </Badge>
                          </div>
                          <h3 className="font-bold text-xl text-foreground mb-2 group-hover:text-primary transition-colors">
                            {project.title}
                          </h3>
                          <p className="text-muted-foreground text-sm line-clamp-1 mb-4 leading-relaxed">
                            {project.description}
                          </p>
                          <div className="flex flex-wrap gap-1.5">
                            {(project.skills || []).slice(0, 4).map((skill) => (
                              <Badge
                                key={skill}
                                variant="outline"
                                className="text-[10px] bg-secondary/30 border-border text-muted-foreground px-2 py-0"
                              >
                                {skill}
                              </Badge>
                            ))}
                            {(project.skills || []).length > 4 && (
                              <Badge variant="outline" className="text-[10px] border-border text-muted-foreground px-2 py-0">
                                +{(project.skills || []).length - 4}
                              </Badge>
                            )}
                          </div>
                        </div>
                        <div className="flex flex-col md:items-end justify-center shrink-0 border-t md:border-t-0 md:border-l border-border pt-4 md:pt-0 md:pl-8">
                          <p className="text-primary font-black text-xl mb-1">
                            {(project.minBudget || 0).toLocaleString()}원 ~
                          </p>
                          <div className="flex items-center gap-3 text-sm text-muted-foreground font-medium">
                            <span>기간: {project.duration || 0}일</span>
                            {userRole === "CLIENT" && (
                              <>
                                <div className="w-1 h-1 rounded-full bg-border" />
                                <span>제안: {project.currentProposalCount || 0}개</span>
                              </>
                            )}
                          </div>
                          <p className="text-[11px] text-muted-foreground/50 mt-4">
                            {project.createdAt 
                              ? new Date(project.createdAt).toLocaleDateString("ko-KR")
                              : "-"}
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>

            {totalPage > 1 && (
              <div className="flex justify-center gap-3 mt-16">
                <Button 
                  variant="outline" 
                  className="rounded-xl h-10 px-6 font-bold border-border text-foreground hover:bg-secondary"
                  disabled={currentPage <= 1} 
                  onClick={() => setCurrentPage((p) => p - 1)}
                >
                  이전
                </Button>
                <div className="flex items-center gap-2">
                  {[...Array(totalPage)].map((_, i) => (
                    <button
                      key={i}
                      onClick={() => setCurrentPage(i + 1)}
                      className={`w-10 h-10 rounded-xl font-bold transition-all ${
                        currentPage === i + 1 
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
                  disabled={currentPage >= totalPage} 
                  onClick={() => setCurrentPage((p) => p + 1)}
                >
                  다음
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
