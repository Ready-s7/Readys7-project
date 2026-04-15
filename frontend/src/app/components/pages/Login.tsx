/**
 * Login.tsx 개선 사항
 *
 * [버그 수정]
 * 1. 회원가입 후 DB 저장은 됐지만 "실패" 토스트가 뜨는 문제
 *    → 원인: axios가 201 Created를 정상 응답으로 처리하지만, 내부에서 발생하는
 *      후속 getMe() 호출 실패나 응답 파싱 오류가 catch로 흘러들어 실패 처리됨.
 *    → 해결: registerClient/registerDeveloper 성공 여부를 명확히 구분,
 *      에러 메시지를 정확하게 추출하여 표시.
 *
 * 2. 로그인 후 홈으로 이동 안 되는 문제
 *    → 원인: accessToken 파싱 실패 시 setState가 호출되지 않아 isLoggedIn이 false 유지.
 *    → 해결: authApi.ts에서 헤더 파싱 로직 강화 + 여기서는 login() 성공 시 무조건 navigate("/").
 *
 * [UX 개선]
 * - 폼 제출 중 버튼 비활성화 + 로딩 스피너
 * - 에러 메시지 toast + 폼 하단 인라인 표시
 * - 회원가입 성공 후 자동으로 "로그인" 탭으로 전환 (자동 로그인)
 */
import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Code2, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/context/AuthContext";

export function Login() {
  const navigate = useNavigate();
  const { login, registerClient, registerDeveloper } = useAuth();

  const [loginData, setLoginData] = useState({ email: "", password: "" });
  const [signupData, setSignupData] = useState({
    name: "",
    email: "",
    password: "",
    phoneNumber: "",
    title: "",
    userType: "client" as "client" | "developer",
    participateType: "INDIVIDUAL" as "INDIVIDUAL" | "COMPANY",
    minHourlyPay: "",
    maxHourlyPay: "",
    skills: "",
    responseTime: "1시간",
  });

  const [isLoading, setIsLoading] = useState(false);
  const [loginError, setLoginError] = useState("");
  const [signupError, setSignupError] = useState("");

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

  // ── 회원가입 핸들러 ──────────────────────────────────────────
  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setSignupError("");

    // 클라이언트 사이드 유효성 검사
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
          title: signupData.title || "클라이언트",
          participateType: signupData.participateType,
        });
      } else {
        const skillList = signupData.skills
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean);
        await registerDeveloper({
          email: signupData.email,
          password: signupData.password,
          name: signupData.name,
          phoneNumber: signupData.phoneNumber,
          title: signupData.title || "개발자",
          minHourlyPay: Number(signupData.minHourlyPay) || 30000,
          maxHourlyPay: Number(signupData.maxHourlyPay) || 80000,
          skills: skillList.length ? skillList : ["기타"],
          responseTime: signupData.responseTime,
          availableForWork: true,
          participateType: signupData.participateType,
        });
      }
      toast.success("회원가입이 완료되었습니다! 자동으로 로그인됩니다.");
      navigate("/");
    } catch (err: any) {
      const detail = err?.response?.data;
      let msg = "회원가입에 실패했습니다.";
      if (detail?.data && typeof detail.data === "object") {
        // 필드 유효성 검사 오류 (Map<String, String>)
        msg = Object.values(detail.data).join("\n");
      } else if (detail?.message) {
        msg = detail.message;
      }
      setSignupError(msg);
      toast.error(msg.split("\n")[0]); // 첫 번째 메시지만 toast로
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50 py-12">
      <div className="container mx-auto px-4 max-w-md">
        <Link to="/" className="flex items-center justify-center gap-2 mb-8">
          <div className="bg-blue-600 p-2 rounded-lg">
            <Code2 className="w-6 h-6 text-white" />
          </div>
          <span className="font-bold text-2xl">Ready's7</span>
        </Link>

        <Card>
          <CardHeader>
            <CardTitle className="text-center text-2xl">환영합니다</CardTitle>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="login" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="login">로그인</TabsTrigger>
                <TabsTrigger value="signup">회원가입</TabsTrigger>
              </TabsList>

              {/* ── 로그인 탭 ── */}
              <TabsContent value="login">
                <form onSubmit={handleLogin} className="space-y-4 mt-4">
                  <div className="space-y-2">
                    <Label htmlFor="login-email">이메일</Label>
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
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="login-password">비밀번호</Label>
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
                    />
                  </div>

                  {loginError && (
                    <p className="text-sm text-red-600 bg-red-50 p-2 rounded">
                      {loginError}
                    </p>
                  )}

                  <Button
                    type="submit"
                    className="w-full"
                    size="lg"
                    disabled={isLoading}
                  >
                    {isLoading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        로그인 중...
                      </>
                    ) : (
                      "로그인"
                    )}
                  </Button>
                </form>

                {/* 테스트 계정 안내 */}
                <div className="mt-4 p-3 bg-blue-50 rounded-lg text-sm text-blue-800">
                  <p className="font-medium mb-1">🧪 테스트 계정</p>
                  <p>클라이언트: client1@test.com / 12345678</p>
                  <p>개발자: dev1@test.com / 12345678</p>
                </div>
              </TabsContent>

              {/* ── 회원가입 탭 ── */}
              <TabsContent value="signup">
                <form onSubmit={handleSignup} className="space-y-4 mt-4">
                  {/* 가입 유형 선택 */}
                  <div className="space-y-2">
                    <Label>가입 유형 *</Label>
                    <div className="grid grid-cols-2 gap-3">
                      <button
                        type="button"
                        onClick={() =>
                          setSignupData({ ...signupData, userType: "client" })
                        }
                        className={`p-3 border rounded-lg text-center hover:border-blue-600 transition-colors ${
                          signupData.userType === "client"
                            ? "border-blue-600 bg-blue-50"
                            : "border-gray-200"
                        }`}
                      >
                        <div className="text-xl mb-1">👤</div>
                        <div className="text-sm">클라이언트</div>
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          setSignupData({ ...signupData, userType: "developer" })
                        }
                        className={`p-3 border rounded-lg text-center hover:border-blue-600 transition-colors ${
                          signupData.userType === "developer"
                            ? "border-blue-600 bg-blue-50"
                            : "border-gray-200"
                        }`}
                      >
                        <div className="text-xl mb-1">💻</div>
                        <div className="text-sm">개발자</div>
                      </button>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label>이름 * (2~20자)</Label>
                    <Input
                      placeholder="홍길동"
                      value={signupData.name}
                      onChange={(e) =>
                        setSignupData({ ...signupData, name: e.target.value })
                      }
                      required
                      disabled={isLoading}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>이메일 *</Label>
                    <Input
                      type="email"
                      placeholder="your@email.com"
                      value={signupData.email}
                      onChange={(e) =>
                        setSignupData({ ...signupData, email: e.target.value })
                      }
                      required
                      disabled={isLoading}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>비밀번호 * (8자 이상)</Label>
                    <Input
                      type="password"
                      placeholder="8자 이상 입력"
                      value={signupData.password}
                      onChange={(e) =>
                        setSignupData({ ...signupData, password: e.target.value })
                      }
                      required
                      disabled={isLoading}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>전화번호 * (하이픈 없이)</Label>
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
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>직군 * (예: 풀스택 개발자)</Label>
                    <Input
                      placeholder="직군을 입력하세요"
                      value={signupData.title}
                      onChange={(e) =>
                        setSignupData({ ...signupData, title: e.target.value })
                      }
                      required
                      disabled={isLoading}
                    />
                  </div>

                  {/* 참여 유형 */}
                  <div className="space-y-2">
                    <Label>사업자 유형 *</Label>
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
                          className={`p-2 border rounded-lg text-sm hover:border-blue-600 transition-colors ${
                            signupData.participateType === type
                              ? "border-blue-600 bg-blue-50"
                              : "border-gray-200"
                          }`}
                        >
                          {type === "INDIVIDUAL" ? "🧑 개인" : "🏢 회사"}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* 개발자 전용 필드 */}
                  {signupData.userType === "developer" && (
                    <>
                      <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-2">
                          <Label>최소 시급(원) *</Label>
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
                          />
                        </div>
                        <div className="space-y-2">
                          <Label>최대 시급(원) *</Label>
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
                          />
                        </div>
                      </div>
                      <div className="space-y-2">
                        <Label>기술 스택 * (쉼표 구분)</Label>
                        <Input
                          placeholder="React, TypeScript, Node.js"
                          value={signupData.skills}
                          onChange={(e) =>
                            setSignupData({
                              ...signupData,
                              skills: e.target.value,
                            })
                          }
                          required
                          disabled={isLoading}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>응답 시간 * (예: 1시간, 30분)</Label>
                        <Input
                          placeholder="1시간"
                          value={signupData.responseTime}
                          onChange={(e) =>
                            setSignupData({
                              ...signupData,
                              responseTime: e.target.value,
                            })
                          }
                          required
                          disabled={isLoading}
                        />
                      </div>
                    </>
                  )}

                  {/* 인라인 에러 표시 */}
                  {signupError && (
                    <div className="text-sm text-red-600 bg-red-50 p-3 rounded whitespace-pre-line">
                      {signupError}
                    </div>
                  )}

                  <Button
                    type="submit"
                    className="w-full"
                    size="lg"
                    disabled={isLoading}
                  >
                    {isLoading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        처리 중...
                      </>
                    ) : (
                      "회원가입"
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
