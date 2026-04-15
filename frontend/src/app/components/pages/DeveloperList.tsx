/**
 * DeveloperList.tsx 수정 사항
 *
 * [버그 수정]
 * 1. 스킬 필터 선택 후 목록에서 조회된 개발자의 스킬만 뜨는 버그 수정
 *    → 원인: allSkills를 매 렌더링 시 `developers` 상태에서 추출하여
 *            필터 후 developers가 바뀌면 allSkills도 바뀜
 *    → 해결: allSkills를 초기 로드 시 skillApi로 한 번만 가져와서 고정
 *
 * 2. 스킬 API가 없거나 빈 경우 개발자 목록의 스킬로 fallback 처리
 */
import { useState, useEffect, useCallback, useRef } from "react";
import { Link } from "react-router";
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
import { Search, Star, Clock, Loader2 } from "lucide-react";
import { developerApi, skillApi } from "../../../api/apiService";
import type { DeveloperDto } from "../../../api/types";

export function DeveloperList() {
  const [developers, setDevelopers] = useState<DeveloperDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [searchTerm, setSearchTerm] = useState("");
  const [selectedSkill, setSelectedSkill] = useState("all");
  const [minRating, setMinRating] = useState<string>("all");

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // ★ 핵심 수정: allSkills는 초기 1회만 로드하여 고정
  const [allSkills, setAllSkills] = useState<string[]>([]);
  const skillsInitialized = useRef(false);

  // 스킬 목록 최초 1회 로드 (필터 변경에 영향 받지 않음)
  useEffect(() => {
    if (skillsInitialized.current) return;
    skillsInitialized.current = true;

    skillApi
      .getAll(0, 200)
      .then((res) => {
        const names = res.data.data.content.map((s) => s.name);
        setAllSkills(names);
      })
      .catch(() => {
        // skillApi 실패 시 첫 번째 개발자 조회 후 스킬 추출로 fallback
        developerApi.getAll(0, 100).then((res) => {
          const skills = Array.from(
            new Set(res.data.data.content.flatMap((d) => d.skills ?? []))
          ).sort();
          setAllSkills(skills);
        });
      });
  }, []);

  const fetchDevelopers = useCallback(async () => {
    setIsLoading(true);
    try {
      const params: any = { page: currentPage, size: 12 };
      if (selectedSkill !== "all") params.skills = [selectedSkill];
      if (minRating !== "all") params.minRating = Number(minRating);

      const res = await developerApi.search(params);
      setDevelopers(res.data.data.content);
      setTotalPages(res.data.data.totalPages);
    } catch {
      setDevelopers([]);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, selectedSkill, minRating]);

  useEffect(() => {
    fetchDevelopers();
  }, [fetchDevelopers]);

  // 클라이언트 사이드 이름/제목 검색
  const filtered = developers.filter(
    (d) =>
      d.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      d.title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl mb-2">개발자 찾기</h1>
          <p className="text-gray-600">검증된 전문 개발자들을 만나보세요</p>
        </div>

        {/* 필터 */}
        <Card className="mb-6">
          <CardContent className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="relative md:col-span-2">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="이름 또는 직군 검색..."
                  className="pl-10"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Select
                value={selectedSkill}
                onValueChange={(v) => {
                  setSelectedSkill(v);
                  setCurrentPage(0);
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="기술" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 기술</SelectItem>
                  {/* ★ allSkills는 고정 목록 사용 (developers 상태와 무관) */}
                  {allSkills.map((skill) => (
                    <SelectItem key={skill} value={skill}>
                      {skill}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select
                value={minRating}
                onValueChange={(v) => {
                  setMinRating(v);
                  setCurrentPage(0);
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="최소 평점" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체</SelectItem>
                  <SelectItem value="4.5">4.5점 이상</SelectItem>
                  <SelectItem value="4.0">4.0점 이상</SelectItem>
                  <SelectItem value="3.5">3.5점 이상</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        <div className="mb-4 text-gray-600">총 {filtered.length}명의 개발자</div>

        {isLoading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filtered.map((dev) => (
                <Link key={dev.id} to={`/developers/${dev.id}`}>
                  <Card className="hover:shadow-lg transition-shadow h-full">
                    <CardContent className="p-6">
                      <div className="text-center mb-4">
                        <div className="w-20 h-20 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-3 text-2xl">
                          {dev.name[0]}
                        </div>
                        <h3 className="text-xl mb-1">{dev.name}</h3>
                        <p className="text-gray-600 text-sm mb-2">{dev.title}</p>
                        <div className="flex items-center justify-center gap-1">
                          <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                          <span>{dev.rating?.toFixed(1)}</span>
                          <span className="text-gray-500 text-sm">
                            ({dev.reviewCount}개 리뷰)
                          </span>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-2 mb-3 text-sm text-gray-600">
                        {dev.responseTime && (
                          <div className="flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {dev.responseTime}
                          </div>
                        )}
                        <div className="flex items-center gap-1">
                          <span>완료: {dev.completedProjects}개</span>
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-1 mb-4">
                        {(dev.skills ?? []).slice(0, 4).map((skill) => (
                          <Badge
                            key={skill}
                            variant="outline"
                            className="text-xs"
                          >
                            {skill}
                          </Badge>
                        ))}
                        {(dev.skills?.length ?? 0) > 4 && (
                          <Badge variant="outline" className="text-xs">
                            +{dev.skills.length - 4}
                          </Badge>
                        )}
                      </div>

                      <div className="pt-4 border-t flex justify-between items-center text-sm">
                        <div>
                          <span className="text-gray-500">시급</span>
                          <p className="text-blue-600">
                            {dev.minHourlyPay?.toLocaleString()}~
                            {dev.maxHourlyPay?.toLocaleString()}원
                          </p>
                        </div>
                        <Badge
                          variant={
                            dev.availableForWork ? "default" : "secondary"
                          }
                          className="text-xs"
                        >
                          {dev.availableForWork ? "작업 가능" : "작업 중"}
                        </Badge>
                      </div>
                      <Button className="w-full mt-3">프로필 보기</Button>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>

            {filtered.length === 0 && (
              <Card>
                <CardContent className="p-12 text-center text-gray-500">
                  <Search className="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p>검색 결과가 없습니다.</p>
                </CardContent>
              </Card>
            )}

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
