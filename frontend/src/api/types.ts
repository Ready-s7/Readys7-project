/**
 * 백엔드 공통 응답 타입
 * 백엔드 ApiResponseDto<T> 구조와 1:1 매핑
 */

// ─────────────────────────────────────────────────────────────
// 공통 응답 래퍼
// ─────────────────────────────────────────────────────────────
export interface SuccessResponse<T> {
  success: boolean;
  status: number;
  data: T;
}

export interface ErrorResponse {
  code: string;
  message: string;
  data: null | Record<string, string>;
}

// 페이지 응답 (Spring Page<T>)
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

// 클라이언트 전용 페이지 응답 (PageResponseDto)
export interface ClientPageResponse<T> {
  content: T[];
  currentPage: number;
  size: number;
  totalCount: number;
  totalPage: number;
}

// ─────────────────────────────────────────────────────────────
// 인증 관련 타입
// ─────────────────────────────────────────────────────────────
export interface LoginResponse {
  refreshToken: string;
  email: string;
}

export interface UserDto {
  id: number;
  email: string;
  name: string;
  role: "CLIENT" | "DEVELOPER" | "ADMIN";
  createdAt: string;
}

export interface GetUserInfoResponse {
  id: number;
  email: string;
  userRole: "CLIENT" | "DEVELOPER" | "ADMIN";
  description: string | null;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────
// 프로젝트 관련 타입
// ─────────────────────────────────────────────────────────────
export interface ProjectDto {
  id: number;
  title: string;
  description: string;
  category: string;
  minBudget: number;
  maxBudget: number;
  duration: number;
  skills: string[];
  status: "OPEN" | "CLOSED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  currentProposalCount: number;
  maxProposalCount: number;
  clientName: string;
  clientRating: number;
  createdAt: string;
  updatedAt: string;
}

// ─────────────────────────────────────────────────────────────
// 카테고리 타입
// ─────────────────────────────────────────────────────────────
export interface CategoryDto {
  id: number;
  adminId: number;
  name: string;
  icon: string | null;
  description: string | null;
  displayOrder: number;
}

// ─────────────────────────────────────────────────────────────
// 개발자 관련 타입
// ─────────────────────────────────────────────────────────────
export interface DeveloperDto {
  id: number;
  name: string;
  title: string;
  rating: number;
  reviewCount: number;
  completedProjects: number;
  skills: string[];
  minHourlyPay: number | null;
  maxHourlyPay: number | null;
  responseTime: string | null;
  description: string | null;
  availableForWork: boolean;
  participateType: "INDIVIDUAL" | "COMPANY";
  createdAt: string;
  updatedAt: string;
}

// ─────────────────────────────────────────────────────────────
// 클라이언트 관련 타입 (기존 누락 → 추가)
// ─────────────────────────────────────────────────────────────
export interface ClientDto {
  id: number;
  name: string;
  title: string;
  completedProject: number;
  rating: number;
  reviewCount?: number;
  participateType: "INDIVIDUAL" | "COMPANY";
  description?: string | null;
}

// ─────────────────────────────────────────────────────────────
// 제안서 관련 타입
// ─────────────────────────────────────────────────────────────
export interface ProposalDto {
  id: number;
  projectId: number;
  projectTitle: string;
  developerId: number;
  developerName: string;
  coverLetter: string;
  proposedBudget: string;
  proposedDuration: string;
  status: "pending" | "accepted" | "rejected" | "withdrawn";
  createdAt: string;
  updatedAt: string;
}

export interface CreateProposalRequest {
  projectId: number;
  coverLetter: string;
  proposedBudget: string;
  proposedDuration: string;
}

// ─────────────────────────────────────────────────────────────
// 리뷰 관련 타입
// ─────────────────────────────────────────────────────────────
export interface ReviewDto {
  id: number;
  developerId: number;
  developerName: string;
  clientId: number;
  clientName: string;
  projectId: number;
  projectTitle: string;
  rating: number;
  comment: string;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────
// 채팅 관련 타입
// ─────────────────────────────────────────────────────────────
export interface ChatRoomDto {
  id: number;
  projectId: number;
  projectTitle: string;
  clientId: number;
  clientName: string;
  developerId: number;
  developerName: string;
  unreadCount: number;
  createdAt: string;
}

export interface MessageResponseDto {
  id: number;
  chatRoomId: number;
  senderId: number;
  senderName: string;
  content: string;
  eventType: "SEND" | "EDIT" | "DELETE" | "ENTER" | "LEAVE";
  isRead: boolean;
  isSystem: boolean;
  sentAt: string;
}

export interface MessageCursorResponse {
  messages: MessageResponseDto[];
  hasNext: boolean;
  nextCursor: number | null;
}

// ─────────────────────────────────────────────────────────────
// 스킬 관련 타입
// ─────────────────────────────────────────────────────────────
export interface SkillDto {
  id: number;
  adminId: number;
  adminName: string;
  name: string;
  category: string;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────
// 포트폴리오 타입 (기존 누락 → 추가)
// ─────────────────────────────────────────────────────────────
export interface PortfolioDto {
  id: number;
  developerId: number;
  title: string;
  description: string;
  imageUrl: string | null;
  projectUrl: string | null;
  skills: string[];
  createdAt: string;
  updatedAt: string;
}

// ─────────────────────────────────────────────────────────────
// 관리자 타입 (기존 누락 → 추가)
// ─────────────────────────────────────────────────────────────
export interface AdminDto {
  id: number;
  email: string;
  name: string;
  adminRole: "SUPER_ADMIN" | "CS_ADMIN" | "OPER_ADMIN";
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
}

export interface AdminListResponse {
  admins: AdminDto[];
  currentPage: number;
  size: number;
  totalCount: number;
  totalPage: number;
}

// ─────────────────────────────────────────────────────────────
// 로그인 요청 타입
// ─────────────────────────────────────────────────────────────
export interface LoginRequest {
  email: string;
  password: string;
}

export interface ClientRegisterRequest {
  email: string;
  password: string;
  name: string;
  phoneNumber: string;
  description?: string;
  title: string;
  participateType: "INDIVIDUAL" | "COMPANY";
}

export interface DeveloperRegisterRequest {
  email: string;
  password: string;
  name: string;
  phoneNumber: string;
  title: string;
  minHourlyPay: number;
  maxHourlyPay: number;
  skills: string[];
  responseTime: string;
  availableForWork: boolean;
  participateType: "INDIVIDUAL" | "COMPANY";
}
