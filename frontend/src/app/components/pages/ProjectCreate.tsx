import { useState } from "react";
import { useNavigate } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Textarea } from "../ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { Badge } from "../ui/badge";
import { ArrowLeft, X } from "lucide-react";
import { categories } from "../../data/mockData";
import { toast } from "sonner";

const skillOptions = [
  "React", "Vue.js", "Angular", "Node.js", "Python", "Java", "Swift",
  "Kotlin", "Flutter", "React Native", "TypeScript", "JavaScript",
  "MongoDB", "PostgreSQL", "MySQL", "AWS", "Docker", "Kubernetes",
  "TensorFlow", "PyTorch", "Solidity", "Web3.js", "Unity", "Unreal Engine"
];

export function ProjectCreate() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    title: "",
    category: "",
    description: "",
    budget: "",
    duration: "",
  });
  const [selectedSkills, setSelectedSkills] = useState<string[]>([]);
  const [skillInput, setSkillInput] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.title || !formData.category || !formData.description) {
      toast.error("필수 항목을 모두 입력해주세요");
      return;
    }

    toast.success("프로젝트가 등록되었습니다!");
    setTimeout(() => {
      navigate("/projects");
    }, 1000);
  };

  const addSkill = (skill: string) => {
    if (skill && !selectedSkills.includes(skill)) {
      setSelectedSkills([...selectedSkills, skill]);
      setSkillInput("");
    }
  };

  const removeSkill = (skill: string) => {
    setSelectedSkills(selectedSkills.filter(s => s !== skill));
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-3xl">
        <Button variant="ghost" className="mb-6" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          뒤로가기
        </Button>

        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">프로젝트 등록</CardTitle>
            <p className="text-gray-600">프로젝트 정보를 입력하고 최적의 개발자를 찾아보세요</p>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Project Title */}
              <div className="space-y-2">
                <Label htmlFor="title">프로젝트 제목 *</Label>
                <Input
                  id="title"
                  placeholder="예: 쇼핑몰 웹사이트 개발"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  required
                />
              </div>

              {/* Category */}
              <div className="space-y-2">
                <Label htmlFor="category">카테고리 *</Label>
                <Select
                  value={formData.category}
                  onValueChange={(value) => setFormData({ ...formData, category: value })}
                  required
                >
                  <SelectTrigger>
                    <SelectValue placeholder="카테고리 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map(category => (
                      <SelectItem key={category.id} value={category.id}>
                        {category.icon} {category.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label htmlFor="description">프로젝트 설명 *</Label>
                <Textarea
                  id="description"
                  placeholder="프로젝트에 대해 자세히 설명해주세요. 필요한 기능, 목표, 요구사항 등을 포함해주세요."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={6}
                  required
                />
                <p className="text-sm text-gray-500">
                  상세한 설명은 더 좋은 제안을 받을 수 있습니다
                </p>
              </div>

              {/* Skills */}
              <div className="space-y-2">
                <Label htmlFor="skills">필요 기술</Label>
                <Select value={skillInput} onValueChange={addSkill}>
                  <SelectTrigger>
                    <SelectValue placeholder="기술 스택 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {skillOptions
                      .filter(skill => !selectedSkills.includes(skill))
                      .map(skill => (
                        <SelectItem key={skill} value={skill}>
                          {skill}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                {selectedSkills.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-3">
                    {selectedSkills.map(skill => (
                      <Badge key={skill} variant="secondary" className="pr-1">
                        {skill}
                        <button
                          type="button"
                          onClick={() => removeSkill(skill)}
                          className="ml-2 hover:bg-gray-300 rounded-full p-0.5"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </Badge>
                    ))}
                  </div>
                )}
              </div>

              {/* Budget */}
              <div className="space-y-2">
                <Label htmlFor="budget">예산</Label>
                <Input
                  id="budget"
                  placeholder="예: 300-500만원"
                  value={formData.budget}
                  onChange={(e) => setFormData({ ...formData, budget: e.target.value })}
                />
                <p className="text-sm text-gray-500">
                  예상 예산 범위를 입력해주세요
                </p>
              </div>

              {/* Duration */}
              <div className="space-y-2">
                <Label htmlFor="duration">예상 기간</Label>
                <Input
                  id="duration"
                  placeholder="예: 2개월"
                  value={formData.duration}
                  onChange={(e) => setFormData({ ...formData, duration: e.target.value })}
                />
              </div>

              {/* Submit */}
              <div className="flex gap-3 pt-4">
                <Button type="submit" size="lg" className="flex-1">
                  프로젝트 등록하기
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  onClick={() => navigate(-1)}
                >
                  취소
                </Button>
              </div>

              {/* Tips */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h4 className="text-blue-900 mb-2">💡 등록 팁</h4>
                <ul className="text-sm text-blue-900 space-y-1">
                  <li>• 프로젝트 목표와 요구사항을 명확히 작성하세요</li>
                  <li>• 예산과 기간을 현실적으로 설정하세요</li>
                  <li>• 필요한 기술 스택을 정확히 선택하세요</li>
                  <li>• 참고 자료나 예시가 있다면 설명에 포함하세요</li>
                </ul>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
