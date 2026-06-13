
export type Role = "CUSTOMER" | "AGENT" | "ADMIN";

export interface TokenResponse {
  userId: string;
  email: string;
  role: Role;
  accessToken: string;
  refreshToken: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  createdAt: string;
  updatedAt?: string;
  active?: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ChangeRoleRequest {
  role: Role;
}

export interface ChangeStatusRequest {
  active: boolean;
}

export type PolicyStatus = "PENDING" | "ACTIVE" | "CANCELLED";

export interface Plan {
  id: string;
  name: string;
  description: string;
  coverageAmount: number;
  premiumAmount: number;
  durationMonths: number;
  active: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface PlanRequest {
  name: string;
  description: string;
  coverageAmount: number;
  premiumAmount: number;
  durationMonths: number;
}

export interface Policy {
  id: string;
  policyNumber: string;
  userId: string;
  planId: string;
  planName: string;
  status: PolicyStatus;
  premiumAmount: number;
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
  updatedAt?: string;
}

export interface PurchaseRequest {
  planId: string;
}

export type ClaimType = "HOSPITALIZATION" | "MEDICATION" | "DENTAL" | "CONSULTATION";

export type ClaimStatus = "SUBMITTED" | "UNDER_REVIEW" | "APPROVED" | "REJECTED";

export interface ClaimSubmitRequest {
  policyId: string;
  claimType: ClaimType;
  amount: number;
  description: string;
}

export interface ClaimReviewRequest {
  decision: "APPROVED" | "REJECTED";
  approvedAmount?: number | null;
  rejectionReason?: string | null;
}

export interface Claim {
  id: string;
  claimNumber: string;
  userId: string;
  policyId: string;
  claimType: ClaimType;
  amount: number;
  description: string;
  status: ClaimStatus;
  approvedAmount: number | null;
  rejectionReason: string | null;
  reviewedBy: string | null;
  riskScore: number | null;
  fraudFlags: string[] | null;
  aiExplanation: string | null;
  createdAt: string;
}

export interface Evidence {
  id: string;
  claimId: string;
  fileName: string;
  filePath: string;
  contentType: string;
  uploadedAt: string;
}

export interface ChatRequest {
  sessionId?: string | null;
  message: string;
}

export interface ChatResponse {
  sessionId: string;
  reply: string;
  timestamp: string;
}

export interface ChatMessageDto {
  id: string;
  sessionId: string;
  role: string;
  content: string;
  createdAt: string;
}

export interface FraudAiResponse {
  claimId: string;
  userId: string;
  ruleScore: number;
  aiScore: number | null;
  finalScore: number;
  flags: string[] | null;
  aiExplanation: string | null;
  createdAt: string;
}

export interface RiskAiResponse {
  userId: string;
  totalClaims: number;
  averageRiskScore: number;
  highRiskCount: number;
  lastClaimAt: string | null;
}

export type NotificationType =
  | "WELCOME"
  | "POLICY_ACTIVATED"
  | "POLICY_PENDING"
  | "CLAIM_SUBMITTED"
  | "CLAIM_APPROVED"
  | "CLAIM_REJECTED"
  | "PAYMENT_SUCCESS"
  | "PAYMENT_FAILED"
  | "FRAUD_ALERT";

export type NotificationStatus = "PENDING" | "SENT" | "FAILED";
export type ChannelType = "EMAIL" | "SMS";

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  channel: ChannelType;
  recipient: string;
  subject: string;
  body: string;
  status: NotificationStatus;
  retryCount: number;
  createdAt: string;
  updatedAt?: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}
