/**
 * ProjectCreate.tsx 수정 사항
 *
 * [버그 수정]
 * 1. 예산 칸이 하나였던 문제 → 최소예산 / 최대예산 두 칸으로 분리
 * 2. 기간(duration)이 텍스트였던 문제 → 백엔드 Integer 타입에 맞게 숫자 입력으로 변경
 * 3. 카테고리를 mockData에서 가져오던 문제 → 실제 API(/v1/categories)에서 가져오도록 수정
 * 4. 폼 제출 시 실제 API(/v1/projects)를 호출하도록 수정 (기존은 toast만 띄우고 DB 저장 없음)
 * 5. 로그인 안 된 경우 또는 DEVELOPER/ADMIN이 접근 시 접근 제한 처리
 *
 * [UX 개선]
 * 1. 예산 입력 시 최소 > 최대 유효성 검사
 * 2. 제출 중 로딩 스피너
 * 3. 최대 제안서 수 입력 추가 (기존 누락)
 */
import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Textarea } from "../ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { Badge } from "../ui/badge";
import { ArrowLeft, X, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { categoryApi, projectApi, skillApi } from "../../../api/apiService";
import type { CategoryDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";

export function ProjectCreate() {
  const navigate = useNavigate();
  const { isLoggedIn, userRole } = useAuth();

  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [allSkills, setAllSkills] = useState<string[]>([]);
  const [isLoadingInit, setIsLoadingInit] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    title: "",
    categoryId: "",
    description: "",
    minBudget: "",
    maxBudget: "",
    duration: "",        // 백엔드 Integer (일 단위)
    maxProposalCount: "10",
  });
  const [selectedSkills, setSelectedSkills] = useState<string[]>([]);
  const [skillInput, setSkillInput] = useState("");

  // 로그인/권한 체크
  useEffect(() => {
    if (!isLoggedIn) {
      toast.error("로그인이 필요합니다.");
      navigate("/login");
      return;
    }
    if (userRole !== "CLIENT") {
      toast.error("클라이언트 계정만 프로젝트를 등록할 수 있습니다.");
      navigate("/projects");
      return;
    }
  }, [isLoggedIn, userRole]);

  // 카테고리 + 스킬 목록 로드
  useEffect(() => {
    const fetchInit = async () => {
      setIsLoadingInit(true);
      try {
        const [catRes, skillRes] = await Promise.allSettled([
          categoryApi.getAll(),
          skillApi.getAll(0, 100),
        ]);
        if (catRes.status === "fulfilled") {
          setCategories(catRes.value.data.data);
        }
        if (skillRes.status === "fulfilled") {
          const names = skillRes.value.data.data.content.map((s) => s.name);
          setAllSkills(names);
        }
      } finally {
        setIsLoadingInit(false);
      }
    };
    fetchInit();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 유효성 검사
    if (!formData.title.trim()) {
      toast.error("프로젝트 제목을 입력해주세요.");
      return;
    }
    if (!formData.categoryId) {
      toast.error("카테고리를 선택해주세요.");
      return;
    }
    if (!formData.description.trim()) {
      toast.error("프로젝트 설명을 입력해주세요.");
      return;
    }

    const minBudget = Number(formData.minBudget);
    const maxBudget = Number(formData.maxBudget);
    const duration = Number(formData.duration);
    const maxProposalCount = Number(formData.maxProposalCount);

    if (!minBudget || minBudget <= 0) {
      toast.error("올바른 최소 예산을 입력해주세요.");
      return;
    }
    if (!maxBudget || maxBudget <= 0) {
      toast.error("올바른 최대 예산을 입력해주세요.");
      return;
    }
    if (minBudget > maxBudget) {
      toast.error("최소 예산은 최대 예산보다 클 수 없습니다.");
      return;
    }
    if (!duration || duration <= 0) {
      toast.error("올바른 기간을 입력해주세요. (숫자, 일 단위)");
      return;
    }
    if (selectedSkills.length === 0) {
      toast.error("기술 스택을 최소 1개 이상 선택해주세요.");
      return;
    }
    if (!maxProposalCount || maxProposalCount <= 0) {
      toast.error("최대 제안서 수를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await projectApi.create({
        title: formData.title,
        description: formData.description,
        categoryId: Number(formData.categoryId),
        minBudget,
        maxBudget,
        duration,
        skills: selectedSkills,
        maxProposalCount,
      });
      toast.success("프로젝트가 성공적으로 등록되었습니다!");
      navigate(`/projects/${res.data.data.id}`);
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || "프로젝트 등록에 실패했습니다.";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const addSkill = (skill: string) => {
    if (skill && !selectedSkills.includes(skill)) {
      setSelectedSkills([...selectedSkills, skill]);
      setSkillInput("");
    }
  };

  const removeSkill = (skill: string) => {
    setSelectedSkills(selectedSkills.filter((s) => s !== skill));
  };

  // 직접 입력 스킬 추가
  const handleSkillDirectInput = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      const val = (e.target as HTMLInputElement).value.trim();
      if (val) addSkill(val);
      (e.target as HTMLInputElement).value = "";
    }
  };

  if (isLoadingInit) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-3xl">
        <Button variant="ghost" className="mb-6 text-muted-foreground hover:text-foreground" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          뒤로가기
        </Button>

        <Card className="bg-card border-border shadow-xl">
          <CardHeader>
            <CardTitle className="text-2xl font-bold text-foreground">프로젝트 등록</CardTitle>
            <p className="text-muted-foreground">
              프로젝트 정보를 입력하고 최적의 개발자를 찾아보세요
            </p>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* 제목 */}
              <div className="space-y-2">
                <Label htmlFor="title" className="text-sm font-semibold text-foreground">프로젝트 제목 *</Label>
                <Input
                  id="title"
                  placeholder="예: 쇼핑몰 웹사이트 개발"
                  value={formData.title}
                  onChange={(e) =>
                    setFormData({ ...formData, title: e.target.value })
                  }
                  required
                  disabled={isSubmitting}
                  className="h-11 bg-secondary/30 border-border text-foreground"
                />
              </div>

              {/* 카테고리 */}
              <div className="space-y-2">
                <Label htmlFor="category" className="text-sm font-semibold text-foreground">카테고리 *</Label>
                <Select
                  value={formData.categoryId}
                  onValueChange={(value) =>
                    setFormData({ ...formData, categoryId: value })
                  }
                >
                  <SelectTrigger className="h-11 bg-secondary/30 border-border text-foreground">
                    <SelectValue placeholder="카테고리 선택" />
                  </SelectTrigger>
                  <SelectContent className="bg-card border-border">
                    {categories.map((category) => (
                      <SelectItem
                        key={category.id}
                        value={String(category.id)}
                      >
                        {category.icon} {category.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 설명 */}
              <div className="space-y-2">
                <Label htmlFor="description" className="text-sm font-semibold text-foreground">프로젝트 설명 *</Label>
                <Textarea
                  id="description"
                  placeholder="프로젝트에 대해 자세히 설명해주세요. 필요한 기능, 목표, 요구사항 등을 포함해주세요."
                  value={formData.description}
                  onChange={(e) =>
                    setFormData({ ...formData, description: e.target.value })
                  }
                  rows={6}
                  required
                  disabled={isSubmitting}
                  className="resize-none bg-secondary/30 border-border text-foreground"
                />
                <p className="text-xs text-muted-foreground">
                  상세한 설명은 더 좋은 제안을 받을 수 있습니다
                </p>
              </div>

              {/* 기술 스택 */}
              <div className="space-y-2">
                <Label className="text-sm font-semibold text-foreground">필요 기술 * (최소 1개)</Label>
                <Select value={skillInput} onValueChange={(val) => { addSkill(val); setSkillInput(""); }}>
                  <SelectTrigger className="h-11 bg-secondary/30 border-border text-foreground">
                    <SelectValue placeholder="목록에서 기술 선택" />
                  </SelectTrigger>
                  <SelectContent className="bg-card border-border">
                    {allSkills
                      .filter((skill) => !selectedSkills.includes(skill))
                      .map((skill) => (
                        <SelectItem key={skill} value={skill}>
                          {skill}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                {selectedSkills.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-3">
                    {selectedSkills.map((skill) => (
                      <Badge key={skill} variant="secondary" className="pl-3 pr-1 py-1 flex items-center gap-1 bg-secondary text-secondary-foreground border-none">
                        {skill}
                        <button
                          type="button"
                          onClick={() => removeSkill(skill)}
                          className="hover:text-destructive transition-colors"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </Badge>
                    ))}
                  </div>
                )}
              </div>

              {/* 예산 - 최소/최대 분리 */}
              <div className="space-y-2">
                <Label className="text-sm font-semibold text-foreground">예산 *</Label>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">최소 예산 (원)</p>
                    <Input
                      type="number"
                      placeholder="예: 3000000"
                      min={0}
                      value={formData.minBudget}
                      onChange={(e) =>
                        setFormData({ ...formData, minBudget: e.target.value })
                      }
                      required
                      disabled={isSubmitting}
                      className="h-11 bg-secondary/30 border-border text-foreground"
                    />
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">최대 예산 (원)</p>
                    <Input
                      type="number"
                      placeholder="예: 5000000"
                      min={0}
                      value={formData.maxBudget}
                      onChange={(e) =>
                        setFormData({ ...formData, maxBudget: e.target.value })
                      }
                      required
                      disabled={isSubmitting}
                      className="h-11 bg-secondary/30 border-border text-foreground"
                    />
                  </div>
                </div>
                {formData.minBudget &&
                  formData.maxBudget &&
                  Number(formData.minBudget) > Number(formData.maxBudget) && (
                    <p className="text-xs text-destructive">
                      최소 예산이 최대 예산보다 클 수 없습니다.
                    </p>
                  )}
              </div>

              {/* 기간 - 숫자(일) */}
              <div className="space-y-2">
                <Label htmlFor="duration" className="text-sm font-semibold text-foreground">예상 기간 * (일 단위)</Label>
                <Input
                  id="duration"
                  type="number"
                  placeholder="예: 30 (30일)"
                  min={1}
                  value={formData.duration}
                  onChange={(e) =>
                    setFormData({ ...formData, duration: e.target.value })
                  }
                  required
                  disabled={isSubmitting}
                  className="h-11 bg-secondary/30 border-border text-foreground"
                />
                <p className="text-xs text-muted-foreground">
                  숫자로 입력해주세요. (예: 30일이면 30 입력)
                </p>
              </div>

              {/* 최대 제안서 수 */}
              <div className="space-y-2">
                <Label htmlFor="maxProposalCount" className="text-sm font-semibold text-foreground">최대 제안서 수 *</Label>
                <Input
                  id="maxProposalCount"
                  type="number"
                  placeholder="예: 10"
                  min={1}
                  max={100}
                  value={formData.maxProposalCount}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      maxProposalCount: e.target.value,
                    })
                  }
                  required
                  disabled={isSubmitting}
                  className="h-11 bg-secondary/30 border-border text-foreground"
                />
                <p className="text-xs text-muted-foreground">
                  이 수에 도달하면 모집이 자동으로 마감됩니다.
                </p>
              </div>

              {/* 제출 */}
              <div className="flex gap-3 pt-4">
                <Button
                  type="submit"
                  size="lg"
                  className="flex-1 bg-primary hover:bg-primary/90 text-primary-foreground font-bold shadow-lg shadow-primary/10 h-12"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      등록 중...
                    </>
                  ) : (
                    "프로젝트 등록하기"
                  )}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  className="border-border text-foreground hover:bg-secondary h-12"
                  onClick={() => navigate(-1)}
                  disabled={isSubmitting}
                >
                  취소
                </Button>
              </div>

              <div className="bg-secondary/50 border border-border rounded-xl p-4">
                <h4 className="text-primary font-bold mb-2 flex items-center gap-2 text-sm">
                   💡 등록 팁
                </h4>
                <ul className="text-xs text-muted-foreground space-y-1">
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
