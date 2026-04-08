import { useParams, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { Separator } from "../ui/separator";
import { Calendar, Clock, DollarSign, Star, ArrowLeft, Send } from "lucide-react";
import { mockProjects, categories } from "../../data/mockData";

export function ProjectDetail() {
  const { id } = useParams();
  const project = mockProjects.find(p => p.id === id);

  if (!project) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <h1 className="text-2xl mb-4">프로젝트를 찾을 수 없습니다</h1>
        <Link to="/projects">
          <Button>프로젝트 목록으로</Button>
        </Link>
      </div>
    );
  }

  const category = categories.find(c => c.id === project.category);

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <Link to="/projects">
          <Button variant="ghost" className="mb-6">
            <ArrowLeft className="w-4 h-4 mr-2" />
            프로젝트 목록
          </Button>
        </Link>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <div className="flex items-center gap-3 mb-3">
                  <Badge variant="secondary">{category?.icon} {category?.label}</Badge>
                  <span className="text-sm text-gray-500">{project.postedDate}</span>
                </div>
                <CardTitle className="text-3xl">{project.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div>
                    <h3 className="text-lg mb-2">프로젝트 설명</h3>
                    <p className="text-gray-600 leading-relaxed">{project.description}</p>
                  </div>

                  <Separator />

                  <div>
                    <h3 className="text-lg mb-3">필요 기술</h3>
                    <div className="flex flex-wrap gap-2">
                      {project.skills.map((skill) => (
                        <Badge key={skill} variant="outline" className="text-sm px-3 py-1">
                          {skill}
                        </Badge>
                      ))}
                    </div>
                  </div>

                  <Separator />

                  <div className="grid grid-cols-2 gap-4">
                    <div className="flex items-center gap-3">
                      <div className="bg-blue-100 p-2 rounded-lg">
                        <DollarSign className="w-5 h-5 text-blue-600" />
                      </div>
                      <div>
                        <div className="text-sm text-gray-600">예산</div>
                        <div>{project.budget}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="bg-blue-100 p-2 rounded-lg">
                        <Clock className="w-5 h-5 text-blue-600" />
                      </div>
                      <div>
                        <div className="text-sm text-gray-600">기간</div>
                        <div>{project.duration}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="bg-blue-100 p-2 rounded-lg">
                        <Calendar className="w-5 h-5 text-blue-600" />
                      </div>
                      <div>
                        <div className="text-sm text-gray-600">등록일</div>
                        <div>{project.postedDate}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="bg-blue-100 p-2 rounded-lg">
                        <Send className="w-5 h-5 text-blue-600" />
                      </div>
                      <div>
                        <div className="text-sm text-gray-600">받은 제안</div>
                        <div>{project.proposals}개</div>
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Similar Projects */}
            <Card>
              <CardHeader>
                <CardTitle>비슷한 프로젝트</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {mockProjects
                    .filter(p => p.id !== project.id && p.category === project.category)
                    .slice(0, 3)
                    .map((similarProject) => (
                      <Link key={similarProject.id} to={`/projects/${similarProject.id}`}>
                        <div className="p-4 border rounded-lg hover:bg-gray-50 transition-colors">
                          <h4 className="mb-2 hover:text-blue-600">{similarProject.title}</h4>
                          <p className="text-sm text-gray-600 mb-2 line-clamp-2">
                            {similarProject.description}
                          </p>
                          <div className="flex justify-between items-center text-sm">
                            <span className="text-blue-600">{similarProject.budget}</span>
                            <span className="text-gray-500">제안 {similarProject.proposals}개</span>
                          </div>
                        </div>
                      </Link>
                    ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Client Info */}
            <Card>
              <CardHeader>
                <CardTitle>클라이언트 정보</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3 mb-4">
                  <Avatar>
                    <AvatarFallback>{project.clientName[0]}</AvatarFallback>
                  </Avatar>
                  <div>
                    <div>{project.clientName}</div>
                    <div className="flex items-center gap-1 text-sm text-gray-600">
                      <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                      {project.clientRating}
                    </div>
                  </div>
                </div>
                <Button className="w-full" size="lg">
                  <Send className="w-4 h-4 mr-2" />
                  제안서 보내기
                </Button>
                <Button variant="outline" className="w-full mt-2">
                  문의하기
                </Button>
              </CardContent>
            </Card>

            {/* Project Stats */}
            <Card>
              <CardHeader>
                <CardTitle>프로젝트 현황</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600">상태</span>
                  <Badge variant={project.status === "open" ? "default" : "secondary"}>
                    {project.status === "open" ? "모집중" : "진행중"}
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">받은 제안</span>
                  <span>{project.proposals}개</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">평균 제안 금액</span>
                  <span className="text-blue-600">{project.budget}</span>
                </div>
              </CardContent>
            </Card>

            {/* Tips */}
            <Card className="bg-blue-50 border-blue-200">
              <CardHeader>
                <CardTitle className="text-blue-900">💡 제안 팁</CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2 text-sm text-blue-900">
                  <li>• 프로젝트 요구사항을 정확히 파악하세요</li>
                  <li>• 포트폴리오와 경력을 강조하세요</li>
                  <li>• 현실적인 일정과 견적을 제시하세요</li>
                  <li>• 빠른 응답으로 성실함을 보여주세요</li>
                </ul>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
