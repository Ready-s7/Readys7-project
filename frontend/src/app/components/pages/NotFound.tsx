import { Link } from "react-router";
import { Button } from "../ui/button";
import { Home } from "lucide-react";

export function NotFound() {
  return (
    <div className="min-h-[80vh] bg-background flex items-center justify-center px-4">
      <div className="text-center max-w-md">
        <h1 className="text-9xl font-black text-primary/20 mb-4 animate-pulse">404</h1>
        <h2 className="text-4xl font-black text-foreground mb-4 tracking-tight">페이지를 찾을 수 없습니다</h2>
        <p className="text-muted-foreground font-medium mb-10 leading-relaxed">
          찾으시는 페이지의 주소가 잘못 입력되었거나,<br />
          변경 혹은 삭제되어 현재 사용이 불가능할 수 있습니다.
        </p>
        <Link to="/">
          <Button size="lg" className="bg-primary hover:bg-primary/90 text-primary-foreground font-black h-14 px-10 rounded-2xl shadow-xl shadow-primary/20 transition-all hover:scale-105 active:scale-95">
            <Home className="w-5 h-5 mr-3" />
            메인으로 돌아가기
          </Button>
        </Link>
      </div>
    </div>
  );
}
