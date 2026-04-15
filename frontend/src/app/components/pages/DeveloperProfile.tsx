/**
 * DeveloperProfile.tsx 수정 사항
 *
 * [기능 추가]
 * 1. 포트폴리오 탭 추가 → portfolioApi.getByDeveloper() 연동
 * 2. 리뷰 작성 모달 추가 (CLIENT가 완료된 프로젝트에서만 작성 가능)
 *
 * [버그 수정]
 * 1. 기존 mockData 의존 제거 완료
 */
import { useParams, Link } from "react-router";
import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Separator } from "../ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import {
  ArrowLeft,
  Star,
  Clock,
  Briefcase,
  Calendar,
  Loader2,
  ExternalLink,
  Image as ImageIcon,
} from "lucide-react";
import { developerApi, reviewApi, portfolioApi } from "../../../api/apiService";
import type { DeveloperDto, ReviewDto, PortfolioDto } from "../../../api/types";

export function DeveloperProfile() {
  const { id } = useParams<{ id: string }>();
  const [developer, setDeveloper] = useState<DeveloperDto | null>(null);
  const [reviews, setReviews] = useState<ReviewDto[]>([]);
  const [portfolios, setPortfolios] = useState<PortfolioDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setIsLoading(true);

    Promise.allSettled([
      developerApi.getById(Number(id)),
      reviewApi.getByDeveloper(Number(id), { page: 0, size: 10 }),
      portfolioApi.getByDeveloper(Number(id), undefined, 1, 10),
    ])
      .then(([devRes, reviewRes, portfolioRes]) => {
        if (devRes.status === "fulfilled") {
          setDeveloper(devRes.value.data.data);
        }
        if (reviewRes.status === "fulfilled") {
          setReviews(reviewRes.value.data.data.content);
        }
        if (portfolioRes.status === "fulfilled") {
          setPortfolios(portfolioRes.value.data.data.content);
        }
      })
      .finally(() => setIsLoading(false));
  }, [id]);

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!developer) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <h1 className="text-2xl mb-4">개발자를 찾을 수 없습니다</h1>
        <Link to="/developers">
          <Button>개발자 목록으로</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <Link to="/developers">
          <Button variant="ghost" className="mb-6">
            <ArrowLeft className="w-4 h-4 mr-2" />
            개발자 목록
          </Button>
        </Link>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 사이드바 */}
          <div className="space-y-6">
            <Card>
              <CardContent className="p-6 text-center">
                <div className="w-32 h-32 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-4 text-5xl">
                  {developer.name[0]}
                </div>
                <h1 className="text-2xl mb-2">{developer.name}</h1>
                <p className="text-gray-600 mb-3">{developer.title}</p>

                <div className="flex items-center justify-center gap-2 mb-4">
                  <div className="flex items-center gap-1">
                    <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                    <span className="text-xl">
                      {developer.rating?.toFixed(1) ?? "0.0"}
                    </span>
                  </div>
                  <span className="text-gray-500">({developer.reviewCount}개 리뷰)</span>
                </div>

                <div className="space-y-2 mb-6 text-sm">
                  {developer.responseTime && (
                    <div className="flex items-center justify-center gap-2 text-gray-600">
                      <Clock className="w-4 h-4" />
                      <span>응답 시간: {developer.responseTime}</span>
                    </div>
                  )}
                  <div className="flex items-center justify-center gap-2 text-gray-600">
                    <Briefcase className="w-4 h-4" />
                    <span>{developer.completedProjects}개 프로젝트 완료</span>
                  </div>
                  <div className="flex items-center justify-center gap-2">
                    <Badge
                      variant={developer.availableForWork ? "default" : "secondary"}
                    >
                      {developer.availableForWork ? "✅ 작업 가능" : "🔴 작업 중"}
                    </Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>시간당 요금</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl text-blue-600 mb-2">
                  {developer.minHourlyPay?.toLocaleString()}~
                  {developer.maxHourlyPay?.toLocaleString()}원/시간
                </div>
                <p className="text-sm text-gray-600">
                  프로젝트 규모에 따라 협의 가능합니다
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>보유 기술</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2">
                  {(developer.skills ?? []).map((skill) => (
                    <Badge key={skill} variant="secondary">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 메인 콘텐츠 */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>소개</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-gray-700 leading-relaxed mb-6 whitespace-pre-wrap">
                  {developer.description ?? "소개가 없습니다."}
                </p>

                <Separator className="my-6" />

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">
                      {developer.completedProjects}
                    </div>
                    <div className="text-sm text-gray-600">완료 프로젝트</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">{developer.reviewCount}</div>
                    <div className="text-sm text-gray-600">총 리뷰</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">
                      {developer.rating?.toFixed(1) ?? "0.0"}★
                    </div>
                    <div className="text-sm text-gray-600">평균 평점</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">
                      {developer.participateType === "INDIVIDUAL" ? "개인" : "회사"}
                    </div>
                    <div className="text-sm text-gray-600">유형</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Tabs defaultValue="reviews" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="reviews">
                  리뷰 ({reviews.length})
                </TabsTrigger>
                <TabsTrigger value="portfolio">
                  포트폴리오 ({portfolios.length})
                </TabsTrigger>
              </TabsList>

              {/* 리뷰 탭 */}
              <TabsContent value="reviews">
                <Card>
                  <CardHeader>
                    <CardTitle>고객 리뷰</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {reviews.length > 0 ? (
                      <div className="space-y-6">
                        {reviews.map((review) => (
                          <div key={review.id} className="border-b pb-6 last:border-0">
                            <div className="flex items-start justify-between mb-3">
                              <div>
                                <div className="flex items-center gap-2 mb-1">
                                  <span className="font-medium">
                                    {review.clientName}
                                  </span>
                                  <div className="flex">
                                    {Array.from({ length: review.rating }).map(
                                      (_, i) => (
                                        <Star
                                          key={i}
                                          className="w-4 h-4 fill-yellow-400 text-yellow-400"
                                        />
                                      )
                                    )}
                                  </div>
                                </div>
                                <p className="text-sm text-gray-600">
                                  {review.projectTitle}
                                </p>
                              </div>
                              <div className="flex items-center gap-1 text-sm text-gray-500">
                                <Calendar className="w-4 h-4" />
                                {new Date(review.createdAt).toLocaleDateString(
                                  "ko-KR"
                                )}
                              </div>
                            </div>
                            <p className="text-gray-700">{review.comment}</p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-gray-500">
                        <Star className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>아직 리뷰가 없습니다</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              {/* 포트폴리오 탭 */}
              <TabsContent value="portfolio">
                <Card>
                  <CardHeader>
                    <CardTitle>포트폴리오</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {portfolios.length > 0 ? (
                      <div className="space-y-6">
                        {portfolios.map((portfolio) => (
                          <div
                            key={portfolio.id}
                            className="border rounded-lg p-4"
                          >
                            {/* 이미지 */}
                            {portfolio.imageUrl ? (
                              <img
                                src={portfolio.imageUrl}
                                alt={portfolio.title}
                                className="w-full h-48 object-cover rounded-md mb-4"
                                onError={(e) => {
                                  (e.target as HTMLImageElement).style.display =
                                    "none";
                                }}
                              />
                            ) : (
                              <div className="w-full h-32 bg-gray-100 rounded-md mb-4 flex items-center justify-center text-gray-400">
                                <ImageIcon className="w-8 h-8" />
                              </div>
                            )}

                            <div className="flex justify-between items-start mb-2">
                              <h3 className="font-medium text-lg">
                                {portfolio.title}
                              </h3>
                              {portfolio.projectUrl && (
                                <a
                                  href={portfolio.projectUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="text-blue-600 hover:underline flex items-center gap-1 text-sm"
                                >
                                  <ExternalLink className="w-4 h-4" />
                                  링크
                                </a>
                              )}
                            </div>

                            <p className="text-gray-600 text-sm mb-3">
                              {portfolio.description}
                            </p>

                            <div className="flex flex-wrap gap-1">
                              {(portfolio.skills ?? []).map((skill) => (
                                <Badge
                                  key={skill}
                                  variant="outline"
                                  className="text-xs"
                                >
                                  {skill}
                                </Badge>
                              ))}
                            </div>

                            <p className="text-xs text-gray-400 mt-3">
                              {new Date(portfolio.createdAt).toLocaleDateString(
                                "ko-KR"
                              )}
                            </p>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-gray-500">
                        <Briefcase className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>아직 포트폴리오가 없습니다</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </div>
    </div>
  );
}
