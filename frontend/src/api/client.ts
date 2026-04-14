/**
 * API 클라이언트 설정
 * - axios 인스턴스 생성
 * - 요청 인터셉터: Authorization 헤더 자동 주입
 * - 응답 인터셉터: 401 발생 시 토큰 재발급 처리
 */
import axios, { AxiosError } from "axios";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

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
        // 리프레시 토큰도 없으면 로그인 페이지로
        localStorage.clear();
        window.location.href = "/login";
        return Promise.reject(error);
      }

      try {
        // 백엔드 reissue API 호출 (POST /api/v1/auth/reissue)
        const res = await axios.post(`${BASE_URL}/v1/auth/reissue`, {
          refreshToken,
        });

        const newAccessToken = res.headers["authorization"]?.replace("Bearer ", "");
        if (newAccessToken) {
          localStorage.setItem("accessToken", newAccessToken);
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        return apiClient(originalRequest);
      } catch {
        localStorage.clear();
        window.location.href = "/login";
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);
