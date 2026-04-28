/**
 * Login.tsx 수정 사항
 *
 * [버그 수정]
 * 1. 어드민 회원가입 항목 추가 (ADMIN 역할 선택 + adminRole 선택)
 * 2. 비밀번호 유효성 검사 개선
 *
 * [UX 개선]
 * 1. 토스트 알림이 버튼을 가리는 문제 → App.tsx에서 position="bottom-right"로 수정
 * 2. 가입 유형 3개(CLIENT/DEVELOPER/ADMIN)로 확장
 * 3. ADMIN 전용 필드(adminRole) 추가
 */
import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Textarea } from "../ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { Code2, Loader2, X } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/context/AuthContext";
import { apiClient } from "../../../api/client";
import type { SuccessResponse, UserDto } from "../../../api/types";
import logo from "../../../assets/logo.png";

type UserType = "client" | "developer" | "admin";
type AdminRole = "SUPER_ADMIN" | "CS_ADMIN" | "OPER_ADMIN";

export function Login() {
  const navigate = useNavigate();
  const { login, registerClient, registerDeveloper } = useAuth();

  const [loginData, setLoginData] = useState({ email: "", password: "" });
  const [signupData, setSignupData] = useState({
    name: "",
    email: "",
    password: "",
    phoneNumber: "",
    description: "",
    title: "",
    userType: "client" as UserType,
    participateType: "INDIVIDUAL" as "INDIVIDUAL" | "COMPANY",
    minHourlyPay: "",
    maxHourlyPay: "",
    skills: "",
    responseTime: "1시간",
    adminRole: "CS_ADMIN" as AdminRole,
  });

  const [isLoading, setIsLoading] = useState(false);
  const [loginError, setLoginError] = useState("");
  const [signupError, setSignupError] = useState("");
  const [skillOptions, setSkillOptions] = useState<string[]>([]);
  const [selectedSkills, setSelectedSkills] = useState<string[]>([]);

  // 기술 목록 로드
  const fetchSkills = async () => {
    try {
      const res = await apiClient.get("/v1/skills", { params: { page: 0, size: 200 } });
      setSkillOptions((res.data?.data?.content ?? []).map((s: any) => s.name));
    } catch (err) {
      console.error("기술 로드 실패:", err);
    }
  };

  const addSkill = (skill: string) => {
    if (skill && !selectedSkills.includes(skill)) {
      setSelectedSkills(prev => [...prev, skill]);
    }
  };

  const removeSkill = (skill: string) => {
    setSelectedSkills(prev => prev.filter(s => s !== skill));
  };

  // Rebuild trigger for deployment verification 2026-04-24 15:40

  // ── 로그인 핸들러 ──────────────────────────────────────────
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError("");
    setIsLoading(true);
    try {
      await login({ email: loginData.email, password: loginData.password });
      toast.success("로그인 되었습니다!");
      navigate("/");
    } catch (err: any) {
      const status = err?.response?.status;
      const message =
        err?.response?.data?.message ||
        (status === 404
          ? "존재하지 않는 이메일입니다."
          : status === 400
          ? "이메일 또는 비밀번호를 확인해주세요."
          : "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
      setLoginError(message);
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  // ── 어드민 회원가입 핸들러 ──────────────────────────────────
  const handleAdminRegister = async () => {
    const res = await apiClient.post<SuccessResponse<UserDto>>(
      "/v1/auth/register/admins",
      {
        email: signupData.email,
        password: signupData.password,
        name: signupData.name,
        phoneNumber: signupData.phoneNumber,
        description: signupData.description,
        adminRole: signupData.adminRole,
      }
    );
    return res;
  };

  // ── 회원가입 핸들러 ──────────────────────────────────────────
  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setSignupError("");

    if (signupData.password.length < 8) {
      setSignupError("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (!/^01(?:0|1|[6-9])(?:\d{3}|\d{4})\d{4}$/.test(signupData.phoneNumber)) {
      setSignupError("올바른 전화번호 형식을 입력해주세요. (예: 01012345678)");
      return;
    }
    if (signupData.userType === "developer") {
      const min = Number(signupData.minHourlyPay);
      const max = Number(signupData.maxHourlyPay);
      if (min <= 0 || max <= 0) {
        setSignupError("시급은 0보다 큰 값을 입력해주세요.");
        return;
      }
      if (min > max) {
        setSignupError("최소 시급은 최대 시급보다 클 수 없습니다.");
        return;
      }
    }

    setIsLoading(true);
    try {
      if (signupData.userType === "client") {
        await registerClient({
          email: signupData.email,
          password: signupData.password,
          name: signupData.name,
          phoneNumber: signupData.phoneNumber,
          description: signupData.description,
          title: signupData.title || "클라이언트",
          participateType: signupData.participateType,
        });
        toast.success("회원가입이 완료되었습니다! 자동으로 로그인됩니다.");
        navigate("/");
      } else if (signupData.userType === "developer") {
        await registerDeveloper({
          email: signupData.email,
          password: signupData.password,
          name: signupData.name,
          phoneNumber: signupData.phoneNumber,
          description: signupData.description,
          title: signupData.title || "개발자",
          minHourlyPay: Number(signupData.minHourlyPay) || 30000,
          maxHourlyPay: Number(signupData.maxHourlyPay) || 80000,
          skills: selectedSkills.length ? selectedSkills : ["기타"],
          responseTime: signupData.responseTime,
          availableForWork: true,
          participateType: signupData.participateType,
        });
        toast.success("회원가입이 완료되었습니다! 자동으로 로그인됩니다.");
        navigate("/");
      } else {
        // ADMIN 회원가입 → 자동 로그인 없음 (PENDING 상태이므로)
        await handleAdminRegister();
        toast.success(
          "관리자 계정이 생성되었습니다. SUPER_ADMIN 승인 후 로그인 가능합니다."
        );
        // 로그인 탭으로 이동
        setSignupData((prev) => ({ ...prev, userType: "client" }));
      }
    } catch (err: any) {
      const detail = err?.response?.data;
      let msg = "회원가입에 실패했습니다.";
      if (detail?.data && typeof detail.data === "object") {
        msg = Object.values(detail.data).join("\n");
      } else if (detail?.message) {
        msg = detail.message;
      }
      setSignupError(msg);
      toast.error(msg.split("\n")[0]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background py-12">
      <div className="container mx-auto px-4 max-w-md">
        <Link to="/" className="flex items-center justify-center gap-2 mb-8">
          <img src={logo} alt="Ready's7 Logo" className="h-10 w-auto" />
          <span className="font-bold text-2xl text-foreground">Ready's7</span>
        </Link>

        <Card className="bg-card border-border">
          <CardHeader>
            <CardTitle className="text-center text-2xl text-foreground">환영합니다</CardTitle>
          </CardHeader>
          <CardContent className="pt-6">
            <Tabs defaultValue="login" className="w-full">
              <TabsList className="grid w-full grid-cols-2 bg-secondary/50 p-1 rounded-2xl h-12 mb-6">
                <TabsTrigger value="login" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-black transition-all">로그인</TabsTrigger>
                <TabsTrigger value="signup" className="rounded-xl data-[state=active]:bg-background data-[state=active]:text-primary font-black transition-all">회원가입</TabsTrigger>
              </TabsList>

              {/* ── 로그인 탭 ── */}
              <TabsContent value="login">
                <form onSubmit={handleLogin} className="space-y-5 mt-4">
                  <div className="space-y-2">
                    <Label htmlFor="login-email" className="text-foreground font-bold ml-1">이메일 계정</Label>
                    <Input
                      id="login-email"
                      type="email"
                      placeholder="your@email.com"
                      value={loginData.email}
                      onChange={(e) =>
                        setLoginData({ ...loginData, email: e.target.value })
                      }
                      required
                      disabled={isLoading}
                      className="bg-secondary/30 border-border text-foreground h-12 rounded-xl focus:ring-primary/20"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="login-password" className="text-foreground font-bold ml-1">비밀번호</Label>
                    <Input
                      id="login-password"
                      type="password"
                      placeholder="비밀번호 입력"
                      value={loginData.password}
                      onChange={(e) =>
                        setLoginData({ ...loginData, password: e.target.value })
                      }
                      required
                      disabled={isLoading}
                      className="bg-secondary/30 border-border text-foreground h-12 rounded-xl focus:ring-primary/20"
                    />
                  </div>

                  {loginError && (
                    <p className="text-sm text-destructive bg-destructive/10 p-3 rounded-xl border border-destructive/20 font-medium">
                      {loginError}
                    </p>
                  )}

                  <Button
                    type="submit"
                    className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-black h-12 rounded-xl shadow-lg shadow-primary/20 transition-all active:scale-[0.98]"
                    size="lg"
                    disabled={isLoading}
                  >
                    {isLoading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        로그인 시도 중...
                      </>
                    ) : (
                      "지금 로그인하기"
                    )}
                  </Button>
                </form>

                <div className="mt-8 p-4 bg-secondary/30 border border-border rounded-2xl text-xs text-muted-foreground">
                  <p className="font-black mb-2 text-primary uppercase tracking-widest">🧪 Quick Test Accounts</p>
                  <div className="space-y-1 font-medium">
                    <p>클라이언트: <span className="text-foreground font-bold">client1@test.com</span> / 12345678</p>
                    <p>개발자: <span className="text-foreground font-bold">dev1@test.com</span> / 12345678</p>
                    <p>슈퍼관리자: <span className="text-foreground font-bold">superAdmin@system.com</span> / 12345678</p>
                  </div>
                </div>
              </TabsContent>

              {/* ── 회원가입 탭 ── */}
              <TabsContent value="signup">
                <form onSubmit={handleSignup} className="space-y-6 mt-4 text-foreground">
                  {/* 가입 유형 선택 - 3개 */}
                  <div className="space-y-3">
                    <Label className="font-bold ml-1">서비스 가입 유형 *</Label>
                    <div className="grid grid-cols-3 gap-2">
                      {(
                        [
                          { key: "client", icon: "👤", label: "클라이언트" },
                          { key: "developer", icon: "💻", label: "개발자" },
                          { key: "admin", icon: "🛡️", label: "관리자" },
                        ] as const
                      ).map(({ key, icon, label }) => (
                        <button
                          key={key}
                          type="button"
                          onClick={() =>
                            setSignupData({ ...signupData, userType: key })
                          }
                          className={`p-3 border rounded-2xl text-center transition-all ${
                            signupData.userType === key
                              ? "border-primary bg-primary/10 text-primary shadow-sm shadow-primary/10"
                              : "border-border bg-secondary/30 text-muted-foreground hover:bg-secondary/50"
                          }`}
                        >
                          <div className="text-xl mb-1">{icon}</div>
                          <div className="text-[11px] font-black">{label}</div>
                        </button>
                      ))}
                    </div>
                    {signupData.userType === "admin" && (
                      <p className="text-xs text-primary bg-primary/5 p-3 rounded-xl border border-primary/10 font-bold">
                        ⚠️ 관리자 계정은 내부 승인 절차(SUPER_ADMIN) 완료 후 활성화됩니다.
                      </p>
                    )}
                  </div>

                  {/* 공통 필드 */}
                  <div className="space-y-2">
                    <Label className="font-bold ml-1">이름 (실명/업체명) *</Label>
                    <Input
                      placeholder="홍길동"
                      value={signupData.name}
                      onChange={(e) =>
                        setSignupData({ ...signupData, name: e.target.value })
                      }
                      required
                      disabled={isLoading}
                      className="bg-secondary/30 border-border text-foreground h-12 rounded-xl"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="font-bold ml-1">이메일 계정 *</Label>
                    <Input
                      type="email"
                      placeholder="your@email.com"
                      value={signupData.email}
                      onChange={(e) =>
                        setSignupData({ ...signupData, email: e.target.value })
                      }
                      required
                      disabled={isLoading}
                      className="bg-secondary/30 border-border text-foreground h-12 rounded-xl"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="font-bold ml-1">비밀번호 설정 *</Label>
                    <Input
                      type="password"
                      placeholder="최소 8자 이상 입력"
                      value={signupData.password}
                      onChange={(e) =>
                        setSignupData({ ...signupData, password: e.target.value })
                      }
                      required
                      disabled={isLoading}
                      className="bg-secondary/30 border-border text-foreground h-12 rounded-xl"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="font-bold ml-1">휴대폰 번호 *</Label>
                    <Input
                      placeholder="01012345678"
                      value={signupData.phoneNumber}
                      onChange={(e) =>
                        setSignupData({
                          ...signupData,
                          phoneNumber: e.target.value,
                        })
                      }
                      required
                      disabled={isLoading}
                      className="bg-secondary/30 border-border text-foreground h-12 rounded-xl"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label className="font-bold ml-1">자기소개 / 업체 브리핑</Label>
                    <Textarea
                      placeholder="간략한 소개를 남겨주시면 매칭 확률이 높아집니다."
                      value={signupData.description}
                      onChange={(e) =>
                        setSignupData({
                          ...signupData,
                          description: e.target.value,
                        })
                      }
                      disabled={isLoading}
                      rows={3}
                      className="bg-secondary/30 border-border text-foreground rounded-xl p-4 resize-none"
                    />
                  </div>

                  {/* CLIENT / DEVELOPER 전용 필드 */}
                  {signupData.userType !== "admin" && (
                    <>
                      <div className="space-y-2">
                        <Label className="font-bold ml-1">전문 직군 *</Label>
                        <Input
                          placeholder="예: 시각 디자인, 풀스택 개발자 등"
                          value={signupData.title}
                          onChange={(e) =>
                            setSignupData({ ...signupData, title: e.target.value })
                          }
                          required
                          disabled={isLoading}
                          className="bg-secondary/30 border-border text-foreground h-12 rounded-xl"
                        />
                      </div>

                      <div className="space-y-2">
                        <Label className="font-bold ml-1">인증 유형 *</Label>
                        <div className="grid grid-cols-2 gap-3">
                          {(["INDIVIDUAL", "COMPANY"] as const).map((type) => (
                            <button
                              key={type}
                              type="button"
                              onClick={() =>
                                setSignupData({
                                  ...signupData,
                                  participateType: type,
                                })
                              }
                              className={`p-3 border rounded-2xl text-sm font-bold transition-all ${
                                signupData.participateType === type
                                  ? "border-primary bg-primary/10 text-primary shadow-sm"
                                  : "border-border bg-secondary/30 text-muted-foreground hover:bg-secondary/50"
                              }`}
                            >
                              {type === "INDIVIDUAL" ? "🧑 개인" : "🏢 법인/회사"}
                            </button>
                          ))}
                        </div>
                      </div>
                    </>
                  )}

                  {/* DEVELOPER 전용 필드 */}
                  {signupData.userType === "developer" && (
                    <>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="font-bold ml-1">최소 시급(원) *</Label>
                          <Input
                            type="number"
                            placeholder="30000"
                            min={1}
                            value={signupData.minHourlyPay}
                            onChange={(e) =>
                              setSignupData({
                                ...signupData,
                                minHourlyPay: e.target.value,
                              })
                            }
                            required
                            disabled={isLoading}
                            className="bg-secondary/30 border-border text-foreground h-12 rounded-xl"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="font-bold ml-1">최대 시급(원) *</Label>
                          <Input
                            type="number"
                            placeholder="80000"
                            min={1}
                            value={signupData.maxHourlyPay}
                            onChange={(e) =>
                              setSignupData({
                                ...signupData,
                                maxHourlyPay: e.target.value,
                              })
                            }
                            required
                            disabled={isLoading}
                            className="bg-secondary/30 border-border text-foreground h-12 rounded-xl"
                          />
                        </div>
                      </div>
                      <div className="space-y-2">
                        <Label className="font-bold ml-1">핵심 기술 스택 *</Label>
                        <Select onValueChange={(val) => addSkill(val)} onOpenChange={(open) => open && skillOptions.length === 0 && fetchSkills()}>
                          <SelectTrigger className="bg-secondary/30 border-border text-foreground h-12 rounded-xl">
                            <SelectValue placeholder="보유한 기술을 선택하세요" />
                          </SelectTrigger>
                          <SelectContent className="bg-card border-border">
                            {skillOptions.map(skill => (
                              <SelectItem key={skill} value={skill}>{skill}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <div className="flex flex-wrap gap-2 mt-3 p-1">
                          {selectedSkills.map(skill => (
                            <Badge key={skill} variant="secondary" className="pl-3 pr-1.5 py-1 bg-primary/10 text-primary border-none font-black rounded-lg">
                              {skill}
                              <button type="button" onClick={() => removeSkill(skill)} className="ml-2 hover:bg-primary/20 rounded-full p-0.5">
                                <X className="w-3 h-3" />
                              </button>
                            </Badge>
                          ))}
                        </div>
                      </div>
                      <div className="space-y-2">
                        <Label className="font-bold ml-1">평균 응답 속도 *</Label>
                        <Select
                          value={signupData.responseTime}
                          onValueChange={(val) =>
                            setSignupData({ ...signupData, responseTime: val })
                          }
                        >
                          <SelectTrigger className="bg-secondary/30 border-border text-foreground h-12 rounded-xl">
                            <SelectValue placeholder="응답 소요 시간을 선택하세요" />
                          </SelectTrigger>
                          <SelectContent className="bg-card border-border">
                            <SelectItem value="30분">⚡ 30분 이내</SelectItem>
                            <SelectItem value="1시간">🕒 1시간 이내</SelectItem>
                            <SelectItem value="2시간">🕒 2시간 이내</SelectItem>
                            <SelectItem value="3시간">🕒 3시간 이내</SelectItem>
                            <SelectItem value="4시간">🕒 4시간 이내</SelectItem>
                            <SelectItem value="12시간">🕒 12시간 이내</SelectItem>
                            <SelectItem value="24시간">🕒 24시간 이내</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </>
                  )}

                  {/* ADMIN 전용 필드 */}
                  {signupData.userType === "admin" && (
                    <div className="space-y-2">
                      <Label className="font-bold ml-1">관리자 세부 역할 *</Label>
                      <Select
                        value={signupData.adminRole}
                        onValueChange={(v) =>
                          setSignupData({
                            ...signupData,
                            adminRole: v as AdminRole,
                          })
                        }
                      >
                        <SelectTrigger className="bg-secondary/30 border-border text-foreground h-12 rounded-xl">
                          <SelectValue placeholder="담당 업무 선택" />
                        </SelectTrigger>
                        <SelectContent className="bg-card border-border">
                          <SelectItem value="SUPER_ADMIN">
                            🛡️ SUPER_ADMIN (최고 관리자)
                          </SelectItem>
                          <SelectItem value="CS_ADMIN">
                            💬 CS_ADMIN (고객 지원)
                          </SelectItem>
                          <SelectItem value="OPER_ADMIN">
                            ⚙️ OPER_ADMIN (운영 관리)
                          </SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  )}

                  {signupError && (
                    <div className="text-sm text-destructive bg-destructive/5 p-4 rounded-xl border border-destructive/10 whitespace-pre-line font-medium leading-relaxed">
                      {signupError}
                    </div>
                  )}

                  <Button
                    type="submit"
                    className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-black h-12 rounded-xl shadow-lg shadow-primary/20 transition-all active:scale-[0.98] mt-4"
                    size="lg"
                    disabled={isLoading}
                  >
                    {isLoading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        가입 정보 처리 중...
                      </>
                    ) : (
                      "서비스 가입하기"
                    )}
                  </Button>
                </form>
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
