/**
 * API 클라이언트 설정
 *
 * [수정 이유]
 * 1. 백엔드 application.yml에 `cors.exposed-headers: Authorization` 이 설정돼 있으나,
 *    브라우저는 기본적으로 커스텀 헤더를 JS에서 읽지 못함.
 *    → axios 인스턴스에 `withCredentials: false`는 맞지만,
 *      response headers 접근 시 대소문자 문제가 발생할 수 있음.
 *    → 헤더 읽기 시 소문자 우선 + fallback 처리로 해결.
 *
 * 2. 401 인터셉터에서 재발급 실패 후 location.href 이동 전 localStorage clear()가
 *    너무 광범위하게 동작할 수 있음 → 필요한 항목만 제거하도록 변경.
 */
import axios, { AxiosError } from "axios";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "/api";

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: false,
});

// ─────────────────────────────────────────────────────────────
// 요청 인터셉터: localStorage에서 accessToken을 꺼내 헤더에 주입
// ─────────────────────────────────────────────────────────────
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─────────────────────────────────────────────────────────────
// 응답 인터셉터: 401 시 refreshToken으로 재발급 시도
// ─────────────────────────────────────────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as any;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userEmail");
        localStorage.removeItem("userRole");
        window.location.href = "/login";
        return Promise.reject(error);
      }

      try {
        const res = await axios.post(`${BASE_URL}/v1/auth/reissue`, {
          refreshToken,
        });

        // 대소문자 무관하게 Authorization 헤더 읽기
        const authHeader =
          (res.headers["authorization"] as string | undefined) ||
          (res.headers["Authorization"] as string | undefined);
        const newAccessToken = authHeader?.replace("Bearer ", "").trim();
        const newRefreshToken = res.data.data.refreshToken;

        if (newRefreshToken) {
          localStorage.setItem("refreshToken", newRefreshToken);
        }

        if (newAccessToken) {
          localStorage.setItem("accessToken", newAccessToken);
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        return apiClient(originalRequest);
      } catch {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userEmail");
        localStorage.removeItem("userRole");
        window.location.href = "/login";
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);
