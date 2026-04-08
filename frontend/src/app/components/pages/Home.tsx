import { Link } from "react-router";
import { Button } from "../ui/button";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Search, ArrowRight, Star, Users, Briefcase, Shield } from "lucide-react";
import { Input } from "../ui/input";
import { categories, mockProjects, mockDevelopers } from "../../data/mockData";
import { ImageWithFallback } from "../figma/ImageWithFallback";

export function Home() {
  const featuredProjects = mockProjects.slice(0, 3);
  const topDevelopers = mockDevelopers.slice(0, 4);

  // @ts-ignore
  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-50 to-indigo-50 py-20">
        <div className="container mx-auto px-4">
          <div className="max-w-3xl mx-auto text-center">
            <h1 className="text-4xl md:text-5xl mb-6">
              전문 개발자와 함께<br />프로젝트를 완성하세요
            </h1>
            <p className="text-xl text-gray-600 mb-8">
              검증된 개발자들과 함께 당신의 아이디어를 현실로 만들어보세요
            </p>
            
            {/* Search Bar */}
            <div className="max-w-2xl mx-auto bg-white rounded-lg shadow-lg p-2 flex flex-col md:flex-row gap-2">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  placeholder="어떤 프로젝트를 찾으시나요?"
                  className="pl-10 border-0 focus-visible:ring-0"
                />
              </div>
              <Button size="lg" className="md:w-auto w-full">
                검색하기
              </Button>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-3 gap-8 mt-16 max-w-2xl mx-auto">
              <div>
                <div className="text-3xl mb-2">1,000+</div>
                <div className="text-gray-600">전문 개발자</div>
              </div>
              <div>
                <div className="text-3xl mb-2">5,000+</div>
                <div className="text-gray-600">완료된 프로젝트</div>
              </div>
              <div>
                <div className="text-3xl mb-2">4.8★</div>
                <div className="text-gray-600">평균 만족도</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl text-center mb-12">카테고리별 전문가 찾기</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {categories.map((category) => (
              <Link key={category.id} to={`/projects?category=${category.id}`}>
                <Card className="hover:shadow-lg transition-shadow cursor-pointer">
                  <CardContent className="p-6 text-center">
                    <div className="text-4xl mb-3">{category.icon}</div>
                    <div>{category.label}</div>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Featured Projects */}
      <section className="py-16 bg-gray-50">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl">인기 프로젝트</h2>
            <Link to="/projects">
              <Button variant="ghost">
                전체보기 <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {featuredProjects.map((project) => (
              <Link key={project.id} to={`/projects/${project.id}`}>
                <Card className="hover:shadow-lg transition-shadow h-full">
                  <CardContent className="p-6">
                    <div className="flex justify-between items-start mb-3">
                      <Badge variant="secondary">{categories.find(c => c.id === project.category)?.label}</Badge>
                      <span className="text-sm text-gray-500">{project.postedDate}</span>
                    </div>
                    <h3 className="text-xl mb-3">{project.title}</h3>
                    <p className="text-gray-600 mb-4 line-clamp-2">{project.description}</p>
                    <div className="flex flex-wrap gap-2 mb-4">
                      {project.skills.slice(0, 3).map((skill) => (
                        <Badge key={skill} variant="outline">{skill}</Badge>
                      ))}
                    </div>
                    <div className="flex justify-between items-center pt-4 border-t">
                      <span className="text-blue-600">{project.budget}</span>
                      <span className="text-sm text-gray-500">제안 {project.proposals}개</span>
                    </div>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Top Developers */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl">추천 개발자</h2>
            <Link to="/developers">
              <Button variant="ghost">
                전체보기 <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {topDevelopers.map((developer) => (
              <Link key={developer.id} to={`/developers/${developer.id}`}>
                <Card className="hover:shadow-lg transition-shadow h-full">
                  <CardContent className="p-6 text-center">
                    <ImageWithFallback
                      src={developer.avatar}
                      alt={developer.name}
                      className="w-20 h-20 rounded-full mx-auto mb-4"
                    />
                    <h3 className="text-lg mb-1">{developer.name}</h3>
                    <p className="text-gray-600 text-sm mb-3">{developer.title}</p>
                    <div className="flex items-center justify-center gap-1 mb-3">
                      <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                      <span>{developer.rating}</span>
                      <span className="text-gray-500 text-sm">({developer.reviewCount})</span>
                    </div>
                    <div className="flex flex-wrap gap-1 justify-center mb-3">
                      {developer.skills.slice(0, 3).map((skill) => (
                        <Badge key={skill} variant="outline" className="text-xs">{skill}</Badge>
                      ))}
                    </div>
                    <p className="text-blue-600 text-sm">{developer.hourlyRate}/시간</p>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-16 bg-gray-50">
        <div className="container mx-auto px-4">
          <h2 className="text-3xl text-center mb-12">왜 Ready's7인가요?</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2">검증된 전문가</h3>
              <p className="text-gray-600">철저한 검증을 거친 전문 개발자들만 활동합니다</p>
            </div>
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Shield className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2">안전한 거래</h3>
              <p className="text-gray-600">에스크로 시스템으로 안전한 대금 지급을 보장합니다</p>
            </div>
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Briefcase className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2">다양한 프로젝트</h3>
              <p className="text-gray-600">웹, 앱, AI 등 모든 분야의 프로젝트를 찾을 수 있습니다</p>
            </div>
            <div className="text-center">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Star className="w-8 h-8 text-blue-600" />
              </div>
              <h3 className="text-xl mb-2">높은 만족도</h3>
              <p className="text-gray-600">평균 4.8점의 높은 고객 만족도를 자랑합니다</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 bg-blue-600 text-white">
        <div className="container mx-auto px-4 text-center">
          <h2 className="text-3xl md:text-4xl mb-4">지금 바로 시작하세요</h2>
          <p className="text-xl mb-8 opacity-90">당신의 프로젝트를 성공으로 이끌어줄 최고의 개발자를 만나보세요</p>
          <div className="flex gap-4 justify-center flex-wrap">
            <Link to="/projects/new">
              <Button size="lg" variant="secondary">
                프로젝트 등록하기
              </Button>
            </Link>
            <Link to="/developers">
              <Button size="lg" variant="outline" className="bg-transparent border-white text-white hover:bg-white hover:text-blue-600">
                개발자 찾아보기
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
