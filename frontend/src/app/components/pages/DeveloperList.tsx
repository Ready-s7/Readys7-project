import { useState } from "react";
import { Link } from "react-router";
import { Card, CardContent } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { Search, Star, MapPin, Clock } from "lucide-react";
import { mockDevelopers, categories } from "../../data/mockData";
import { ImageWithFallback } from "../figma/ImageWithFallback";

export function DeveloperList() {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedSkill, setSelectedSkill] = useState("all");
  const [sortBy, setSortBy] = useState("rating");

  const allSkills = Array.from(
    new Set(mockDevelopers.flatMap(dev => dev.skills))
  ).sort();

  const filteredDevelopers = mockDevelopers
    .filter(developer => {
      const matchesSearch = 
        developer.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        developer.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
        developer.description.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesSkill = 
        selectedSkill === "all" || 
        developer.skills.includes(selectedSkill);
      return matchesSearch && matchesSkill;
    })
    .sort((a, b) => {
      if (sortBy === "rating") {
        return b.rating - a.rating;
      } else if (sortBy === "projects") {
        return b.completedProjects - a.completedProjects;
      } else if (sortBy === "reviews") {
        return b.reviewCount - a.reviewCount;
      }
      return 0;
    });

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl mb-2">개발자 찾기</h1>
          <p className="text-gray-600">검증된 전문 개발자들을 만나보세요</p>
        </div>

        {/* Filters */}
        <Card className="mb-6">
          <CardContent className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="relative md:col-span-2">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <Input
                  placeholder="개발자 또는 기술 검색..."
                  className="pl-10"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Select value={selectedSkill} onValueChange={setSelectedSkill}>
                  <SelectTrigger>
                    <SelectValue placeholder="기술" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체 기술</SelectItem>
                    {allSkills.map(skill => (
                      <SelectItem key={skill} value={skill}>
                        {skill}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Select value={sortBy} onValueChange={setSortBy}>
                  <SelectTrigger>
                    <SelectValue placeholder="정렬" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="rating">평점순</SelectItem>
                    <SelectItem value="projects">프로젝트순</SelectItem>
                    <SelectItem value="reviews">리뷰순</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Results */}
        <div className="mb-4 text-gray-600">
          총 {filteredDevelopers.length}명의 개발자
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredDevelopers.map((developer) => (
            <Link key={developer.id} to={`/developers/${developer.id}`}>
              <Card className="hover:shadow-lg transition-shadow h-full">
                <CardContent className="p-6">
                  {/* Avatar and Basic Info */}
                  <div className="text-center mb-4">
                    <ImageWithFallback
                      src={developer.avatar}
                      alt={developer.name}
                      className="w-24 h-24 rounded-full mx-auto mb-3"
                    />
                    <h3 className="text-xl mb-1">{developer.name}</h3>
                    <p className="text-gray-600 mb-2">{developer.title}</p>
                    <div className="flex items-center justify-center gap-2 mb-2">
                      <div className="flex items-center gap-1">
                        <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                        <span>{developer.rating}</span>
                      </div>
                      <span className="text-gray-500 text-sm">
                        ({developer.reviewCount}개 리뷰)
                      </span>
                    </div>
                  </div>

                  {/* Description */}
                  <p className="text-gray-600 text-sm mb-4 line-clamp-2">
                    {developer.description}
                  </p>

                  {/* Stats */}
                  <div className="grid grid-cols-2 gap-2 mb-4 text-sm">
                    <div className="flex items-center gap-2 text-gray-600">
                      <MapPin className="w-4 h-4" />
                      <span>{developer.location}</span>
                    </div>
                    <div className="flex items-center gap-2 text-gray-600">
                      <Clock className="w-4 h-4" />
                      <span>{developer.responseTime}</span>
                    </div>
                  </div>

                  {/* Skills */}
                  <div className="flex flex-wrap gap-1 mb-4">
                    {developer.skills.slice(0, 4).map((skill) => (
                      <Badge key={skill} variant="outline" className="text-xs">
                        {skill}
                      </Badge>
                    ))}
                    {developer.skills.length > 4 && (
                      <Badge variant="outline" className="text-xs">
                        +{developer.skills.length - 4}
                      </Badge>
                    )}
                  </div>

                  {/* Footer */}
                  <div className="pt-4 border-t flex justify-between items-center">
                    <div>
                      <div className="text-sm text-gray-600">시간당</div>
                      <div className="text-blue-600">{developer.hourlyRate}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-sm text-gray-600">완료 프로젝트</div>
                      <div>{developer.completedProjects}개</div>
                    </div>
                  </div>

                  <Button className="w-full mt-4">프로필 보기</Button>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>

        {filteredDevelopers.length === 0 && (
          <Card>
            <CardContent className="p-12 text-center text-gray-500">
              <Search className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p>검색 결과가 없습니다.</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
