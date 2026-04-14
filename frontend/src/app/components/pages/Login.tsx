import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Separator } from "../ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Code2 } from "lucide-react";
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
    // 개발자 전용
    minHourlyPay: "",
    maxHourlyPay: "",
    skills: "",
    responseTime: "1시간",
  });

  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      await login({ email: loginData.email, password: loginData.password });
      toast.success("로그인 되었습니다!");
      navigate("/");
    } catch (err: any) {
      const message = err?.response?.data?.message || "이메일 또는 비밀번호를 확인해주세요.";
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
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
      toast.success("회원가입이 완료되었습니다!");
      navigate("/");
    } catch (err: any) {
      const detail = err?.response?.data;
      if (detail?.data) {
        // 필드 유효성 검사 오류 (Map<String, String>)
        const messages = Object.values(detail.data).join(", ");
        toast.error(messages);
      } else {
        toast.error(detail?.message || "회원가입에 실패했습니다.");
      }
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
                <form onSubmit={handleLogin} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="login-email">이메일</Label>
                    <Input
                      id="login-email"
                      type="email"
                      placeholder="your@email.com"
                      value={loginData.email}
                      onChange={(e) => setLoginData({ ...loginData, email: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="login-password">비밀번호</Label>
                    <Input
                      id="login-password"
                      type="password"
                      placeholder="8자 이상"
                      value={loginData.password}
                      onChange={(e) => setLoginData({ ...loginData, password: e.target.value })}
                      required
                    />
                  </div>
                  <Button type="submit" className="w-full" size="lg" disabled={isLoading}>
                    {isLoading ? "로그인 중..." : "로그인"}
                  </Button>
                </form>

                {/* 테스트 계정 안내 */}
                <div className="mt-4 p-3 bg-blue-50 rounded-lg text-sm text-blue-800">
                  <p className="font-medium mb-1">테스트 계정</p>
                  <p>클라이언트: client1@test.com / 12345678</p>
                  <p>개발자: dev1@test.com / 12345678</p>
                </div>
              </TabsContent>

              {/* ── 회원가입 탭 ── */}
              <TabsContent value="signup">
                <form onSubmit={handleSignup} className="space-y-4">
                  {/* 가입 유형 선택 */}
                  <div className="space-y-2">
                    <Label>가입 유형</Label>
                    <div className="grid grid-cols-2 gap-3">
                      <button
                        type="button"
                        onClick={() => setSignupData({ ...signupData, userType: "client" })}
                        className={`p-3 border rounded-lg text-center hover:border-blue-600 transition-colors ${
                          signupData.userType === "client" ? "border-blue-600 bg-blue-50" : ""
                        }`}
                      >
                        <div className="text-xl mb-1">👤</div>
                        <div className="text-sm">클라이언트</div>
                      </button>
                      <button
                        type="button"
                        onClick={() => setSignupData({ ...signupData, userType: "developer" })}
                        className={`p-3 border rounded-lg text-center hover:border-blue-600 transition-colors ${
                          signupData.userType === "developer" ? "border-blue-600 bg-blue-50" : ""
                        }`}
                      >
                        <div className="text-xl mb-1">💻</div>
                        <div className="text-sm">개발자</div>
                      </button>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label>이름</Label>
                    <Input
                      placeholder="홍길동"
                      value={signupData.name}
                      onChange={(e) => setSignupData({ ...signupData, name: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>이메일</Label>
                    <Input
                      type="email"
                      placeholder="your@email.com"
                      value={signupData.email}
                      onChange={(e) => setSignupData({ ...signupData, email: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>비밀번호</Label>
                    <Input
                      type="password"
                      placeholder="8자 이상"
                      value={signupData.password}
                      onChange={(e) => setSignupData({ ...signupData, password: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>전화번호 (하이픈 없이)</Label>
                    <Input
                      placeholder="01012345678"
                      value={signupData.phoneNumber}
                      onChange={(e) => setSignupData({ ...signupData, phoneNumber: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>직군 (예: 풀스택 개발자)</Label>
                    <Input
                      placeholder="직군을 입력하세요"
                      value={signupData.title}
                      onChange={(e) => setSignupData({ ...signupData, title: e.target.value })}
                    />
                  </div>

                  {/* 개발자 전용 필드 */}
                  {signupData.userType === "developer" && (
                    <>
                      <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-2">
                          <Label>최소 시급(원)</Label>
                          <Input
                            type="number"
                            placeholder="30000"
                            value={signupData.minHourlyPay}
                            onChange={(e) => setSignupData({ ...signupData, minHourlyPay: e.target.value })}
                          />
                        </div>
                        <div className="space-y-2">
                          <Label>최대 시급(원)</Label>
                          <Input
                            type="number"
                            placeholder="80000"
                            value={signupData.maxHourlyPay}
                            onChange={(e) => setSignupData({ ...signupData, maxHourlyPay: e.target.value })}
                          />
                        </div>
                      </div>
                      <div className="space-y-2">
                        <Label>기술 스택 (쉼표 구분)</Label>
                        <Input
                          placeholder="React, TypeScript, Node.js"
                          value={signupData.skills}
                          onChange={(e) => setSignupData({ ...signupData, skills: e.target.value })}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>응답 시간 (예: 1시간, 30분)</Label>
                        <Input
                          placeholder="1시간"
                          value={signupData.responseTime}
                          onChange={(e) => setSignupData({ ...signupData, responseTime: e.target.value })}
                        />
                      </div>
                    </>
                  )}

                  <Button type="submit" className="w-full" size="lg" disabled={isLoading}>
                    {isLoading ? "가입 중..." : "회원가입"}
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
