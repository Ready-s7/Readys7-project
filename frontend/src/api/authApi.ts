/**
 * authApi.ts (수정판)
 *
 * 주요 수정 사항:
 * 1. login() 후 getMe()에서 name도 저장 (헤더에서 이름 표시 가능)
 * 2. GetUserInfoResponse 타입 업데이트 반영
 */
import { apiClient } from "./client";
import type {
  SuccessResponse,
  LoginResponse,
  UserDto,
  GetUserInfoResponse,
  LoginRequest,
  ClientRegisterRequest,
  DeveloperRegisterRequest,
} from "./types";

export const authApi = {
  /** 클라이언트 회원가입 */
  registerClient: (data: ClientRegisterRequest) =>
      apiClient.post<SuccessResponse<UserDto>>("/v1/auth/register/clients", data),

  /** 개발자 회원가입 */
  registerDeveloper: (data: DeveloperRegisterRequest) =>
      apiClient.post<SuccessResponse<UserDto>>("/v1/auth/register/developers", data),

  /** 로그인 */
  login: async (data: LoginRequest) => {
    const response = await apiClient.post<SuccessResponse<LoginResponse>>(
        "/v1/auth/login",
        data
    );

    const authHeader =
        (response.headers["authorization"] as string | undefined) ||
        (response.headers["Authorization"] as string | undefined);

    // Bearer 접두사 제거 및 순수 토큰 추출
    const accessToken = authHeader?.startsWith("Bearer ") 
        ? authHeader.substring(7).trim() 
        : authHeader?.trim() ?? null;

    if (!accessToken) {
      console.error("[Login] Authorization 헤더를 찾을 수 없습니다.", response.headers);
    } else {
      localStorage.setItem("accessToken", accessToken);
    }    if (response.data.data.refreshToken) {
      localStorage.setItem("refreshToken", response.data.data.refreshToken);
    }
    if (response.data.data.email) {
      localStorage.setItem("userEmail", response.data.data.email);
    }

    return { accessToken, loginResponse: response.data.data };
  },

  /** 로그아웃 */
  logout: async () => {
    try {
      await apiClient.post("/v1/auth/logout");
    } catch {
      // 토큰 만료 등으로 실패해도 로컬 상태는 무조건 초기화
    } finally {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userEmail");
      localStorage.removeItem("userRole");
      localStorage.removeItem("userId");
      localStorage.removeItem("userName");
    }
  },

  /** 내 정보 조회 - name, phoneNumber 포함 */
  getMe: () =>
      apiClient.get<SuccessResponse<GetUserInfoResponse>>("/v1/users/me"),

  /** 회원 탈퇴 */
  withdraw: () =>
      apiClient.delete("/v1/users/me"),
};
