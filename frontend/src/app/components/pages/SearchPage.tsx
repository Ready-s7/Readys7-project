import { useState, useEffect } from "react";
import { useSearchParams, Link } from "react-router";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Search, Loader2, Briefcase, Tag, Wrench, Users, Star, User, Calendar, Target } from "lucide-react";
import { searchApi } from "../../../api/apiService";
import type { TotalSearchResponseDto } from "../../../api/types";

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get("keyword") || "";
  const pageParam = parseInt(searchParams.get("page") || "1");

  const [results, setResults] = useState<TotalSearchResponseDto | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [searchInput, setSearchInput] = useState(keyword);
  const [activeTab, setActiveTab] = useState("projects");

  useEffect(() => {
    if (keyword) {
      handleSearch(keyword, pageParam - 1);
    }
  }, [keyword, pageParam]);

  const handleSearch = async (term: string, page: number) => {
    setIsLoading(true);
    try {
      const res = await searchApi.getTotalSearch(term, page);
      setResults(res.data.data);
    } finally {
      setIsLoading(false);
    }
  };

  const onSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      setSearchParams({ keyword: searchInput.trim(), page: "1" });
    }
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams({ keyword, page: newPage.toString() });
    window.scrollTo(0, 0);
  };

  // 현재 활성화된 탭의 페이징 정보 추출
  const currentPageInfo = results ? (results as any)[activeTab] : null;
  const totalPages = currentPageInfo?.totalPages || 0;

  return (
    <div className="min-h-screen bg-background py-12">
      <div className="container mx-auto px-4 max-w-6xl">
        {/* Search Header */}
        <div className="mb-16 text-center">
          <div className="inline-flex items-center gap-2 bg-primary/10 px-4 py-1.5 rounded-full text-primary font-black text-[10px] uppercase tracking-[0.2em] mb-6 border border-primary/20">
            <Search className="w-3.5 h-3.5" /> Total Search
          </div>
          <h1 className="text-5xl font-black mb-8 text-foreground tracking-tight italic">통합 검색 센터</h1>
          
          <form onSubmit={onSearchSubmit} className="max-w-3xl mx-auto flex flex-col md:flex-row gap-3">
            <div className="flex-1 relative group">
              <Search className="absolute left-6 top-1/2 -translate-y-1/2 w-6 h-6 text-muted-foreground group-focus-within:text-primary transition-colors" />
              <Input
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="어떤 프로젝트나 전문가를 찾으시나요?"
                className="pl-16 h-18 rounded-[24px] border-border shadow-2xl focus-visible:ring-primary/20 bg-card text-foreground text-xl font-bold transition-all"
              />
            </div>
            <Button type="submit" size="lg" className="h-18 px-12 bg-primary hover:bg-primary/90 text-primary-foreground rounded-[24px] font-black text-xl shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-[0.98]">검색 실행</Button>
          </form>
          
          {keyword && !isLoading && (
            <p className="mt-8 text-muted-foreground font-medium text-lg">
              "<span className="text-primary font-black">{keyword}</span>" 키워드로 분석된 최적의 결과입니다.
            </p>
          )}
        </div>

        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-40 bg-card/30 rounded-[40px] border border-dashed border-border shadow-inner">
            <Loader2 className="w-16 h-16 animate-spin text-primary mb-6" />
            <p className="text-muted-foreground font-black tracking-widest uppercase text-sm animate-pulse">Analyzing Repository Data...</p>
          </div>
        ) : !results ? (
          <div className="text-center py-40 bg-secondary/10 rounded-[40px] border-2 border-dashed border-border">
            <Search className="w-24 h-24 mx-auto mb-8 opacity-5 text-primary" />
            <p className="text-xl font-bold text-muted-foreground">Ready's7의 모든 자원을 검색해 보세요.</p>
          </div>
        ) : (
          <Tabs defaultValue="projects" value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2 md:grid-cols-4 mb-12 bg-secondary/50 p-1.5 rounded-[24px] border border-border h-18">
              <TabsTrigger value="projects" className="flex gap-2 rounded-2xl data-[state=active]:bg-background data-[state=active]:text-primary data-[state=active]:shadow-xl font-black transition-all">
                <Briefcase className="w-4 h-4" /> 프로젝트 ({results.projects?.totalElements || 0})
              </TabsTrigger>
              <TabsTrigger value="developers" className="flex gap-2 rounded-2xl data-[state=active]:bg-background data-[state=active]:text-primary data-[state=active]:shadow-xl font-black transition-all">
                <Users className="w-4 h-4" /> 전문가 ({results.developers?.totalElements || 0})
              </TabsTrigger>
              <TabsTrigger value="categories" className="flex gap-2 rounded-2xl data-[state=active]:bg-background data-[state=active]:text-primary data-[state=active]:shadow-xl font-black transition-all">
                <Tag className="w-4 h-4" /> 카테고리 ({results.categories?.totalElements || 0})
              </TabsTrigger>
              <TabsTrigger value="skills" className="flex gap-2 rounded-2xl data-[state=active]:bg-background data-[state=active]:text-primary data-[state=active]:shadow-xl font-black transition-all">
                <Wrench className="w-4 h-4" /> 기술 스택 ({results.skills?.totalElements || 0})
              </TabsTrigger>
            </TabsList>

            <div className="mb-20">
              <TabsContent value="projects">
                {(!results.projects?.content || results.projects.content.length === 0) ? (
                  <NoResults message="일치하는 프로젝트를 찾지 못했습니다." />
                ) : (
                  <div className="grid grid-cols-1 gap-6">
                    {results.projects.content.map((p) => (
                      <Link key={p.id} to={`/projects/${p.id}`}>
                        <Card className="hover:border-primary/40 transition-all border border-border bg-card shadow-lg hover:shadow-2xl hover:shadow-primary/5 rounded-[32px] overflow-hidden group">
                          <CardContent className="p-8">
                            <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-6">
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-3 mb-4 flex-wrap">
                                  <Badge className="bg-primary/10 text-primary border-none font-black text-[10px] px-3 py-1 rounded-lg uppercase tracking-wider">{p.category || "General"}</Badge>
                                  <Badge variant="outline" className={`border-border font-bold rounded-lg text-[10px] ${p.status === 'OPEN' ? 'text-green-500' : 'text-muted-foreground'}`}>{p.status === "OPEN" ? "● 모집중" : p.status === "IN_PROGRESS" ? "○ 진행중" : "완료"}</Badge>
                                  <div className="w-1 h-1 rounded-full bg-border" />
                                  <span className="text-[11px] text-muted-foreground font-bold italic">
                                    {p.createdAt ? new Date(p.createdAt).toLocaleDateString() : ""}
                                  </span>
                                </div>
                                <h3 className="text-2xl font-black text-foreground mb-3 group-hover:text-primary transition-colors tracking-tight">{p.title}</h3>
                                <p className="text-muted-foreground text-sm mb-6 line-clamp-2 leading-relaxed font-medium">{p.description}</p>
                                <div className="flex flex-wrap gap-1.5 mb-6">
                                  {(p.skills || []).map(skill => (
                                    <Badge key={skill} variant="outline" className="bg-secondary/20 border-border text-muted-foreground text-[10px] px-2.5 py-0.5 font-bold rounded-md uppercase tracking-tight">
                                      {skill}
                                    </Badge>
                                  ))}
                                </div>
                                <div className="flex items-center gap-6 text-[11px] text-muted-foreground font-black uppercase tracking-widest border-t border-border/50 pt-4">
                                  <div className="flex items-center gap-1.5 hover:text-primary transition-colors cursor-default"><User className="w-3.5 h-3.5" /> {p.clientName || "Anonymous"}</div>
                                  <div className="flex items-center gap-1.5 text-yellow-500 bg-yellow-400/5 px-2 py-0.5 rounded-md border border-yellow-400/10"><Star className="w-3.5 h-3.5 fill-current" /> {typeof p.clientRating === 'number' ? p.clientRating.toFixed(1) : "0.0"}</div>
                                  <div className="flex items-center gap-1.5 text-primary bg-primary/5 px-2 py-0.5 rounded-md border border-primary/10"><Target className="w-3.5 h-3.5" /> PROPOSALS: {p.currentProposalCount || 0}</div>
                                </div>
                              </div>
                              <div className="md:text-right shrink-0 flex flex-col justify-between items-end min-w-[200px] border-t md:border-t-0 md:border-l border-border/50 pt-6 md:pt-0 md:pl-8">
                                <div className="mb-6 w-full">
                                  <p className="text-[10px] text-muted-foreground font-black uppercase tracking-[0.2em] mb-2">Estimated Budget</p>
                                  <div className="text-2xl font-black text-primary leading-tight">{(p.minBudget || 0).toLocaleString()}원 <span className="text-xs text-muted-foreground/40 font-bold block mt-1 md:inline md:mt-0">~ {(p.maxBudget || 0).toLocaleString()}원</span></div>
                                  <div className="text-[10px] text-muted-foreground font-black mt-3 flex items-center md:justify-end gap-1.5 uppercase tracking-widest"><Calendar className="w-3 h-3" /> PERIOD: {p.duration || 0} DAYS</div>
                                </div>
                                <Button className="w-full md:w-auto bg-foreground text-background hover:bg-foreground/90 font-black rounded-xl px-10 h-12 shadow-lg">상세 브리핑 보기</Button>
                              </div>
                            </div>
                          </CardContent>
                        </Card>
                      </Link>
                    ))}
                  </div>
                )}
              </TabsContent>

              <TabsContent value="developers">
                {(!results.developers?.content || results.developers.content.length === 0) ? (
                  <NoResults message="일치하는 개발자를 찾지 못했습니다." />
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {results.developers.content.map((dev) => (
                      <Link key={dev.id} to={`/developers/${dev.id}`}>
                        <Card className="hover:border-primary/40 transition-all border border-border bg-card shadow-lg hover:shadow-2xl hover:shadow-primary/5 rounded-[32px] overflow-hidden group h-full flex flex-col">
                          <CardContent className="p-8 flex-1">
                            <div className="flex items-center gap-5 mb-6">
                              <div className="w-16 h-16 rounded-[20px] bg-primary text-primary-foreground flex items-center justify-center text-2xl font-black shadow-lg shadow-primary/20">
                                {dev.name[0]}
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-1">
                                  <h3 className="text-xl font-black text-foreground group-hover:text-primary transition-colors truncate">{dev.name}</h3>
                                  <Badge className="bg-secondary text-muted-foreground border-none font-black text-[9px] px-2 py-0.5 rounded-md uppercase tracking-tighter shrink-0">{dev.participateType === "INDIVIDUAL" ? "Personal" : "Company"}</Badge>
                                </div>
                                <p className="text-primary font-bold text-sm truncate uppercase tracking-widest">{dev.title}</p>
                              </div>
                            </div>
                            <p className="text-muted-foreground text-sm mb-8 line-clamp-3 font-medium leading-relaxed min-h-[4.5rem] italic opacity-80">"{dev.description}"</p>
                            <div className="flex flex-wrap gap-1.5 mb-8">
                              {(dev.skills || []).slice(0, 5).map(skill => (
                                <Badge key={skill} variant="outline" className="bg-secondary/10 border-border text-muted-foreground text-[10px] px-2 font-bold uppercase">{skill}</Badge>
                              ))}
                              {(dev.skills || []).length > 5 && <span className="text-[10px] text-muted-foreground font-black ml-1">+{(dev.skills || []).length - 5}</span>}
                            </div>
                            <div className="grid grid-cols-3 gap-4 border-t border-border/50 pt-6">
                              <div className="text-center">
                                <div className="flex items-center justify-center gap-1 text-yellow-500 mb-1"><Star className="w-3.5 h-3.5 fill-current" /><span className="text-sm font-black text-foreground">{Number(dev.rating ?? 0).toFixed(1)}</span></div>
                                <p className="text-[9px] text-muted-foreground font-black uppercase tracking-widest">RATING</p>
                              </div>
                              <div className="text-center">
                                <div className="text-sm font-black text-foreground mb-1">{dev.completedProjects || 0}</div>
                                <p className="text-[9px] text-muted-foreground font-black uppercase tracking-widest">PROJECTS</p>
                              </div>
                              <div className="text-center">
                                <div className="text-sm font-black text-foreground mb-1">{dev.responseTime || "FAST"}</div>
                                <p className="text-[9px] text-muted-foreground font-black uppercase tracking-widest">RESPONSE</p>
                              </div>
                            </div>
                          </CardContent>
                          <div className="p-6 pt-0 mt-auto">
                             <Button className="w-full bg-secondary text-foreground hover:bg-primary hover:text-primary-foreground font-black rounded-2xl h-12 transition-all shadow-sm">프로필 포트폴리오 조회</Button>
                          </div>
                        </Card>
                      </Link>
                    ))}
                  </div>
                )}
              </TabsContent>

              <TabsContent value="categories">
                {(!results.categories?.content || results.categories.content.length === 0) ? (
                  <NoResults message="일치하는 카테고리가 없습니다." />
                ) : (
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                    {results.categories.content.map((c) => (
                      <Link key={c.id} to={`/projects?categoryId=${c.id}`}>
                        <Card className="hover:bg-primary/5 hover:border-primary/30 transition-all border border-border bg-card shadow-lg hover:shadow-2xl rounded-[32px] text-center p-12 group h-full flex flex-col items-center justify-center">
                          <div className="text-7xl mb-6 group-hover:scale-110 group-hover:-rotate-3 transition-transform duration-500 drop-shadow-2xl">{c.icon || "📦"}</div>
                          <div className="font-black text-foreground text-xl tracking-tight uppercase tracking-[0.1em]">{c.name}</div>
                        </Card>
                      </Link>
                    ))}
                  </div>
                )}
              </TabsContent>

              <TabsContent value="skills">
                {(!results.skills?.content || results.skills.content.length === 0) ? (
                  <NoResults message="일치하는 기술 스택이 없습니다." />
                ) : (
                  <div className="flex flex-wrap gap-4 p-8 bg-card border border-border rounded-[40px] shadow-inner">
                    {results.skills.content.map((s) => (
                      <Badge key={s.id} variant="outline" className="px-8 py-4 text-base bg-background hover:bg-primary hover:text-primary-foreground hover:border-primary border-border text-foreground cursor-pointer shadow-sm rounded-2xl transition-all duration-300 group">
                        <span className="font-black tracking-tight">{s.name}</span>
                        <span className="text-[10px] text-muted-foreground group-hover:text-primary-foreground/70 ml-4 uppercase font-black tracking-widest">{s.skillCategory}</span>
                      </Badge>
                    ))}
                  </div>
                )}
              </TabsContent>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-6 py-12 border-t border-border mt-10">
                <Button 
                  variant="outline" 
                  disabled={pageParam === 1}
                  onClick={() => handlePageChange(pageParam - 1)}
                  className="rounded-2xl border-border text-foreground hover:bg-secondary h-12 px-8 font-black uppercase tracking-widest text-xs"
                >
                  PREV
                </Button>
                <div className="flex items-center gap-3">
                  {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                    const pageNum = i + 1;
                    return (
                      <button
                        key={pageNum}
                        onClick={() => handlePageChange(pageNum)}
                        className={`w-12 h-12 rounded-2xl font-black text-sm transition-all ${
                          pageParam === pageNum 
                            ? 'bg-primary text-primary-foreground shadow-xl shadow-primary/20 scale-110' 
                            : 'bg-card text-muted-foreground hover:bg-secondary border border-border'
                        }`}
                      >
                        {pageNum}
                      </button>
                    );
                  })}
                  {totalPages > 5 && <span className="text-muted-foreground font-black px-2 tracking-[0.3em]">...</span>}
                </div>
                <Button 
                  variant="outline" 
                  disabled={pageParam >= totalPages}
                  onClick={() => handlePageChange(pageParam + 1)}
                  className="rounded-2xl border-border text-foreground hover:bg-secondary h-12 px-8 font-black uppercase tracking-widest text-xs"
                >
                  NEXT
                </Button>
              </div>
            )}
          </Tabs>
        )}
      </div>
    </div>
  );
}

function NoResults({ message }: { message: string }) {
  return (
    <div className="text-center py-32 bg-card rounded-3xl border-2 border-dashed border-border shadow-sm">
      <div className="bg-secondary w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
        <Search className="w-8 h-8 text-muted-foreground" />
      </div>
      <p className="text-muted-foreground font-medium">{message}</p>
    </div>
  );
}
