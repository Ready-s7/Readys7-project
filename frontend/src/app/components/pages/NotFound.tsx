import { Link } from "react-router";
import { Button } from "../ui/button";
import { Home } from "lucide-react";

export function NotFound() {
  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4">
      <div className="text-center">
        <h1 className="text-6xl mb-4">404</h1>
        <h2 className="text-2xl mb-4">페이지를 찾을 수 없습니다</h2>
        <p className="text-gray-600 mb-8">
          요청하신 페이지가 존재하지 않거나 이동되었습니다.
        </p>
        <Link to="/">
          <Button size="lg">
            <Home className="w-4 h-4 mr-2" />
            홈으로 돌아가기
          </Button>
        </Link>
      </div>
    </div>
  );
}
