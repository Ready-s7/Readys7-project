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
  CreateProposalRequest,
  AdminDto,
  AdminListResponse,
  PortfolioDto,
  ClientDto,
  ClientPageResponse,
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
  getAll: () =>
    apiClient.get<SuccessResponse<ProjectDto[]>>("/v1/projects"),

  getById: (projectId: number) =>
    apiClient.get<SuccessResponse<ProjectDto>>(`/v1/projects/${projectId}`),

  search: (params: {
    categoryId?: number;
    status?: string;
    skill?: string[];
    page?: number;
    size?: number;
  }) =>
    apiClient.get<SuccessResponse<PageResponse<ProjectDto>>>(
      "/v1/projects/search",
      { params }
    ),

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
  ) =>
    apiClient.put<SuccessResponse<ProjectDto>>(
      `/v1/projects/${projectId}`,
      data
    ),

  delete: (projectId: number) =>
    apiClient.delete(`/v1/projects/${projectId}`),

  changeStatus: (projectId: number, status: string) =>
    apiClient.patch<SuccessResponse<ProjectDto>>(
      `/v1/projects/${projectId}/status`,
      null,
      { params: { status } }
    ),
};

// ─────────────────────────────────────────────────────────────
// 개발자 API
// ─────────────────────────────────────────────────────────────
export const developerApi = {
  getAll: (page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<DeveloperDto>>>(
      "/v1/developers",
      { params: { page, size } }
    ),

  getById: (developerId: number) =>
    apiClient.get<SuccessResponse<DeveloperDto>>(
      `/v1/developers/${developerId}`
    ),

  search: (params: {
    skills?: string[];
    minRating?: number;
    page?: number;
    size?: number;
  }) =>
    apiClient.get<SuccessResponse<PageResponse<DeveloperDto>>>(
      "/v1/developers/search",
      { params }
    ),

  updateProfile: (
    data: Partial<{
      title: string;
      skills: string[];
      minHourlyPay: number;
      maxHourlyPay: number;
      responseTime: string;
      availableForWork: boolean;
    }>
  ) =>
    apiClient.put<SuccessResponse<DeveloperDto>>("/v1/developers/profile", data),

  getMyProjects: (page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<ProjectDto>>>(
      "/v1/developers/me/my-projects",
      { params: { page, size } }
    ),
};

// ─────────────────────────────────────────────────────────────
// 클라이언트 API
// ─────────────────────────────────────────────────────────────
export const clientApi = {
  getAll: (page = 1, size = 10) =>
    apiClient.get<SuccessResponse<ClientPageResponse<ClientDto>>>(
      "/v1/clients",
      { params: { page, size } }
    ),

  getById: (clientId: number) =>
    apiClient.get<SuccessResponse<ClientDto>>(`/v1/clients/${clientId}`),

  updateProfile: (
    clientId: number,
    data: { title?: string; participateType?: "INDIVIDUAL" | "COMPANY" }
  ) =>
    apiClient.patch<SuccessResponse<ClientDto>>(
      `/v1/clients/${clientId}`,
      data
    ),

  getMyProjects: (page = 1, size = 10) =>
    apiClient.get<SuccessResponse<ClientPageResponse<ProjectDto>>>(
      "/v1/clients/my-projects",
      { params: { page, size } }
    ),
};

// ─────────────────────────────────────────────────────────────
// 제안서 API
// ─────────────────────────────────────────────────────────────
export const proposalApi = {
  create: (data: CreateProposalRequest) =>
    apiClient.post<SuccessResponse<ProposalDto>>("/v1/proposals", data),

  getByProject: (projectId: number, page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<ProposalDto>>>("/v1/proposals", {
      params: { projectId, page, size },
    }),

  getById: (proposalId: number) =>
    apiClient.get<SuccessResponse<ProposalDto>>(
      `/v1/proposals/${proposalId}`
    ),

  getMyProposals: (page = 0, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<ProposalDto>>>(
      "/v1/proposals/my-proposals",
      { params: { page, size } }
    ),

  updateStatus: (
    proposalId: number,
    status: "ACCEPTED" | "REJECTED" | "WITHDRAWN" | "PENDING"
  ) =>
    apiClient.patch<SuccessResponse<ProposalDto>>(
      `/v1/proposals/${proposalId}`,
      { status }
    ),
};

// ─────────────────────────────────────────────────────────────
// 리뷰 API
// ─────────────────────────────────────────────────────────────
export const reviewApi = {
  create: (
    targetUserId: number,
    data: { projectId: number; rating: number; comment: string }
  ) =>
    apiClient.post<SuccessResponse<ReviewDto>>("/v1/reviews", data, {
      params: { targetUserId },
    }),

  getByDeveloper: (
    developerId: number,
    params?: {
      rating?: number;
      minRating?: number;
      maxRating?: number;
      page?: number;
      size?: number;
    }
  ) =>
    apiClient.get<SuccessResponse<PageResponse<ReviewDto>>>("/v1/reviews", {
      params: { developerId, ...params },
    }),

  getByClient: (
    clientId: number,
    params?: {
      rating?: number;
      minRating?: number;
      maxRating?: number;
      page?: number;
      size?: number;
    }
  ) =>
    apiClient.get<SuccessResponse<PageResponse<ReviewDto>>>("/v1/reviews", {
      params: { clientId, ...params },
    }),

  update: (reviewId: number, data: { rating?: number; comment?: string }) =>
    apiClient.patch<SuccessResponse<ReviewDto>>(
      `/v1/reviews/${reviewId}`,
      data
    ),

  delete: (reviewId: number) => apiClient.delete(`/v1/reviews/${reviewId}`),
};

// ─────────────────────────────────────────────────────────────
// 채팅 API (REST)
// ─────────────────────────────────────────────────────────────
export const chatApi = {
  createRoom: (projectId: number, developerId: number) =>
    apiClient.post<SuccessResponse<ChatRoomDto>>("/v1/chat/rooms", {
      projectId,
      developerId,
    }),

  getMyRooms: (page = 0, size = 20) =>
    apiClient.get<SuccessResponse<PageResponse<ChatRoomDto>>>(
      "/v1/chat/rooms",
      { params: { page, size } }
    ),

  getMessages: (roomId: number, lastMessageId?: number, size = 30) =>
    apiClient.get<SuccessResponse<MessageCursorResponse>>(
      `/v1/chat/rooms/${roomId}/messages`,
      { params: { lastMessageId, size } }
    ),

  updateMessage: (messageId: number, content: string) =>
    apiClient.patch(`/v1/chat/messages/${messageId}`, { content }),

  deleteMessage: (messageId: number) =>
    apiClient.delete(`/v1/chat/messages/${messageId}`),
};

// ─────────────────────────────────────────────────────────────
// 스킬 API
// ─────────────────────────────────────────────────────────────
export const skillApi = {
  getAll: (page = 0, size = 100) =>
    apiClient.get<SuccessResponse<PageResponse<SkillDto>>>("/v1/skills", {
      params: { page, size },
    }),

  search: (name?: string, category?: string, page = 0, size = 20) =>
    apiClient.get<SuccessResponse<PageResponse<SkillDto>>>(
      "/v1/skills/search",
      { params: { name, category, page, size } }
    ),
};

// ─────────────────────────────────────────────────────────────
// 포트폴리오 API (기존 누락 → 추가)
// ─────────────────────────────────────────────────────────────
export const portfolioApi = {
  getByDeveloper: (developerId: number, skill?: string, page = 1, size = 10) =>
    apiClient.get<SuccessResponse<PageResponse<PortfolioDto>>>(
      "/v1/portfolios",
      { params: { developerId, skill, page, size } }
    ),

  create: (data: {
    title: string;
    description: string;
    imageUrl?: string;
    projectUrl?: string;
    skills: string[];
  }) =>
    apiClient.post<SuccessResponse<PortfolioDto>>("/v1/portfolios", data),

  update: (
    developerId: number,
    data: {
      title?: string;
      description?: string;
      imageUrl?: string;
      projectUrl?: string;
      skills?: string[];
    }
  ) =>
    apiClient.patch<SuccessResponse<PortfolioDto>>("/v1/portfolios", data, {
      params: { developerId },
    }),

  delete: (developerId: number) =>
    apiClient.delete("/v1/portfolios", { params: { developerId } }),
};

// ─────────────────────────────────────────────────────────────
// 관리자 API (기존 누락 → 추가)
// ─────────────────────────────────────────────────────────────
export const adminApi = {
  getPendingList: (page = 1, size = 10) =>
    apiClient.get<SuccessResponse<AdminListResponse>>(
      "/v1/admins",
      { params: { status: "PENDING", page, size } }
    ),

  updateStatus: (
    adminId: number,
    adminStatus: "APPROVED" | "REJECTED"
  ) =>
    apiClient.patch<SuccessResponse<AdminDto>>(
      `/v1/admins/${adminId}`,
      { adminStatus }
    ),
};
