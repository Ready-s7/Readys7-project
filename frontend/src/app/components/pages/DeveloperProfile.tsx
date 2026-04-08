import { useParams, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Separator } from "../ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { 
  ArrowLeft, Star, MapPin, Clock, Briefcase, 
  MessageCircle, Calendar, Award 
} from "lucide-react";
import { mockDevelopers, reviews } from "../../data/mockData";
import { ImageWithFallback } from "../figma/ImageWithFallback";

export function DeveloperProfile() {
  const { id } = useParams();
  const developer = mockDevelopers.find(d => d.id === id);
  const developerReviews = reviews.filter(r => r.developerName === developer?.name);

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
          {/* Sidebar */}
          <div className="space-y-6">
            <Card>
              <CardContent className="p-6 text-center">
                <ImageWithFallback
                  src={developer.avatar}
                  alt={developer.name}
                  className="w-32 h-32 rounded-full mx-auto mb-4"
                />
                <h1 className="text-2xl mb-2">{developer.name}</h1>
                <p className="text-gray-600 mb-3">{developer.title}</p>
                
                <div className="flex items-center justify-center gap-2 mb-4">
                  <div className="flex items-center gap-1">
                    <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                    <span className="text-xl">{developer.rating}</span>
                  </div>
                  <span className="text-gray-500">
                    ({developer.reviewCount}개 리뷰)
                  </span>
                </div>

                <div className="space-y-2 mb-6 text-sm">
                  <div className="flex items-center justify-center gap-2 text-gray-600">
                    <MapPin className="w-4 h-4" />
                    <span>{developer.location}</span>
                  </div>
                  <div className="flex items-center justify-center gap-2 text-gray-600">
                    <Clock className="w-4 h-4" />
                    <span>응답 시간: {developer.responseTime}</span>
                  </div>
                  <div className="flex items-center justify-center gap-2 text-gray-600">
                    <Briefcase className="w-4 h-4" />
                    <span>{developer.completedProjects}개 프로젝트 완료</span>
                  </div>
                </div>

                <Button className="w-full mb-2" size="lg">
                  <MessageCircle className="w-4 h-4 mr-2" />
                  문의하기
                </Button>
                <Button variant="outline" className="w-full">
                  프로젝트 제안
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>시간당 요금</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-3xl text-blue-600 mb-2">
                  {developer.hourlyRate}
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
                  {developer.skills.map((skill) => (
                    <Badge key={skill} variant="secondary">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>소개</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-gray-700 leading-relaxed mb-6">
                  {developer.description}
                </p>
                
                <Separator className="my-6" />

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">{developer.completedProjects}</div>
                    <div className="text-sm text-gray-600">완료 프로젝트</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">{developer.reviewCount}</div>
                    <div className="text-sm text-gray-600">총 리뷰</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">{developer.rating}★</div>
                    <div className="text-sm text-gray-600">평균 평점</div>
                  </div>
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl mb-1">95%</div>
                    <div className="text-sm text-gray-600">만족도</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Tabs defaultValue="portfolio" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="portfolio">포트폴리오</TabsTrigger>
                <TabsTrigger value="reviews">리뷰</TabsTrigger>
              </TabsList>
              
              <TabsContent value="portfolio">
                <Card>
                  <CardHeader>
                    <CardTitle>포트폴리오</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {developer.portfolio.length > 0 ? (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {developer.portfolio.map((image, index) => (
                          <div 
                            key={index} 
                            className="aspect-video rounded-lg overflow-hidden border"
                          >
                            <ImageWithFallback
                              src={image}
                              alt={`Portfolio ${index + 1}`}
                              className="w-full h-full object-cover hover:scale-105 transition-transform"
                            />
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-center py-12 text-gray-500">
                        <Award className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>등록된 포트폴리오가 없습니다</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="reviews">
                <Card>
                  <CardHeader>
                    <CardTitle>고객 리뷰</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {developerReviews.length > 0 ? (
                      <div className="space-y-6">
                        {developerReviews.map((review) => (
                          <div key={review.id} className="border-b pb-6 last:border-0">
                            <div className="flex items-start justify-between mb-3">
                              <div>
                                <div className="flex items-center gap-2 mb-1">
                                  <span>{review.clientName}</span>
                                  <div className="flex">
                                    {Array.from({ length: review.rating }).map((_, i) => (
                                      <Star key={i} className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                                    ))}
                                  </div>
                                </div>
                                <p className="text-sm text-gray-600">{review.projectTitle}</p>
                              </div>
                              <div className="flex items-center gap-1 text-sm text-gray-500">
                                <Calendar className="w-4 h-4" />
                                {review.date}
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
            </Tabs>

            {/* Similar Developers */}
            <Card>
              <CardHeader>
                <CardTitle>비슷한 개발자</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {mockDevelopers
                    .filter(d => d.id !== developer.id && 
                           d.skills.some(skill => developer.skills.includes(skill)))
                    .slice(0, 2)
                    .map((similarDev) => (
                      <Link key={similarDev.id} to={`/developers/${similarDev.id}`}>
                        <div className="p-4 border rounded-lg hover:bg-gray-50 transition-colors">
                          <div className="flex items-center gap-3 mb-3">
                            <ImageWithFallback
                              src={similarDev.avatar}
                              alt={similarDev.name}
                              className="w-12 h-12 rounded-full"
                            />
                            <div>
                              <h4 className="hover:text-blue-600">{similarDev.name}</h4>
                              <p className="text-sm text-gray-600">{similarDev.title}</p>
                            </div>
                          </div>
                          <div className="flex items-center gap-2 text-sm">
                            <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                            <span>{similarDev.rating}</span>
                            <span className="text-gray-500">
                              • {similarDev.completedProjects}개 프로젝트
                            </span>
                          </div>
                        </div>
                      </Link>
                    ))}
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
