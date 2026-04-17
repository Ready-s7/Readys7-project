import { useState, useEffect, useCallback } from "react";
import { Link, useSearchParams } from "react-router";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { Search, Filter, Loader2 } from "lucide-react";
import { projectApi, categoryApi } from "../../../api/apiService";
import type { ProjectDto, CategoryDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

const STATUS_LABELS: Record<string, string> = {
  OPEN: "모집중",
  CLOSED: "마감",
  IN_PROGRESS: "진행중",
  COMPLETED: "완료",
  CANCELLED: "중단",
};

export function ProjectList() {
  const { userRole } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);

  const [searchTerm, setSearchTerm] = useState(searchParams.get("search") ?? "");
  const [appliedSearch, setAppliedSearch] = useState(searchParams.get("search") ?? "");
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>(
    searchParams.get("categoryId") ?? "all"
  );
  const [selectedStatus, setSelectedStatus] = useState<string>("all");

  useEffect(() => {
    categoryApi.getAll().then((res) => {
      // res.data.data가 배열인지, ['Type', Array] 인지 체크
      let rawData = res.data.data;
      if (Array.isArray(rawData) && rawData.length === 2 && typeof rawData[0] === 'string') {
        rawData = rawData[1];
      }
      setCategories(Array.isArray(rawData) ? rawData : []);
    }).catch(err => console.error("Categories load failed:", err));
  }, []);

  const fetchProjects = useCallback(async () => {
    setIsLoading(true);
    try {
      const params: any = { page: currentPage, size: 10 };
      if (appliedSearch.trim()) params.keyword = appliedSearch;
      if (selectedCategoryId !== "all") params.categoryId = Number(selectedCategoryId);
      if (selectedStatus !== "all") params.status = selectedStatus;

      const res = await projectApi.search(params);
      console.log("Raw API Response:", res.data);

      const responseBody = res.data;
      if (!responseBody || !responseBody.success) {
        setProjects([]);
        return;
      }

      let innerData = responseBody.data;
      // ['org.springframework.data.domain.PageImpl', { content: [...] }] 구조 대응
      if (Array.isArray(innerData) && innerData.length === 2 && typeof innerData[0] === 'string') {
        innerData = innerData[1];
      }

      if (innerData && Array.isArray(innerData.content)) {
        setProjects(innerData.content);
        setTotalPages(innerData.totalPages || 0);
      } else if (Array.isArray(innerData)) {
        setProjects(innerData);
        setTotalPages(1);
      } else {
        setProjects([]);
      }
    } catch (error) {
      console.error("Fetch projects error:", error);
      setProjects([]);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, selectedCategoryId, selectedStatus, appliedSearch]);

  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  const handleSearch = () => {
    setAppliedSearch(searchTerm);
    setCurrentPage(0);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") handleSearch();
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl mb-2 font-bold">프로젝트 찾기</h1>
          <p className="text-gray-600">당신에게 맞는 프로젝트를 찾아보세요</p>
        </div>

        <Card className="mb-6">
          <CardContent className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="relative md:col-span-2 flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input
                    placeholder="프로젝트 검색..."
                    className="pl-10"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyDown={handleKeyDown}
                  />
                </div>
                <Button onClick={handleSearch} variant="outline">검색</Button>
              </div>
              <Select value={selectedCategoryId} onValueChange={(v) => {setSelectedCategoryId(v); setCurrentPage(0);}}>
                <SelectTrigger><SelectValue placeholder="카테고리" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 카테고리</SelectItem>
                  {(categories || []).map((c) => (
                    <SelectItem key={c.id || c.name} value={String(c.id)}>{c.icon} {c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={selectedStatus} onValueChange={(v) => {setSelectedStatus(v); setCurrentPage(0);}}>
                <SelectTrigger><SelectValue placeholder="상태" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 상태</SelectItem>
                  {Object.entries(STATUS_LABELS).map(([key, label]) => (
                    <SelectItem key={key} value={key}>{label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        <div className="mb-4 text-gray-600 flex items-center gap-2">
          <span>총 {
            selectedStatus === "all"
                ? projects.filter(p => p.status === "OPEN").length
                : projects.length
          }개의 프로젝트</span>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-20"><Loader2 className="w-8 h-8 animate-spin text-blue-600" /></div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-4">
              {(projects || [])
                  .filter(p => selectedStatus !== "all" || p.status === "OPEN")
                  .map((project) => {
                if (!project || typeof project !== 'object') return null;
                const cat = (categories || []).find(
                  (c) => c.name && project.category && c.name.toLowerCase() === project.category.toLowerCase()
                );
                return (
                  <Link key={project.id} to={`/projects/${project.id}`}>
                    <Card className="hover:shadow-lg transition-shadow">
                      <CardContent className="p-6">
                        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-3 flex-wrap">
                              <Badge variant="secondary">{cat?.icon || "📦"} {project.category || "기타"}</Badge>
                              <Badge variant="outline">{STATUS_LABELS[project.status] ?? project.status ?? "모집중"}</Badge>
                              <span className="text-sm text-gray-500">
                                {project.createdAt ? new Date(project.createdAt).toLocaleDateString("ko-KR") : "-"}
                              </span>
                            </div>
                            <h3 className="text-xl mb-2 hover:text-blue-600 font-bold">{project.title}</h3>
                            <p className="text-gray-600 mb-4 line-clamp-2">{project.description}</p>
                            <div className="flex flex-wrap gap-2 mb-3">
                              {(project.skills || []).map((skill) => (
                                <Badge key={skill} variant="outline" className="bg-blue-50">{skill}</Badge>
                              ))}
                            </div>
                            <div className="flex items-center gap-4 text-sm text-gray-600 flex-wrap">
                              <span>👤 {project.clientName || "익명"}</span>
                              <span>⭐ {project.clientRating?.toFixed(1) || "0.0"}</span>
                              <span>📝 제안: {project.currentProposalCount || 0}/{project.maxProposalCount || 0}개</span>
                            </div>
                          </div>
                          <div className="md:text-right shrink-0">
                            <div className="text-xl text-blue-600 font-bold mb-1">
                              {(project.minBudget || 0).toLocaleString()} ~ {(project.maxBudget || 0).toLocaleString()}원
                            </div>
                            <div className="text-gray-600 text-sm mb-4">예상 기간: {project.duration || 0}일</div>
                            {project.status === "OPEN" && userRole === "DEVELOPER" && <Button className="w-full md:w-auto">제안하기</Button>}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </Link>
                );
              })}
            </div>

            {(!projects || projects.length === 0) && (
              <Card><CardContent className="p-12 text-center text-gray-500">
                <Filter className="w-12 h-12 mx-auto mb-4 opacity-50" /><p className="text-lg">검색 결과가 없습니다.</p>
              </CardContent></Card>
            )}

            {totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-8">
                <Button variant="outline" size="sm" disabled={currentPage === 0} onClick={() => setCurrentPage((p) => p - 1)}>이전</Button>
                <span className="flex items-center px-4 text-sm">{currentPage + 1} / {totalPages}</span>
                <Button variant="outline" size="sm" disabled={currentPage >= totalPages - 1} onClick={() => setCurrentPage((p) => p + 1)}>다음</Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
