import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { 
  Loader2, User, Mail, Phone, FileText, 
  Briefcase, Clock, Shield, Pencil, 
  CheckCircle2, AlertCircle, Info 
} from "lucide-react";
import { useAuth } from "../../../context/AuthContext";
import { apiClient } from "../../../api/client";
import { developerApi } from "../../../api/apiService";
import { toast } from "sonner";

export function ProfileView() {
  const navigate = useNavigate();
  const { isLoggedIn, userRole, userEmail, userId } = useAuth();
  
  const [profileData, setProfileData] = useState<any>(null);
  const [roleData, setRoleData] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);

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

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-32 gap-4">
        <Loader2 className="w-10 h-10 animate-spin text-blue-600" />
        <p className="text-gray-500 font-medium">프로필 정보를 불러오는 중입니다...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="container mx-auto px-4 max-w-3xl">
        {/* 상단 헤더 영역 */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-6 mb-8">
          <div className="flex items-center gap-5">
            <div className="w-20 h-20 bg-gradient-to-br from-blue-600 to-indigo-700 rounded-3xl flex items-center justify-center text-white shadow-xl">
              <User className="w-10 h-10" />
            </div>
            <div className="text-center md:text-left">
              <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">{profileData?.name}</h1>
              <div className="flex flex-wrap gap-2 mt-2 justify-center md:justify-start">
                <Badge className="px-3 py-1 bg-gray-900 text-white border-none uppercase tracking-wider text-[10px]">
                  {userRole}
                </Badge>
                <Badge variant="outline" className="px-3 py-1 text-green-600 border-green-200 bg-green-50 flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> 정상 활성
                </Badge>
              </div>
            </div>
          </div>
          <Link to="/my-profile">
            <Button className="bg-white text-gray-700 border-gray-200 hover:bg-gray-50 shadow-sm gap-2 px-6 h-11 border">
              <Pencil className="w-4 h-4" /> 내 프로필 수정
            </Button>
          </Link>
        </div>

        <div className="grid gap-8">
          {/* ── 기본 계정 정보 카드 ── */}
          <Card className="overflow-hidden border-none shadow-md">
            <div className="h-2 bg-blue-600" />
            <CardHeader className="bg-white pb-2">
              <CardTitle className="text-xl flex items-center gap-2 text-gray-800">
                <Shield className="w-5 h-5 text-blue-600" /> 기본 계정 정보
              </CardTitle>
            </CardHeader>
            <CardContent className="p-6 pt-4 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="space-y-1.5">
                  <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1.5">
                    <Mail className="w-3 h-3" /> Login Email
                  </p>
                  <p className="text-base font-semibold text-gray-700">{userEmail}</p>
                </div>
                <div className="space-y-1.5">
                  <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1.5">
                    <Phone className="w-3 h-3" /> Phone Number
                  </p>
                  <p className="text-base font-semibold text-gray-700">{profileData?.phoneNumber || "연락처 미등록"}</p>
                </div>
              </div>
              <div className="pt-6 border-t border-gray-100 space-y-2">
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1.5">
                  <FileText className="w-3 h-3" /> 자기소개
                </p>
                <div className="bg-gray-50 rounded-xl p-4 min-h-[100px]">
                  <p className="text-sm text-gray-600 leading-relaxed whitespace-pre-wrap">
                    {profileData?.description || "아직 등록된 자기소개가 없습니다. 프로필 수정에서 본인을 소개해보세요!"}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* ── 역할별 상세 정보 카드 (CLIENT / DEVELOPER) ── */}
          {userRole !== "ADMIN" && (
            <Card className="overflow-hidden border-none shadow-md">
              <div className="h-2 bg-indigo-600" />
              <CardHeader className="bg-white pb-2">
                <CardTitle className="text-xl flex items-center gap-2 text-gray-800">
                  <Briefcase className="w-5 h-5 text-indigo-600" /> {userRole === "CLIENT" ? "클라이언트" : "개발자"} 전문 정보
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6 pt-4">
                {!roleData ? (
                  <div className="flex items-start gap-3 p-4 bg-amber-50 rounded-xl border border-amber-100 text-amber-800">
                    <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
                    <div className="text-sm">
                      <p className="font-bold mb-1">상세 정보가 확인되지 않습니다.</p>
                      <p className="opacity-90">아직 프로필 정보를 완성하지 않으셨다면 '내 프로필 수정' 메뉴에서 정보를 입력해 주세요.</p>
                    </div>
                  </div>
                ) : userRole === "CLIENT" ? (
                  <div className="space-y-5">
                    <div className="flex justify-between items-center py-3 border-b border-gray-50">
                      <span className="text-sm font-medium text-gray-500">현재 직군/직책</span>
                      <Badge variant="secondary" className="px-3 py-1 bg-indigo-50 text-indigo-700 border-none font-bold">
                        {roleData.title || "미지정"}
                      </Badge>
                    </div>
                    <div className="flex justify-between items-center py-3 border-b border-gray-50">
                      <span className="text-sm font-medium text-gray-500">회원 유형</span>
                      <div className="flex items-center gap-2">
                        {roleData.participateType === "COMPANY" ? "🏢 기업 회원" : "🧑 개인 회원"}
                      </div>
                    </div>
                    <div className="flex justify-between items-center py-3">
                      <span className="text-sm font-medium text-gray-500">누적 등록 프로젝트</span>
                      <span className="text-lg font-black text-gray-800">{roleData.completedProject || 0} <span className="text-sm font-normal text-gray-400 ml-0.5">건</span></span>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-8">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100">
                        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">전문 분야</p>
                        <p className="text-lg font-bold text-gray-800">{roleData.title || "미지정"}</p>
                      </div>
                      <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100 flex flex-col justify-between">
                        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">현재 작업 상태</p>
                        <div className="flex items-center gap-2">
                          <div className={`w-3 h-3 rounded-full ${roleData.availableForWork ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]' : 'bg-red-500'}`} />
                          <span className="font-bold text-gray-700">{roleData.availableForWork ? "새 프로젝트 수령 가능" : "현재 작업 중"}</span>
                        </div>
                      </div>
                    </div>
                    
                    <div className="space-y-3">
                      <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1.5">
                        <Clock className="w-3 h-3" /> 예상 희망 시급
                      </p>
                      <div className="flex items-baseline gap-1 text-2xl font-black text-blue-600">
                        {roleData.minHourlyPay?.toLocaleString() || "0"} 
                        <span className="text-sm font-medium text-gray-400 mx-1">~</span>
                        {roleData.maxHourlyPay?.toLocaleString() || "0"}
                        <span className="text-sm font-bold text-gray-500 ml-1">원</span>
                      </div>
                    </div>

                    <div className="space-y-3">
                      <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1.5">
                        <Briefcase className="w-3 h-3" /> 보유 기술 및 스택
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {roleData.skills && roleData.skills.length > 0 ? (
                          roleData.skills.map((skill: string) => (
                            <Badge key={skill} className="px-4 py-1.5 bg-white text-gray-700 border-gray-200 shadow-sm hover:border-blue-400 transition-colors">
                              {skill}
                            </Badge>
                          ))
                        ) : (
                          <span className="text-sm text-gray-400 italic">등록된 스택 정보가 없습니다.</span>
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
            <Card className="overflow-hidden border-none shadow-md bg-gray-900 text-white">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-white">
                  <Shield className="w-6 h-6 text-blue-400" /> 관리자 시스템 권한
                </CardTitle>
              </CardHeader>
              <CardContent className="p-8 space-y-8">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="p-5 bg-white/10 rounded-2xl backdrop-blur-sm border border-white/5">
                    <p className="text-[10px] font-bold text-blue-300 uppercase tracking-widest mb-3">Security Level</p>
                    <div className="flex items-center gap-3">
                      <Badge className="bg-blue-500 text-white border-none px-4 py-1">LEVEL 4</Badge>
                      <span className="font-bold">SYSTEM OWNER</span>
                    </div>
                  </div>
                  <div className="p-5 bg-white/10 rounded-2xl backdrop-blur-sm border border-white/5">
                    <p className="text-[10px] font-bold text-blue-300 uppercase tracking-widest mb-3">System Status</p>
                    <div className="flex items-center gap-2 font-black text-green-400">
                      <div className="w-2 h-2 bg-green-400 rounded-full animate-ping" />
                      CONNECTED
                    </div>
                  </div>
                </div>
                <div className="pt-6 border-t border-white/10">
                  <Link to="/admin">
                    <Button className="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold h-14 text-lg rounded-2xl shadow-xl shadow-blue-900/20">
                      관리자 대시보드 진입하기
                    </Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          )}
          
          <div className="flex items-center gap-2 justify-center text-gray-400">
            <Info className="w-4 h-4" />
            <p className="text-xs">마지막 업데이트: {new Date().toLocaleDateString()}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
