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

  const [allSkills, setAllSkills] = useState<string[]>([]);
  const skillsInitialized = useRef(false);

  useEffect(() => {
    if (skillsInitialized.current) return;
    skillsInitialized.current = true;

    skillApi.getAll(0, 200).then((res) => {
      let rawData = res.data.data;
      if (Array.isArray(rawData) && rawData.length === 2 && typeof rawData[0] === 'string') {
        rawData = rawData[1];
      }
      const content = (rawData && (rawData as any).content) || [];
      const names = content.map((s: any) => s.name);
      setAllSkills(names);
    }).catch(() => {
      developerApi.getAll(0, 100).then((res) => {
        let rawData = res.data.data;
        if (Array.isArray(rawData) && rawData.length === 2 && typeof rawData[0] === 'string') {
          rawData = rawData[1];
        }
        const content = (rawData && (rawData as any).content) || [];
        const skills = Array.from(new Set(content.flatMap((d: any) => d.skills ?? []))).sort() as string[];
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
      const responseBody = res.data;
      if (!responseBody || !responseBody.success) {
        setDevelopers([]);
        return;
      }

      let innerData = responseBody.data;
      if (Array.isArray(innerData) && innerData.length === 2 && typeof innerData[0] === 'string') {
        innerData = innerData[1];
      }

      if (innerData && Array.isArray(innerData.content)) {
        setDevelopers(innerData.content);
        setTotalPages(innerData.totalPages || 0);
      } else if (Array.isArray(innerData)) {
        setDevelopers(innerData);
        setTotalPages(1);
      } else {
        setDevelopers([]);
      }
    } catch (error) {
      console.error("Fetch developers error:", error);
      setDevelopers([]);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, selectedSkill, minRating]);

  useEffect(() => {
    fetchDevelopers();
  }, [fetchDevelopers]);

  const filtered = (developers || []).filter(
    (d) =>
      (d.name || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (d.title || "").toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl mb-2 font-bold">개발자 찾기</h1>
          <p className="text-gray-600">검증된 전문 개발자들을 만나보세요</p>
        </div>

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
              <Select value={selectedSkill} onValueChange={(v) => { setSelectedSkill(v); setCurrentPage(0); }}>
                <SelectTrigger><SelectValue placeholder="기술" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 기술</SelectItem>
                  {(allSkills || []).map((skill) => (
                    <SelectItem key={skill} value={skill}>{skill}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={minRating} onValueChange={(v) => { setMinRating(v); setCurrentPage(0); }}>
                <SelectTrigger><SelectValue placeholder="최소 평점" /></SelectTrigger>
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

        <div className="mb-4 text-gray-600">총 {filtered?.length || 0}명의 개발자</div>

        {isLoading ? (
          <div className="flex justify-center py-20"><Loader2 className="w-8 h-8 animate-spin text-blue-600" /></div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {(filtered || []).map((dev) => (
                <Link key={dev.id} to={`/developers/${dev.id}`}>
                  <Card className="hover:shadow-lg transition-shadow h-full">
                    <CardContent className="p-6">
                      <div className="text-center mb-4">
                        <div className="w-20 h-20 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-3 text-2xl font-bold text-blue-600">
                          {dev.name ? dev.name[0] : "?"}
                        </div>
                        <h3 className="text-xl mb-1 font-bold">{dev.name || "익명 개발자"}</h3>
                        <p className="text-gray-600 text-sm mb-2 line-clamp-1">{dev.title || "전문 개발자"}</p>
                        <div className="flex items-center justify-center gap-1">
                          <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                          <span className="font-bold">{dev.rating?.toFixed(1) || "0.0"}</span>
                          <span className="text-gray-500 text-sm">({dev.reviewCount || 0}개 리뷰)</span>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-2 mb-3 text-sm text-gray-600">
                        <div className="flex items-center gap-1"><Clock className="w-3 h-3" />{dev.responseTime || "24시간 이내"}</div>
                        <div className="flex items-center gap-1"><span>✅ 완료: {dev.completedProjects || 0}개</span></div>
                      </div>

                      <div className="flex flex-wrap gap-1 mb-4 h-20 overflow-hidden content-start">
                        {(dev.skills || []).slice(0, 4).map((skill) => (
                          <Badge key={skill} variant="outline" className="text-xs bg-gray-50">{skill}</Badge>
                        ))}
                        {(dev.skills?.length || 0) > 4 && (
                          <Badge variant="outline" className="text-xs">+{dev.skills.length - 4}</Badge>
                        )}
                      </div>

                      <div className="pt-4 border-t flex justify-between items-center text-sm">
                        <div>
                          <span className="text-gray-500 text-xs">예상 시급</span>
                          <p className="text-blue-600 font-bold">{(dev.minHourlyPay || 0).toLocaleString()} ~ {(dev.maxHourlyPay || 0).toLocaleString()}원</p>
                        </div>
                        <Badge variant={dev.availableForWork ? "default" : "secondary"} className="text-xs">
                          {dev.availableForWork ? "작업 가능" : "작업 중"}
                        </Badge>
                      </div>
                      <Button className="w-full mt-4">프로필 상세보기</Button>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>

            {(!filtered || filtered.length === 0) && (
              <Card><CardContent className="p-12 text-center text-gray-500">
                <Search className="w-12 h-12 mx-auto mb-4 opacity-50" /><p>검색 결과가 없습니다.</p>
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
