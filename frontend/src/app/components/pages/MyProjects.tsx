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
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-4xl">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <FolderOpen className="w-7 h-7 text-blue-600" />
            <h1 className="text-3xl">{pageTitle}</h1>
          </div>
          {userRole === "CLIENT" && (
            <Link to="/projects/new">
              <Button>
                <Plus className="w-4 h-4 mr-2" />
                새 프로젝트 등록
              </Button>
            </Link>
          )}
        </div>

        {isLoading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          </div>
        ) : !projects || projects.length === 0 ? (
          <Card>
            <CardContent className="p-16 text-center text-gray-500">
              <Briefcase className="w-14 h-14 mx-auto mb-4 opacity-30" />
              <p className="text-lg mb-2">
                {userRole === "CLIENT"
                  ? "아직 등록한 프로젝트가 없습니다."
                  : "참여 중인 프로젝트가 없습니다."}
              </p>
              {userRole === "CLIENT" && (
                <Link to="/projects/new">
                  <Button className="mt-4">
                    <Plus className="w-4 h-4 mr-2" />
                    첫 프로젝트 등록하기
                  </Button>
                </Link>
              )}
              {userRole === "DEVELOPER" && (
                <Link to="/projects">
                  <Button variant="outline" className="mt-4">
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
                  <Card className="hover:shadow-md transition-shadow">
                    <CardContent className="p-6">
                      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-2 flex-wrap">
                            <span
                              className={`text-xs px-2 py-1 rounded-full font-medium ${
                                STATUS_COLORS[project.status] ??
                                "bg-gray-100 text-gray-600"
                              }`}
                            >
                              {STATUS_LABELS[project.status] ?? project.status}
                            </span>
                            <Badge variant="secondary" className="text-xs">
                              {project.category}
                            </Badge>
                          </div>
                          <h3 className="font-semibold text-lg mb-1">
                            {project.title}
                          </h3>
                          <p className="text-gray-600 text-sm line-clamp-1 mb-2">
                            {project.description}
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {(project.skills || []).slice(0, 4).map((skill) => (
                              <Badge
                                key={skill}
                                variant="outline"
                                className="text-xs"
                              >
                                {skill}
                              </Badge>
                            ))}
                            {(project.skills || []).length > 4 && (
                              <Badge variant="outline" className="text-xs">
                                +{(project.skills || []).length - 4}
                              </Badge>
                            )}
                          </div>                        </div>
                        <div className="text-right shrink-0">
                          <p className="text-blue-600 font-medium">
                            {(project.minBudget || 0).toLocaleString()}~
                            {(project.maxBudget || 0).toLocaleString()}원
                          </p>
                          <p className="text-sm text-gray-500">
                            기간: {project.duration || 0}일
                          </p>
                          {userRole === "CLIENT" && (
                            <p className="text-sm text-gray-500">
                              제안: {project.currentProposalCount || 0}/
                              {project.maxProposalCount || 0}개
                            </p>
                          )}
                          <p className="text-xs text-gray-400 mt-1">
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
              <div className="flex justify-center gap-2 mt-8">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage <= 1}
                  onClick={() => setCurrentPage((p) => p - 1)}
                >
                  이전
                </Button>
                <span className="flex items-center px-4 text-sm text-gray-600">
                  {currentPage} / {totalPage}
                </span>
                <Button
                  variant="outline"
                  size="sm"
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
