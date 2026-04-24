import { apiClient } from "./client";

/**
 * types.ts - 전면 최적화판
 */

export interface SuccessResponse<T> {
  success: boolean;
  status: number;
  message?: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface SearchPageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  pageSize: number;
  pageNumber: number;
}

// ─────────────────────────────────────────────────────────────
// 카테고리 관련 타입
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
  userId: number;
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
// 클라이언트 관련 타입
// ─────────────────────────────────────────────────────────────
export interface ClientDto {
  id: number;
  userId: number;
  name: string;
  title: string;
  completedProject: number;
  rating: number;
  reviewCount?: number;
  participateType: "INDIVIDUAL" | "COMPANY";
  description?: string | null;
}

// ─────────────────────────────────────────────────────────────
// 프로젝트 관련 타입
// ─────────────────────────────────────────────────────────────
export interface ProjectDto {
  id: number;
  clientId: number;
  clientUserId: number; // 추가됨
  title: string;
  description: string;
  category: string;
  minBudget: number;
  maxBudget: number;
  duration: number;
  skills: string[];
  status: string;
  currentProposalCount: number;
  maxProposalCount: number;
  clientName: string;
  clientRating: number;
  createdAt: string;
  updatedAt: string;
}

// ─────────────────────────────────────────────────────────────
// 제안서 관련 타입
// ─────────────────────────────────────────────────────────────
export interface ProposalDto {
  id: number;
  projectId: number;
  projectTitle: string;
  developerId: number;
  developerUserId: number; // 추가됨
  developerName: string;
  coverLetter: string;
  proposedBudget: string;
  proposedDuration: string;
  status: string;
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
  writerRole?: string;
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
// 포트폴리오 타입
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
// 관리자 타입
// ─────────────────────────────────────────────────────────────
export interface AdminDto {
  id: number;
  adminId?: number;
  email: string;
  name: string;
  adminRole: "SUPER_ADMIN" | "CS_ADMIN" | "OPER_ADMIN";
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
}

export interface AdminListResponse {
  admins: AdminDto[];
  pageNumber: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ─────────────────────────────────────────────────────────────
// CS 채팅 관련 타입
// ─────────────────────────────────────────────────────────────
export interface CsChatRoomDto {
  id: number;
  title: string;
  inquirerId: number;
  inquirerName: string;
  status: "WAITING" | "IN_PROGRESS" | "COMPLETED";
  createdAt: string;
  updatedAt: string;
}

export interface CsMessageDto {
  id: number;
  senderId: number;
  senderName: string;
  content: string;
  eventType: "SEND" | "EDIT" | "DELETE" | "ENTER" | "LEAVE";
  isRead: boolean;
  createdAt: string;
}

// ─────────────────────────────────────────────────────────────
// 인증 요청 타입
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

// ─────────────────────────────────────────────────────────────
// 검색 관련 타입
// ─────────────────────────────────────────────────────────────
export interface PopularRankingResponseDto {
  keyword: string;
  score: number;
}

export interface SearchProjectDto {
  id: number;
  title: string;
  description: string;
  category: string;
  minBudget: number;
  maxBudget: number;
  duration: number;
  skills: string[];
  status: string;
  currentProposalCount: number;
  maxProposalCount: number;
  clientName: string;
  clientRating: number;
  createdAt: string;
  updatedAt: string;
}

export interface SearchCategoryDto {
  id: number;
  name: string;
  icon: string | null;
}

export interface SearchSkillDto {
  id: number;
  name: string;
  skillCategory: string;
}

export interface SearchDeveloperDto {
  id: number;
  userId: number;
  name: string;
  title: string;
  rating: number;
  reviewCount: number;
  completedProjects: number;
  skills: string[];
  minHourlyPay: number;
  maxHourlyPay: number;
  responseTime: string;
  description: string;
  availableForWork: boolean;
  participateType: "INDIVIDUAL" | "COMPANY";
  createdAt: string;
  updatedAt: string;
}

export interface TotalSearchResponseDto {
  projects: SearchPageResponse<SearchProjectDto>;
  categories: SearchPageResponse<SearchCategoryDto>;
  skills: SearchPageResponse<SearchSkillDto>;
  developers: SearchPageResponse<SearchDeveloperDto>;
}

export interface DeveloperRegisterRequest {
  email: string;
  password: string;
  name: string;
  phoneNumber: string;
  description?: string;
  title: string;
  minHourlyPay: number;
  maxHourlyPay: number;
  skills: string[];
  responseTime: string;
  availableForWork: boolean;
  participateType: "INDIVIDUAL" | "COMPANY";
}

export interface ClientPageResponse<T> {
  content: T[];
  pageNumber: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
