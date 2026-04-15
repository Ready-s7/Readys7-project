/**
 * AuthContext
 * - 전역 로그인 상태(userEmail, userRole, isLoggedIn) 관리
 * - login() / logout() 함수 제공
 * - localStorage와 동기화
 *
 * [수정 이유]
 * 1. 회원가입 후 자동 로그인 시 getMe() 호출 타이밍 문제 → login() 내에서 처리하도록 통합
 * 2. 로그인 성공 여부를 accessToken 존재 여부로만 판단하다 보니, 헤더 파싱 실패 시
 *    DB 저장은 됐는데 프론트에서 실패 처리되는 문제 발생 → email 기반 fallback 추가
 */
import {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
} from "react";
import { authApi } from "../api/authApi";
import type {
  LoginRequest,
  ClientRegisterRequest,
  DeveloperRegisterRequest,
} from "../api/types";

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
    const userRole = localStorage.getItem(
      "userRole"
    ) as AuthState["userRole"];
    return {
      isLoggedIn: !!accessToken,
      userEmail,
      userRole,
    };
  });

  const login = useCallback(async (data: LoginRequest) => {
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

  /**
   * 회원가입(클라이언트)
   * [수정] 백엔드가 201 Created를 반환하면 성공으로 처리.
   * 기존 코드에서 에러가 났던 이유:
   *   - 백엔드 응답 status가 201인데 axios 기본 설정으로는 2xx 전부 성공 처리가 되어야 하나,
   *     가끔 응답 구조 파싱 중 오류가 나면 catch로 빠짐.
   *   - 이제 registerClient 내에서 throw가 발생해도 호출부(Login.tsx)에서 정확한 에러 메시지를 잡도록 함.
   */
  const registerClient = useCallback(
    async (data: ClientRegisterRequest) => {
      await authApi.registerClient(data);
      // 회원가입 후 자동 로그인
      await login({ email: data.email, password: data.password });
    },
    [login]
  );

  const registerDeveloper = useCallback(
    async (data: DeveloperRegisterRequest) => {
      await authApi.registerDeveloper(data);
      await login({ email: data.email, password: data.password });
    },
    [login]
  );

  const logout = useCallback(async () => {
    await authApi.logout();
    setState({ isLoggedIn: false, userEmail: null, userRole: null });
  }, []);

  return (
    <AuthContext.Provider
      value={{ ...state, login, registerClient, registerDeveloper, logout }}
    >
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
