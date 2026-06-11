// ─── Shared domain types ─────────────────────────────────────────────────────
// Field names match the backend DTOs and entities exactly so the API client
// can pass payloads through without remapping.

export type Role = "MEMBER" | "STAFF" | "ADMIN";
export type UserStatus = "ACTIVE" | "DISABLED";

// ─── IAM ─────────────────────────────────────────────────────────────────────
// Auth endpoints (register/login/refresh) return just the token pair.
// User identity and role live inside the signed JWT (and on `GET /users/me`).
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

// Legacy alias — kept so existing imports don't break. Prefer TokenResponse.
export type AuthResponse = TokenResponse;

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  role: Role;
  status: UserStatus;
  createdAt: string;
  lastLoginAt?: string | null;
}

// ─── Policy ──────────────────────────────────────────────────────────────────
export type PlanStatus = "ACTIVE" | "RETIRED";
export type PolicyStatus =
  | "PENDING_PAYMENT"
  | "PAYMENT_FAILED"
  | "SCHEDULED"
  | "ACTIVE"
  | "GRACE_PERIOD"
  | "EXPIRED"
  | "CANCELLED";
export type PolicyPaymentStatus = "PENDING" | "PAID" | "FAILED";

export interface Plan {
  id: string;
  name: string;
  description?: string;
  monthlyPremium: number;
  coveragePercent: number;
  annualLimit: number;
  deductible: number;
  // Lifecycle fields returned by the policy service PlanResponse.
  durationMonths?: number;
  gracePeriodDays?: number;
  waitingPeriodDays?: number;
  effectiveFrom?: string;
  effectiveTo?: string | null;
  coveredProcedures: string[];
  status: PlanStatus;
  createdAt?: string;
}

// Matches com.eHealthInsurance.dto.request.CreatePlanRequest. The policy
// service requires description, durationMonths, gracePeriodDays,
// waitingPeriodDays and effectiveFrom (all @NotNull), so they are required here.
export interface PlanRequest {
  name: string;
  description: string;
  monthlyPremium: number;
  coveragePercent: number;
  annualLimit: number;
  deductible: number;
  durationMonths: number;
  gracePeriodDays: number;
  waitingPeriodDays: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  coveredProcedures: string[];
}

export interface Policy {
  id: string;
  memberId: string;
  planId: string;
  startDate: string;
  endDate: string;
  status: PolicyStatus;
  paymentStatus?: PolicyPaymentStatus;
  deductiblePaid: number;
  claimsTotal: number;
  createdAt?: string;
  plan?: Plan;
}

export interface PurchaseRequest {
  planId: string;
  startDate: string;
}

export interface CoverageResponse {
  covered: boolean;
  coveragePercent: number;
  reimbursementAmount: number;
  reason?: string;
}

// ─── Claim ───────────────────────────────────────────────────────────────────
export type ClaimStatus =
  | "PENDING"
  | "AWAITING_EVIDENCE"
  | "APPROVED"
  | "REJECTED"
  | "MANUAL_REVIEW";
export type EvidenceFileType = "BILL" | "PRESCRIPTION" | "REPORT" | "OTHER";

export interface ClaimSubmitRequest {
  policyId: string;
  amount: number;
  procedureCode: string;
  diagnosisCode?: string;
  providerName: string;
  serviceDate: string;
}

export interface Claim {
  id: string;
  memberId: string;
  policyId: string;
  amount: number;
  procedureCode: string;
  diagnosisCode?: string;
  providerName: string;
  serviceDate: string;
  status: ClaimStatus;
  decisionReason?: string;
  approvedAmount?: number | null;
  submittedAt: string;
  decidedAt?: string | null;
  fraudScore?: number | null;
}

export interface Evidence {
  id: string;
  claimId: string;
  filePath: string;
  originalFileName: string;
  contentType?: string;
  fileType: EvidenceFileType;
  fileSize: number;
  uploadedAt: string;
}

export interface ClaimReviewRequest {
  decision: "APPROVED" | "REJECTED";
  approvedAmount?: number;
  reason: string;
}

// ─── AI ──────────────────────────────────────────────────────────────────────
// Mirrors the AI service enum com.eHealthInsurance.entity.enums.RecommendedAction.
export type RecommendedAction = "APPROVE" | "REJECT" | "HUMAN_REVIEW";
export type ChatRole = "USER" | "ASSISTANT";

export interface ClaimAnalysis {
  id: string;
  claimId: string;
  fraudScore: number;
  recommendedAction: RecommendedAction;
  flags: string[];
  explanationText?: string;
  createdAt: string;
}

export interface ChatMessage {
  id?: string;
  conversationId?: string;
  role: ChatRole;
  content: string;
  createdAt?: string;
}

export interface ChatConversation {
  id: string;
  memberId: string;
  status: "ACTIVE" | "CLOSED";
  lastMessageAt?: string;
}

export interface PlanRecommendation {
  planId: string;
  planName: string;
  reason: string;
  score?: number;
}

export interface RecommendInputs {
  age?: number;
  budget?: number;
  conditions?: string[];
  family?: number;
  notes?: string;
}

// ─── Notification ────────────────────────────────────────────────────────────
export type NotificationStatus = "PENDING" | "SENT" | "FAILED";
export type ChannelType = "EMAIL" | "SMS";

export interface Notification {
  id: string;
  recipientId: string;
  recipientEmail: string;
  channel: ChannelType;
  template: string;
  subject: string;
  body?: string;
  status: NotificationStatus;
  sentAt?: string | null;
  errorMessage?: string | null;
  createdAt: string;
}

// ─── Payment ─────────────────────────────────────────────────────────────────
// Mirrors com.eHealthInsurance (payment service) DTOs and enums.
export type PaymentProvider = "MOCK" | "STRIPE" | "MANUAL" | "BANK_TRANSFER";
export type PaymentStatus =
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"
  | "REFUNDED";
export type RefundStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
export type InvoiceStatus = "PENDING" | "PAID" | "OVERDUE" | "CANCELLED";

export interface Payment {
  id: string;
  policyId: string;
  memberId: string;
  amount: number;
  currency: string;
  provider: string;
  status: PaymentStatus | string;
  providerReference?: string | null;
  failureCode?: string | null;
  failureReason?: string | null;
  paidAt?: string | null;
  failedAt?: string | null;
  createdAt: string;
}

export interface CreatePaymentRequest {
  policyId: string;
  amount: number;
  provider: PaymentProvider;
  idempotencyKey?: string;
  paymentMethodToken?: string;
}

export interface Invoice {
  id: string;
  policyId: string;
  amount: number;
  dueDate: string;
  status: InvoiceStatus | string;
  invoiceNumber: string;
  createdAt: string;
}

export interface Refund {
  id: string;
  paymentId: string;
  amount: number;
  reason?: string | null;
  providerReference?: string | null;
  status: RefundStatus | string;
  requestedAt: string;
  processedAt?: string | null;
}

export interface CreateRefundRequest {
  paymentId: string;
  amount: number;
  reason?: string;
}

// ─── Health record ───────────────────────────────────────────────────────────
// Mirrors com.eHealthInsurance (health-record service) DTOs and enums.
export type EntryType =
  | "DIAGNOSIS"
  | "PROCEDURE"
  | "VISIT"
  | "SURGERY"
  | "HOSPITALIZATION"
  | "IMAGING"
  | "LAB_ORDER";
export type RecordStatus = "ACTIVE" | "ARCHIVED" | "DELETED";

export interface MedicalEntry {
  id: string;
  entryType: EntryType;
  diagnosisCode?: string | null;
  description?: string | null;
  providerName?: string | null;
  entryDate: string;
  createdAt: string;
}

export interface Prescription {
  id: string;
  medicationName: string;
  dosage?: string | null;
  frequency?: string | null;
  prescribedDate: string;
  endDate?: string | null;
  status?: string | null;
  createdAt: string;
}

export interface LabResult {
  id: string;
  testName: string;
  testDate: string;
  resultValue?: string | null;
  referenceRange?: string | null;
  unit?: string | null;
  status?: string | null;
  createdAt: string;
}

export interface HealthSummary {
  recordId: string;
  memberId: string;
  status: RecordStatus | string;
  createdAt: string;
  totalEntries: number;
  totalPrescriptions: number;
  totalLabResults: number;
  recentEntries: MedicalEntry[];
  activePrescriptions: Prescription[];
  recentLabResults: LabResult[];
}

export interface AddMedicalEntryRequest {
  entryType: EntryType;
  diagnosisCode?: string;
  description?: string;
  providerName?: string;
  entryDate: string;
}

export interface AddPrescriptionRequest {
  medicationName: string;
  dosage?: string;
  frequency?: string;
  prescribedDate: string;
  endDate?: string;
  status?: string;
}

export interface AddLabResultRequest {
  testName: string;
  testDate: string;
  resultValue?: string;
  referenceRange?: string;
  unit?: string;
  status?: string;
}
