/**
 * AuthContext
 * - 전역 로그인 상태(userEmail, userRole, isLoggedIn) 관리
 * - login() / logout() 함수 제공
 * - localStorage와 동기화
 */
import { createContext, useContext, useState, useCallback, ReactNode } from "react";
import { authApi } from "../api/authApi";
import type { LoginRequest, ClientRegisterRequest, DeveloperRegisterRequest } from "../api/types";

interface AuthState {
  isLoggedIn: boolean;
  userEmail: string | null;
  userRole: "CLIENT" | "DEVELOPER" | "ADMIN" | null;
}

interface AuthContextType extends AuthState {
  login: (data: LoginRequest) => Promise<void>;
  registerClient: (data: ClientRegisterRequest) => Promise<void>;
  registerDeveloper: (data: DeveloperRegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    // 앱 초기 마운트 시 localStorage에서 상태 복원
    const accessToken = localStorage.getItem("accessToken");
    const userEmail = localStorage.getItem("userEmail");
    const userRole = localStorage.getItem("userRole") as AuthState["userRole"];
    return {
      isLoggedIn: !!accessToken,
      userEmail,
      userRole,
    };
  });

  const login = useCallback(async (data: LoginRequest) => {
    // authApi.login() 내부에서 localStorage 저장까지 처리
    const { loginResponse } = await authApi.login(data);

    // 로그인 후 내 정보 조회해서 역할(role) 저장
    const meRes = await authApi.getMe();
    const role = meRes.data.data.userRole;
    localStorage.setItem("userRole", role);

    setState({
      isLoggedIn: true,
      userEmail: loginResponse.email,
      userRole: role,
    });
  }, []);

  const registerClient = useCallback(async (data: ClientRegisterRequest) => {
    await authApi.registerClient(data);
    // 회원가입 후 자동 로그인 처리
    await login({ email: data.email, password: data.password });
  }, [login]);

  const registerDeveloper = useCallback(async (data: DeveloperRegisterRequest) => {
    await authApi.registerDeveloper(data);
    await login({ email: data.email, password: data.password });
  }, [login]);

  const logout = useCallback(async () => {
    await authApi.logout();
    setState({ isLoggedIn: false, userEmail: null, userRole: null });
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, registerClient, registerDeveloper, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

/** AuthContext를 사용하는 커스텀 훅 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
