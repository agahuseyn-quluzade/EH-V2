import { api } from "./client";
import {
  Claim,
  ClaimReviewRequest,
  ClaimStatus,
  ClaimSubmitRequest,
  Evidence,
  EvidenceFileType,
} from "../types";

// Gateway strips /api/claim and prepends /api.
// Upstream paths: /api/claims/**
const ROOT = "/api/claim/claims";

// ── Backend wire shapes ──────────────────────────────────────────────────────
// ClaimController returns ClaimResponse (detail/submit/review) with a nested
// `decision` object, and ClaimSummaryResponse (list/queue) which is a subset.
// The UI works with a single flat `Claim`, so we adapt both shapes here.
interface ClaimDecisionResponse {
  decisionType: "APPROVED" | "REJECTED" | "MANUAL_REVIEW";
  decisionSource: "AUTO" | "STAFF";
  reason?: string | null;
  approvedAmount?: number | null;
  fraudScore?: number | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
}

interface ClaimResponse {
  id: string;
  memberId: string;
  policyId: string;
  amount: number;
  procedureCode: string;
  diagnosisCode?: string | null;
  providerName: string;
  serviceDate: string;
  status: ClaimStatus;
  submittedAt: string;
  decision?: ClaimDecisionResponse | null;
}

interface ClaimSummaryResponse {
  id: string;
  policyId: string;
  amount: number;
  procedureCode: string;
  status: ClaimStatus;
  submittedAt: string;
}

function fromDetail(r: ClaimResponse): Claim {
  const d = r.decision ?? null;
  return {
    id: r.id,
    memberId: r.memberId,
    policyId: r.policyId,
    amount: r.amount,
    procedureCode: r.procedureCode,
    diagnosisCode: r.diagnosisCode ?? undefined,
    providerName: r.providerName,
    serviceDate: r.serviceDate,
    status: r.status,
    submittedAt: r.submittedAt,
    decisionReason: d?.reason ?? undefined,
    approvedAmount: d?.approvedAmount ?? null,
    decidedAt: d?.decidedAt ?? null,
    fraudScore: d?.fraudScore ?? null,
  };
}

function fromSummary(r: ClaimSummaryResponse): Claim {
  return {
    id: r.id,
    memberId: "",
    policyId: r.policyId,
    amount: r.amount,
    procedureCode: r.procedureCode,
    providerName: "",
    serviceDate: "",
    status: r.status,
    submittedAt: r.submittedAt,
    approvedAmount: null,
    decidedAt: null,
    fraudScore: null,
  };
}

export const claimApi = {
  submit: (body: ClaimSubmitRequest) =>
    api.post<ClaimResponse>(ROOT, body).then((r) => fromDetail(r.data)),

  mine: () =>
    api
      .get<ClaimSummaryResponse[]>(`${ROOT}/me`)
      .then((r) => (r.data || []).map(fromSummary)),

  get: (id: string) =>
    api.get<ClaimResponse>(`${ROOT}/${id}`).then((r) => fromDetail(r.data)),

  // Backend does not yet expose an evidence-upload endpoint — this call will
  // 404 until the claim service adds POST /api/claims/{id}/evidence.
  uploadEvidence: (claimId: string, file: File, fileType: EvidenceFileType) => {
    const form = new FormData();
    form.append("file", file);
    form.append("fileType", fileType);
    return api
      .post<Evidence>(`${ROOT}/${claimId}/evidence`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  listEvidence: (claimId: string) =>
    api.get<Evidence[]>(`${ROOT}/${claimId}/evidence`).then((r) => r.data),

  // Manual-review queue — STAFF / ADMIN only. Backed by
  // GET /api/claims/queue, sorted oldest-first.
  manualReviewQueue: () =>
    api
      .get<ClaimSummaryResponse[]>(`${ROOT}/queue`)
      .then((r) => (r.data || []).map(fromSummary)),

  review: (id: string, body: ClaimReviewRequest) =>
    api
      .patch<ClaimResponse>(`${ROOT}/${id}/review`, body)
      .then((r) => fromDetail(r.data)),
};
