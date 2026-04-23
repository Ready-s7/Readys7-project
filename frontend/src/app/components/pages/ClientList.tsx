import { useState, useEffect, useCallback } from "react";
import { Link, useSearchParams } from "react-router";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Search, Loader2, Star, Briefcase, MessageSquare } from "lucide-react";
import { clientApi } from "../../../api/apiService";
import type { ClientDto } from "../../../api/types";

export function ClientList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [clients, setClients] = useState<ClientDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState(searchParams.get("search") ?? "");
  const [appliedSearch, setAppliedSearch] = useState(searchParams.get("search") ?? "");

  const fetchClients = useCallback(async () => {
    setIsLoading(true);
    try {
      // clientApi.getAll은 현재 검색 기능이 없으므로 전체 목록을 가져오거나 
      // 백엔드 스펙에 따라 검색 파라미터를 추가해야 함.
      // 현재는 페이징 처리 위주로 구현.
      const res = await clientApi.getAll(currentPage + 1, 12);
      
      const responseBody = res.data;
      if (responseBody.success) {
        setClients(responseBody.data.content);
        setTotalPages(responseBody.data.totalPages);
      }
    } catch (error) {
      console.error("Fetch clients error:", error);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage]);

  useEffect(() => {
    fetchClients();
  }, [fetchClients]);

  const handleSearch = () => {
    setAppliedSearch(searchTerm);
    setCurrentPage(0);
    // 실제 검색 API가 구현되어 있다면 여기서 호출
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") handleSearch();
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="container mx-auto px-4">
        <div className="mb-10 text-center md:text-left">
          <h1 className="text-4xl font-black text-gray-900 mb-3">클라이언트 찾기</h1>
          <p className="text-gray-500 text-lg">Ready's7과 함께하는 신뢰할 수 있는 파트너들을 만나보세요.</p>
        </div>

        <Card className="mb-10 border-none shadow-sm overflow-hidden">
          <CardContent className="p-6 bg-white">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="relative flex-1">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  placeholder="클라이언트 이름 또는 소개 검색..."
                  className="pl-12 h-14 text-lg border-gray-200 focus:ring-blue-500 rounded-xl"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={handleKeyDown}
                />
              </div>
              <Button 
                onClick={handleSearch} 
                className="h-14 px-10 bg-blue-600 hover:bg-blue-700 text-lg font-bold rounded-xl shadow-md transition-all active:scale-95"
              >
                검색하기
              </Button>
            </div>
          </CardContent>
        </Card>

        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-32">
            <Loader2 className="w-12 h-12 animate-spin text-blue-600 mb-4" />
            <p className="text-gray-500 font-medium">클라이언트 목록을 불러오고 있습니다...</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {clients.map((client) => (
                <Link key={client.id} to={`/clients/${client.id}`} className="group">
                  <Card className="h-full border-none shadow-sm hover:shadow-xl transition-all duration-300 rounded-2xl overflow-hidden group-hover:-translate-y-1">
                    <CardContent className="p-0">
                      <div className="h-24 bg-gradient-to-r from-blue-500 to-indigo-600 opacity-80 group-hover:opacity-100 transition-opacity" />
                      <div className="px-6 pb-6">
                        <div className="relative -mt-10 mb-4">
                          <div className="w-20 h-20 rounded-2xl bg-white shadow-md flex items-center justify-center text-3xl font-black text-blue-600 border-4 border-white">
                            {client.name[0]}
                          </div>
                        </div>
                        
                        <div className="flex items-center gap-2 mb-2">
                          <h3 className="text-xl font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
                            {client.name}
                          </h3>
                          <Badge variant="secondary" className="text-[10px] uppercase font-bold py-0 h-5">
                            {client.participateType === "COMPANY" ? "🏢 기업" : "🧑 개인"}
                          </Badge>
                        </div>
                        
                        <p className="text-gray-600 text-sm font-medium mb-6 line-clamp-1">
                          {client.title || "Ready's7 클라이언트"}
                        </p>
                        
                        <div className="grid grid-cols-3 gap-2 py-4 border-t border-gray-50">
                          <div className="text-center">
                            <div className="flex items-center justify-center gap-1 text-yellow-500 mb-1">
                              <Star className="w-4 h-4 fill-current" />
                              <span className="font-bold text-sm text-gray-900">{Number(client.rating ?? 0).toFixed(1)}</span>
                            </div>
                            <p className="text-[10px] text-gray-400 font-bold uppercase">평점</p>
                          </div>
                          <div className="text-center border-x border-gray-50">
                            <div className="flex items-center justify-center gap-1 text-blue-500 mb-1">
                              <Briefcase className="w-4 h-4" />
                              <span className="font-bold text-sm text-gray-900">{client.completedProject}</span>
                            </div>
                            <p className="text-[10px] text-gray-400 font-bold uppercase">완료</p>
                          </div>
                          <div className="text-center">
                            <div className="flex items-center justify-center gap-1 text-green-500 mb-1">
                              <MessageSquare className="w-4 h-4" />
                              <span className="font-bold text-sm text-gray-900">{client.reviewCount || 0}</span>
                            </div>
                            <p className="text-[10px] text-gray-400 font-bold uppercase">리뷰</p>
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>

            {clients.length === 0 && (
              <Card className="border-none shadow-sm py-20 text-center">
                <CardContent>
                  <Search className="w-16 h-16 mx-auto mb-4 text-gray-200" />
                  <p className="text-xl font-bold text-gray-900 mb-2">검색 결과가 없습니다.</p>
                  <p className="text-gray-500">다른 키워드로 검색해보시겠어요?</p>
                </CardContent>
              </Card>
            )}

            {totalPages > 1 && (
              <div className="flex justify-center gap-3 mt-12">
                <Button 
                  variant="outline" 
                  className="rounded-xl h-10 px-6 font-bold border-gray-200"
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
                          ? "bg-blue-600 text-white shadow-md shadow-blue-200 scale-110" 
                          : "bg-white text-gray-500 hover:bg-gray-50 border border-gray-100"
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
                <Button 
                  variant="outline" 
                  className="rounded-xl h-10 px-6 font-bold border-gray-200"
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
