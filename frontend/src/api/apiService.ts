import { apiClient } from "./client";
import type {
  SuccessResponse,
  ProjectDto,
  CategoryDto,
  DeveloperDto,
  ProposalDto,
  ReviewDto,
  ChatRoomDto,
  MessageCursorResponse,
  SkillDto,
  PageResponse,
  ClientPageResponse,
  CreateProposalRequest,
} from "./types";

// ─────────────────────────────────────────────────────────────
// 카테고리 API
// ─────────────────────────────────────────────────────────────
export const categoryApi = {
  getAll: () =>
    apiClient.get<SuccessResponse<CategoryDto[]>>("/v1/categories"),

  search: (name: string, description?: string) =>
    apiClient.get<SuccessResponse<CategoryDto[]>>("/v1/categories/search", {
      params: { name, description },
    }),
};

// ─────────────────────────────────────────────────────────────
// 프로젝트 API
// ─────────────────────────────────────────────────────────────
export const projectApi = {
  /** 전체 목록 조회 */
  getAll: () =>
    apiClient.get<SuccessResponse<ProjectDto[]>>("/v1/projects"),

  /** 단건 조회 */
  getById: (projectId: number) =>
    apiClient.get<SuccessResponse<ProjectDto>>(`/v1/projects/${projectId}`),

  /**
   * 검색 (페이징)
   * @param categoryId 카테고리 ID (optional)
   * @param status 상태 문자열 (optional)
   * @param skill 기술 배열 (optional)
   * @param page 페이지 번호 (0-based, Spring 기준)
   * @param size 페이지 크기
   */
  search: (params: {
    categoryId?: number;
    status?: string;
    skill?: string[];
    page?: number;
    size?: number;
  }) =>
    apiClient.get<SuccessResponse<PageResponse<ProjectDto>>>("/v1/projects/search", {
      params,
    }),

  /** 프로젝트 등록 (CLIENT 전용) */
  create: (data: {
    title: string;
    description: string;
    categoryId: number;
    minBudget: number;
    maxBudget: number;
    duration: number;
    skills: string[];
    maxProposalCount: number;
  }) => apiClient.post<SuccessResponse<ProjectDto>>("/v1/projects", data),

  /** 프로젝트 수정 */
  update: (
    projectId: number,
    data: Partial<{
      title: string;
      description: string;
      categoryId: number;
      minBudget: number;
      maxBudget: number;
      duration: number;
      skills: string[];
      maxProposalCount: number;
    }>
  ) => apiClient.put<SuccessResponse<ProjectDto>>(`/v1/projects/${projectId}`, data),

  /** 프로젝트 삭제 */
  delete: (projectId: number) =>
    apiClient.delete(`/v1/projects/${projectId}`),

  /** 상태 변경 */
  changeStatus: (projectId: number, status: string) =>
    apiClient.patch<SuccessResponse<ProjectDto>>(`/v1/projects/${projectId}/status`, null, {
      params: { status },
    }),
};

// ─────────────────────────────────────────────────────────────
// 개발자 API
// ─────────────────────────────────────────────────────────────
export const developerApi = {
  /** 전체 목록 (페이징) */
  getAll: (page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<DeveloperDto>>>("/v1/developers", {
      params: { page, size },
    }),

  /** 단건 조회 */
  getById: (developerId: number) =>
    apiClient.get<SuccessResponse<DeveloperDto>>(`/v1/developers/${developerId}`),

  /**
   * 검색
   * @param skills 기술 배열 (optional)
   * @param minRating 최소 평점 (optional)
   */
  search: (params: { skills?: string[]; minRating?: number; page?: number; size?: number }) =>
    apiClient.get<SuccessResponse<PageResponse<DeveloperDto>>>("/v1/developers/search", {
      params,
    }),

  /** 내 프로필 수정 (DEVELOPER 전용) */
  updateProfile: (data: Partial<{
    title: string;
    skills: string[];
    minHourlyPay: number;
    maxHourlyPay: number;
    responseTime: string;
    availableForWork: boolean;
  }>) => apiClient.put<SuccessResponse<DeveloperDto>>("/v1/developers/profile", data),

  /** 내 프로젝트 목록 (DEVELOPER 전용) */
  getMyProjects: (page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<ProjectDto>>>("/v1/developers/me/my-projects", {
      params: { page, size },
    }),
};

// ─────────────────────────────────────────────────────────────
// 제안서 API
// ─────────────────────────────────────────────────────────────
export const proposalApi = {
  /** 제안서 제출 (DEVELOPER 전용) */
  create: (data: CreateProposalRequest) =>
    apiClient.post<SuccessResponse<ProposalDto>>("/v1/proposals", data),

  /** 특정 프로젝트의 제안서 목록 (프로젝트 소유 CLIENT 또는 ADMIN) */
  getByProject: (projectId: number, page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<ProposalDto>>>("/v1/proposals", {
      params: { projectId, page, size },
    }),

  /** 제안서 단건 조회 */
  getById: (proposalId: number) =>
    apiClient.get<SuccessResponse<ProposalDto>>(`/v1/proposals/${proposalId}`),

  /** 내가 제출한 제안서 목록 (DEVELOPER 전용) */
  getMyProposals: (page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<ProposalDto>>>("/v1/proposals/my-proposals", {
      params: { page, size },
    }),

  /** 제안서 상태 변경 (CLIENT: ACCEPTED/REJECTED, DEVELOPER: WITHDRAWN) */
  updateStatus: (proposalId: number, status: "ACCEPTED" | "REJECTED" | "WITHDRAWN" | "PENDING") =>
    apiClient.patch<SuccessResponse<ProposalDto>>(`/v1/proposals/${proposalId}`, { status }),
};

// ─────────────────────────────────────────────────────────────
// 리뷰 API
// ─────────────────────────────────────────────────────────────
export const reviewApi = {
  /** 리뷰 작성 */
  create: (
    targetUserId: number,
    data: { projectId: number; rating: number; comment: string }
  ) =>
    apiClient.post<SuccessResponse<ReviewDto>>("/v1/reviews", data, {
      params: { targetUserId },
    }),

  /** 개발자 리뷰 목록 조회 */
  getByDeveloper: (
    developerId: number,
    params?: { rating?: number; minRating?: number; maxRating?: number; page?: number; size?: number }
  ) =>
    apiClient.get<SuccessResponse<PageResponse<ReviewDto>>>("/v1/reviews", {
      params: { developerId, ...params },
    }),

  /** 클라이언트 리뷰 목록 조회 */
  getByClient: (
    clientId: number,
    params?: { rating?: number; minRating?: number; maxRating?: number; page?: number; size?: number }
  ) =>
    apiClient.get<SuccessResponse<PageResponse<ReviewDto>>>("/v1/reviews", {
      params: { clientId, ...params },
    }),

  /** 리뷰 수정 */
  update: (reviewId: number, data: { rating?: number; comment?: string }) =>
    apiClient.patch<SuccessResponse<ReviewDto>>(`/v1/reviews/${reviewId}`, data),

  /** 리뷰 삭제 */
  delete: (reviewId: number) => apiClient.delete(`/v1/reviews/${reviewId}`),
};

// ─────────────────────────────────────────────────────────────
// 채팅 API (REST)
// ─────────────────────────────────────────────────────────────
export const chatApi = {
  /** 채팅방 생성 (CLIENT 전용, ACCEPTED 제안서 필요) */
  createRoom: (projectId: number, developerId: number) =>
    apiClient.post<SuccessResponse<ChatRoomDto>>("/v1/chat/rooms", { projectId, developerId }),

  /** 내 채팅방 목록 조회 */
  getMyRooms: (page = 0, size = 20) =>
    apiClient.get<SuccessResponse<PageResponse<ChatRoomDto>>>("/v1/chat/rooms", {
      params: { page, size },
    }),

  /**
   * 이전 메시지 조회 (커서 기반 페이징)
   * @param roomId 채팅방 ID
   * @param lastMessageId 이 ID보다 작은 메시지 조회 (optional, 최초 조회 시 생략)
   * @param size 조회 개수
   */
  getMessages: (roomId: number, lastMessageId?: number, size = 30) =>
    apiClient.get<SuccessResponse<MessageCursorResponse>>(
      `/v1/chat/rooms/${roomId}/messages`,
      { params: { lastMessageId, size } }
    ),
};

// ─────────────────────────────────────────────────────────────
// 스킬 API
// ─────────────────────────────────────────────────────────────
export const skillApi = {
  /** 전체 스킬 목록 */
  getAll: (page = 0, size = 100) =>
    apiClient.get<SuccessResponse<PageResponse<SkillDto>>>("/v1/skills", {
      params: { page, size },
    }),

  /** 스킬 검색 */
  search: (name?: string, category?: string, page = 0, size = 20) =>
    apiClient.get<SuccessResponse<PageResponse<SkillDto>>>("/v1/skills/search", {
      params: { name, category, page, size },
    }),
};
