import { Link, useLocation, useNavigate } from "react-router";
import { Button } from "./ui/button";
import { Code2, Menu, Search, MessageCircle, FileText, LogOut } from "lucide-react";
import { useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "./ui/sheet";
import { Input } from "./ui/input";
import { useAuth } from "../../context/AuthContext";
import { toast } from "sonner";

export function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const { isLoggedIn, userEmail, userRole, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { path: "/projects", label: "프로젝트" },
    { path: "/developers", label: "개발자" },
  ];
  const isActive = (path: string) => location.pathname.startsWith(path);

  const handleLogout = async () => {
    await logout();
    toast.success("로그아웃 되었습니다.");
    navigate("/");
  };

  return (
    <header className="border-b bg-white sticky top-0 z-50">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          {/* 로고 */}
          <Link to="/" className="flex items-center gap-2">
            <div className="bg-blue-600 p-2 rounded-lg">
              <Code2 className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-xl">Ready's7</span>
          </Link>

          {/* 데스크탑 내비 */}
          <nav className="hidden md:flex items-center gap-8">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`hover:text-blue-600 transition-colors ${
                  isActive(item.path) ? "text-blue-600" : "text-gray-700"
                }`}
              >
                {item.label}
              </Link>
            ))}
            {isLoggedIn && (
              <>
                <Link
                  to="/chat"
                  className={`hover:text-blue-600 transition-colors flex items-center gap-1 ${
                    isActive("/chat") ? "text-blue-600" : "text-gray-700"
                  }`}
                >
                  <MessageCircle className="w-4 h-4" />채팅
                </Link>
                {userRole === "DEVELOPER" && (
                  <Link
                    to="/my-proposals"
                    className={`hover:text-blue-600 transition-colors flex items-center gap-1 ${
                      isActive("/my-proposals") ? "text-blue-600" : "text-gray-700"
                    }`}
                  >
                    <FileText className="w-4 h-4" />내 제안서
                  </Link>
                )}
              </>
            )}
          </nav>

          {/* 데스크탑 액션 버튼 */}
          <div className="hidden md:flex items-center gap-3">
            {isLoggedIn ? (
              <>
                <span className="text-sm text-gray-600 max-w-[120px] truncate">{userEmail}</span>
                <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded-full">
                  {userRole}
                </span>
                <Button variant="ghost" size="sm" onClick={handleLogout}>
                  <LogOut className="w-4 h-4 mr-1" />로그아웃
                </Button>
                {userRole === "CLIENT" && (
                  <Link to="/projects/new">
                    <Button size="sm">프로젝트 등록</Button>
                  </Link>
                )}
              </>
            ) : (
              <>
                <Link to="/projects/new">
                  <Button variant="outline" size="sm">프로젝트 등록</Button>
                </Link>
                <Link to="/login">
                  <Button size="sm">로그인</Button>
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
            <SheetContent side="right" className="w-[300px]">
              <div className="flex flex-col gap-6 mt-8">
                <nav className="flex flex-col gap-4">
                  {navItems.map((item) => (
                    <Link key={item.path} to={item.path}
                      onClick={() => setMobileMenuOpen(false)}
                      className={`text-lg ${isActive(item.path) ? "text-blue-600" : "text-gray-700"}`}
                    >
                      {item.label}
                    </Link>
                  ))}
                  {isLoggedIn && (
                    <>
                      <Link to="/chat" onClick={() => setMobileMenuOpen(false)} className="text-lg text-gray-700">채팅</Link>
                      {userRole === "DEVELOPER" && (
                        <Link to="/my-proposals" onClick={() => setMobileMenuOpen(false)} className="text-lg text-gray-700">내 제안서</Link>
                      )}
                    </>
                  )}
                </nav>
                <div className="flex flex-col gap-3 pt-4 border-t">
                  {isLoggedIn ? (
                    <Button variant="outline" onClick={handleLogout} className="w-full">
                      로그아웃
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
