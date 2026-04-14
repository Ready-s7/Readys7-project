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

  /**
   * 로그인
   * - 응답 헤더 Authorization에서 accessToken 추출
   * - 응답 바디의 refreshToken은 localStorage에 저장
   */
  login: async (data: LoginRequest) => {
    const response = await apiClient.post<SuccessResponse<LoginResponse>>(
      "/v1/auth/login",
      data
    );
    // 헤더에서 accessToken 추출
    const authHeader = response.headers["authorization"] as string | undefined;
    const accessToken = authHeader?.replace("Bearer ", "") ?? null;

    if (accessToken) {
      localStorage.setItem("accessToken", accessToken);
    }
    if (response.data.data.refreshToken) {
      localStorage.setItem("refreshToken", response.data.data.refreshToken);
    }
    if (response.data.data.email) {
      localStorage.setItem("userEmail", response.data.data.email);
    }

    return { accessToken, loginResponse: response.data.data };
  },

  /** 로그아웃 */
  logout: async () => {
    await apiClient.post("/v1/auth/logout");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userRole");
  },

  /** 내 정보 조회 */
  getMe: () =>
    apiClient.get<SuccessResponse<GetUserInfoResponse>>("/v1/users/me"),
};
