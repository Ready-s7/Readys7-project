import { useState, useEffect } from "react";
import { Link } from "react-router";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { 
  Users, Star, MessageSquare, Layout, Loader2 
} from "lucide-react";
import { clientApi } from "../../../api/apiService";
import type { ClientDto } from "../../../api/types";

export function ClientList() {
  const [clients, setClients] = useState<ClientDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // 페이지 변경 시 데이터 로드
  useEffect(() => {
    fetchClients();
  }, [currentPage]);

  const fetchClients = async () => {
    setIsLoading(true);
    try {
      // clientApi.getAll(page, size) positional arguments
      // 백엔드 Pageable 규격이 1-based인 경우 +1
      const res = await clientApi.getAll(currentPage + 1, 9);
      
      if (res.data?.data) {
        setClients(res.data.data.content || []);
        setTotalPages(res.data.data.totalPages || 0);
      }
    } catch (e) {
      console.error("클라이언트 로드 실패:", e);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading && clients.length === 0) {
    return (
      <div className="flex justify-center py-32 bg-background min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-5xl">
        <div className="flex items-center gap-3 mb-10">
          <div className="bg-primary p-2 rounded-xl">
            <Users className="w-6 h-6 text-primary-foreground" />
          </div>
          <h1 className="text-3xl font-bold text-foreground">클라이언트 찾기</h1>
        </div>

        {clients.length === 0 && !isLoading ? (
          <div className="text-center py-32 bg-card rounded-3xl border border-dashed border-border">
            <Users className="w-16 h-16 mx-auto mb-4 text-muted-foreground/20" />
            <p className="text-xl font-bold text-foreground mb-2">등록된 클라이언트가 없습니다.</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {clients.map((client) => (
                <Link key={client.id} to={`/clients/${client.id}`}>
                  <Card className="hover:shadow-xl hover:shadow-primary/5 transition-all duration-300 h-full border-border bg-card group overflow-hidden">
                    <CardContent className="p-6">
                      <div className="flex items-start justify-between mb-4">
                        <div className="w-14 h-14 rounded-2xl bg-secondary/50 flex items-center justify-center text-2xl font-bold text-primary group-hover:scale-110 transition-transform">
                          {client.name[0]}
                        </div>
                        <Badge variant="outline" className="bg-secondary/20 border-none text-muted-foreground">
                          {client.participateType === "COMPANY" ? "🏢 기업" : "🧑 개인"}
                        </Badge>
                      </div>
                      
                      <h3 className="text-xl font-bold text-foreground mb-1 group-hover:text-primary transition-colors">{client.name}</h3>
                      <p className="text-sm text-muted-foreground mb-4 line-clamp-1">{client.title}</p>
                      
                      <div className="flex items-center gap-4 mb-5 p-3 bg-secondary/20 rounded-xl">
                        <div className="flex items-center gap-1.5">
                          <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                          <span className="font-bold text-foreground">{client.rating?.toFixed(1) || "0.0"}</span>
                        </div>
                        <div className="w-px h-3 bg-border" />
                        <div className="flex items-center gap-1.5 text-muted-foreground">
                          <MessageSquare className="w-4 h-4" />
                          <span className="text-sm font-medium">{client.reviewCount} 리뷰</span>
                        </div>
                      </div>

                      <div className="flex items-center justify-between text-sm text-muted-foreground pt-1">
                        <div className="flex items-center gap-1.5">
                          <Layout className="w-4 h-4" />
                          <span>{client.totalProjects || 0} 프로젝트 등록</span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>

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
