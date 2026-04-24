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
  const [selectedStatus, setSelectedStatus] = useState<string>("OPEN");

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

  useEffect(() => {
    setCurrentPage(0);
  }, [appliedSearch, selectedCategoryId, selectedStatus]);

  const fetchProjects = useCallback(async () => {
    setIsLoading(true);
    try {
      // 백엔드 ProjectController는 표준 0-indexed Pageable을 사용함
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
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl mb-2 font-bold text-foreground">프로젝트 찾기</h1>
          <p className="text-muted-foreground">당신에게 맞는 프로젝트를 찾아보세요</p>
        </div>

        <Card className="mb-6 bg-card border-border">
          <CardContent className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="relative md:col-span-2 flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                  <Input
                    placeholder="프로젝트 검색..."
                    className="pl-10 bg-secondary/50 border-border text-foreground"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyDown={handleKeyDown}
                  />
                </div>
                <Button onClick={handleSearch} variant="outline" className="border-border text-foreground hover:bg-secondary">검색</Button>
              </div>
              <Select value={selectedCategoryId} onValueChange={(v) => {setSelectedCategoryId(v); setCurrentPage(0);}}>
                <SelectTrigger className="bg-secondary/50 border-border text-foreground"><SelectValue placeholder="카테고리" /></SelectTrigger>
                <SelectContent className="bg-card border-border">
                  <SelectItem value="all">전체 카테고리</SelectItem>
                  {(categories || []).map((c) => (
                    <SelectItem key={c.id || c.name} value={String(c.id)}>{c.icon} {c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={selectedStatus} onValueChange={(v) => {setSelectedStatus(v); setCurrentPage(0);}}>
                <SelectTrigger className="bg-secondary/50 border-border text-foreground"><SelectValue placeholder="상태" /></SelectTrigger>
                <SelectContent className="bg-card border-border">
                  <SelectItem value="OPEN">모집중</SelectItem>
                  <SelectItem value="all">전체 상태</SelectItem>
                  {Object.entries(STATUS_LABELS)
                    .filter(([key]) => key !== "OPEN")
                    .map(([key, label]) => (
                      <SelectItem key={key} value={key}>{label}</SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        <div className="mb-4 text-muted-foreground flex items-center gap-2">
          <span>총 {projects.length}개의 프로젝트</span>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-20"><Loader2 className="w-8 h-8 animate-spin text-primary" /></div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-4">
              {(projects || []).map((project) => {
                if (!project || typeof project !== 'object') return null;
                const cat = (categories || []).find(
                  (c) => c.name && project.category && c.name.toLowerCase() === project.category.toLowerCase()
                );
                return (
                  <Link key={project.id} to={`/projects/${project.id}`}>
                    <Card className="hover:border-primary/50 transition-colors bg-card border-border">
                      <CardContent className="p-6">
                        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-3 flex-wrap">
                              <Badge variant="secondary" className="bg-secondary text-secondary-foreground border-none">{cat?.icon || "📦"} {project.category || "기타"}</Badge>
                              <Badge variant="outline" className="border-border text-muted-foreground">{STATUS_LABELS[project.status] ?? project.status ?? "모집중"}</Badge>
                              <span className="text-sm text-muted-foreground">
                                {project.createdAt ? new Date(project.createdAt).toLocaleDateString("ko-KR") : "-"}
                              </span>
                            </div>
                            <h3 className="text-xl mb-2 hover:text-primary font-bold text-foreground transition-colors">{project.title}</h3>
                            <p className="text-muted-foreground mb-4 line-clamp-2">{project.description}</p>
                            <div className="flex flex-wrap gap-2 mb-3">
                              {(project.skills || []).map((skill) => (
                                <Badge key={skill} variant="outline" className="bg-secondary/30 border-border text-muted-foreground">{skill}</Badge>
                              ))}
                            </div>
                            <div className="flex items-center gap-4 text-sm text-muted-foreground flex-wrap">
                              <span>👤 {project.clientName || "익명"}</span>
                              <span>⭐ {project.clientRating?.toFixed(1) || "0.0"}</span>
                              <span className="text-primary">📝 제안: {project.currentProposalCount || 0}/{project.maxProposalCount || 0}개</span>
                            </div>
                          </div>
                          <div className="md:text-right shrink-0">
                            <div className="text-xl text-primary font-bold mb-1">
                              {(project.minBudget || 0).toLocaleString()} ~ {(project.maxBudget || 0).toLocaleString()}원
                            </div>
                            <div className="text-muted-foreground text-sm mb-4">예상 기간: {project.duration || 0}일</div>
                            {project.status === "OPEN" && userRole === "DEVELOPER" && <Button className="w-full md:w-auto bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl">제안하기</Button>}
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </Link>
                );
              })}
            </div>

            {(!projects || projects.length === 0) && (
              <Card className="bg-card border-border"><CardContent className="p-12 text-center text-muted-foreground">
                <Filter className="w-12 h-12 mx-auto mb-4 opacity-50" /><p className="text-lg">검색 결과가 없습니다.</p>
              </CardContent></Card>
            )}

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
          </>
        )}
      </div>
    </div>
  );
}
