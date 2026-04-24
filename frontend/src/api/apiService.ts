/**
 * apiService.ts - 전면 최적화판
 */
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
    CsChatRoomDto,
    PopularRankingResponseDto,
    TotalSearchResponseDto,
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

    create: (data: { name: string; icon?: string; description?: string; displayOrder: number }) =>
        apiClient.post<SuccessResponse<CategoryDto>>("/v1/categories", data),

    update: (categoryId: number, data: { name?: string; icon?: string; description?: string; displayOrder?: number }) =>
        apiClient.patch<SuccessResponse<CategoryDto>>(`/v1/categories/${categoryId}`, data),

    delete: (categoryId: number) =>
        apiClient.delete(`/v1/categories/${categoryId}`),
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
        keyword?: string;
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
            { status }
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
    // 백엔드 ClientController가 keyword를 받지 않으므로 page, size만 유지
    getAll: (page = 0, size = 10) =>
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

    getMyProjects: (page = 0, size = 10) =>
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

    deleteRoom: (roomId: number) =>
        apiClient.delete(`/v1/chat/rooms/${roomId}`),

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

    create: (data: { name: string; category: string }) =>
        apiClient.post<SuccessResponse<SkillDto>>("/v1/skills", data),

    update: (skillId: number, data: { name?: string; category?: string }) =>
        apiClient.patch<SuccessResponse<SkillDto>>(`/v1/skills/${skillId}`, data),

    delete: (skillId: number) =>
        apiClient.delete(`/v1/skills/${skillId}`),
};

// ─────────────────────────────────────────────────────────────
// 포트폴리오 API
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
        portfolioId: number,
        data: {
            title?: string;
            description?: string;
            imageUrl?: string;
            projectUrl?: string;
            skills?: string[];
        }
    ) =>
        apiClient.patch<SuccessResponse<PortfolioDto>>(
            `/v1/portfolios/${portfolioId}`,
            data
        ),

    delete: (portfolioId: number) =>
        apiClient.delete(`/v1/portfolios/${portfolioId}`),
};

// ─────────────────────────────────────────────────────────────
// 관리자 API
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

// ─────────────────────────────────────────────────────────────
// 검색 API
// ─────────────────────────────────────────────────────────────
export const searchApi = {
    getTotalSearch: (keyword?: string, page = 0, size = 5) =>
        apiClient.get<SuccessResponse<TotalSearchResponseDto>>("/v2/search", {
            params: { keyword, page, size },
        }),

    getPopularRanking: (limit = 10) =>
        apiClient.get<SuccessResponse<PopularRankingResponseDto[]>>("/v1/search/popular", {
            params: { limit },
        }),
};

// ─────────────────────────────────────────────────────────────
// CS API
// ─────────────────────────────────────────────────────────────
export const csApi = {
    createRoom: (data: { title: string }) =>
        apiClient.post<SuccessResponse<CsChatRoomDto>>("/v1/cs/rooms", data),

    getMyRooms: (page = 1, size = 10) =>
        apiClient.get<SuccessResponse<PageResponse<CsChatRoomDto>>>("/v1/cs/rooms", {
            params: { page, size },
        }),

    getAllRooms: (params: { status?: string; page?: number; size?: number }) =>
        apiClient.get<SuccessResponse<PageResponse<CsChatRoomDto>>>("/v1/admin/cs/rooms", {
            params,
        }),

    updateStatus: (roomId: number, status: string) =>
        apiClient.patch<SuccessResponse<CsChatRoomDto>>(`/v1/admin/cs/rooms/${roomId}/status`, null, {
            params: { status },
        }),

    updateMessage: (messageId: number, content: string) =>
        apiClient.patch<SuccessResponse<CsMessageDto>>(`/v1/cs/messages/${messageId}`, { content }),

    deleteMessage: (messageId: number) =>
        apiClient.delete(`/v1/cs/messages/${messageId}`),
};
