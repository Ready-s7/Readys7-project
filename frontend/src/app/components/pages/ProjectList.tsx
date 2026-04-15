/**
 * ProjectList.tsx 개선 사항
 *
 * [버그 수정]
 * - URL 쿼리파라미터(?categoryId=, ?search=)를 읽어서 필터에 반영
 * - Home.tsx에서 카테고리 클릭 시 해당 카테고리로 필터링되도록 연동
 *
 * [UX 개선]
 * - 검색어 입력 후 Enter 또는 검색 버튼 클릭 시 API 호출 (매 키 입력마다 X)
 * - 필터 변경 시 페이지 자동으로 0으로 리셋
 * - 검색 결과 없을 때 명확한 안내 메시지
 */
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

const STATUS_LABELS: Record<string, string> = {
  OPEN: "모집중",
  CLOSED: "마감",
  IN_PROGRESS: "진행중",
  COMPLETED: "완료",
  CANCELLED: "중단",
};

export function ProjectList() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);

  // URL 파라미터에서 초기값 읽기
  const [searchTerm, setSearchTerm] = useState(searchParams.get("search") ?? "");
  const [appliedSearch, setAppliedSearch] = useState(searchParams.get("search") ?? "");
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>(
    searchParams.get("categoryId") ?? "all"
  );
  const [selectedStatus, setSelectedStatus] = useState<string>("all");

  // 카테고리 목록 로드 (초기 1회)
  useEffect(() => {
    categoryApi.getAll().then((res) => {
      setCategories(res.data.data);
    });
  }, []);

  // 프로젝트 검색
  const fetchProjects = useCallback(async () => {
    setIsLoading(true);
    try {
      const params: any = { page: currentPage, size: 10 };
      if (selectedCategoryId !== "all") params.categoryId = Number(selectedCategoryId);
      if (selectedStatus !== "all") params.status = selectedStatus;

      const res = await projectApi.search(params);
      let content = res.data.data.content;

      // 클라이언트 사이드 제목/설명 검색 (백엔드 LIKE 검색이 없으므로)
      if (appliedSearch.trim()) {
        const lower = appliedSearch.toLowerCase();
        content = content.filter(
          (p) =>
            p.title.toLowerCase().includes(lower) ||
            p.description.toLowerCase().includes(lower)
        );
      }

      setProjects(content);
      setTotalPages(res.data.data.totalPages);
    } catch {
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

  const handleCategoryChange = (v: string) => {
    setSelectedCategoryId(v);
    setCurrentPage(0);
  };

  const handleStatusChange = (v: string) => {
    setSelectedStatus(v);
    setCurrentPage(0);
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl mb-2">프로젝트 찾기</h1>
          <p className="text-gray-600">당신에게 맞는 프로젝트를 찾아보세요</p>
        </div>

        {/* 필터 */}
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
                <Button onClick={handleSearch} variant="outline">
                  검색
                </Button>
              </div>
              <Select value={selectedCategoryId} onValueChange={handleCategoryChange}>
                <SelectTrigger>
                  <SelectValue placeholder="카테고리" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 카테고리</SelectItem>
                  {categories.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.icon} {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={selectedStatus} onValueChange={handleStatusChange}>
                <SelectTrigger>
                  <SelectValue placeholder="상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 상태</SelectItem>
                  {Object.entries(STATUS_LABELS).map(([key, label]) => (
                    <SelectItem key={key} value={key}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 검색 결과 요약 */}
        <div className="mb-4 text-gray-600 flex items-center gap-2">
          <span>총 {projects.length}개의 프로젝트</span>
          {appliedSearch && (
            <Badge variant="secondary" className="cursor-pointer" onClick={() => {
              setSearchTerm("");
              setAppliedSearch("");
            }}>
              "{appliedSearch}" ✕
            </Badge>
          )}
          {selectedCategoryId !== "all" && (
            <Badge variant="secondary" className="cursor-pointer" onClick={() => setSelectedCategoryId("all")}>
              {categories.find(c => String(c.id) === selectedCategoryId)?.name} ✕
            </Badge>
          )}
        </div>

        {isLoading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-4">
              {projects.map((project) => {
                const cat = categories.find((c) => c.name === project.category);
                return (
                  <Link key={project.id} to={`/projects/${project.id}`}>
                    <Card className="hover:shadow-lg transition-shadow">
                      <CardContent className="p-6">
                        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-3 flex-wrap">
                              <Badge variant="secondary">
                                {cat?.icon} {project.category}
                              </Badge>
                              <Badge variant="outline">
                                {STATUS_LABELS[project.status] ?? project.status}
                              </Badge>
                              <span className="text-sm text-gray-500">
                                {new Date(project.createdAt).toLocaleDateString("ko-KR")}
                              </span>
                            </div>
                            <h3 className="text-xl mb-2 hover:text-blue-600">
                              {project.title}
                            </h3>
                            <p className="text-gray-600 mb-4 line-clamp-2">
                              {project.description}
                            </p>
                            <div className="flex flex-wrap gap-2 mb-3">
                              {project.skills.map((skill) => (
                                <Badge key={skill} variant="outline">
                                  {skill}
                                </Badge>
                              ))}
                            </div>
                            <div className="flex items-center gap-4 text-sm text-gray-600 flex-wrap">
                              <span>클라이언트: {project.clientName}</span>
                              <span>
                                ⭐ {project.clientRating != null
                                  ? project.clientRating.toFixed(1)
                                  : "없음"}
                              </span>
                              <span>
                                제안: {project.currentProposalCount}/{project.maxProposalCount}개
                              </span>
                            </div>
                          </div>
                          <div className="md:text-right shrink-0">
                            <div className="text-xl text-blue-600 mb-1">
                              {project.minBudget.toLocaleString()}~
                              {project.maxBudget.toLocaleString()}원
                            </div>
                            <div className="text-gray-600 text-sm mb-4">
                              기간: {project.duration}일
                            </div>
                            <Button>제안하기</Button>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </Link>
                );
              })}
            </div>

            {projects.length === 0 && (
              <Card>
                <CardContent className="p-12 text-center text-gray-500">
                  <Filter className="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p className="text-lg mb-2">검색 결과가 없습니다.</p>
                  <p className="text-sm">검색어 또는 필터 조건을 변경해보세요.</p>
                </CardContent>
              </Card>
            )}

            {/* 페이지네이션 */}
            {totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-8">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage((p) => p - 1)}
                >
                  이전
                </Button>
                <span className="flex items-center px-4 text-sm text-gray-600">
                  {currentPage + 1} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
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
