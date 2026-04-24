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
  MessageSquare,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { adminApi, categoryApi, skillApi, csApi } from "../../../api/apiService";
import type { AdminDto, CategoryDto, SkillDto, CsChatRoomDto } from "../../../api/types";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";

export function AdminDashboard() {
  const navigate = useNavigate();
  const { isLoggedIn, userRole } = useAuth();

  const [isSuperAdmin, setIsSuperAdmin] = useState(false);
  const [pendingAdmins, setPendingAdmins] = useState<AdminDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [skills, setSkills] = useState<SkillDto[]>([]);
  const [csRooms, setCsRooms] = useState<CsChatRoomDto[]>([]);
  const [selectedCsStatus, setSelectedCsStatus] = useState<string>("all");
  const [isLoading, setIsLoading] = useState(true);

  // 처리 중인 adminId
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [processingCsId, setProcessingCsId] = useState<number | null>(null);

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
  const [skillForm, setSkillForm] = useState({ name: "", category: "BACKEND" });
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

      // 대기 중 관리자 목록 (SUPER_ADMIN만 가능)
      try {
        const adminsRes = await adminApi.getPendingList(1, 20);
        setPendingAdmins(adminsRes.data.data.admins || []);
        setIsSuperAdmin(true);
      } catch {
        setIsSuperAdmin(false);
      }

      const [catRes, skillRes, csRes] = await Promise.allSettled([
        categoryApi.getAll(),
        skillApi.getAll(0, 200),
        csApi.getAllRooms({ page: 1, size: 50 }),
      ]);
      if (catRes.status === "fulfilled") setCategories(catRes.value.data.data);
      if (skillRes.status === "fulfilled") setSkills(skillRes.value.data.data.content);
      if (csRes.status === "fulfilled") setCsRooms(csRes.value.data.data.content);
    } finally {
      setIsLoading(false);
    }
  };

  // CS 문의 목록 필터링 조회
  const loadCsRooms = async (status?: string) => {
    try {
      const res = await csApi.getAllRooms({ 
        status: status === "all" ? undefined : status, 
        page: 1, 
        size: 50 
      });
      setCsRooms(res.data.data.content);
    } catch {
      toast.error("CS 목록을 불러오는데 실패했습니다.");
    }
  };

  const handleCsStatusChange = async (roomId: number, status: string) => {
    setProcessingCsId(roomId);
    try {
      await csApi.updateStatus(roomId, status);
      toast.success("상태가 변경되었습니다.");
      loadCsRooms(selectedCsStatus);
    } catch {
      toast.error("상태 변경에 실패했습니다.");
    } finally {
      setProcessingCsId(null);
    }
  };

  // ── 관리자 승인/거절 ──
  const handleAdminAction = async (adminId: number, action: "APPROVED" | "REJECTED") => {
    setProcessingId(adminId);
    try {
      await adminApi.updateStatus(adminId, action);
      toast.success(action === "APPROVED" ? "관리자를 승인했습니다." : "관리자를 거절했습니다.");
      setPendingAdmins((prev) => prev.filter((a) => a.adminId !== adminId));
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
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-5xl">
        <div className="flex items-center gap-3 mb-6">
          <Shield className="w-7 h-7 text-primary" />
          <h1 className="text-3xl font-bold text-foreground">관리자 대시보드</h1>
        </div>

        {/* 통계 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <Card className="bg-card border-border shadow-sm rounded-2xl hover:shadow-md transition-shadow">
            <CardContent className="p-5 flex items-center gap-4">
              <div className="bg-amber-500/10 p-2.5 rounded-xl">
                <Users className="w-6 h-6 text-amber-500" />
              </div>
              <div>
                <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">대기 중 관리자</p>
                <p className="text-2xl font-black text-foreground">{pendingAdmins.length}</p>
              </div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border shadow-sm rounded-2xl hover:shadow-md transition-shadow">
            <CardContent className="p-5 flex items-center gap-4">
              <div className="bg-blue-500/10 p-2.5 rounded-xl">
                <Tag className="w-6 h-6 text-blue-500" />
              </div>
              <div>
                <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">카테고리 수</p>
                <p className="text-2xl font-black text-foreground">{categories.length}</p>
              </div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border shadow-sm rounded-2xl hover:shadow-md transition-shadow">
            <CardContent className="p-5 flex items-center gap-4">
              <div className="bg-green-500/10 p-2.5 rounded-xl">
                <Wrench className="w-6 h-6 text-green-500" />
              </div>
              <div>
                <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">스킬 수</p>
                <p className="text-2xl font-black text-foreground">{skills.length}</p>
              </div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border shadow-sm rounded-2xl hover:shadow-md transition-shadow">
            <CardContent className="p-5 flex items-center gap-4">
              <div className="bg-indigo-500/10 p-2.5 rounded-xl">
                <MessageSquare className="w-6 h-6 text-indigo-500" />
              </div>
              <div>
                <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mb-1">CS 문의 수</p>
                <p className="text-2xl font-black text-foreground">{csRooms.length}</p>
              </div>
            </CardContent>
          </Card>
        </div>

        <Tabs 
          defaultValue={isSuperAdmin ? "admins" : "categories"}
          onValueChange={(val) => {
            if (val === "cs") loadCsRooms(selectedCsStatus);
          }}
          className="w-full"
        >
          <TabsList className="grid w-full grid-cols-2 md:grid-cols-4 mb-8 bg-secondary/50 p-1 rounded-2xl border border-border h-14">
            {isSuperAdmin && <TabsTrigger value="admins" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-bold">관리자 승인</TabsTrigger>}
            <TabsTrigger value="categories" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-bold">카테고리</TabsTrigger>
            <TabsTrigger value="skills" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-bold">스킬</TabsTrigger>
            <TabsTrigger value="cs" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-bold">CS 문의</TabsTrigger>
          </TabsList>

          {/* ── 관리자 승인 탭 (SUPER_ADMIN 전용) ── */}
          {isSuperAdmin && (
            <TabsContent value="admins">
              <Card className="bg-card border-border shadow-md rounded-3xl overflow-hidden">
                <CardHeader className="bg-secondary/20 pb-6 border-b border-border/50">
                  <CardTitle className="text-foreground">승인 대기 중인 관리자</CardTitle>
                </CardHeader>
                <CardContent className="pt-8">
                  {pendingAdmins.length === 0 ? (
                    <p className="text-center text-muted-foreground py-10 font-medium">
                      대기 중인 관리자가 없습니다.
                    </p>
                  ) : (
                    <div className="space-y-3">
                      {pendingAdmins.map((admin) => (
                        <div
                          key={admin.id}
                          className="flex items-center justify-between p-5 border border-border rounded-2xl bg-secondary/10 hover:bg-secondary/20 transition-colors"
                        >
                          <div>
                            <p className="font-bold text-lg text-foreground">{admin.name}</p>
                            <p className="text-sm text-muted-foreground mb-2">{admin.email}</p>
                            <Badge variant="outline" className="text-[10px] border-primary/20 bg-primary/5 text-primary font-bold px-2 py-0">
                              {admin.adminRole}
                            </Badge>
                          </div>
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl px-4 h-10"
                              onClick={() => handleAdminAction(admin.adminId!, "APPROVED")}
                              disabled={processingId === admin.adminId}
                            >
                              {processingId === admin.id ? (
                                <Loader2 className="w-4 h-4 animate-spin mr-1" />
                              ) : (
                                <CheckCircle className="w-4 h-4 mr-2" />
                              )}
                              승인
                            </Button>
                            <Button
                              size="sm"
                              variant="destructive"
                              className="rounded-xl px-4 h-10 font-bold"
                              onClick={() => handleAdminAction(admin.adminId!, "REJECTED")}
                              disabled={processingId === admin.adminId}
                            >
                              <XCircle className="w-4 h-4 mr-2" />
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
            <Card className="bg-card border-border shadow-md rounded-3xl overflow-hidden">
              <CardHeader className="bg-secondary/20 pb-6 border-b border-border/50 flex flex-row items-center justify-between">
                <CardTitle className="text-foreground">카테고리 리스트</CardTitle>
                <Button size="sm" onClick={openCatCreate} className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl px-4">
                  <Plus className="w-4 h-4 mr-1" />
                  카테고리 추가
                </Button>
              </CardHeader>
              <CardContent className="pt-8">
                <div className="space-y-3">
                  {categories.map((cat) => (
                    <div
                      key={cat.id}
                      className="flex items-center justify-between p-5 border border-border rounded-2xl bg-secondary/10 hover:bg-secondary/20 transition-colors"
                    >
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-card rounded-xl flex items-center justify-center text-2xl shadow-sm border border-border">
                          {cat.icon ?? "📦"}
                        </div>
                        <div>
                          <p className="font-bold text-lg text-foreground">{cat.name}</p>
                          <p className="text-sm text-muted-foreground font-medium">
                            정렬 순서: <span className="text-primary">{cat.displayOrder}</span>
                            {cat.description && ` · ${cat.description}`}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-muted-foreground hover:text-primary hover:bg-primary/10 rounded-xl w-10 h-10 p-0"
                          onClick={() => openCatEdit(cat)}
                        >
                          <Pencil className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-destructive hover:text-destructive hover:bg-destructive/10 rounded-xl w-10 h-10 p-0"
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
            <Card className="bg-card border-border shadow-md">
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle className="text-foreground">스킬 관리</CardTitle>
                <Button size="sm" onClick={openSkillCreate} className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold">
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
                      <h4 className="text-sm font-semibold text-muted-foreground mb-2">
                        {cat}
                      </h4>
                      <div className="flex flex-wrap gap-2">
                        {catSkills.map((skill) => (
                          <div
                            key={skill.id}
                            className="flex items-center gap-1 border border-border rounded-full px-3 py-1 bg-secondary/30"
                          >
                            <span className="text-sm text-foreground">{skill.name}</span>
                            <button
                              onClick={() => openSkillEdit(skill)}
                              className="text-primary hover:text-primary/80 ml-1"
                            >
                              <Pencil className="w-3 h-3" />
                            </button>
                            <button
                              onClick={() => setDeleteSkill(skill)}
                              className="text-destructive hover:text-destructive/80 ml-1"
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

          {/* ── CS 문의 관리 탭 ── */}
          <TabsContent value="cs">
            <Card className="bg-card border-border shadow-md">
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle className="text-foreground">CS 문의 관리</CardTitle>
                <div className="flex gap-2">
                  <Select value={selectedCsStatus} onValueChange={(v) => { setSelectedCsStatus(v); loadCsRooms(v); }}>
                    <SelectTrigger className="w-[120px] bg-secondary/50 border-border text-foreground">
                      <SelectValue placeholder="상태 필터" />
                    </SelectTrigger>
                    <SelectContent className="bg-card border-border">
                      <SelectItem value="all">전체</SelectItem>
                      <SelectItem value="WAITING">대기중</SelectItem>
                      <SelectItem value="IN_PROGRESS">처리중</SelectItem>
                      <SelectItem value="COMPLETED">완료</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </CardHeader>
              <CardContent>
                {csRooms.length === 0 ? (
                  <p className="text-center text-muted-foreground py-10">
                    문의 내역이 없습니다.
                  </p>
                ) : (
                  <div className="space-y-3">
                    {csRooms.map((room) => (
                      <div key={room.id} className="flex flex-col md:flex-row md:items-center justify-between p-4 border border-border rounded-lg bg-secondary/20 gap-4">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <h4 className="font-bold text-foreground">{room.title}</h4>
                            <Badge variant={
                              room.status === "WAITING" ? "destructive" :
                              room.status === "IN_PROGRESS" ? "default" : "secondary"
                            } className={
                              room.status === "WAITING" ? "bg-destructive text-destructive-foreground" :
                              room.status === "IN_PROGRESS" ? "bg-primary text-primary-foreground" : "bg-secondary text-secondary-foreground"
                            }>
                              {room.status === "WAITING" ? "대기중" :
                               room.status === "IN_PROGRESS" ? "처리중" : "완료"}
                            </Badge>
                          </div>
                          <p className="text-sm text-muted-foreground">
                            문의자: {room.inquirerName} ({new Date(room.createdAt).toLocaleString()})
                          </p>
                        </div>
                        <div className="flex gap-2 shrink-0">
                          {room.status === "WAITING" && (
                            <Button size="sm" variant="outline" className="border-border text-foreground hover:bg-secondary" onClick={() => handleCsStatusChange(room.id, "IN_PROGRESS")} disabled={processingCsId === room.id}>
                              시작
                            </Button>
                          )}
                          {room.status === "IN_PROGRESS" && (
                            <Button size="sm" variant="outline" className="text-primary border-primary/50 hover:bg-primary/10" onClick={() => handleCsStatusChange(room.id, "COMPLETED")} disabled={processingCsId === room.id}>
                              완료
                            </Button>
                          )}
                          <Button size="sm" className="bg-primary hover:bg-primary/90 text-primary-foreground font-bold" onClick={() => navigate(`/chat?csRoomId=${room.id}`)}>
                            상담 채팅
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>

      {/* 카테고리 추가/수정 모달 */}
      <Dialog open={showCatModal} onOpenChange={setShowCatModal}>
        <DialogContent className="max-w-sm bg-card border-border text-foreground">
          <DialogHeader>
            <DialogTitle className="text-foreground">{editCat ? "카테고리 수정" : "카테고리 추가"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCatSubmit} className="space-y-4 mt-2">
            <div className="space-y-2">
              <Label className="text-muted-foreground">이름 *</Label>
              <Input
                value={catForm.name}
                onChange={(e) => setCatForm({ ...catForm, name: e.target.value })}
                placeholder="예: 백엔드"
                className="bg-secondary/30 border-border text-foreground"
                required
              />
            </div>
            <div className="space-y-2">
              <Label className="text-muted-foreground">아이콘 (이모지)</Label>
              <Input
                value={catForm.icon}
                onChange={(e) => setCatForm({ ...catForm, icon: e.target.value })}
                placeholder="예: 🖥️"
                className="bg-secondary/30 border-border text-foreground"
              />
            </div>
            <div className="space-y-2">
              <Label className="text-muted-foreground">설명</Label>
              <Input
                value={catForm.description}
                onChange={(e) =>
                  setCatForm({ ...catForm, description: e.target.value })
                }
                placeholder="카테고리 설명"
                className="bg-secondary/30 border-border text-foreground"
              />
            </div>
            <div className="space-y-2">
              <Label className="text-muted-foreground">정렬 순서 *</Label>
              <Input
                type="number"
                value={catForm.displayOrder}
                onChange={(e) =>
                  setCatForm({ ...catForm, displayOrder: e.target.value })
                }
                placeholder="1, 2, 3..."
                min={1}
                className="bg-secondary/30 border-border text-foreground"
                required
              />
            </div>
            <div className="flex gap-3 pt-2">
              <Button type="submit" className="flex-1 bg-primary hover:bg-primary/90 text-primary-foreground font-bold" disabled={isCatSubmitting}>
                {isCatSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                {editCat ? "수정" : "추가"}
              </Button>
              <Button type="button" variant="outline" className="border-border text-foreground hover:bg-secondary" onClick={() => setShowCatModal(false)}>
                취소
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 스킬 추가/수정 모달 */}
      <Dialog open={showSkillModal} onOpenChange={setShowSkillModal}>
        <DialogContent className="max-w-sm bg-card border-border text-foreground">
          <DialogHeader>
            <DialogTitle className="text-foreground">{editSkill ? "스킬 수정" : "스킬 추가"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSkillSubmit} className="space-y-4 mt-2">
            <div className="space-y-2">
              <Label className="text-muted-foreground">스킬 이름 *</Label>
              <Input
                value={skillForm.name}
                onChange={(e) =>
                  setSkillForm({ ...skillForm, name: e.target.value })
                }
                placeholder="예: Spring Boot"
                className="bg-secondary/30 border-border text-foreground"
                required
              />
            </div>
            <div className="space-y-2">
              <Label className="text-muted-foreground">카테고리 *</Label>
              <Select
                value={skillForm.category}
                onValueChange={(v) => setSkillForm({ ...skillForm, category: v })}
              >
                <SelectTrigger className="bg-secondary/30 border-border text-foreground">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="bg-card border-border">
                  {SKILL_CATEGORIES.map((c) => (
                    <SelectItem key={c} value={c}>{c}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-3 pt-2">
              <Button type="submit" className="flex-1 bg-primary hover:bg-primary/90 text-primary-foreground font-bold" disabled={isSkillSubmitting}>
                {editSkill ? "수정" : "추가"}
              </Button>
              <Button type="button" variant="outline" className="border-border text-foreground hover:bg-secondary" onClick={() => setShowSkillModal(false)}>
                취소
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* 카테고리 삭제 확인 */}
      <AlertDialog open={!!deleteCat} onOpenChange={(o) => !o && setDeleteCat(null)}>
        <AlertDialogContent className="bg-card border-border text-foreground">
          <AlertDialogHeader>
            <AlertDialogTitle>카테고리 삭제</AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground">
              "{deleteCat?.name}" 카테고리를 삭제하시겠습니까?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-border text-foreground hover:bg-secondary">취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleCatDelete} className="bg-destructive hover:bg-destructive/80 text-white font-bold">
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 스킬 삭제 확인 */}
      <AlertDialog open={!!deleteSkill} onOpenChange={(o) => !o && setDeleteSkill(null)}>
        <AlertDialogContent className="bg-card border-border text-foreground">
          <AlertDialogHeader>
            <AlertDialogTitle>스킬 삭제</AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground">
              "{deleteSkill?.name}" 스킬을 삭제하시겠습니까?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-border text-foreground hover:bg-secondary">취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleSkillDelete} className="bg-destructive hover:bg-destructive/80 text-white font-bold">
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
