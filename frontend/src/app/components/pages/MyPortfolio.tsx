/**
 * MyPortfolio.tsx - 개발자 포트폴리오 CRUD 전용 페이지
 * - 포트폴리오 목록 조회, 추가, 수정, 삭제
 * - DEVELOPER 전용
 */
import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Textarea } from "../ui/textarea";
import { Badge } from "../ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../ui/alert-dialog";
import {
  Plus,
  Pencil,
  Trash2,
  Loader2,
  ExternalLink,
  Image as ImageIcon,
  Briefcase,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { portfolioApi, developerApi } from "../../../api/apiService";
import type { PortfolioDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";

interface PortfolioForm {
  title: string;
  description: string;
  imageUrl: string;
  projectUrl: string;
  skills: string[];
}

const EMPTY_FORM: PortfolioForm = {
  title: "",
  description: "",
  imageUrl: "",
  projectUrl: "",
  skills: [],
};

export function MyPortfolio() {
  const navigate = useNavigate();
  const { isLoggedIn, userRole } = useAuth();

  const [portfolios, setPortfolios] = useState<PortfolioDto[]>([]);
  const [myDeveloperId, setMyDeveloperId] = useState<number | null>(null);
  const [skillOptions, setSkillOptions] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 모달 상태
  const [showModal, setShowModal] = useState(false);
  const [editTarget, setEditTarget] = useState<PortfolioDto | null>(null);
  const [form, setForm] = useState<PortfolioForm>(EMPTY_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 삭제 확인 다이얼로그
  const [deleteTarget, setDeleteTarget] = useState<PortfolioDto | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    if (!isLoggedIn) { navigate("/login"); return; }
    if (userRole !== "DEVELOPER") { navigate("/"); return; }
    fetchMyPortfolios();
    fetchSkills();
  }, [isLoggedIn, userRole]);

  const fetchSkills = async () => {
    try {
      const res = await apiClient.get("/v1/skills", { params: { page: 0, size: 200 } });
      setSkillOptions((res.data?.data?.content ?? []).map((s: any) => s.name));
    } catch (err) {
      console.error("스킬 목록 조회 실패:", err);
    }
  };

  const fetchMyPortfolios = async () => {
    setIsLoading(true);
    try {
      // 내 개발자 ID 찾기 (이름 기반)
      const meRes = await apiClient.get("/v1/users/me");
      const myName = meRes.data.data.name;

      const devRes = await developerApi.getAll(0, 200);
      const devs = devRes.data.data.content || [];
      const myDev = devs.find((d: any) => d.name === myName);

      if (!myDev) {
        toast.error("개발자 정보를 찾을 수 없습니다.");
        return;
      }
      setMyDeveloperId(myDev.id);

      const portRes = await portfolioApi.getByDeveloper(myDev.id, undefined, 1, 50);
      setPortfolios(portRes.data.data.content || []);
    } catch {
      toast.error("포트폴리오를 불러오는 데 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const openCreateModal = () => {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setShowModal(true);
  };

  const openEditModal = (portfolio: PortfolioDto) => {
    setEditTarget(portfolio);
    setForm({
      title: portfolio.title,
      description: portfolio.description,
      imageUrl: portfolio.imageUrl || "",
      projectUrl: portfolio.projectUrl || "",
      skills: [...portfolio.skills],
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) { toast.error("제목을 입력해주세요."); return; }
    if (!form.description.trim()) { toast.error("설명을 입력해주세요."); return; }
    if (form.skills.length === 0) { toast.error("기술을 최소 1개 입력해주세요."); return; }

    setIsSubmitting(true);
    try {
      if (editTarget) {
        // 수정 (백엔드: PATCH /v1/portfolios?developerId=)
        await portfolioApi.update(editTarget.id, {
          title: form.title,
          description: form.description,
          imageUrl: form.imageUrl || undefined,
          projectUrl: form.projectUrl || undefined,
          skills: form.skills,
        });
        toast.success("포트폴리오가 수정되었습니다.");
      } else {
        // 생성 (백엔드: POST /v1/portfolios)
        await portfolioApi.create({
          title: form.title,
          description: form.description,
          imageUrl: form.imageUrl || undefined,
          projectUrl: form.projectUrl || undefined,
          skills: form.skills,
        });
        toast.success("포트폴리오가 추가되었습니다.");
      }
      setShowModal(false);
      fetchMyPortfolios();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "저장에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      // 백엔드: DELETE /v1/portfolios?developerId={portfolioId}
      // 백엔드 PortfolioController를 보면 @RequestParam Long developerId 인데 실제로는 portfolioId임
      await portfolioApi.delete(deleteTarget.id);
      toast.success("포트폴리오가 삭제되었습니다.");
      setDeleteTarget(null);
      fetchMyPortfolios();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "삭제에 실패했습니다.");
    } finally {
      setIsDeleting(false);
    }
  };

  const addSkill = (skill: string) => {
    if (skill && !form.skills.includes(skill)) {
      setForm((prev) => ({ ...prev, skills: [...prev.skills, skill] }));
    }
  };

  const removeSkill = (skill: string) => {
    setForm((prev) => ({ ...prev, skills: prev.skills.filter((s) => s !== skill) }));
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-4xl">
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-3">
            <div className="bg-primary p-2 rounded-xl">
              <Briefcase className="w-6 h-6 text-primary-foreground" />
            </div>
            <h1 className="text-3xl font-bold text-foreground">내 포트폴리오</h1>
          </div>
          <Button onClick={openCreateModal} className="rounded-xl bg-primary hover:bg-primary/90 text-primary-foreground font-bold h-11 px-6 shadow-lg shadow-primary/20">
            <Plus className="w-4 h-4 mr-2" />
            포트폴리오 추가
          </Button>
        </div>

        {portfolios.length === 0 ? (
          <Card className="bg-card border-border border-dashed rounded-3xl">
            <CardContent className="p-16 text-center text-muted-foreground">
              <Briefcase className="w-16 h-16 mx-auto mb-4 opacity-20" />
              <p className="text-xl font-bold mb-2 text-foreground">아직 포트폴리오가 없습니다.</p>
              <p className="text-sm mb-8 text-muted-foreground">
                첫 번째 포트폴리오를 추가하여 클라이언트에게 역량을 보여주세요!
              </p>
              <Button onClick={openCreateModal} className="rounded-xl bg-primary hover:bg-primary/90 text-primary-foreground font-bold px-8 h-12">
                <Plus className="w-4 h-4 mr-2" />
                첫 포트폴리오 추가
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {portfolios.map((portfolio) => (
              <Card key={portfolio.id} className="bg-card border-border hover:shadow-xl hover:shadow-primary/5 transition-all duration-300 rounded-2xl group overflow-hidden">
                {portfolio.imageUrl ? (
                  <div className="relative aspect-video overflow-hidden bg-secondary/30">
                    <img
                      src={portfolio.imageUrl}
                      alt={portfolio.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                      onError={(e) => {
                        (e.target as HTMLImageElement).parentElement!.style.display = "none";
                      }}
                    />
                  </div>
                ) : (
                  <div className="aspect-video bg-gradient-to-br from-primary/5 to-primary/10 flex items-center justify-center">
                    <ImageIcon className="w-16 h-16 text-primary/20" />
                  </div>
                )}
                <CardContent className="p-6">
                  <div className="flex justify-between items-start mb-4">
                    <h3 className="font-bold text-xl text-foreground line-clamp-1 group-hover:text-primary transition-colors">
                      {portfolio.title}
                    </h3>
                    <div className="flex gap-2 ml-2 shrink-0">
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-primary hover:bg-primary/10 h-9 w-9 p-0 rounded-lg"
                        onClick={() => openEditModal(portfolio)}
                      >
                        <Pencil className="w-4 h-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-destructive hover:bg-destructive/10 h-9 w-9 p-0 rounded-lg"
                        onClick={() => setDeleteTarget(portfolio)}
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>

                  <p className="text-muted-foreground text-sm mb-6 line-clamp-2 leading-relaxed h-10">
                    {portfolio.description}
                  </p>

                  <div className="flex flex-wrap gap-1.5 mb-6 min-h-[28px]">
                    {portfolio.skills.map((skill) => (
                      <Badge key={skill} variant="outline" className="text-[10px] bg-secondary/30 border-border text-muted-foreground px-2 py-0">
                        {skill}
                      </Badge>
                    ))}
                  </div>

                  <div className="flex items-center justify-between pt-4 border-t border-border/50">
                    {portfolio.projectUrl ? (
                      <a
                        href={portfolio.projectUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-primary text-xs font-bold flex items-center gap-1.5 hover:underline"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                        프로젝트 보기
                      </a>
                    ) : <div />}
                    <p className="text-[11px] text-muted-foreground/50 font-medium">
                      {new Date(portfolio.createdAt).toLocaleDateString("ko-KR")}
                    </p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* 추가/수정 모달 */}
      <Dialog open={showModal} onOpenChange={setShowModal}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto bg-card border-border rounded-3xl">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold text-foreground">
              {editTarget ? "포트폴리오 수정" : "포트폴리오 추가"}
            </DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-6 mt-4">
            <div className="space-y-2">
              <Label className="text-sm font-bold text-muted-foreground ml-1">제목 *</Label>
              <Input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="포트폴리오 제목"
                className="h-12 bg-secondary/30 border-border rounded-xl focus:ring-primary/20"
                required
              />
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-bold text-muted-foreground ml-1">설명 *</Label>
              <Textarea
                value={form.description}
                onChange={(e) =>
                  setForm({ ...form, description: e.target.value })
                }
                placeholder="프로젝트 설명, 역할, 성과 등을 작성해주세요."
                rows={4}
                className="bg-secondary/30 border-border rounded-xl focus:ring-primary/20 p-4 resize-none"
                required
              />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label className="text-sm font-bold text-muted-foreground ml-1">이미지 URL</Label>
                <Input
                  value={form.imageUrl}
                  onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                  placeholder="https://..."
                  className="h-12 bg-secondary/30 border-border rounded-xl"
                />
              </div>
              <div className="space-y-2">
                <Label className="text-sm font-bold text-muted-foreground ml-1">프로젝트 링크</Label>
                <Input
                  value={form.projectUrl}
                  onChange={(e) =>
                    setForm({ ...form, projectUrl: e.target.value })
                  }
                  placeholder="https://github.com/..."
                  className="h-12 bg-secondary/30 border-border rounded-xl"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label className="text-sm font-bold text-muted-foreground ml-1">기술 스택 * (최소 1개)</Label>
              <Select onValueChange={(val) => addSkill(val)}>
                <SelectTrigger className="h-12 bg-secondary/30 border-border rounded-xl">
                  <SelectValue placeholder="기술 스택 선택" />
                </SelectTrigger>
                <SelectContent className="bg-card border-border">
                  {skillOptions.map((skill) => (
                    <SelectItem key={skill} value={skill}>
                      {skill}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {form.skills.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-3 p-3 bg-secondary/20 rounded-xl border border-border/50">
                  {form.skills.map((skill) => (
                    <Badge key={skill} variant="secondary" className="bg-primary/10 text-primary border-none font-bold px-3 py-1">
                      {skill}
                      <button
                        type="button"
                        onClick={() => removeSkill(skill)}
                        className="ml-2 hover:bg-primary/20 rounded-full p-0.5"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              )}
            </div>
            <div className="flex gap-3 pt-4">
              <Button type="submit" className="flex-1 h-12 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl" disabled={isSubmitting}>
                {isSubmitting ? (
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                ) : null}
                {editTarget ? "수정 완료" : "추가 완료"}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="h-12 border-border text-foreground hover:bg-secondary font-bold rounded-xl px-8"
                onClick={() => setShowModal(false)}
              >
                취소
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 삭제 확인 다이얼로그 */}
      <AlertDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>포트폴리오 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              "{deleteTarget?.title}" 포트폴리오를 삭제하시겠습니까? 이 작업은
              되돌릴 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-600 hover:bg-red-700"
              disabled={isDeleting}
            >
              {isDeleting ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : null}
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
