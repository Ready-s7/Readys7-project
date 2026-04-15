/**
 * MyPage.tsx (완전 수정판)
 *
 * 핵심 수정 사항:
 * 1. userId 기반으로 클라이언트/개발자 프로필 정확하게 매칭 (이름 중복 문제 해결)
 * 2. /v1/users/me 응답에서 name, phoneNumber 올바르게 추출
 * 3. 응답 구조 안전하게 접근 (optional chaining)
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
import { Loader2, Save, X, User, Settings, Info } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";
import { developerApi, clientApi } from "../../../api/apiService";

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
  const [skillInput, setSkillInput] = useState("");

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
      // 1. 기본 유저 정보 조회 - name, phoneNumber 포함
      const meRes = await apiClient.get("/v1/users/me");
      const me = meRes.data?.data;

      if (!me) {
        throw new Error("사용자 정보를 찾을 수 없습니다.");
      }

      setUserForm({
        name: me.name ?? "",
        phoneNumber: me.phoneNumber ?? "",
        description: me.description ?? "",
      });

      const myUserId = me.id;

      // 2. 역할별 추가 정보 조회 - userId 기반으로 정확히 매칭
      if (userRole === "CLIENT") {
        try {
          // 페이지 크기를 충분히 크게 해서 본인 프로필 찾기
          const allClientsRes = await apiClient.get("/v1/clients", {
            params: { page: 1, size: 100 },
          });
          const clientContent: any[] = allClientsRes.data?.data?.content ?? [];

          // ★ userId 기반으로 정확히 매칭 (이름 중복 문제 해결)
          const myClient = clientContent.find(
              (c: any) => Number(c.userId) === Number(myUserId)
          );

          if (myClient) {
            setClientId(myClient.id);
            setClientForm({
              title: myClient.title ?? "",
              participateType: myClient.participateType ?? "INDIVIDUAL",
            });
          }
        } catch (e) {
          console.warn("클라이언트 프로필 조회 실패:", e);
        }
      } else if (userRole === "DEVELOPER") {
        try {
          const devRes = await developerApi.getAll(0, 100);
          const devContent: any[] = devRes.data?.data?.content ?? [];

          // ★ userId 기반으로 정확히 매칭
          const myDev = devContent.find(
              (d: any) => Number(d.userId) === Number(myUserId)
          );

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
        } catch (e) {
          console.warn("개발자 프로필 조회 실패:", e);
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
    if (!userForm.name.trim()) {
      toast.error("이름을 입력해주세요.");
      return;
    }
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
    if (!clientId) {
      toast.error("클라이언트 정보를 찾을 수 없습니다. 페이지를 새로고침해주세요.");
      return;
    }
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

  const addSkill = (skill: string) => {
    const trimmed = skill.trim();
    if (trimmed && !devForm.skills.includes(trimmed)) {
      setDevForm((prev) => ({ ...prev, skills: [...prev.skills, trimmed] }));
      setSkillInput("");
    }
  };

  const removeSkill = (skill: string) => {
    setDevForm((prev) => ({
      ...prev,
      skills: prev.skills.filter((s) => s !== skill),
    }));
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
        <div className="container mx-auto px-4 max-w-2xl">
          <div className="flex items-center gap-3 mb-6">
            <div className="bg-blue-600 p-2 rounded-lg">
              <User className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-3xl font-bold">내 프로필 수정</h1>
          </div>

          <Tabs defaultValue="basic" className="w-full">
            <TabsList className="grid w-full grid-cols-2 mb-6">
              <TabsTrigger value="basic">기본 정보 수정</TabsTrigger>
              <TabsTrigger value="role">
                {userRole === "CLIENT"
                    ? "클라이언트 정보 수정"
                    : userRole === "ADMIN"
                        ? "관리자 계정 상태"
                        : "개발자 정보 수정"}
              </TabsTrigger>
            </TabsList>

            {/* ── 기본 정보 탭 ── */}
            <TabsContent value="basic">
              <Card className="border-none shadow-md">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-xl">
                    <Settings className="w-5 h-5 text-blue-600" />
                    계정 기본 설정
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="space-y-2">
                    <Label className="text-sm font-semibold">이름 *</Label>
                    <Input
                        value={userForm.name}
                        onChange={(e) =>
                            setUserForm({ ...userForm, name: e.target.value })
                        }
                        placeholder="이름을 입력하세요"
                        className="h-11"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-sm font-semibold">전화번호</Label>
                    <Input
                        value={userForm.phoneNumber}
                        onChange={(e) =>
                            setUserForm({ ...userForm, phoneNumber: e.target.value })
                        }
                        placeholder="01012345678 (하이픈 없이)"
                        className="h-11"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-sm font-semibold">자기소개</Label>
                    <Textarea
                        value={userForm.description}
                        onChange={(e) =>
                            setUserForm({ ...userForm, description: e.target.value })
                        }
                        placeholder="자기소개를 입력하세요"
                        rows={5}
                        className="resize-none"
                    />
                  </div>
                  <Button
                      onClick={handleSaveUser}
                      disabled={isSavingUser}
                      className="w-full h-11 bg-blue-600 hover:bg-blue-700"
                  >
                    {isSavingUser ? (
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : (
                        <Save className="w-4 h-4 mr-2" />
                    )}
                    수정 사항 저장하기
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            {/* ── 역할별 정보 탭 ── */}
            <TabsContent value="role">
              {userRole === "CLIENT" ? (
                  <Card className="border-none shadow-md">
                    <CardHeader>
                      <CardTitle className="text-xl">클라이언트 프로필 관리</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-5">
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">직군/직책</Label>
                        <Input
                            value={clientForm.title}
                            onChange={(e) =>
                                setClientForm({ ...clientForm, title: e.target.value })
                            }
                            placeholder="예: 스타트업 CTO, 개인 사업자"
                            className="h-11"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">사업자 유형</Label>
                        <div className="grid grid-cols-2 gap-3">
                          {(["INDIVIDUAL", "COMPANY"] as const).map((type) => (
                              <button
                                  key={type}
                                  type="button"
                                  onClick={() =>
                                      setClientForm({
                                        ...clientForm,
                                        participateType: type,
                                      })
                                  }
                                  className={`p-4 border rounded-xl text-sm font-medium transition-all ${
                                      clientForm.participateType === type
                                          ? "border-blue-600 bg-blue-50 text-blue-700"
                                          : "border-gray-200 text-gray-500 hover:border-gray-300"
                                  }`}
                              >
                                {type === "INDIVIDUAL" ? "🧑 개인 회원" : "🏢 기업 회원"}
                              </button>
                          ))}
                        </div>
                      </div>
                      <Button
                          onClick={handleSaveClient}
                          disabled={isSavingRole}
                          className="w-full h-11 bg-blue-600 hover:bg-blue-700"
                      >
                        {isSavingRole ? (
                            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        ) : (
                            <Save className="w-4 h-4 mr-2" />
                        )}
                        클라이언트 정보 업데이트
                      </Button>
                    </CardContent>
                  </Card>
              ) : userRole === "ADMIN" ? (
                  <Card className="border-none shadow-md">
                    <CardHeader>
                      <CardTitle className="text-xl">관리자 계정 안내</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="p-4 bg-blue-50 rounded-xl">
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-xs font-bold text-blue-600 uppercase">Account Role</span>
                          <Badge className="bg-blue-600 px-3">ADMINISTRATOR</Badge>
                        </div>
                        <div className="flex justify-between items-center pt-2 border-t border-blue-100">
                          <span className="text-xs font-bold text-blue-600 uppercase">Email</span>
                          <span className="text-sm font-bold text-blue-700">{userEmail}</span>
                        </div>
                      </div>
                      <div className="flex items-start gap-3 p-4 bg-amber-50 rounded-xl border border-amber-100">
                        <Info className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                        <p className="text-xs text-amber-800 leading-relaxed">
                          관리자 계정의 역할 권한은 관리자 대시보드에서만 수정할 수 있습니다.
                        </p>
                      </div>
                    </CardContent>
                  </Card>
              ) : (
                  <Card className="border-none shadow-md">
                    <CardHeader>
                      <CardTitle className="text-xl">개발자 프로필 관리</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-5">
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">현재 직군</Label>
                        <Input
                            value={devForm.title}
                            onChange={(e) =>
                                setDevForm({ ...devForm, title: e.target.value })
                            }
                            placeholder="예: 풀스택 개발자, 백엔드 개발자"
                            className="h-11"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">작업 가능 상태</Label>
                        <div className="grid grid-cols-2 gap-3">
                          {[
                            { value: true, label: "✅ 지금 작업 가능" },
                            { value: false, label: "🔴 현재 작업 중" },
                          ].map((opt) => (
                              <button
                                  key={String(opt.value)}
                                  type="button"
                                  onClick={() =>
                                      setDevForm({ ...devForm, availableForWork: opt.value })
                                  }
                                  className={`p-4 border rounded-xl text-sm font-medium transition-all ${
                                      devForm.availableForWork === opt.value
                                          ? "border-blue-600 bg-blue-50 text-blue-700"
                                          : "border-gray-200 text-gray-500 hover:border-gray-300"
                                  }`}
                              >
                                {opt.label}
                              </button>
                          ))}
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">최소 시급 (원)</Label>
                          <Input
                              type="number"
                              value={devForm.minHourlyPay}
                              onChange={(e) =>
                                  setDevForm({ ...devForm, minHourlyPay: e.target.value })
                              }
                              placeholder="30000"
                              className="h-11"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">최대 시급 (원)</Label>
                          <Input
                              type="number"
                              value={devForm.maxHourlyPay}
                              onChange={(e) =>
                                  setDevForm({ ...devForm, maxHourlyPay: e.target.value })
                              }
                              placeholder="80000"
                              className="h-11"
                          />
                        </div>
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">응답 시간</Label>
                        <Input
                            value={devForm.responseTime}
                            onChange={(e) =>
                                setDevForm({ ...devForm, responseTime: e.target.value })
                            }
                            placeholder="예: 1시간, 30분"
                            className="h-11"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">보유 기술 스택</Label>
                        <div className="flex gap-2">
                          <Input
                              value={skillInput}
                              onChange={(e) => setSkillInput(e.target.value)}
                              placeholder="기술 입력 후 Enter 또는 추가 버튼"
                              onKeyDown={(e) => {
                                if (e.key === "Enter") {
                                  e.preventDefault();
                                  addSkill(skillInput);
                                }
                              }}
                              className="h-11"
                          />
                          <Button
                              type="button"
                              variant="outline"
                              onClick={() => addSkill(skillInput)}
                              className="h-11 px-6"
                          >
                            추가
                          </Button>
                        </div>
                        {devForm.skills.length > 0 && (
                            <div className="flex flex-wrap gap-2 mt-3">
                              {devForm.skills.map((skill) => (
                                  <Badge
                                      key={skill}
                                      variant="secondary"
                                      className="px-3 py-1 rounded-lg"
                                  >
                                    {skill}
                                    <button
                                        type="button"
                                        onClick={() => removeSkill(skill)}
                                        className="ml-2 hover:text-red-600 transition-colors"
                                    >
                                      <X className="w-3 h-3" />
                                    </button>
                                  </Badge>
                              ))}
                            </div>
                        )}
                      </div>
                      <Button
                          onClick={handleSaveDeveloper}
                          disabled={isSavingRole}
                          className="w-full h-11 bg-blue-600 hover:bg-blue-700"
                      >
                        {isSavingRole ? (
                            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        ) : (
                            <Save className="w-4 h-4 mr-2" />
                        )}
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