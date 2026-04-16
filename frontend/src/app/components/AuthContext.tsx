/**
 * AuthContext.tsx (수정판)
 *
 * 주요 수정 사항:
 * 1. login() 후 userName도 localStorage에 저장 (헤더/프로필에서 이름 표시)
 * 2. 초기 상태 복원 시 userName도 포함
 * 3. 로그아웃 시 userName도 제거
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
    userId: number | null;
    userEmail: string | null;
    userName: string | null;    // ★ 추가
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
        const accessToken = localStorage.getItem("accessToken");
        const userId = localStorage.getItem("userId");
        const userEmail = localStorage.getItem("userEmail");
        const userName = localStorage.getItem("userName");  // ★ 추가
        const userRole = localStorage.getItem("userRole") as AuthState["userRole"];
        return {
            isLoggedIn: !!accessToken,
            userId: userId ? parseInt(userId, 10) : null,
            userEmail,
            userName,   // ★ 추가
            userRole,
        };
    });

    const login = useCallback(async (data: LoginRequest) => {
        const { loginResponse } = await authApi.login(data);

        // 로그인 후 내 정보 조회
        const meRes = await authApi.getMe();
        const { id, userRole: role, name } = meRes.data.data;

        localStorage.setItem("userId", id.toString());
        localStorage.setItem("userRole", role);
        localStorage.setItem("userName", name || "");   // ★ 추가

        setState({
            isLoggedIn: true,
            userId: id,
            userEmail: loginResponse.email,
            userName: name || null,   // ★ 추가
            userRole: role,
        });
    }, []);

    const registerClient = useCallback(
        async (data: ClientRegisterRequest) => {
            await authApi.registerClient(data);
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
        localStorage.removeItem("userId");
        localStorage.removeItem("userName");   // ★ 추가
        setState({
            isLoggedIn: false,
            userId: null,
            userEmail: null,
            userName: null,   // ★ 추가
            userRole: null,
        });
    }, []);

    return (
        <AuthContext.Provider
            value={{ ...state, login, registerClient, registerDeveloper, logout }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within AuthProvider");
    return ctx;
}