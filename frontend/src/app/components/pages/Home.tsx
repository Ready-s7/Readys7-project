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
      <section className="bg-background py-20 relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-full bg-[radial-gradient(circle_at_50%_0%,rgba(0,209,255,0.05),transparent)] pointer-events-none" />
        <div className="container mx-auto px-4 relative z-10">
          <div className="max-w-3xl mx-auto text-center">
            <h1 className="text-4xl md:text-5xl mb-6 font-bold text-foreground leading-tight">
              전문 개발자와 함께<br />프로젝트를 완성하세요
            </h1>
            <p className="text-xl text-muted-foreground mb-10">
              검증된 개발자들과 함께 당신의 아이디어를 현실로 만들어보세요
            </p>

            {/* Search Bar Container */}
            <div className="max-w-2xl mx-auto mb-16">
              <form onSubmit={handleSearch} className="bg-card border border-border rounded-2xl shadow-xl p-2 flex flex-col md:flex-row gap-2 mb-6">
                <div className="flex-1 relative">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    placeholder="기술 스택, 프로젝트 제목 등으로 검색해보세요"
                    className="pl-12 border-0 focus-visible:ring-0 text-lg h-12 bg-transparent"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
                <Button type="submit" size="lg" className="md:w-auto w-full h-12 px-8 bg-primary hover:bg-primary/90 text-primary-foreground rounded-xl">
                  검색하기
                </Button>
              </form>

              {/* Popular Keywords */}
              {popularKeywords.length > 0 && (
                <div className="flex flex-wrap items-center justify-center gap-3">
                  <span className="text-sm text-muted-foreground font-medium">인기 검색어:</span>
                  {popularKeywords.map((item, idx) => (
                    <button
                      key={item.keyword}
                      onClick={() => handleSearch(undefined, item.keyword)}
                      className="text-sm bg-secondary/50 hover:bg-secondary px-3 py-1 rounded-full border border-border transition-all text-foreground"
                    >
                      <span className="text-primary font-bold mr-1">{idx + 1}</span>
                      {item.keyword}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Stats */}
            <div className="grid grid-cols-3 gap-8 max-w-2xl mx-auto">
              <div>
                <div className="text-3xl mb-2 font-bold text-foreground">1,000+</div>
                <div className="text-muted-foreground">전문 개발자</div>
              </div>
              <div>
                <div className="text-3xl mb-2 font-bold text-foreground">5,000+</div>
                <div className="text-muted-foreground">완료된 프로젝트</div>
              </div>
              <div>
                <div className="text-3xl mb-2 font-bold text-foreground">4.8★</div>
                <div className="text-muted-foreground">평균 만족도</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Categories */}
      {categories.length > 0 && (
        <section className="py-16">
          <div className="container mx-auto px-4">
            <h2 className="text-3xl text-center mb-12 font-bold text-foreground">카테고리별 전문가 찾기</h2>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              {categories.map((category) => (
                <Link key={category.id} to={`/projects?categoryId=${category.id}`}>
                  <Card className="hover:border-primary/50 transition-colors cursor-pointer bg-card border-border">
                    <CardContent className="p-6 text-center">
                      <div className="text-4xl mb-3">{category.icon ?? "📦"}</div>
                      <div className="text-sm font-medium text-foreground">{category.name}</div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Featured Projects */}
      <section className="py-16 bg-secondary/30">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold text-foreground">인기 프로젝트</h2>
            <Link to="/projects">
              <Button variant="ghost" className="text-muted-foreground hover:text-foreground">
                전체보기 <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
            </div>
          ) : featuredProjects.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">등록된 프로젝트가 없습니다.</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {featuredProjects.map((project) => (
                <Link key={project.id} to={`/projects/${project.id}`}>
                  <Card className="hover:border-primary/50 transition-colors h-full bg-card border-border">
                    <CardContent className="p-6">
                      <div className="flex justify-between items-start mb-3">
                        <Badge variant="secondary" className="bg-secondary text-secondary-foreground">{project.category}</Badge>
                        <div className="flex gap-1">
                          <Badge variant="outline" className="text-xs border-border text-muted-foreground">
                            {STATUS_LABELS[project.status] ?? project.status}
                          </Badge>
                        </div>
                      </div>
                      <h3 className="text-xl mb-3 line-clamp-1 font-bold text-foreground">{project.title}</h3>
                      <p className="text-muted-foreground mb-4 line-clamp-2">{project.description}</p>
                      <div className="flex flex-wrap gap-2 mb-4">
                        {(project.skills || []).slice(0, 3).map((skill) => (
                          <Badge key={skill} variant="outline" className="border-border text-muted-foreground">{skill}</Badge>
                        ))}
                        {(project.skills?.length ?? 0) > 3 && (
                          <Badge variant="outline" className="border-border text-muted-foreground">+{(project.skills?.length ?? 0) - 3}</Badge>
                        )}
                      </div>
                      <div className="flex justify-between items-center pt-4 border-t border-border">
                        <span className="text-primary text-sm font-bold">
                          {(project.minBudget ?? 0).toLocaleString()}~{(project.maxBudget ?? 0).toLocaleString()}원
                        </span>
                        <span className="text-sm text-muted-foreground">
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
            <h2 className="text-3xl font-bold text-foreground">추천 개발자</h2>
            <Link to="/developers">
              <Button variant="ghost" className="text-muted-foreground hover:text-foreground">
                전체보기 <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
            </div>
          ) : topDevelopers.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">등록된 개발자가 없습니다.</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {topDevelopers.map((developer) => (
                <Link key={developer.id} to={`/developers/${developer.id}`}>
                  <Card className="hover:border-primary/50 transition-colors h-full bg-card border-border">
                    <CardContent className="p-6 text-center">
                      <div className="w-20 h-20 rounded-full bg-secondary flex items-center justify-center mx-auto mb-4 text-3xl font-bold text-primary">
                        {developer.name[0]}
                      </div>
                      <h3 className="text-lg mb-1 font-bold text-foreground">{developer.name}</h3>
                      <p className="text-muted-foreground text-sm mb-3">{developer.title}</p>
                      <div className="flex items-center justify-center gap-1 mb-3">
                        <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                        <span className="font-bold text-foreground">{developer.rating?.toFixed(1)}</span>
                        <span className="text-muted-foreground text-sm">({developer.reviewCount})</span>
                      </div>
                      <div className="flex flex-wrap gap-1 justify-center mb-3">
                        {(developer.skills ?? []).slice(0, 3).map((skill) => (
                          <Badge key={skill} variant="outline" className="text-[10px] border-border text-muted-foreground">{skill}</Badge>
                        ))}
                      </div>
                      <p className="text-primary text-sm font-bold">
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
      <section className="py-16 bg-secondary/30">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl text-center mb-12 font-bold text-foreground">왜 Ready's7인가요?</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="bg-secondary w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border border-border">
                <Users className="w-8 h-8 text-primary" />
              </div>
              <h3 className="text-xl mb-2 font-bold text-foreground">검증된 전문가</h3>
              <p className="text-muted-foreground">철저한 검증을 거친 전문 개발자들만 활동합니다</p>
            </div>
            <div className="text-center">
              <div className="bg-secondary w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border border-border">
                <Shield className="w-8 h-8 text-primary" />
              </div>
              <h3 className="text-xl mb-2 font-bold text-foreground">안전한 거래</h3>
              <p className="text-muted-foreground">에스크로 시스템으로 안전한 대금 지급을 보장합니다</p>
            </div>
            <div className="text-center">
              <div className="bg-secondary w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border border-border">
                <Briefcase className="w-8 h-8 text-primary" />
              </div>
              <h3 className="text-xl mb-2 font-bold text-foreground">다양한 프로젝트</h3>
              <p className="text-muted-foreground">웹, 앱, AI 등 모든 분야의 프로젝트를 찾을 수 있습니다</p>
            </div>
            <div className="text-center">
              <div className="bg-secondary w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border border-border">
                <Star className="w-8 h-8 text-primary" />
              </div>
              <h3 className="text-xl mb-2 font-bold text-foreground">높은 만족도</h3>
              <p className="text-muted-foreground">평균 4.8점의 높은 고객 만족도를 자랑합니다</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-24 bg-card border-t border-border text-foreground relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-full bg-[radial-gradient(circle_at_50%_50%,rgba(0,209,255,0.03),transparent)] pointer-events-none" />
        <div className="container mx-auto px-4 text-center relative z-10">
          <h2 className="text-3xl md:text-4xl mb-6 font-bold tracking-tight">지금 바로 시작하세요</h2>
          <p className="text-xl mb-10 text-muted-foreground max-w-2xl mx-auto">
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
                className="bg-transparent border-border text-foreground hover:bg-secondary font-bold px-8 rounded-xl"
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
