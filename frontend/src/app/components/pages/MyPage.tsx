/**
 * MyPage.tsx (원복 - 프로필 수정 전용)
 */
import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Textarea } from "../ui/textarea";
import { Badge } from "../ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { Loader2, Save, X, User, Settings, Info, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";
import { developerApi, clientApi } from "../../../api/apiService";
import { authApi } from "../../../api/authApi";

export function MyPage() {
  const navigate = useNavigate();
  const { isLoggedIn, userRole, userId, userEmail } = useAuth();

  const [userForm, setUserForm] = useState({
    name: "",
    phoneNumber: "",
    description: "",
  });

  const [clientForm, setClientForm] = useState({
    title: "",
    participateType: "INDIVIDUAL" as "INDIVIDUAL" | "COMPANY",
  });
  const [clientId, setClientId] = useState<number | null>(null);

  const [devForm, setDevForm] = useState({
    title: "",
    minHourlyPay: "",
    maxHourlyPay: "",
    skills: [] as string[],
    responseTime: "",
    availableForWork: true,
  });
  const [skillOptions, setSkillOptions] = useState<string[]>([]);
  const [isWithdrawing, setIsWithdrawing] = useState(false);

  const [isLoading, setIsLoading] = useState(true);
  const [isSavingUser, setIsSavingUser] = useState(false);
  const [isSavingRole, setIsSavingRole] = useState(false);

  useEffect(() => {
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    fetchMyInfo();
  }, [isLoggedIn]);

  const fetchMyInfo = async () => {
    setIsLoading(true);
    try {
      const [meRes, skillsRes] = await Promise.all([
        apiClient.get("/v1/users/me"),
        apiClient.get("/v1/skills", { params: { page: 0, size: 200 } })
      ]);
      const me = meRes.data?.data;
      if (!me) throw new Error("사용자 정보를 찾을 수 없습니다.");

      setSkillOptions((skillsRes.data?.data?.content ?? []).map((s: any) => s.name));

      setUserForm({
        name: me.name ?? "",
        phoneNumber: me.phoneNumber ?? "",
        description: me.description ?? "",
      });

      const myUserId = me.id;

      if (userRole === "CLIENT") {
        const allClientsRes = await apiClient.get("/v1/clients", { params: { page: 1, size: 100 } });
        const clientContent: any[] = allClientsRes.data?.data?.content ?? [];
        const myClient = clientContent.find((c: any) => Number(c.userId) === Number(myUserId));
        if (myClient) {
          setClientId(myClient.id);
          setClientForm({
            title: myClient.title ?? "",
            participateType: myClient.participateType ?? "INDIVIDUAL",
          });
        }
      } else if (userRole === "DEVELOPER") {
        const devRes = await developerApi.getAll(0, 100);
        const devContent: any[] = devRes.data?.data?.content ?? [];
        const myDev = devContent.find((d: any) => Number(d.userId) === Number(myUserId));
        if (myDev) {
          setDevForm({
            title: myDev.title ?? "",
            minHourlyPay: myDev.minHourlyPay?.toString() ?? "",
            maxHourlyPay: myDev.maxHourlyPay?.toString() ?? "",
            skills: myDev.skills ?? [],
            responseTime: myDev.responseTime ?? "",
            availableForWork: myDev.availableForWork ?? true,
          });
        }
      }
    } catch (err) {
      console.error("프로필 조회 오류:", err);
      toast.error("프로필 정보를 불러오는 데 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveUser = async () => {
    if (!userForm.name.trim()) { toast.error("이름을 입력해주세요."); return; }
    setIsSavingUser(true);
    try {
      await apiClient.put("/v1/users/me", {
        name: userForm.name || undefined,
        phoneNumber: userForm.phoneNumber || undefined,
        description: userForm.description || undefined,
      });
      toast.success("기본 정보가 수정되었습니다.");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "기본 정보 수정에 실패했습니다.");
    } finally {
      setIsSavingUser(false);
    }
  };

  const handleSaveClient = async () => {
    if (!clientId) { toast.error("클라이언트 정보를 찾을 수 없습니다."); return; }
    setIsSavingRole(true);
    try {
      await clientApi.updateProfile(clientId, {
        title: clientForm.title || undefined,
        participateType: clientForm.participateType,
      });
      toast.success("클라이언트 프로필이 수정되었습니다.");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "클라이언트 프로필 수정에 실패했습니다.");
    } finally {
      setIsSavingRole(false);
    }
  };

  const handleSaveDeveloper = async () => {
    const min = Number(devForm.minHourlyPay);
    const max = Number(devForm.maxHourlyPay);
    if (devForm.minHourlyPay && devForm.maxHourlyPay && min > max) {
      toast.error("최소 시급은 최대 시급보다 클 수 없습니다.");
      return;
    }
    setIsSavingRole(true);
    try {
      await developerApi.updateProfile({
        title: devForm.title || undefined,
        skills: devForm.skills.length > 0 ? devForm.skills : undefined,
        minHourlyPay: min || undefined,
        maxHourlyPay: max || undefined,
        responseTime: devForm.responseTime || undefined,
        availableForWork: devForm.availableForWork,
      });
      toast.success("개발자 프로필이 수정되었습니다.");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "개발자 프로필 수정에 실패했습니다.");
    } finally {
      setIsSavingRole(false);
    }
  };

  const handleWithdraw = async () => {
    if (!confirm("정말로 탈퇴하시겠습니까? 모든 정보가 삭제되며 되돌릴 수 없습니다.")) return;
    setIsWithdrawing(true);
    try {
      await authApi.withdraw();
      toast.success("탈퇴 처리되었습니다. 이용해 주셔서 감사합니다.");
      localStorage.clear();
      window.location.href = "/";
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "탈퇴 처리 중 오류가 발생했습니다.");
    } finally {
      setIsWithdrawing(false);
    }
  };

  const addSkill = (skill: string) => {
    if (skill && !devForm.skills.includes(skill)) {
      setDevForm((prev) => ({ ...prev, skills: [...prev.skills, skill] }));
    }
  };

  const removeSkill = (skill: string) => {
    setDevForm((prev) => ({ ...prev, skills: prev.skills.filter((s) => s !== skill) }));
  };

  if (isLoading) {
    return (
        <div className="flex justify-center py-32 bg-background min-h-screen">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
    );
  }

  return (
      <div className="min-h-screen bg-background py-8">
        <div className="container mx-auto px-4 max-w-2xl">
          <div className="flex items-center gap-3 mb-6">
            <div className="bg-primary p-2 rounded-lg">
              <User className="w-6 h-6 text-primary-foreground" />
            </div>
            <h1 className="text-3xl font-bold text-foreground">내 프로필 수정</h1>
          </div>

          <Tabs defaultValue="basic" className="w-full">
            <TabsList className="grid w-full grid-cols-2 mb-6 bg-secondary/50">
              <TabsTrigger value="basic" className="data-[state=active]:bg-background data-[state=active]:text-primary">기본 정보 수정</TabsTrigger>
              <TabsTrigger value="role" className="data-[state=active]:bg-background data-[state=active]:text-primary">
                {userRole === "CLIENT" ? "클라이언트" : userRole === "ADMIN" ? "관리자" : "개발자"}
              </TabsTrigger>
            </TabsList>

            {/* ── 기본 정보 탭 ── */}
            <TabsContent value="basic">
              <Card className="border-border bg-card shadow-md">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl text-foreground">
                    <Settings className="w-5 h-5 text-primary" />
                    계정 기본 설정
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="space-y-2">
                    <Label className="text-sm font-semibold text-foreground">이름 *</Label>
                    <Input
                        value={userForm.name}
                        onChange={(e) => setUserForm({ ...userForm, name: e.target.value })}
                        placeholder="이름을 입력하세요"
                        className="h-11 bg-secondary/30 border-border text-foreground"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-sm font-semibold text-foreground">전화번호</Label>
                    <Input
                        value={userForm.phoneNumber}
                        onChange={(e) => setUserForm({ ...userForm, phoneNumber: e.target.value })}
                        placeholder="01012345678 (하이픈 없이)"
                        className="h-11 bg-secondary/30 border-border text-foreground"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-sm font-semibold text-foreground">자기소개</Label>
                    <Textarea
                        value={userForm.description}
                        onChange={(e) => setUserForm({ ...userForm, description: e.target.value })}
                        placeholder="자기소개를 입력하세요"
                        rows={5}
                        className="resize-none bg-secondary/30 border-border text-foreground"
                    />
                  </div>
                  <Button onClick={handleSaveUser} disabled={isSavingUser} className="w-full h-11 bg-primary hover:bg-primary/90 text-primary-foreground font-bold">
                    {isSavingUser ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Save className="w-4 h-4 mr-2" />}
                    수정 사항 저장하기
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            {/* ── 역할별 정보 탭 ── */}
            <TabsContent value="role">
              {userRole === "CLIENT" ? (
                  <Card className="border-border bg-card shadow-md">
                    <CardHeader><CardTitle className="text-xl text-foreground">클라이언트 프로필 관리</CardTitle></CardHeader>
                    <CardContent className="space-y-5">
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold text-foreground">직군/직책</Label>
                        <Input
                            value={clientForm.title}
                            onChange={(e) => setClientForm({ ...clientForm, title: e.target.value })}
                            placeholder="예: 스타트업 CTO"
                            className="h-11 bg-secondary/30 border-border text-foreground"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold text-foreground">사업자 유형</Label>
                        <div className="grid grid-cols-2 gap-3">
                          {(["INDIVIDUAL", "COMPANY"] as const).map((type) => (
                              <button
                                  key={type}
                                  type="button"
                                  onClick={() => setClientForm({ ...clientForm, participateType: type })}
                                  className={`p-4 border rounded-xl text-sm font-medium transition-all ${
                                      clientForm.participateType === type
                                          ? "border-primary bg-primary/10 text-primary"
                                          : "border-border text-muted-foreground bg-secondary/30 hover:border-border/80"
                                  }`}
                              >
                                {type === "INDIVIDUAL" ? "🧑 개인 회원" : "🏢 기업 회원"}
                              </button>
                          ))}
                        </div>
                      </div>
                      <Button onClick={handleSaveClient} disabled={isSavingRole} className="w-full h-11 bg-primary hover:bg-primary/90 text-primary-foreground font-bold">
                        {isSavingRole ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Save className="w-4 h-4 mr-2" />}
                        클라이언트 정보 업데이트
                      </Button>
                    </CardContent>
                  </Card>
              ) : userRole === "ADMIN" ? (
                  <Card className="border-border bg-card shadow-md">
                    <CardHeader><CardTitle className="text-xl text-foreground">관리자 계정 안내</CardTitle></CardHeader>
                    <CardContent className="space-y-4">
                      <div className="p-4 bg-secondary/50 border border-border rounded-xl">
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-xs font-bold text-muted-foreground uppercase">Account Role</span>
                          <Badge className="bg-primary text-primary-foreground px-3">ADMINISTRATOR</Badge>
                        </div>
                        <div className="flex justify-between items-center pt-2 border-t border-border">
                          <span className="text-xs font-bold text-muted-foreground uppercase">Email</span>
                          <span className="text-sm font-bold text-foreground">{userEmail}</span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
              ) : (
                  <Card className="border-border bg-card shadow-md">
                    <CardHeader><CardTitle className="text-xl text-foreground">개발자 프로필 관리</CardTitle></CardHeader>
                    <CardContent className="space-y-5">
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold text-foreground">현재 직군</Label>
                        <Input
                            value={devForm.title}
                            onChange={(e) => setDevForm({ ...devForm, title: e.target.value })}
                            placeholder="예: 풀스택 개발자"
                            className="h-11 bg-secondary/30 border-border text-foreground"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold text-foreground">작업 가능 상태</Label>
                        <div className="grid grid-cols-2 gap-3">
                          {[
                            { value: true, label: "✅ 지금 작업 가능" },
                            { value: false, label: "🔴 현재 작업 중" },
                          ].map((opt) => (
                              <button
                                  key={String(opt.value)}
                                  type="button"
                                  onClick={() => setDevForm({ ...devForm, availableForWork: opt.value })}
                                  className={`p-4 border rounded-xl text-sm font-medium transition-all ${
                                      devForm.availableForWork === opt.value
                                          ? "border-primary bg-primary/10 text-primary"
                                          : "border-border text-muted-foreground bg-secondary/30 hover:border-border/80"
                                  }`}
                              >
                                {opt.label}
                              </button>
                          ))}
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold text-foreground">최소 시급 (원)</Label>
                          <Input
                              type="number"
                              value={devForm.minHourlyPay}
                              onChange={(e) => setDevForm({ ...devForm, minHourlyPay: e.target.value })}
                              className="h-11 bg-secondary/30 border-border text-foreground"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold text-foreground">최대 시급 (원)</Label>
                          <Input
                              type="number"
                              value={devForm.maxHourlyPay}
                              onChange={(e) => setDevForm({ ...devForm, maxHourlyPay: e.target.value })}
                              className="h-11 bg-secondary/30 border-border text-foreground"
                          />
                        </div>
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold text-foreground">평균 응답 시간</Label>
                        <Select
                            value={devForm.responseTime}
                            onValueChange={(val) => setDevForm({ ...devForm, responseTime: val })}
                        >
                          <SelectTrigger className="h-11 bg-secondary/30 border-border text-foreground">
                            <SelectValue placeholder="응답 시간 선택" />
                          </SelectTrigger>
                          <SelectContent className="bg-card border-border">
                            <SelectItem value="30분">30분</SelectItem>
                            <SelectItem value="1시간">1시간</SelectItem>
                            <SelectItem value="2시간">2시간</SelectItem>
                            <SelectItem value="3시간">3시간</SelectItem>
                            <SelectItem value="4시간">4시간</SelectItem>
                            <SelectItem value="12시간">12시간</SelectItem>
                            <SelectItem value="24시간">24시간</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold text-foreground">보유 기술 스택</Label>
                        <div className="flex gap-2">
                          <Select onValueChange={(val) => addSkill(val)}>
                            <SelectTrigger className="h-11 bg-secondary/30 border-border text-foreground">
                              <SelectValue placeholder="기술 스택 선택" />
                            </SelectTrigger>
                            <SelectContent className="bg-card border-border">
                              {skillOptions.map(skill => (
                                <SelectItem key={skill} value={skill}>{skill}</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="flex flex-wrap gap-2 mt-3">
                          {devForm.skills.map((skill) => (
                              <Badge key={skill} variant="secondary" className="px-3 py-1 rounded-lg bg-secondary text-secondary-foreground border border-border">
                                {skill}
                                <button type="button" onClick={() => removeSkill(skill)} className="ml-2 hover:text-destructive"><X className="w-3 h-3" /></button>
                              </Badge>
                          ))}
                        </div>
                      </div>
                      <Button onClick={handleSaveDeveloper} disabled={isSavingRole} className="w-full h-11 bg-primary hover:bg-primary/90 text-primary-foreground font-bold">
                        {isSavingRole ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Save className="w-4 h-4 mr-2" />}
                        개발자 정보 업데이트
                      </Button>
                    </CardContent>
                  </Card>
              )}
            </TabsContent>
          </Tabs>
        </div>
      </div>
  );
}
