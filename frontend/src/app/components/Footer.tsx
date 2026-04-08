import { Link } from "react-router";
import { Code2 } from "lucide-react";

export function Footer() {
  return (
    <footer className="bg-gray-50 border-t mt-20">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Logo and Description */}
          <div className="md:col-span-2">
            <Link to="/" className="flex items-center gap-2 mb-4">
              <div className="bg-blue-600 p-2 rounded-lg">
                <Code2 className="w-5 h-5 text-white" />
              </div>
              <span className="font-bold text-xl">Ready's7</span>
            </Link>
            <p className="text-gray-600 max-w-md">
              전문 개발자와 프로젝트를 연결하는 최고의 플랫폼입니다.
              믿을 수 있는 개발자를 만나보세요.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="font-semibold mb-4">서비스</h3>
            <ul className="space-y-2 text-gray-600">
              <li>
                <Link to="/projects" className="hover:text-blue-600">
                  프로젝트 찾기
                </Link>
              </li>
              <li>
                <Link to="/developers" className="hover:text-blue-600">
                  개발자 찾기
                </Link>
              </li>
              <li>
                <Link to="/projects/new" className="hover:text-blue-600">
                  프로젝트 등록
                </Link>
              </li>
            </ul>
          </div>

          {/* Company */}
          <div>
            <h3 className="font-semibold mb-4">회사</h3>
            <ul className="space-y-2 text-gray-600">
              <li>
                <a href="#" className="hover:text-blue-600">
                  회사 소개
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-blue-600">
                  이용약관
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-blue-600">
                  개인정보처리방침
                </a>
              </li>
            </ul>
          </div>
        </div>

        <div className="border-t mt-8 pt-8 text-center text-gray-600 text-sm">
          © 2026 DevMatch. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
