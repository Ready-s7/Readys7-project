import { RouterProvider } from "react-router";
import { Toaster } from "./components/ui/sonner";
import { AuthProvider } from "../context/AuthContext";
import { router } from "./routes";

function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
      {/* 
        position을 "bottom-right"로 변경하여 로그인 버튼을 가리지 않도록 수정
        offset을 추가하여 버튼 영역과 겹치지 않게 처리
      */}
      <Toaster
        richColors
        position="bottom-right"
        toastOptions={{
          duration: 2500,
        }}
      />
    </AuthProvider>
  );
}

export default App;
