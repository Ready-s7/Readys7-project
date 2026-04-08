import { Link, useLocation } from "react-router";
import { Button } from "./ui/button";
import { Code2, Menu, Search } from "lucide-react";
import { useState } from "react";
import { Sheet, SheetContent, SheetTrigger } from "./ui/sheet";
import { Input } from "./ui/input";

export function Header() {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { path: "/projects", label: "프로젝트" },
    { path: "/developers", label: "개발자" },
  ];

  const isActive = (path: string) => location.pathname.startsWith(path);

  return (
    <header className="border-b bg-white sticky top-0 z-50">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <div className="bg-blue-600 p-2 rounded-lg">
              <Code2 className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-xl">Ready's7</span>
          </Link>

          {/* Desktop Navigation */}
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
          </nav>

          {/* Desktop Actions */}
          <div className="hidden md:flex items-center gap-3">
            <Button variant="ghost" size="sm">
              <Search className="w-4 h-4 mr-2" />
              검색
            </Button>
            <Link to="/projects/new">
              <Button variant="outline" size="sm">
                프로젝트 등록
              </Button>
            </Link>
            <Link to="/login">
              <Button size="sm">로그인</Button>
            </Link>
          </div>

          {/* Mobile Menu */}
          <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
            <SheetTrigger asChild className="md:hidden">
              <Button variant="ghost" size="icon">
                <Menu className="w-5 h-5" />
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-[300px]">
              <div className="flex flex-col gap-6 mt-8">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input placeholder="검색..." className="pl-10" />
                </div>
                <nav className="flex flex-col gap-4">
                  {navItems.map((item) => (
                    <Link
                      key={item.path}
                      to={item.path}
                      onClick={() => setMobileMenuOpen(false)}
                      className={`text-lg ${
                        isActive(item.path) ? "text-blue-600" : "text-gray-700"
                      }`}
                    >
                      {item.label}
                    </Link>
                  ))}
                </nav>
                <div className="flex flex-col gap-3 pt-4 border-t">
                  <Link to="/projects/new" onClick={() => setMobileMenuOpen(false)}>
                    <Button variant="outline" className="w-full">
                      프로젝트 등록
                    </Button>
                  </Link>
                  <Link to="/login" onClick={() => setMobileMenuOpen(false)}>
                    <Button className="w-full">로그인</Button>
                  </Link>
                </div>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}
