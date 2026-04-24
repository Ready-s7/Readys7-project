/**
 * ProjectCreate.tsx - 프로젝트 등록 페이지 (UI 통합 버전)
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
import { ArrowLeft, X, Loader2, Info, Briefcase, Tag, Target, Calendar, Wallet } from "lucide-react";
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
    duration: "",
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

    if (!formData.title.trim()) { toast.error("프로젝트 제목을 입력해주세요."); return; }
    if (!formData.categoryId) { toast.error("카테고리를 선택해주세요."); return; }
    if (!formData.description.trim()) { toast.error("프로젝트 설명을 입력해주세요."); return; }

    const minBudget = Number(formData.minBudget);
    const maxBudget = Number(formData.maxBudget);
    const duration = Number(formData.duration);
    const maxProposalCount = Number(formData.maxProposalCount);

    if (!minBudget || minBudget <= 0) { toast.error("올바른 최소 예산을 입력해주세요."); return; }
    if (!maxBudget || maxBudget <= 0) { toast.error("올바른 최대 예산을 입력해주세요."); return; }
    if (minBudget > maxBudget) { toast.error("최소 예산은 최대 예산보다 클 수 없습니다."); return; }
    if (!duration || duration <= 0) { toast.error("올바른 기간을 입력해주세요."); return; }
    if (selectedSkills.length === 0) { toast.error("기술 스택을 최소 1개 이상 선택해주세요."); return; }
    if (!maxProposalCount || maxProposalCount <= 0) { toast.error("최대 제안서 수를 입력해주세요."); return; }

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
      toast.error(err?.response?.data?.message || "프로젝트 등록에 실패했습니다.");
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

  if (isLoadingInit) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center gap-4">
        <Loader2 className="w-12 h-12 animate-spin text-primary" />
        <p className="text-muted-foreground font-bold tracking-widest">INITIALIZING FORM...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8 pb-20">
      <div className="container mx-auto px-4 max-w-4xl">
        <Button variant="ghost" className="mb-8 text-muted-foreground hover:text-foreground font-bold hover:bg-secondary rounded-xl" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          뒤로가기
        </Button>

        <Card className="bg-card border-border shadow-2xl rounded-[40px] overflow-hidden">
          <CardHeader className="bg-secondary/10 border-b border-border/50 pb-10 pt-12 px-10">
            <div className="flex items-center gap-4 mb-4">
              <div className="bg-primary p-3 rounded-2xl shadow-lg shadow-primary/20">
                <Briefcase className="w-8 h-8 text-primary-foreground" />
              </div>
              <div>
                <CardTitle className="text-4xl font-black text-foreground tracking-tight">새 프로젝트 등록</CardTitle>
                <p className="text-muted-foreground font-medium mt-1">
                  Ready's7의 검증된 전문가들에게 당신의 아이디어를 제안하세요.
                </p>
              </div>
            </div>
          </CardHeader>
          <CardContent className="p-10">
            <form onSubmit={handleSubmit} className="space-y-12">
              
              {/* ── 기본 정보 섹션 ── */}
              <div className="space-y-8">
                <div className="flex items-center gap-2 text-primary font-black uppercase tracking-[0.2em] text-xs">
                  <Target className="w-4 h-4" /> Basic Information
                </div>
                
                <div className="space-y-3">
                  <Label htmlFor="title" className="text-sm font-black text-foreground ml-1">프로젝트 제목 *</Label>
                  <Input
                    id="title"
                    placeholder="예: 기업용 AI 챗봇 구축 및 서비스 연동"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    required
                    disabled={isSubmitting}
                    className="h-14 bg-secondary/30 border-border text-foreground rounded-2xl focus:ring-primary/20 text-lg font-bold px-6"
                  />
                </div>

                <div className="space-y-3">
                  <Label htmlFor="category" className="text-sm font-black text-foreground ml-1">비즈니스 카테고리 *</Label>
                  <Select value={formData.categoryId} onValueChange={(v) => setFormData({ ...formData, categoryId: v })}>
                    <SelectTrigger className="h-14 bg-secondary/30 border-border text-foreground rounded-2xl font-bold px-6 focus:ring-primary/20">
                      <SelectValue placeholder="카테고리를 선택해 주세요" />
                    </SelectTrigger>
                    <SelectContent className="bg-card border-border">
                      {categories.map((c) => (
                        <SelectItem key={c.id} value={String(c.id)} className="font-bold py-3 cursor-pointer">
                          <span className="mr-2">{c.icon}</span> {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* ── 상세 요구사항 섹션 ── */}
              <div className="space-y-8">
                <div className="flex items-center gap-2 text-primary font-black uppercase tracking-[0.2em] text-xs">
                  <Tag className="w-4 h-4" /> Requirements & Skills
                </div>

                <div className="space-y-3">
                  <Label htmlFor="description" className="text-sm font-black text-foreground ml-1">상세 요구사항 설명 *</Label>
                  <Textarea
                    id="description"
                    placeholder="프로젝트의 배경, 필수 기능 리스트, 선호하는 기술 스택 및 작업 방식을 자유롭게 적어주세요."
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    rows={10}
                    required
                    disabled={isSubmitting}
                    className="resize-none bg-secondary/30 border-border text-foreground rounded-[24px] p-8 font-medium leading-relaxed focus:ring-primary/20"
                  />
                  <div className="bg-primary/5 p-4 rounded-xl border border-primary/10">
                    <p className="text-xs text-primary font-bold flex items-center gap-2">
                      <Info className="w-4 h-4" /> 요구사항이 구체적일수록 더 정확한 견적의 제안을 받을 수 있습니다.
                    </p>
                  </div>
                </div>

                <div className="space-y-3">
                  <Label className="text-sm font-black text-foreground ml-1">필요 기술 스택 *</Label>
                  <Select value={skillInput} onValueChange={(val) => { addSkill(val); setSkillInput(""); }}>
                    <SelectTrigger className="h-14 bg-secondary/30 border-border text-foreground rounded-2xl font-bold px-6">
                      <SelectValue placeholder="필요한 핵심 기술을 목록에서 선택하세요" />
                    </SelectTrigger>
                    <SelectContent className="bg-card border-border">
                      {allSkills.filter((s) => !selectedSkills.includes(s)).map((s) => (
                        <SelectItem key={s} value={s} className="font-medium cursor-pointer">{s}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {selectedSkills.length > 0 && (
                    <div className="flex flex-wrap gap-2 mt-4 p-5 bg-secondary/20 rounded-[20px] border border-border/50">
                      {selectedSkills.map((skill) => (
                        <Badge key={skill} variant="secondary" className="pl-4 pr-1.5 py-2 flex items-center gap-2 bg-background text-primary border border-primary/20 font-black rounded-xl text-xs shadow-sm">
                          {skill}
                          <button type="button" onClick={() => removeSkill(skill)} className="hover:bg-primary/10 rounded-full p-0.5 transition-colors">
                            <X className="w-4 h-4" />
                          </button>
                        </Badge>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* ── 예산 및 기간 섹션 ── */}
              <div className="space-y-8">
                <div className="flex items-center gap-2 text-primary font-black uppercase tracking-[0.2em] text-xs">
                  <Wallet className="w-4 h-4" /> Budget & Timeline
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <Label className="text-sm font-black text-foreground ml-1">최소 예상 예산 *</Label>
                    <div className="relative group">
                      <span className="absolute left-6 top-1/2 -translate-y-1/2 text-muted-foreground font-black text-xs group-focus-within:text-primary transition-colors">KRW</span>
                      <Input
                        type="number"
                        placeholder="0"
                        value={formData.minBudget}
                        onChange={(e) => setFormData({ ...formData, minBudget: e.target.value })}
                        required
                        disabled={isSubmitting}
                        className="h-14 bg-secondary/30 border-border text-foreground rounded-2xl pl-16 font-black text-right pr-6 focus:ring-primary/20 text-lg"
                      />
                    </div>
                  </div>
                  <div className="space-y-3">
                    <Label className="text-sm font-black text-foreground ml-1">최대 예상 예산 *</Label>
                    <div className="relative group">
                      <span className="absolute left-6 top-1/2 -translate-y-1/2 text-muted-foreground font-black text-xs group-focus-within:text-primary transition-colors">KRW</span>
                      <Input
                        type="number"
                        placeholder="0"
                        value={formData.maxBudget}
                        onChange={(e) => setFormData({ ...formData, maxBudget: e.target.value })}
                        required
                        disabled={isSubmitting}
                        className="h-14 bg-secondary/30 border-border text-foreground rounded-2xl pl-16 font-black text-right pr-6 focus:ring-primary/20 text-lg"
                      />
                    </div>
                  </div>
                </div>
                
                {formData.minBudget && formData.maxBudget && Number(formData.minBudget) > Number(formData.maxBudget) && (
                  <p className="text-xs text-destructive font-black bg-destructive/5 p-3 rounded-xl border border-destructive/10 text-center tracking-tight">
                    ⚠ 최소 예산 설정값이 최대 예산보다 높습니다. 확인해 주세요.
                  </p>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-3">
                    <Label htmlFor="duration" className="text-sm font-black text-foreground ml-1 flex items-center gap-2">
                      <Calendar className="w-4 h-4 text-muted-foreground" /> 개발 희망 기간 (일) *
                    </Label>
                    <Input
                      id="duration"
                      type="number"
                      placeholder="예: 30"
                      value={formData.duration}
                      onChange={(e) => setFormData({ ...formData, duration: e.target.value })}
                      required
                      disabled={isSubmitting}
                      className="h-14 bg-secondary/30 border-border text-foreground rounded-2xl font-black focus:ring-primary/20 px-6 text-lg"
                    />
                  </div>
                  <div className="space-y-3">
                    <Label htmlFor="maxProposalCount" className="text-sm font-black text-foreground ml-1 flex items-center gap-2">
                      <Target className="w-4 h-4 text-muted-foreground" /> 최대 제안 수신 제한 *
                    </Label>
                    <Input
                      id="maxProposalCount"
                      type="number"
                      placeholder="예: 10"
                      value={formData.maxProposalCount}
                      onChange={(e) => setFormData({ ...formData, maxProposalCount: e.target.value })}
                      required
                      disabled={isSubmitting}
                      className="h-14 bg-secondary/30 border-border text-foreground rounded-2xl font-black focus:ring-primary/20 px-6 text-lg"
                    />
                  </div>
                </div>
              </div>

              {/* ── 제출 버튼 ── */}
              <div className="flex flex-col md:flex-row gap-4 pt-10 border-t border-border/50">
                <Button
                  type="submit"
                  size="lg"
                  className="flex-1 bg-primary hover:bg-primary/90 text-primary-foreground font-black h-18 text-xl rounded-2xl shadow-2xl shadow-primary/30 transition-all hover:scale-[1.01] active:scale-[0.99] py-8"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-6 h-6 mr-3 animate-spin" />
                      데이터 무결성 검사 및 등록 중...
                    </>
                  ) : (
                    "프로젝트 공개 및 전문가 모집 시작"
                  )}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  className="md:w-40 border-border text-foreground hover:bg-secondary h-18 rounded-2xl font-black py-8"
                  onClick={() => navigate(-1)}
                  disabled={isSubmitting}
                >
                  등록 취소
                </Button>
              </div>

              <div className="bg-foreground text-background rounded-3xl p-8 flex items-start gap-6 shadow-2xl">
                <div className="bg-primary p-3 rounded-2xl mt-1">
                  <Info className="w-6 h-6 text-primary-foreground" />
                </div>
                <div className="space-y-2">
                  <h4 className="text-primary font-black text-lg uppercase tracking-widest italic">
                    Ready's7 Policy
                  </h4>
                  <ul className="text-sm font-bold opacity-70 space-y-2 leading-relaxed">
                    <li>• 등록된 프로젝트는 검수 후 즉시 공개되며, 적합한 전문가들에게 알림이 발송됩니다.</li>
                    <li>• 허위 정보 기재나 부적절한 게시물은 관리자에 의해 제재될 수 있습니다.</li>
                    <li>• 전문가와의 매칭 후 계약 체결 전까지는 어떠한 비용도 발생하지 않습니다.</li>
                  </ul>
                </div>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
