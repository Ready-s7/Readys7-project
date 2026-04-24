/**
 * Header.tsx (개선판)
 * - 로그인 상태에 따른 네비게이션 메뉴 추가
 * - CLIENT: 내 프로필, 내 프로젝트, 채팅
 * - DEVELOPER: 내 프로필, 내 프로젝트, 내 제안서, 포트폴리오 관리, 채팅
 * - ADMIN: 관리자 대시보드
 */
import { Link, useLocation, useNavigate } from "react-router";
import { Button } from "./ui/button";
import {
  Code2,
  Menu,
  MessageCircle,
  FileText,
  LogOut,
  User,
  Pencil,
  FolderOpen,
  Briefcase,
  Shield,
  ChevronDown,
} from "lucide-react";
import { useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "./ui/sheet";
import { Badge } from "./ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { useAuth } from "../../context/AuthContext";
import { toast } from "sonner";

import logo from "../../assets/logo.png";

export function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const { isLoggedIn, userEmail, userRole, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { path: "/projects", label: "프로젝트" },
    { path: "/developers", label: "개발자" },
    { path: "/clients", label: "클라이언트" },
    { path: "/cs", label: "고객센터" },
  ];

  const isActive = (path: string) => location.pathname.startsWith(path);

  const handleLogout = async () => {
    await logout();
    toast.success("로그아웃 되었습니다.");
    navigate("/");
  };

  // 디버깅용 로그 (브라우저 콘솔에서 확인 가능)
  console.log("[Header] State:", { isLoggedIn, userEmail, userRole });

  // 역할별 추가 메뉴
  const roleMenuItems = () => {
    if (!isLoggedIn || !userRole) return [];
    const items = [];
    const role = userRole.toUpperCase();
    
    if (role === "CLIENT") {
      items.push({ path: "/my-projects", label: "내 프로젝트", icon: FolderOpen });
      items.push({ path: "/chat", label: "채팅", icon: MessageCircle });
    }
    else if (role === "DEVELOPER") {
      items.push({ path: "/my-projects", label: "내 프로젝트", icon: FolderOpen });
      items.push({ path: "/my-proposals", label: "내 제안서", icon: FileText });
      items.push({ path: "/my-portfolio", label: "포트폴리오 관리", icon: Briefcase });
      items.push({ path: "/chat", label: "채팅", icon: MessageCircle });
    }
    else if (role === "ADMIN") {
      items.push({ path: "/admin", label: "관리자 대시보드", icon: Shield });
    }
    return items;
  };

  return (
    <header className="border-b border-white/5 bg-background/80 backdrop-blur-md sticky top-0 z-[100] w-full">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16 gap-4">
          {/* 로고 (왼쪽) */}
          <Link to="/" className="flex items-center gap-2 flex-shrink-0">
            <img src={logo} alt="Ready's7 Logo" className="h-8 w-auto" />
            <span className="font-bold text-xl tracking-tight text-white">Ready's7</span>
          </Link>

          {/* 데스크탑 내비 (중앙) */}
          <nav className="hidden md:flex items-center gap-6 flex-1 justify-center">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`hover:text-primary transition-colors text-sm font-medium ${
                  isActive(item.path) ? "text-primary" : "text-gray-400"
                }`}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          {/* 데스크탑 오른쪽 액션 (오른쪽) */}
          <div className="hidden md:flex items-center gap-3 flex-shrink-0">
            {isLoggedIn ? (
              <>
                <DropdownMenu>
                  {/* asChild 제거 후 직접 스타일링 - 클릭 신뢰성 확보 */}
                  <DropdownMenuTrigger className="flex items-center gap-2 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-all outline-none border border-transparent focus:border-blue-200 cursor-pointer">
                    <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white shadow-sm">
                      <User className="w-4 h-4" />
                    </div>
                    <div className="flex flex-col items-start leading-none gap-1">
                      <span className="max-w-[120px] truncate text-xs font-bold text-gray-800">{userEmail}</span>
                      <span className="text-[10px] text-blue-600 font-semibold uppercase tracking-wider">{userRole}</span>
                    </div>
                    <ChevronDown className="w-4 h-4 text-gray-400 ml-1" />
                  </DropdownMenuTrigger>
                  
                  <DropdownMenuContent align="end" sideOffset={10} className="w-56 p-1 shadow-2xl bg-white border-gray-200 z-[110]">
                    <div className="px-3 py-2 mb-1 border-b bg-gray-50/50 rounded-t-md">
                      <p className="text-[10px] font-bold text-gray-400 uppercase mb-0.5">Logged in as</p>
                      <p className="text-sm font-semibold text-gray-900 truncate">{userEmail}</p>
                    </div>

                    <DropdownMenuItem onClick={() => navigate("/profile")} className="cursor-pointer py-2 focus:bg-blue-50">
                      <User className="w-4 h-4 mr-3 text-gray-500" />
                      <span className="text-sm">내 프로필 보기</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem onClick={() => navigate("/my-profile")} className="cursor-pointer py-2 focus:bg-blue-50">
                      <Pencil className="w-4 h-4 mr-3 text-gray-500" />
                      <span className="text-sm">내 프로필 수정</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem onClick={() => navigate("/cs")} className="cursor-pointer py-2 focus:bg-blue-50">
                      <MessageCircle className="w-4 h-4 mr-3 text-gray-500" />
                      <span className="text-sm">고객센터 문의</span>
                    </DropdownMenuItem>
                    
                    {roleMenuItems().length > 0 && (
                      <>
                        <DropdownMenuSeparator className="my-1" />
                        {roleMenuItems().map((item) => (
                          <DropdownMenuItem key={item.path} onClick={() => navigate(item.path)} className="cursor-pointer py-2 focus:bg-blue-50">
                            <item.icon className="w-4 h-4 mr-3 text-gray-500" />
                            <span className="text-sm">{item.label}</span>
                          </DropdownMenuItem>
                        ))}
                      </>
                    )}
                    
                    <DropdownMenuSeparator className="my-1" />
                    <DropdownMenuItem onClick={handleLogout} className="text-red-600 cursor-pointer py-2 focus:bg-red-50 focus:text-red-600">
                      <LogOut className="w-4 h-4 mr-3" />
                      <span className="text-sm font-bold">로그아웃</span>
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>

                {userRole?.toUpperCase() === "CLIENT" && (
                  <Link to="/projects/new">
                    <Button size="sm" className="bg-blue-600 hover:bg-blue-700 shadow-sm">프로젝트 등록</Button>
                  </Link>
                )}
              </>
            ) : (
              <>
                <Link to="/projects/new">
                  <Button variant="outline" size="sm" className="border-gray-300 hover:bg-gray-50">프로젝트 등록</Button>
                </Link>
                <Link to="/login">
                  <Button size="sm" className="bg-blue-600 hover:bg-blue-700 shadow-sm px-6">로그인</Button>
                </Link>
              </>
            )}
          </div>

          {/* 모바일 메뉴 */}
          <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
            <SheetTrigger asChild className="md:hidden">
              <Button variant="ghost" size="icon">
                <Menu className="w-5 h-5" />
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-[280px]">
              <div className="flex flex-col gap-6 mt-8">
                {isLoggedIn && (
                  <div className="pb-4 border-b">
                    <p className="text-sm font-medium truncate">{userEmail}</p>
                    <Badge variant="secondary" className="mt-1">{userRole}</Badge>
                  </div>
                )}
                <nav className="flex flex-col gap-3">
                  {navItems.map((item) => (
                    <Link
                      key={item.path}
                      to={item.path}
                      onClick={() => setMobileMenuOpen(false)}
                      className={`text-base ${isActive(item.path) ? "text-blue-600" : "text-gray-700"}`}
                    >
                      {item.label}
                    </Link>
                  ))}
                  {isLoggedIn && (
                    <>
                      <Link to="/my-profile" onClick={() => setMobileMenuOpen(false)} className="text-base text-gray-700">
                        내 프로필 수정
                      </Link>
                      {roleMenuItems().map((item) => (
                        <Link
                          key={item.path}
                          to={item.path}
                          onClick={() => setMobileMenuOpen(false)}
                          className="text-base text-gray-700 flex items-center gap-2"
                        >
                          <item.icon className="w-4 h-4" />{item.label}
                        </Link>
                      ))}
                    </>
                  )}
                </nav>
                <div className="flex flex-col gap-3 pt-4 border-t">
                  {isLoggedIn ? (
                    <Button variant="outline" onClick={handleLogout} className="w-full">
                      <LogOut className="w-4 h-4 mr-2" />로그아웃
                    </Button>
                  ) : (
                    <>
                      <Link to="/projects/new" onClick={() => setMobileMenuOpen(false)}>
                        <Button variant="outline" className="w-full">프로젝트 등록</Button>
                      </Link>
                      <Link to="/login" onClick={() => setMobileMenuOpen(false)}>
                        <Button className="w-full">로그인</Button>
                      </Link>
                    </>
                  )}
                </div>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}
