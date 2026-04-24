/**
 * Home.tsx 개선 사항
 *
 * [버그 수정]
 * - 기존: mockData의 정적 데이터를 항상 보여줌.
 * - 수정: 실제 백엔드 API(/v1/projects, /v1/developers, /v1/categories)에서 데이터를 가져옴.
 * - API 실패 시 graceful fallback (빈 배열로 처리, 에러 토스트 없이 조용히 처리).
 *
 * [UX 개선]
 * - 로딩 스피너 표시
 * - 데이터 없을 때 빈 섹션 안 보이도록 처리
 */
import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router";
import { Button } from "../ui/button";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Search, ArrowRight, Star, Users, Briefcase, Shield, Loader2 } from "lucide-react";
import { Input } from "../ui/input";
import { projectApi, developerApi, categoryApi, searchApi } from "../../../api/apiService";
import type { ProjectDto, DeveloperDto, CategoryDto, PopularRankingResponseDto } from "../../../api/types";

const STATUS_LABELS: Record<string, string> = {
  OPEN: "모집중",
  CLOSED: "마감",
  IN_PROGRESS: "진행중",
  COMPLETED: "완료",
  CANCELLED: "중단",
};

export function Home() {
  const navigate = useNavigate();
  const [featuredProjects, setFeaturedProjects] = useState<ProjectDto[]>([]);
  const [topDevelopers, setTopDevelopers] = useState<DeveloperDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [popularKeywords, setPopularKeywords] = useState<PopularRankingResponseDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    const fetchAll = async () => {
      setIsLoading(true);
      try {
        const [projectRes, developerRes, categoryRes, searchRes] = await Promise.allSettled([
          projectApi.search({ page: 0, size: 3 }),
          developerApi.search({ page: 0, size: 4 }),
          categoryApi.getAll(),
          searchApi.getPopularRanking(10),
        ]);

        if (projectRes.status === "fulfilled") setFeaturedProjects(projectRes.value.data.data?.content ?? []);
        if (developerRes.status === "fulfilled") setTopDevelopers(developerRes.value.data.data?.content ?? []);
        if (categoryRes.status === "fulfilled") setCategories(categoryRes.value.data.data ?? []);
        if (searchRes.status === "fulfilled") setPopularKeywords(searchRes.value.data.data ?? []);
      } finally {
        setIsLoading(false);
      }
    };
    fetchAll();
  }, []);

  const handleSearch = (e?: React.FormEvent, keyword?: string) => {
    if (e) e.preventDefault();
    const finalKeyword = keyword || searchTerm;
    if (finalKeyword.trim()) {
      navigate(`/search?keyword=${encodeURIComponent(finalKeyword.trim())}`);
    }
  };

  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-50 to-indigo-50 py-20">
        <div className="container mx-auto px-4">
          <div className="max-w-3xl mx-auto text-center">
            <h1 className="text-4xl md:text-5xl mb-6 font-bold text-gray-900 leading-tight">
              전문 개발자와 함께<br />프로젝트를 완성하세요
            </h1>
            <p className="text-xl text-gray-600 mb-10">
              검증된 개발자들과 함께 당신의 아이디어를 현실로 만들어보세요
            </p>

            {/* Search Bar Container */}
            <div className="max-w-2xl mx-auto mb-16">
              <form onSubmit={handleSearch} className="bg-white rounded-2xl shadow-xl p-2 flex flex-col md:flex-row gap-2 mb-6">
                <div className="flex-1 relative">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input
                    placeholder="기술 스택, 프로젝트 제목 등으로 검색해보세요"
                    className="pl-12 border-0 focus-visible:ring-0 text-lg h-12"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
                <Button type="submit" size="lg" className="md:w-auto w-full h-12 px-8 bg-blue-600 hover:bg-blue-700 rounded-xl">
                  검색하기
                </Button>
              </form>

              {/* Popular Keywords */}
              {popularKeywords.length > 0 && (
                <div className="flex flex-wrap items-center justify-center gap-3">
                  <span className="text-sm text-gray-500 font-medium">인기 검색어:</span>
                  {popularKeywords.map((item, idx) => (
                    <button
                      key={item.keyword}
                      onClick={() => handleSearch(undefined, item.keyword)}
                      className="text-sm bg-white/50 hover:bg-white px-3 py-1 rounded-full border border-gray-200 transition-all text-gray-700"
                    >
                      <span className="text-blue-500 font-bold mr-1">{idx + 1}</span>
                      {item.keyword}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Stats */}
            <div className="grid grid-cols-3 gap-8 max-w-2xl mx-auto">
              <div>
                <div className="text-3xl mb-2 font-bold">1,000+</div>
                <div className="text-gray-600">전문 개발자</div>
              </div>
              <div>
                <div className="text-3xl mb-2 font-bold">5,000+</div>
                <div className="text-gray-600">완료된 프로젝트</div>
              </div>
              <div>
                <div className="text-3xl mb-2 font-bold">4.8★</div>
                <div className="text-gray-600">평균 만족도</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Categories */}
      {categories.length > 0 && (
        <section className="py-16">
          <div className="container mx-auto px-4">
            <h2 className="text-3xl text-center mb-12 font-bold">카테고리별 전문가 찾기</h2>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              {categories.map((category) => (
                <Link key={category.id} to={`/projects?categoryId=${category.id}`}>
                  <Card className="hover:shadow-lg transition-shadow cursor-pointer">
                    <CardContent className="p-6 text-center">
                      <div className="text-4xl mb-3">{category.icon ?? "📦"}</div>
                      <div className="text-sm font-medium">{category.name}</div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Featured Projects */}
      <section className="py-16 bg-gray-50">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold">인기 프로젝트</h2>
            <Link to="/projects">
              <Button variant="ghost">
                전체보기 <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : featuredProjects.length === 0 ? (
            <p className="text-center text-gray-500 py-8">등록된 프로젝트가 없습니다.</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {featuredProjects.map((project) => (
                <Link key={project.id} to={`/projects/${project.id}`}>
                  <Card className="hover:shadow-lg transition-shadow h-full">
                    <CardContent className="p-6">
                      <div className="flex justify-between items-start mb-3">
                        <Badge variant="secondary">{project.category}</Badge>
                        <div className="flex gap-1">
                          <Badge variant="outline" className="text-xs">
                            {STATUS_LABELS[project.status] ?? project.status}
                          </Badge>
                        </div>
                      </div>
                      <h3 className="text-xl mb-3 line-clamp-1 font-bold">{project.title}</h3>
                      <p className="text-gray-600 mb-4 line-clamp-2">{project.description}</p>
                      <div className="flex flex-wrap gap-2 mb-4">
                        {(project.skills || []).slice(0, 3).map((skill) => (
                          <Badge key={skill} variant="outline">{skill}</Badge>
                        ))}
                        {(project.skills?.length ?? 0) > 3 && (
                          <Badge variant="outline">+{(project.skills?.length ?? 0) - 3}</Badge>
                        )}
                      </div>
                      <div className="flex justify-between items-center pt-4 border-t">
                        <span className="text-blue-600 text-sm font-bold">
                          {(project.minBudget ?? 0).toLocaleString()}~{(project.maxBudget ?? 0).toLocaleString()}원
                        </span>
                        <span className="text-sm text-gray-500">
                          제안 {project.currentProposalCount}개
                        </span>
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Top Developers */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold">추천 개발자</h2>
            <Link to="/developers">
              <Button variant="ghost">
                전체보기 <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : topDevelopers.length === 0 ? (
            <p className="text-center text-gray-500 py-8">등록된 개발자가 없습니다.</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {topDevelopers.map((developer) => (
                <Link key={developer.id} to={`/developers/${developer.id}`}>
                  <Card className="hover:shadow-lg transition-shadow h-full">
                    <CardContent className="p-6 text-center">
                      <div className="w-20 h-20 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-4 text-3xl font-bold text-blue-600">
                        {developer.name[0]}
                      </div>
                      <h3 className="text-lg mb-1 font-bold">{developer.name}</h3>
                      <p className="text-gray-600 text-sm mb-3">{developer.title}</p>
                      <div className="flex items-center justify-center gap-1 mb-3">
                        <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                        <span className="font-bold">{developer.rating?.toFixed(1)}</span>
                        <span className="text-gray-500 text-sm">({developer.reviewCount})</span>
                      </div>
                      <div className="flex flex-wrap gap-1 justify-center mb-3">
                        {(developer.skills ?? []).slice(0, 3).map((skill) => (
                          <Badge key={skill} variant="outline" className="text-[10px]">{skill}</Badge>
                        ))}
                      </div>
                      <p className="text-blue-600 text-sm font-bold">
                        {(developer.minHourlyPay ?? 0).toLocaleString()}~{(developer.maxHourlyPay ?? 0).toLocaleString()}원/시간
                      </p>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Features */}
      <section className="py-16 bg-gray-50">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl text-center mb-12 font-bold">왜 Ready's7인가요?</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2 font-bold">검증된 전문가</h3>
              <p className="text-gray-600">철저한 검증을 거친 전문 개발자들만 활동합니다</p>
            </div>
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Shield className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2 font-bold">안전한 거래</h3>
              <p className="text-gray-600">에스크로 시스템으로 안전한 대금 지급을 보장합니다</p>
            </div>
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Briefcase className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2 font-bold">다양한 프로젝트</h3>
              <p className="text-gray-600">웹, 앱, AI 등 모든 분야의 프로젝트를 찾을 수 있습니다</p>
            </div>
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Star className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2 font-bold">높은 만족도</h3>
              <p className="text-gray-600">평균 4.8점의 높은 고객 만족도를 자랑합니다</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-24 bg-[#111318] border-t border-white/5 text-white relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-full bg-[radial-gradient(circle_at_50%_50%,rgba(0,209,255,0.03),transparent)] pointer-events-none" />
        <div className="container mx-auto px-4 text-center relative z-10">
          <h2 className="text-3xl md:text-4xl mb-6 font-bold tracking-tight">지금 바로 시작하세요</h2>
          <p className="text-xl mb-10 text-gray-400 max-w-2xl mx-auto">
            당신의 프로젝트를 성공으로 이끌어줄 최고의 개발자를 만나보세요
          </p>
          <div className="flex gap-4 justify-center flex-wrap">
            <Link to="/projects/new">
              <Button size="lg" className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold px-8 rounded-xl shadow-lg shadow-primary/10">
                프로젝트 등록하기
              </Button>
            </Link>
            <Link to="/developers">
              <Button
                size="lg"
                variant="outline"
                className="bg-transparent border-white/20 text-white hover:bg-white/5 font-bold px-8 rounded-xl"
              >
                개발자 찾아보기
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
