// In-memory store that backs the mock adapter. Lives only in the dev
// session — refreshing the page resets everything except the current
// auth token (because we keep tokens in localStorage like the real client).

import {
  Claim,
  Evidence,
  Notification,
  Plan,
  Policy,
  Role,
  UserProfile,
} from "../../types";

const now = () => new Date().toISOString();

// ─── Helpers ─────────────────────────────────────────────────────────────────

export function uuid(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

// ─── Seed users ──────────────────────────────────────────────────────────────

export const users: UserProfile[] = [
  {
    id: "00000000-0000-0000-0000-000000000001",
    email: "alex.morgan@test.com",
    firstName: "Alex",
    lastName: "Morgan",
    phone: "+1 555 0100",
    role: "MEMBER",
    status: "ACTIVE",
    createdAt: "2024-11-01T09:00:00Z",
    lastLoginAt: "2025-06-05T08:30:00Z",
  },
  {
    id: "00000000-0000-0000-0000-000000000002",
    email: "jordan.kim@test.com",
    firstName: "Jordan",
    lastName: "Kim",
    phone: "+1 555 0200",
    role: "STAFF",
    status: "ACTIVE",
    createdAt: "2024-09-15T10:00:00Z",
    lastLoginAt: "2026-06-06T07:45:00Z",
  },
  {
    id: "00000000-0000-0000-0000-000000000003",
    email: "sam.patel@test.com",
    firstName: "Sam",
    lastName: "Patel",
    phone: "+1 555 0300",
    role: "MEMBER",
    status: "ACTIVE",
    createdAt: "2025-01-20T14:00:00Z",
    lastLoginAt: "2025-06-04T11:20:00Z",
  },
];

// Map of token → userId
export const sessions = new Map<string, string>();

export function makeToken(userId: string, kind: "access" | "refresh"): string {
  const tok = `mock_${kind}_${userId}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  sessions.set(tok, userId);
  return tok;
}

export function userFromToken(token: string): UserProfile | null {
  const id = sessions.get(token);
  if (!id) return null;
  return users.find((u) => u.id === id) || null;
}

export function roleForEmail(email: string): Role {
  const e = email.toLowerCase();
  if (e.startsWith("admin")) return "ADMIN";
  if (e.startsWith("staff")) return "STAFF";
  return "MEMBER";
}

// ─── Plans ───────────────────────────────────────────────────────────────────

export const plans: Plan[] = [
  {
    id: "plan-essential",
    name: "Essential Care",
    description: "Basic coverage for essential medical procedures.",
    monthlyPremium: 89,
    coveragePercent: 0.7,
    annualLimit: 15000,
    deductible: 800,
    coveredProcedures: ["GP01", "ER01", "LAB01"],
    status: "ACTIVE",
    createdAt: "2024-06-01T00:00:00Z",
  },
  {
    id: "plan-standard",
    name: "Standard Plus",
    description: "Comprehensive coverage for most medical needs.",
    monthlyPremium: 149,
    coveragePercent: 0.8,
    annualLimit: 30000,
    deductible: 500,
    coveredProcedures: ["GP01", "ER01", "LAB01", "SUR01", "IMG01"],
    status: "ACTIVE",
    createdAt: "2024-06-01T00:00:00Z",
  },
  {
    id: "plan-premium",
    name: "Premium Shield",
    description: "Premium coverage with the widest procedure network.",
    monthlyPremium: 229,
    coveragePercent: 0.9,
    annualLimit: 60000,
    deductible: 250,
    coveredProcedures: [
      "GP01",
      "ER01",
      "LAB01",
      "SUR01",
      "IMG01",
      "PHY01",
      "MEN01",
      "DEN01",
    ],
    status: "ACTIVE",
    createdAt: "2024-06-01T00:00:00Z",
  },
  {
    id: "plan-senior",
    name: "Senior Flex",
    description: "Tailored for members 60+ with enhanced chronic care benefits.",
    monthlyPremium: 189,
    coveragePercent: 0.85,
    annualLimit: 45000,
    deductible: 350,
    coveredProcedures: ["GP01", "ER01", "LAB01", "SUR01", "IMG01", "PHY01"],
    status: "ACTIVE",
    createdAt: "2024-06-01T00:00:00Z",
  },
];

// ─── Policies (member → policy) ──────────────────────────────────────────────

export const policies: Policy[] = [
  {
    id: "pol-alex",
    memberId: "00000000-0000-0000-0000-000000000001",
    planId: "plan-standard",
    startDate: "2024-12-01",
    endDate: "2025-12-01",
    status: "ACTIVE",
    deductiblePaid: 320,
    claimsTotal: 5680,
    createdAt: "2024-12-01T00:00:00Z",
  },
  {
    id: "pol-sam",
    memberId: "00000000-0000-0000-0000-000000000003",
    planId: "plan-premium",
    startDate: "2025-02-01",
    endDate: "2026-02-01",
    status: "ACTIVE",
    deductiblePaid: 250,
    claimsTotal: 12400,
    createdAt: "2025-02-01T00:00:00Z",
  },
];

// ─── Claims ──────────────────────────────────────────────────────────────────

export const claims: Claim[] = [
  {
    id: "clm-001",
    memberId: "00000000-0000-0000-0000-000000000001",
    policyId: "pol-alex",
    amount: 1200,
    procedureCode: "SUR01",
    diagnosisCode: "K35",
    providerName: "City Medical Center",
    serviceDate: "2025-05-12",
    status: "APPROVED",
    decisionReason:
      "Covered procedure under active policy. Fraud score 0.05 (low).",
    approvedAmount: 960,
    submittedAt: "2025-05-14T10:23:00Z",
    decidedAt: "2025-05-14T10:24:00Z",
    fraudScore: 0.05,
  },
  {
    id: "clm-002",
    memberId: "00000000-0000-0000-0000-000000000001",
    policyId: "pol-alex",
    amount: 350,
    procedureCode: "LAB01",
    diagnosisCode: "Z00",
    providerName: "QuickLab Diagnostics",
    serviceDate: "2025-06-01",
    status: "PENDING",
    submittedAt: "2025-06-03T08:11:00Z",
    fraudScore: 0.12,
  },
  {
    id: "clm-003",
    memberId: "00000000-0000-0000-0000-000000000001",
    policyId: "pol-alex",
    amount: 5400,
    procedureCode: "IMG01",
    diagnosisCode: "M54",
    providerName: "Radiology Partners",
    serviceDate: "2025-04-20",
    status: "MANUAL_REVIEW",
    decisionReason:
      "Elevated risk. Amount exceeds the 95th percentile for this procedure.",
    submittedAt: "2025-04-22T14:55:00Z",
    fraudScore: 0.72,
  },
  {
    id: "clm-004",
    memberId: "00000000-0000-0000-0000-000000000001",
    policyId: "pol-alex",
    amount: 200,
    procedureCode: "GP01",
    diagnosisCode: "J06",
    providerName: "HealthFirst Clinic",
    serviceDate: "2025-03-10",
    status: "REJECTED",
    decisionReason: "Procedure not covered under current plan.",
    approvedAmount: 0,
    submittedAt: "2025-03-11T09:30:00Z",
    decidedAt: "2025-03-11T09:31:00Z",
    fraudScore: 0.08,
  },
  {
    id: "clm-010",
    memberId: "00000000-0000-0000-0000-000000000003",
    policyId: "pol-sam",
    amount: 8900,
    procedureCode: "SUR01",
    diagnosisCode: "C18",
    providerName: "Metro Surgery Center",
    serviceDate: "2025-05-28",
    status: "MANUAL_REVIEW",
    decisionReason: "Duplicate claim pattern detected. High amount flagged.",
    submittedAt: "2025-05-30T11:00:00Z",
    fraudScore: 0.81,
  },
  {
    id: "clm-011",
    memberId: "00000000-0000-0000-0000-000000000003",
    policyId: "pol-sam",
    amount: 3200,
    procedureCode: "MEN01",
    diagnosisCode: "F32",
    providerName: "Wellness Psychiatric",
    serviceDate: "2025-06-02",
    status: "MANUAL_REVIEW",
    decisionReason: "Unusual frequency for procedure code.",
    submittedAt: "2025-06-04T14:00:00Z",
    fraudScore: 0.64,
  },
];

// ─── Evidence ────────────────────────────────────────────────────────────────

export const evidence: Evidence[] = [
  {
    id: "ev-1",
    claimId: "clm-001",
    filePath: "/uploads/bill-001.pdf",
    originalFileName: "city-medical-bill.pdf",
    contentType: "application/pdf",
    fileType: "BILL",
    fileSize: 124_500,
    uploadedAt: "2025-05-14T10:21:00Z",
  },
  {
    id: "ev-2",
    claimId: "clm-001",
    filePath: "/uploads/rx-001.jpg",
    originalFileName: "prescription.jpg",
    contentType: "image/jpeg",
    fileType: "PRESCRIPTION",
    fileSize: 380_200,
    uploadedAt: "2025-05-14T10:22:00Z",
  },
  {
    id: "ev-3",
    claimId: "clm-003",
    filePath: "/uploads/mri-report.pdf",
    originalFileName: "mri-report.pdf",
    contentType: "application/pdf",
    fileType: "REPORT",
    fileSize: 980_100,
    uploadedAt: "2025-04-22T14:50:00Z",
  },
];

// ─── Notifications ───────────────────────────────────────────────────────────

export const notifications: Notification[] = [
  {
    id: uuid(),
    recipientId: "00000000-0000-0000-0000-000000000001",
    recipientEmail: "alex.morgan@test.com",
    channel: "EMAIL",
    template: "claim_decision",
    subject: "Your claim CLM-001 has been approved",
    body: "Good news — your claim for SUR01 at City Medical Center has been approved. We'll reimburse $960.00 within 5–7 business days.",
    status: "SENT",
    sentAt: "2025-05-14T10:25:00Z",
    createdAt: "2025-05-14T10:24:30Z",
  },
  {
    id: uuid(),
    recipientId: "00000000-0000-0000-0000-000000000001",
    recipientEmail: "alex.morgan@test.com",
    channel: "EMAIL",
    template: "claim_submitted",
    subject: "We received your claim CLM-002",
    body: "We've received your claim for LAB01 and are processing it now.",
    status: "SENT",
    sentAt: "2025-06-03T08:12:00Z",
    createdAt: "2025-06-03T08:11:30Z",
  },
  {
    id: uuid(),
    recipientId: "00000000-0000-0000-0000-000000000001",
    recipientEmail: "alex.morgan@test.com",
    channel: "EMAIL",
    template: "claim_flagged",
    subject: "Your claim CLM-003 is under review",
    body: "Your claim is being reviewed by our team. We'll get back to you within 48 hours.",
    status: "SENT",
    sentAt: "2025-04-22T15:00:00Z",
    createdAt: "2025-04-22T14:56:00Z",
  },
];

// ─── AI chat ─────────────────────────────────────────────────────────────────

export const chatReplies = [
  "Your Standard Plus plan covers GP visits, emergency care, lab tests, surgeries, and imaging at 80% after a $500 deductible. Once you hit your $30,000 annual limit, you'd pay any extra out of pocket.",
  "Looking at your recent claims, you've used about $5,680 of your $30,000 annual reimbursement so far this year. You still have plenty of headroom.",
  "I can't see that claim in your history yet — newly submitted claims appear here within a few minutes. Want me to check again in a moment?",
  "For a $5,400 imaging procedure, your plan would normally reimburse 80% of the covered amount, but since this one was flagged for manual review the final figure depends on what our staff decide.",
  "Sure! Generally, prescriptions are covered if your plan includes the PHY01 procedure code. Standard Plus doesn't include it — you'd want Premium Shield or higher for prescription coverage.",
];

let _chatIdx = 0;
export function nextChatReply(): string {
  const reply = chatReplies[_chatIdx % chatReplies.length];
  _chatIdx++;
  return reply;
}

// ─── Reset helper (for tests / dev tools) ────────────────────────────────────

export function _resetMocks() {
  sessions.clear();
}

export { now };
