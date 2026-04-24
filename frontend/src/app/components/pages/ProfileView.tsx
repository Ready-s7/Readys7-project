import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { 
  Loader2, User, Mail, Phone, FileText, 
  Briefcase, Clock, Shield, Pencil, 
  CheckCircle2, AlertCircle, Info, Trash2 
} from "lucide-react";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";
import { developerApi } from "../../../api/apiService";
import { authApi } from "../../../api/authApi";
import { toast } from "sonner";

export function ProfileView() {
  const navigate = useNavigate();
  const { isLoggedIn, userRole, userEmail, userId } = useAuth();
  
  const [profileData, setProfileData] = useState<any>(null);
  const [roleData, setRoleData] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isWithdrawing, setIsWithdrawing] = useState(false);

  useEffect(() => {
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    fetchProfile();
  }, [isLoggedIn]);

  const fetchProfile = async () => {
    setIsLoading(true);
    try {
      // 1. 기본 유저 정보 조회 (이름 확보)
      const meRes = await apiClient.get("/v1/users/me");
      const me = meRes.data.data;
      setProfileData(me);

      const myName = me.name?.trim();

      // 2. 역할별 추가 정보 조회 로직 강화 (userId 기준 매칭)
      if (userRole === "CLIENT") {
        const allClientsRes = await apiClient.get("/v1/clients", { params: { page: 1, size: 200 } });
        const clients = allClientsRes.data.data.content || [];
        const myClient = clients.find((c: any) => c.userId === me.id); // userId로 매칭
        if (myClient) setRoleData(myClient);
      } 
      else if (userRole === "DEVELOPER") {
        const devRes = await developerApi.getAll(0, 200);
        const devs = devRes.data.data.content || [];
        const myDev = devs.find((d: any) => d.userId === me.id); // userId로 매칭
        if (myDev) setRoleData(myDev);
      }
      else if (userRole === "ADMIN") {
        // 관리자 전용 정보는 별도 API가 있으면 좋으나, 현재는 /v1/users/me 정보 활용
        // 추후 /v1/admins/{id} 등이 있다면 연동
      }
    } catch (err) {
      console.error("프로필 조회 실패:", err);
      toast.error("일부 정보를 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
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

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-32 gap-4">
        <Loader2 className="w-10 h-10 animate-spin text-blue-600" />
        <p className="text-gray-500 font-medium">프로필 정보를 불러오는 중입니다...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-12">
      <div className="container mx-auto px-4 max-w-3xl">
        {/* 상단 헤더 영역 */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-6 mb-10">
          <div className="flex items-center gap-6">
            <div className="w-24 h-24 bg-gradient-to-br from-primary to-primary/60 rounded-[32px] flex items-center justify-center text-primary-foreground shadow-2xl shadow-primary/20 transition-transform hover:scale-105 duration-300">
              <User className="w-12 h-12" />
            </div>
            <div className="text-center md:text-left">
              <div className="flex items-center gap-3 mb-2 justify-center md:justify-start">
                <h1 className="text-4xl font-black text-foreground tracking-tight">{profileData?.name}</h1>
                <Badge className="px-3 py-1 bg-primary text-primary-foreground border-none font-black text-[10px] rounded-lg">
                  {userRole}
                </Badge>
              </div>
              <div className="flex flex-wrap gap-2 justify-center md:justify-start">
                <Badge variant="outline" className="px-3 py-1 text-primary border-primary/20 bg-primary/5 font-bold flex items-center gap-1.5 rounded-lg">
                  <CheckCircle2 className="w-3.5 h-3.5" /> 정상 활성 계정
                </Badge>
                <span className="text-sm text-muted-foreground font-medium flex items-center gap-1.5 ml-1">
                  <Clock className="w-3.5 h-3.5" /> 가입일: {new Date(profileData?.createdAt || Date.now()).toLocaleDateString()}
                </span>
              </div>
            </div>
          </div>
          <Link to="/my-profile">
            <Button className="bg-card text-foreground border-border hover:bg-secondary shadow-md gap-2 px-8 h-12 border rounded-2xl font-bold transition-all active:scale-95">
              <Pencil className="w-4 h-4" /> 프로필 수정하기
            </Button>
          </Link>
        </div>

        <div className="grid gap-8">
          {/* ── 기본 계정 정보 카드 ── */}
          <Card className="overflow-hidden border-border bg-card shadow-xl rounded-[32px]">
            <div className="h-2 bg-primary/80" />
            <CardHeader className="pb-2 px-8 pt-8">
              <CardTitle className="text-2xl font-black flex items-center gap-2.5 text-foreground">
                <Shield className="w-6 h-6 text-primary" /> 기본 계정 정보
              </CardTitle>
            </CardHeader>
            <CardContent className="p-8 pt-6 space-y-8">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
                <div className="space-y-2">
                  <p className="text-[11px] font-black text-muted-foreground uppercase tracking-[0.2em] flex items-center gap-2 ml-1">
                    <Mail className="w-3.5 h-3.5" /> Login Email
                  </p>
                  <div className="bg-secondary/30 p-4 rounded-2xl border border-border/50">
                    <p className="text-lg font-bold text-foreground">{userEmail}</p>
                  </div>
                </div>
                <div className="space-y-2">
                  <p className="text-[11px] font-black text-muted-foreground uppercase tracking-[0.2em] flex items-center gap-2 ml-1">
                    <Phone className="w-3.5 h-3.5" /> Phone Number
                  </p>
                  <div className="bg-secondary/30 p-4 rounded-2xl border border-border/50">
                    <p className="text-lg font-bold text-foreground">{profileData?.phoneNumber || "연락처 미등록"}</p>
                  </div>
                </div>
              </div>
              <div className="pt-8 border-t border-border/50 space-y-3">
                <p className="text-[11px] font-black text-muted-foreground uppercase tracking-[0.2em] flex items-center gap-2 ml-1">
                  <FileText className="w-3.5 h-3.5" /> 자기소개 전문
                </p>
                <div className="bg-secondary/30 rounded-[24px] p-6 border border-border/50 min-h-[120px]">
                  <p className="text-base text-muted-foreground leading-relaxed whitespace-pre-wrap font-medium">
                    {profileData?.description || "아직 등록된 자기소개가 없습니다. 프로필 수정에서 본인을 멋지게 소개해보세요!"}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* ── 역할별 상세 정보 카드 (CLIENT / DEVELOPER) ── */}
          {userRole !== "ADMIN" && (
            <Card className="overflow-hidden border-border bg-card shadow-xl rounded-[32px]">
              <div className="h-2 bg-primary/40" />
              <CardHeader className="pb-2 px-8 pt-8">
                <CardTitle className="text-2xl font-black flex items-center gap-2.5 text-foreground">
                  <Briefcase className="w-6 h-6 text-primary" /> {userRole === "CLIENT" ? "클라이언트" : "개발자"} 전문 정보
                </CardTitle>
              </CardHeader>
              <CardContent className="p-8 pt-6">
                {!roleData ? (
                  <div className="flex items-start gap-4 p-6 bg-primary/5 rounded-2xl border border-primary/10 text-primary">
                    <AlertCircle className="w-6 h-6 shrink-0 mt-0.5" />
                    <div className="text-sm">
                      <p className="font-black text-base mb-1">상세 정보가 아직 부족합니다.</p>
                      <p className="opacity-80 font-medium">아직 프로필 정보를 완성하지 않으셨다면 '내 프로필 수정' 메뉴에서 전문 정보를 입력해 주세요.</p>
                    </div>
                  </div>
                ) : userRole === "CLIENT" ? (
                  <div className="space-y-6">
                    <div className="flex justify-between items-center py-4 border-b border-border/50">
                      <span className="text-sm font-bold text-muted-foreground">현재 직군/직책</span>
                      <Badge variant="secondary" className="px-4 py-1.5 bg-primary/10 text-primary border-none font-black rounded-lg">
                        {roleData.title || "미지정"}
                      </Badge>
                    </div>
                    <div className="flex justify-between items-center py-4 border-b border-border/50">
                      <span className="text-sm font-bold text-muted-foreground">회원 인증 유형</span>
                      <div className="flex items-center gap-2 font-black text-foreground">
                        {roleData.participateType === "COMPANY" ? "🏢 기업 회원" : "🧑 개인 회원"}
                      </div>
                    </div>
                    <div className="flex justify-between items-center py-4">
                      <span className="text-sm font-bold text-muted-foreground">누적 완료 프로젝트</span>
                      <span className="text-3xl font-black text-primary">{roleData.completedProject || 0} <span className="text-sm font-bold text-muted-foreground ml-0.5">건</span></span>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-10">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="p-6 bg-secondary/30 rounded-[24px] border border-border/50 hover:bg-secondary/50 transition-colors">
                        <p className="text-[11px] font-black text-muted-foreground uppercase tracking-widest mb-3">전문 분야</p>
                        <p className="text-2xl font-black text-foreground">{roleData.title || "미지정"}</p>
                      </div>
                      <div className="p-6 bg-secondary/30 rounded-[24px] border border-border/50 flex flex-col justify-between hover:bg-secondary/50 transition-colors">
                        <p className="text-[11px] font-black text-muted-foreground uppercase tracking-widest mb-3">현재 활동 상태</p>
                        <div className="flex items-center gap-3">
                          <div className={`w-3.5 h-3.5 rounded-full ${roleData.availableForWork ? 'bg-green-500 shadow-[0_0_12px_rgba(34,197,94,0.6)] animate-pulse' : 'bg-destructive'}`} />
                          <span className="font-black text-lg text-foreground">{roleData.availableForWork ? "새 프로젝트 가능" : "현재 작업 중"}</span>
                        </div>
                      </div>
                    </div>
                    
                    <div className="space-y-4">
                      <p className="text-[11px] font-black text-muted-foreground uppercase tracking-[0.2em] flex items-center gap-2 ml-1">
                        <Clock className="w-3.5 h-3.5" /> 예상 희망 시급
                      </p>
                      <div className="inline-flex items-baseline gap-2 text-4xl font-black text-primary bg-primary/5 px-6 py-3 rounded-2xl border border-primary/10">
                        {roleData.minHourlyPay?.toLocaleString() || "0"} 
                        <span className="text-lg font-bold text-muted-foreground/40 mx-1">~</span>
                        {roleData.maxHourlyPay?.toLocaleString() || "0"}
                        <span className="text-base font-black text-muted-foreground ml-2">KRW / hr</span>
                      </div>
                    </div>

                    <div className="space-y-4">
                      <p className="text-[11px] font-black text-muted-foreground uppercase tracking-[0.2em] flex items-center gap-2 ml-1">
                        <Briefcase className="w-3.5 h-3.5" /> 핵심 기술 스택
                      </p>
                      <div className="flex flex-wrap gap-2.5">
                        {roleData.skills && roleData.skills.length > 0 ? (
                          roleData.skills.map((skill: string) => (
                            <Badge key={skill} className="px-5 py-2 bg-card text-foreground border-border shadow-sm hover:border-primary/50 hover:text-primary transition-all font-bold rounded-xl cursor-default">
                              {skill}
                            </Badge>
                          ))
                        ) : (
                          <span className="text-sm text-muted-foreground italic font-medium">등록된 기술 정보가 없습니다.</span>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* ── 관리자 전용 섹션 ── */}
          {userRole === "ADMIN" && (
            <Card className="overflow-hidden border-none bg-foreground text-background shadow-2xl rounded-[40px]">
              <CardHeader className="pt-10 px-10">
                <CardTitle className="flex items-center gap-3 text-3xl font-black italic">
                  <Shield className="w-8 h-8 text-primary" /> SYSTEM AUTHORITY
                </CardTitle>
              </CardHeader>
              <CardContent className="p-10 pt-6 space-y-10">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="p-6 bg-background/5 rounded-3xl backdrop-blur-md border border-background/10">
                    <p className="text-[10px] font-black text-primary uppercase tracking-[0.3em] mb-4 opacity-70">Security Access</p>
                    <div className="flex items-center gap-4">
                      <Badge className="bg-primary text-primary-foreground border-none px-5 py-1.5 font-black rounded-lg">LEVEL 07</Badge>
                      <span className="font-black text-xl tracking-tight uppercase">Authorized Personnel</span>
                    </div>
                  </div>
                  <div className="p-6 bg-background/5 rounded-3xl backdrop-blur-md border border-background/10">
                    <p className="text-[10px] font-black text-primary uppercase tracking-[0.3em] mb-4 opacity-70">Infrastructure Status</p>
                    <div className="flex items-center gap-3 font-black text-xl text-green-400">
                      <div className="w-2.5 h-2.5 bg-green-400 rounded-full animate-ping" />
                      OPERATIONAL
                    </div>
                  </div>
                </div>
                <div className="pt-4">
                  <Link to="/admin">
                    <Button className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-black h-16 text-xl rounded-2xl shadow-2xl shadow-primary/40 transition-all hover:scale-[1.02] active:scale-[0.98]">
                      관리 시스템 대시보드 진입
                    </Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          )}
          
          <div className="flex items-center gap-2 justify-center text-muted-foreground font-medium">
            <Info className="w-4 h-4" />
            <p className="text-xs">데이터 무결성 확인됨: {new Date().toLocaleString('ko-KR')}</p>
          </div>

          <div className="mt-10 pt-10 border-t border-border/50 flex justify-center">
            <Button 
              variant="ghost" 
              onClick={handleWithdraw} 
              disabled={isWithdrawing}
              className="text-muted-foreground/50 hover:text-destructive hover:bg-destructive/10 gap-2 font-bold transition-colors rounded-xl px-6"
            >
              {isWithdrawing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
              회원 탈퇴 요청 (Withdrawal)
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
