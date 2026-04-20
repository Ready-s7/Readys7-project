import { useState, useEffect } from "react";
import { useSearchParams, Link } from "react-router";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Search, Loader2, Briefcase, Tag, Wrench, Users, Star } from "lucide-react";
import { searchApi } from "../../../api/apiService";
import type { TotalSearchResponseDto } from "../../../api/types";

export function SearchPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const keyword = searchParams.get("keyword") || "";

    const [results, setResults] = useState<TotalSearchResponseDto | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [searchInput, setSearchInput] = useState(keyword);

    useEffect(() => {
        if (keyword) {
            handleSearch(keyword);
        }
    }, [keyword]);

    const handleSearch = async (term: string) => {
        setIsLoading(true);
        try {
            const res = await searchApi.getTotalSearch(term);
            setResults(res.data.data);
        } finally {
            setIsLoading(false);
        }
    };

    const onSearchSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (searchInput.trim()) {
            setSearchParams({ keyword: searchInput.trim() });
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="container mx-auto px-4 max-w-5xl">
                {/* Search Header */}
                <div className="mb-10 text-center">
                    <h1 className="text-3xl font-bold mb-6 text-gray-900">통합 검색 결과</h1>
                    <form onSubmit={onSearchSubmit} className="max-w-2xl mx-auto flex gap-2">
                        <div className="flex-1 relative">
                            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <Input
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                placeholder="검색어를 입력하세요"
                                className="pl-12 h-14 rounded-2xl border-gray-200 shadow-lg focus-visible:ring-blue-500 bg-white"
                            />
                        </div>
                        <Button type="submit" size="lg" className="h-14 px-8 bg-blue-600 hover:bg-blue-700 rounded-2xl font-bold">검색</Button>
                    </form>
                    {keyword && !isLoading && (
                        <p className="mt-6 text-gray-500">
                            "<span className="text-blue-600 font-bold">{keyword}</span>"에 대한 검색 결과입니다.
                        </p>
                    )}
                </div>

                {isLoading ? (
                    <div className="flex flex-col items-center justify-center py-32">
                        <Loader2 className="w-12 h-12 animate-spin text-blue-600 mb-4" />
                        <p className="text-gray-500 font-medium">최적의 결과를 찾는 중입니다...</p>
                    </div>
                ) : !results ? (
                    <div className="text-center py-32 text-gray-400">
                        <Search className="w-20 h-20 mx-auto mb-6 opacity-10" />
                        <p className="text-lg">검색어를 입력하고 준비된 프로젝트와 전문가를 만나보세요.</p>
                    </div>
                ) : (
                    <Tabs defaultValue="projects" className="w-full">
                        <TabsList className="grid w-full grid-cols-4 mb-10 bg-white p-1 rounded-2xl shadow-sm border h-14">
                            <TabsTrigger value="projects" className="flex gap-2 rounded-xl data-[state=active]:bg-blue-50 data-[state=active]:text-blue-700">
                                <Briefcase className="w-4 h-4" /> 프로젝트 ({results.projects?.totalElements || 0})
                            </TabsTrigger>
                            <TabsTrigger value="developers" className="flex gap-2 rounded-xl data-[state=active]:bg-blue-50 data-[state=active]:text-blue-700">
                                <Users className="w-4 h-4" /> 개발자 ({results.developers?.totalElements || 0})
                            </TabsTrigger>
                            <TabsTrigger value="categories" className="flex gap-2 rounded-xl data-[state=active]:bg-blue-50 data-[state=active]:text-blue-700">
                                <Tag className="w-4 h-4" /> 카테고리 ({results.categories?.totalElements || 0})
                            </TabsTrigger>
                            <TabsTrigger value="skills" className="flex gap-2 rounded-xl data-[state=active]:bg-blue-50 data-[state=active]:text-blue-700">
                                <Wrench className="w-4 h-4" /> 기술 스택 ({results.skills?.totalElements || 0})
                            </TabsTrigger>
                        </TabsList>

                        {/* Projects Result */}
                        <TabsContent value="projects">
                            {(!results.projects?.content || results.projects.content.length === 0) ? (
                                <NoResults message="일치하는 프로젝트를 찾지 못했습니다." />
                            ) : (
                                <div className="grid grid-cols-1 gap-4">
                                    {results.projects.content.map((p) => (
                                        <Link key={p.id} to={`/projects/${p.id}`}>
                                            <Card className="hover:shadow-lg transition-shadow border-none shadow-sm rounded-2xl overflow-hidden">
                                                <CardContent className="p-6">
                                                    <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                                                        <div className="flex-1 min-w-0">
                                                            <div className="flex items-center gap-3 mb-3 flex-wrap">
                                                                <Badge variant="secondary" className="bg-blue-50 text-blue-700 border-none">{p.category || "기타"}</Badge>
                                                                <Badge variant="outline" className="border-gray-200">{p.status === "OPEN" ? "모집중" : p.status === "IN_PROGRESS" ? "진행중" : "완료"}</Badge>
                                                                <span className="text-xs text-gray-400 font-medium">
                                  {p.createdAt ? new Date(p.createdAt).toLocaleDateString() : ""}
                                </span>
                                                            </div>
                                                            <h3 className="text-xl font-bold text-gray-900 mb-2 group-hover:text-blue-600 truncate">{p.title}</h3>
                                                            <p className="text-gray-600 text-sm mb-4 line-clamp-2 leading-relaxed">{p.description}</p>

                                                            <div className="flex flex-wrap gap-1.5 mb-4">
                                                                {(p.skills || []).map(skill => (
                                                                    <Badge key={skill} variant="outline" className="bg-gray-50/50 border-gray-100 text-gray-500 text-[10px] px-2">
                                                                        {skill}
                                                                    </Badge>
                                                                ))}
                                                            </div>

                                                            <div className="flex items-center gap-4 text-xs text-gray-500 font-bold">
                                                                <div className="flex items-center gap-1">
                                                                    <span className="text-gray-300">👤</span> {p.clientName || "익명"}
                                                                </div>
                                                                <div className="flex items-center gap-1">
                                                                    <span className="text-yellow-400">⭐</span> {p.clientRating?.toFixed(1) || "0.0"}
                                                                </div>
                                                                <div className="flex items-center gap-1">
                                                                    <span className="text-blue-400">📝</span> 제안 {p.currentProposalCount || 0}개
                                                                </div>
                                                            </div>
                                                        </div>

                                                        <div className="md:text-right shrink-0 flex flex-col justify-between items-end min-w-[150px]">
                                                            <div className="mb-4">
                                                                <div className="text-lg font-black text-blue-600">
                                                                    {(p.minBudget || 0).toLocaleString()}원 ~ {(p.maxBudget || 0).toLocaleString()}원
                                                                </div>
                                                                <div className="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-1">예상 기간: {p.duration || 0}일</div>
                                                            </div>
                                                            <Button size="sm" className="bg-blue-600 hover:bg-blue-700 rounded-xl px-6">상세보기</Button>
                                                        </div>
                                                    </div>
                                                </CardContent>
                                            </Card>
                                        </Link>
                                    ))}
                                </div>
                            )}
                        </TabsContent>

                        {/* Developers Result */}
                        <TabsContent value="developers">
                            {(!results.developers?.content || results.developers.content.length === 0) ? (
                                <NoResults message="일치하는 개발자를 찾지 못했습니다." />
                            ) : (
                                <div className="grid grid-cols-1 gap-4">
                                    {results.developers.content.map((dev) => (
                                        <Link key={dev.id} to={`/developers/${dev.id}`}>
                                            <Card className="hover:shadow-lg transition-shadow border-none shadow-sm rounded-2xl overflow-hidden">
                                                <CardContent className="p-6">
                                                    <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-6">
                                                        <div className="flex-1 min-w-0">
                                                            <div className="flex items-center gap-3 mb-3">
                                                                <Badge className="bg-green-50 text-green-700 border-none hover:bg-green-100">
                                                                    {dev.participateType === "INDIVIDUAL" ? "개인" : "업체"}
                                                                </Badge>
                                                                {dev.availableForWork && (
                                                                    <Badge className="bg-blue-50 text-blue-700 border-none">활동중</Badge>
                                                                )}
                                                            </div>
                                                            <h3 className="text-xl font-bold text-gray-900 mb-1">{dev.name}</h3>
                                                            <p className="text-blue-600 font-bold text-sm mb-3">{dev.title}</p>
                                                            <p className="text-gray-600 text-sm mb-4 line-clamp-2">{dev.description}</p>

                                                            <div className="flex flex-wrap gap-1.5 mb-4">
                                                                {(dev.skills || []).map(skill => (
                                                                    <Badge key={skill} variant="outline" className="bg-gray-50 border-gray-100 text-gray-500 text-[10px]">
                                                                        {skill}
                                                                    </Badge>
                                                                ))}
                                                            </div>

                                                            <div className="flex items-center gap-6 text-xs text-gray-500 font-bold">
                                                                <div className="flex items-center gap-1.5">
                                                                    <Star className="w-3.5 h-3.5 text-yellow-400 fill-yellow-400" />
                                                                    <span className="text-gray-900">{dev.rating?.toFixed(1) || "0.0"}</span>
                                                                    <span className="text-gray-400">({dev.reviewCount || 0})</span>
                                                                </div>
                                                                <div className="flex items-center gap-1">
                                                                    <span className="text-gray-400 font-medium">완료 프로젝트</span>
                                                                    <span className="text-gray-900">{dev.completedProjects || 0}건</span>
                                                                </div>
                                                                <div className="flex items-center gap-1">
                                                                    <span className="text-gray-400 font-medium">응답 시간</span>
                                                                    <span className="text-gray-900">{dev.responseTime || "-"}</span>
                                                                </div>
                                                            </div>
                                                        </div>

                                                        <div className="md:text-right shrink-0 flex flex-col justify-between items-end min-w-[160px]">
                                                            <div className="mb-4">
                                                                <div className="text-xs text-gray-400 font-bold mb-1">희망 시급</div>
                                                                <div className="text-lg font-black text-gray-900">
                                                                    {dev.minHourlyPay?.toLocaleString() || 0}원 ~
                                                                </div>
                                                            </div>
                                                            <Button size="sm" className="bg-gray-900 hover:bg-gray-800 rounded-xl px-6 text-white">프로필 보기</Button>
                                                        </div>
                                                    </div>
                                                </CardContent>
                                            </Card>
                                        </Link>
                                    ))}
                                </div>
                            )}
                        </TabsContent>

                        {/* Categories Result */}
                        <TabsContent value="categories">
                            {(!results.categories?.content || results.categories.content.length === 0) ? (
                                <NoResults message="일치하는 카테고리가 없습니다." />
                            ) : (
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    {results.categories.content.map((c) => (
                                        <Link key={c.id} to={`/projects?categoryId=${c.id}`}>
                                            <Card className="hover:bg-blue-50 transition-all border-none shadow-sm rounded-2xl text-center p-8 group">
                                                <div className="text-5xl mb-4 group-hover:scale-110 transition-transform">{c.icon || "📦"}</div>
                                                <div className="font-black text-gray-800 text-lg">{c.name}</div>
                                            </Card>
                                        </Link>
                                    ))}
                                </div>
                            )}
                        </TabsContent>

                        {/* Skills Result */}
                        <TabsContent value="skills">
                            {(!results.skills?.content || results.skills.content.length === 0) ? (
                                <NoResults message="일치하는 기술 스택이 없습니다." />
                            ) : (
                                <div className="flex flex-wrap gap-3">
                                    {results.skills.content.map((s) => (
                                        <Badge key={s.id} variant="outline" className="px-6 py-3 text-sm bg-white hover:bg-blue-50 border-gray-200 hover:border-blue-200 hover:text-blue-700 cursor-pointer shadow-sm rounded-xl transition-all">
                                            <span className="font-bold">{s.name}</span>
                                            <span className="text-[10px] text-gray-400 ml-3 uppercase font-black">{s.skillCategory}</span>
                                        </Badge>
                                    ))}
                                </div>
                            )}
                        </TabsContent>
                    </Tabs>
                )}
            </div>
        </div>
    );
}

function NoResults({ message }: { message: string }) {
    return (
        <div className="text-center py-32 bg-white rounded-3xl border-2 border-dashed border-gray-100 shadow-sm">
            <div className="bg-gray-50 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Search className="w-8 h-8 text-gray-300" />
            </div>
            <p className="text-gray-400 font-medium">{message}</p>
        </div>
    );
}
