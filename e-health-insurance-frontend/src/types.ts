// Domen tipləri — backend DTO-ları ilə birəbir uyğundur.

// Backend UserRole enum: CUSTOMER, AGENT, ADMIN
export type Role = "CUSTOMER" | "AGENT" | "ADMIN";

// ─── IAM ─────────────────────────────────────────────────────────────────────
// Backend AuthResponse: userId, email, role, accessToken, refreshToken
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

// Backend UpdateUserRequest has only firstName and lastName
export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
}

// Backend UserDto: id, email, firstName, lastName, role, createdAt
export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  createdAt: string;
  updatedAt?: string;
}

// ─── Policy ──────────────────────────────────────────────────────────────────
// Backend PolicyStatus infra enum: PENDING, ACTIVE, CANCELLED
export type PolicyStatus = "PENDING" | "ACTIVE" | "CANCELLED";

// Backend PlanDto: id, name, description, coverageAmount, premiumAmount, durationMonths, active, createdAt, updatedAt
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

// Backend CreatePlanRequest: name, description, coverageAmount, premiumAmount, durationMonths
export interface PlanRequest {
  name: string;
  description: string;
  coverageAmount: number;
  premiumAmount: number;
  durationMonths: number;
}

// Backend PolicyDto: id, policyNumber, userId, planId, planName, status, premiumAmount, startDate, endDate, createdAt, updatedAt
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

// Backend PurchasePolicyRequest: planId only
export interface PurchaseRequest {
  planId: string;
}

// ─── Claim ───────────────────────────────────────────────────────────────────
// Backend ClaimType infra enum
export type ClaimType = "HOSPITALIZATION" | "MEDICATION" | "DENTAL" | "CONSULTATION";

// Backend ClaimStatus infra enum
export type ClaimStatus = "SUBMITTED" | "UNDER_REVIEW" | "APPROVED" | "REJECTED";

// Backend SubmitClaimRequest: policyId, claimType, amount, description
export interface ClaimSubmitRequest {
  policyId: string;
  claimType: ClaimType;
  amount: number;
  description: string;
}

// Backend ReviewClaimRequest: decision (ClaimStatus), approvedAmount, rejectionReason
export interface ClaimReviewRequest {
  decision: "APPROVED" | "REJECTED";
  approvedAmount?: number | null;
  rejectionReason?: string | null;
}

// Backend ClaimDto (15 fields)
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

// Backend ClaimEvidenceDto: id, claimId, fileName, filePath, contentType, uploadedAt
export interface Evidence {
  id: string;
  claimId: string;
  fileName: string;
  filePath: string;
  contentType: string;
  uploadedAt: string;
}

// ─── AI ──────────────────────────────────────────────────────────────────────
// Backend ChatRequest: sessionId (nullable UUID), message
export interface ChatRequest {
  sessionId?: string | null;
  message: string;
}

// Backend ChatResponse: sessionId, reply, timestamp
export interface ChatResponse {
  sessionId: string;
  reply: string;
  timestamp: string;
}

// Backend ChatMessageDto: id, sessionId, role, content, createdAt
export interface ChatMessageDto {
  id: string;
  sessionId: string;
  role: string;
  content: string;
  createdAt: string;
}

// Backend FraudAiResponse: claimId, userId, ruleScore, aiScore, finalScore, flags, aiExplanation, createdAt
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

// Backend RiskAiResponse: userId, totalClaims, averageRiskScore, highRiskCount, lastClaimAt
export interface RiskAiResponse {
  userId: string;
  totalClaims: number;
  averageRiskScore: number;
  highRiskCount: number;
  lastClaimAt: string | null;
}

// ─── Notification ────────────────────────────────────────────────────────────
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

// Backend Notification entity fields
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

// ─── Ümumi ───────────────────────────────────────────────────────────────────
export interface SpringPage<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}
