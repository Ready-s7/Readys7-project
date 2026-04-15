/**
 * AdminDashboard.tsx - 관리자 전용 대시보드
 * - SUPER_ADMIN: 대기 중 관리자 승인/거절
 * - 카테고리 관리 (CRUD)
 * - 스킬 관리 (CRUD)
 * - ADMIN 전용 페이지
 */
import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Badge } from "../ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../ui/dialog";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import {
  CheckCircle,
  XCircle,
  Loader2,
  Shield,
  Tag,
  Wrench,
  Plus,
  Pencil,
  Trash2,
  Users,
} from "lucide-react";
import { toast } from "sonner";
import { adminApi, categoryApi, skillApi } from "../../../api/apiService";
import type { AdminDto, CategoryDto, SkillDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";

export function AdminDashboard() {
  const navigate = useNavigate();
  const { isLoggedIn, userRole } = useAuth();

  const [isSuperAdmin, setIsSuperAdmin] = useState(false);
  const [pendingAdmins, setPendingAdmins] = useState<AdminDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [skills, setSkills] = useState<SkillDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 처리 중인 adminId
  const [processingId, setProcessingId] = useState<number | null>(null);

  // 카테고리 모달
  const [showCatModal, setShowCatModal] = useState(false);
  const [editCat, setEditCat] = useState<CategoryDto | null>(null);
  const [catForm, setCatForm] = useState({
    name: "",
    icon: "",
    description: "",
    displayOrder: "",
  });
  const [isCatSubmitting, setIsCatSubmitting] = useState(false);
  const [deleteCat, setDeleteCat] = useState<CategoryDto | null>(null);

  // 스킬 모달
  const [showSkillModal, setShowSkillModal] = useState(false);
  const [editSkill, setEditSkill] = useState<SkillDto | null>(null);
  const [skillForm, setSkillForm] = useState({ name: "", category: "" });
  const [isSkillSubmitting, setIsSkillSubmitting] = useState(false);
  const [deleteSkill, setDeleteSkill] = useState<SkillDto | null>(null);

  const SKILL_CATEGORIES = [
    "FRONTEND", "BACKEND", "DEVOPS", "GAME", "MOBILE", "EMBEDDED", "BIGDATA", "AI",
  ];

  useEffect(() => {
    if (!isLoggedIn) { navigate("/login"); return; }
    if (userRole !== "ADMIN") { navigate("/"); return; }
    loadAll();
  }, [isLoggedIn, userRole]);

  const loadAll = async () => {
    setIsLoading(true);
    try {
      // 내 관리자 정보 확인 (슈퍼어드민 여부)
      const meRes = await apiClient.get("/v1/users/me");
      const myName = meRes.data.data.name;

      // 대기 중 관리자 목록 (SUPER_ADMIN만 가능)
      try {
        const adminsRes = await adminApi.getPendingList(1, 20);
        setPendingAdmins(adminsRes.data.data.admins || []);
        setIsSuperAdmin(true);
      } catch {
        setIsSuperAdmin(false);
      }

      const [catRes, skillRes] = await Promise.allSettled([
        categoryApi.getAll(),
        skillApi.getAll(0, 200),
      ]);
      if (catRes.status === "fulfilled") setCategories(catRes.value.data.data);
      if (skillRes.status === "fulfilled") setSkills(skillRes.value.data.data.content);
    } finally {
      setIsLoading(false);
    }
  };

  // ── 관리자 승인/거절 ──
  const handleAdminAction = async (adminId: number, action: "APPROVED" | "REJECTED") => {
    setProcessingId(adminId);
    try {
      await adminApi.updateStatus(adminId, action);
      toast.success(action === "APPROVED" ? "관리자를 승인했습니다." : "관리자를 거절했습니다.");
      setPendingAdmins((prev) => prev.filter((a) => a.id !== adminId));
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "처리에 실패했습니다.");
    } finally {
      setProcessingId(null);
    }
  };

  // ── 카테고리 CRUD ──
  const openCatCreate = () => {
    setEditCat(null);
    setCatForm({ name: "", icon: "", description: "", displayOrder: "" });
    setShowCatModal(true);
  };

  const openCatEdit = (cat: CategoryDto) => {
    setEditCat(cat);
    setCatForm({
      name: cat.name,
      icon: cat.icon || "",
      description: cat.description || "",
      displayOrder: cat.displayOrder?.toString() || "",
    });
    setShowCatModal(true);
  };

  const handleCatSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!catForm.name.trim()) { toast.error("카테고리 이름을 입력해주세요."); return; }
    if (!catForm.displayOrder) { toast.error("정렬 순서를 입력해주세요."); return; }
    setIsCatSubmitting(true);
    try {
      if (editCat) {
        await apiClient.patch(`/v1/categories/${editCat.id}`, {
          name: catForm.name,
          icon: catForm.icon || undefined,
          description: catForm.description || undefined,
          displayOrder: Number(catForm.displayOrder),
        });
        toast.success("카테고리가 수정되었습니다.");
      } else {
        await apiClient.post("/v1/categories", {
          name: catForm.name,
          icon: catForm.icon || undefined,
          description: catForm.description || undefined,
          displayOrder: Number(catForm.displayOrder),
        });
        toast.success("카테고리가 생성되었습니다.");
      }
      setShowCatModal(false);
      const res = await categoryApi.getAll();
      setCategories(res.data.data);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "저장에 실패했습니다.");
    } finally {
      setIsCatSubmitting(false);
    }
  };

  const handleCatDelete = async () => {
    if (!deleteCat) return;
    try {
      await apiClient.delete(`/v1/categories/${deleteCat.id}`);
      toast.success("카테고리가 삭제되었습니다.");
      setDeleteCat(null);
      const res = await categoryApi.getAll();
      setCategories(res.data.data);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "삭제에 실패했습니다.");
    }
  };

  // ── 스킬 CRUD ──
  const openSkillCreate = () => {
    setEditSkill(null);
    setSkillForm({ name: "", category: "BACKEND" });
    setShowSkillModal(true);
  };

  const openSkillEdit = (skill: SkillDto) => {
    setEditSkill(skill);
    setSkillForm({ name: skill.name, category: skill.category });
    setShowSkillModal(true);
  };

  const handleSkillSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!skillForm.name.trim()) { toast.error("스킬 이름을 입력해주세요."); return; }
    if (!skillForm.category) { toast.error("카테고리를 선택해주세요."); return; }
    setIsSkillSubmitting(true);
    try {
      if (editSkill) {
        await apiClient.patch(`/v1/skills/${editSkill.id}`, {
          name: skillForm.name,
          category: skillForm.category,
        });
        toast.success("스킬이 수정되었습니다.");
      } else {
        await apiClient.post("/v1/skills", {
          name: skillForm.name,
          category: skillForm.category,
        });
        toast.success("스킬이 생성되었습니다.");
      }
      setShowSkillModal(false);
      const res = await skillApi.getAll(0, 200);
      setSkills(res.data.data.content);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "저장에 실패했습니다.");
    } finally {
      setIsSkillSubmitting(false);
    }
  };

  const handleSkillDelete = async () => {
    if (!deleteSkill) return;
    try {
      await apiClient.delete(`/v1/skills/${deleteSkill.id}`);
      toast.success("스킬이 삭제되었습니다.");
      setDeleteSkill(null);
      const res = await skillApi.getAll(0, 200);
      setSkills(res.data.data.content);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "삭제에 실패했습니다.");
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="container mx-auto px-4 max-w-5xl">
        <div className="flex items-center gap-3 mb-6">
          <Shield className="w-7 h-7 text-blue-600" />
          <h1 className="text-3xl">관리자 대시보드</h1>
        </div>

        {/* 통계 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <Card>
            <CardContent className="p-5 flex items-center gap-4">
              <Users className="w-8 h-8 text-amber-500" />
              <div>
                <p className="text-sm text-gray-500">대기 중 관리자</p>
                <p className="text-2xl font-bold">{pendingAdmins.length}</p>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-5 flex items-center gap-4">
              <Tag className="w-8 h-8 text-blue-500" />
              <div>
                <p className="text-sm text-gray-500">카테고리 수</p>
                <p className="text-2xl font-bold">{categories.length}</p>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-5 flex items-center gap-4">
              <Wrench className="w-8 h-8 text-green-500" />
              <div>
                <p className="text-sm text-gray-500">스킬 수</p>
                <p className="text-2xl font-bold">{skills.length}</p>
              </div>
            </CardContent>
          </Card>
        </div>

        <Tabs defaultValue={isSuperAdmin ? "admins" : "categories"}>
          <TabsList className="mb-6 flex flex-wrap gap-1">
            {isSuperAdmin && <TabsTrigger value="admins">관리자 승인</TabsTrigger>}
            <TabsTrigger value="categories">카테고리 관리</TabsTrigger>
            <TabsTrigger value="skills">스킬 관리</TabsTrigger>
          </TabsList>

          {/* ── 관리자 승인 탭 (SUPER_ADMIN 전용) ── */}
          {isSuperAdmin && (
            <TabsContent value="admins">
              <Card>
                <CardHeader>
                  <CardTitle>승인 대기 중인 관리자</CardTitle>
                </CardHeader>
                <CardContent>
                  {pendingAdmins.length === 0 ? (
                    <p className="text-center text-gray-500 py-10">
                      대기 중인 관리자가 없습니다.
                    </p>
                  ) : (
                    <div className="space-y-3">
                      {pendingAdmins.map((admin) => (
                        <div
                          key={admin.id}
                          className="flex items-center justify-between p-4 border rounded-lg"
                        >
                          <div>
                            <p className="font-medium">{admin.name}</p>
                            <p className="text-sm text-gray-500">{admin.email}</p>
                            <Badge variant="outline" className="mt-1 text-xs">
                              {admin.adminRole}
                            </Badge>
                          </div>
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              onClick={() => handleAdminAction(admin.id, "APPROVED")}
                              disabled={processingId === admin.id}
                            >
                              {processingId === admin.id ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                              ) : (
                                <CheckCircle className="w-4 h-4 mr-1" />
                              )}
                              승인
                            </Button>
                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={() => handleAdminAction(admin.id, "REJECTED")}
                              disabled={processingId === admin.id}
                            >
                              <XCircle className="w-4 h-4 mr-1" />
                              거절
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>
          )}

          {/* ── 카테고리 관리 탭 ── */}
          <TabsContent value="categories">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle>카테고리 관리</CardTitle>
                <Button size="sm" onClick={openCatCreate}>
                  <Plus className="w-4 h-4 mr-1" />
                  추가
                </Button>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {categories.map((cat) => (
                    <div
                      key={cat.id}
                      className="flex items-center justify-between p-4 border rounded-lg"
                    >
                      <div className="flex items-center gap-3">
                        <span className="text-2xl">{cat.icon ?? "📦"}</span>
                        <div>
                          <p className="font-medium">{cat.name}</p>
                          <p className="text-sm text-gray-500">
                            순서: {cat.displayOrder}
                            {cat.description && ` · ${cat.description}`}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => openCatEdit(cat)}
                        >
                          <Pencil className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-red-500"
                          onClick={() => setDeleteCat(cat)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* ── 스킬 관리 탭 ── */}
          <TabsContent value="skills">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle>스킬 관리</CardTitle>
                <Button size="sm" onClick={openSkillCreate}>
                  <Plus className="w-4 h-4 mr-1" />
                  추가
                </Button>
              </CardHeader>
              <CardContent>
                {/* 카테고리별 그룹화 */}
                {SKILL_CATEGORIES.map((cat) => {
                  const catSkills = skills.filter((s) => s.category === cat);
                  if (catSkills.length === 0) return null;
                  return (
                    <div key={cat} className="mb-6">
                      <h4 className="text-sm font-semibold text-gray-500 mb-2">
                        {cat}
                      </h4>
                      <div className="flex flex-wrap gap-2">
                        {catSkills.map((skill) => (
                          <div
                            key={skill.id}
                            className="flex items-center gap-1 border rounded-full px-3 py-1"
                          >
                            <span className="text-sm">{skill.name}</span>
                            <button
                              onClick={() => openSkillEdit(skill)}
                              className="text-blue-500 hover:text-blue-700 ml-1"
                            >
                              <Pencil className="w-3 h-3" />
                            </button>
                            <button
                              onClick={() => setDeleteSkill(skill)}
                              className="text-red-400 hover:text-red-600"
                            >
                              <X className="w-3 h-3" />
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>

      {/* 카테고리 추가/수정 모달 */}
      <Dialog open={showCatModal} onOpenChange={setShowCatModal}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>{editCat ? "카테고리 수정" : "카테고리 추가"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCatSubmit} className="space-y-4 mt-2">
            <div className="space-y-2">
              <Label>이름 *</Label>
              <Input
                value={catForm.name}
                onChange={(e) => setCatForm({ ...catForm, name: e.target.value })}
                placeholder="예: 백엔드"
                required
              />
            </div>
            <div className="space-y-2">
              <Label>아이콘 (이모지)</Label>
              <Input
                value={catForm.icon}
                onChange={(e) => setCatForm({ ...catForm, icon: e.target.value })}
                placeholder="예: 🖥️"
              />
            </div>
            <div className="space-y-2">
              <Label>설명</Label>
              <Input
                value={catForm.description}
                onChange={(e) =>
                  setCatForm({ ...catForm, description: e.target.value })
                }
                placeholder="카테고리 설명"
              />
            </div>
            <div className="space-y-2">
              <Label>정렬 순서 *</Label>
              <Input
                type="number"
                value={catForm.displayOrder}
                onChange={(e) =>
                  setCatForm({ ...catForm, displayOrder: e.target.value })
                }
                placeholder="1, 2, 3..."
                min={1}
                required
              />
            </div>
            <div className="flex gap-3">
              <Button type="submit" className="flex-1" disabled={isCatSubmitting}>
                {isCatSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                {editCat ? "수정" : "추가"}
              </Button>
              <Button type="button" variant="outline" onClick={() => setShowCatModal(false)}>
                취소
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 스킬 추가/수정 모달 */}
      <Dialog open={showSkillModal} onOpenChange={setShowSkillModal}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>{editSkill ? "스킬 수정" : "스킬 추가"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSkillSubmit} className="space-y-4 mt-2">
            <div className="space-y-2">
              <Label>스킬 이름 *</Label>
              <Input
                value={skillForm.name}
                onChange={(e) =>
                  setSkillForm({ ...skillForm, name: e.target.value })
                }
                placeholder="예: Spring Boot"
                required
              />
            </div>
            <div className="space-y-2">
              <Label>카테고리 *</Label>
              <Select
                value={skillForm.category}
                onValueChange={(v) => setSkillForm({ ...skillForm, category: v })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SKILL_CATEGORIES.map((c) => (
                    <SelectItem key={c} value={c}>{c}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-3">
              <Button type="submit" className="flex-1" disabled={isSkillSubmitting}>
                {editSkill ? "수정" : "추가"}
              </Button>
              <Button type="button" variant="outline" onClick={() => setShowSkillModal(false)}>
                취소
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 카테고리 삭제 확인 */}
      <AlertDialog open={!!deleteCat} onOpenChange={(o) => !o && setDeleteCat(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>카테고리 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              "{deleteCat?.name}" 카테고리를 삭제하시겠습니까?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleCatDelete} className="bg-red-600 hover:bg-red-700">
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 스킬 삭제 확인 */}
      <AlertDialog open={!!deleteSkill} onOpenChange={(o) => !o && setDeleteSkill(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>스킬 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              "{deleteSkill?.name}" 스킬을 삭제하시겠습니까?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleSkillDelete} className="bg-red-600 hover:bg-red-700">
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

// X 아이콘 임시 (이미 lucide-react에 있음)
function X({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}
