import { api } from "./client";
import {
  CoverageResponse,
  Plan,
  PlanRequest,
  Policy,
  PurchaseRequest,
} from "../types";

// Spring Data returns Page<T> as { content: T[], ...metadata }.
// Some endpoints return plain arrays; this helper handles both.
type SpringPage<T> = { content: T[] };
function unwrap<T>(data: T[] | SpringPage<T>): T[] {
  return Array.isArray(data) ? data : data?.content ?? [];
}

// Gateway strips /api/policy and prepends /api/v1.
// Upstream controllers: /api/v1/plans/**, /api/v1/policies/**,
// /api/v1/admin/** and /api/v1/internal/**.
const PLANS = "/api/policy/plans";
const POLICIES = "/api/policy/policies";
const ADMIN = "/api/policy/admin";

export const policyApi = {
  // ── Plans ────────────────────────────────────────────────────────────────
  listPlans: (size = 100) =>
    api
      .get<Plan[] | SpringPage<Plan>>(`${PLANS}?size=${size}`)
      .then((r) => unwrap(r.data)),

  getPlan: (id: string) =>
    api.get<Plan>(`${PLANS}/${id}`).then((r) => r.data),

  createPlan: (body: PlanRequest) =>
    api.post<Plan>(PLANS, body).then((r) => r.data),

  updatePlan: (id: string, body: PlanRequest) =>
    api.put<Plan>(`${PLANS}/${id}`, body).then((r) => r.data),

  retirePlan: (id: string) =>
    api.patch<Plan>(`${PLANS}/${id}/retire`).then((r) => r.data),

  activatePlan: (id: string) =>
    api.patch<Plan>(`${PLANS}/${id}/activate`).then((r) => r.data),

  comparePlans: (ids: string[]) =>
    api
      .get<Plan[]>(`${PLANS}/compare?ids=${ids.join(",")}`)
      .then((r) => r.data),

  // ── Member policies ──────────────────────────────────────────────────────
  purchase: (body: PurchaseRequest) =>
    api.post<Policy>(POLICIES, body).then((r) => r.data),

  myPolicy: () => api.get<Policy>(`${POLICIES}/me`).then((r) => r.data),

  myPolicyHistory: () =>
    api.get<Policy[]>(`${POLICIES}/me/history`).then((r) => r.data),

  cancelPolicy: (policyId: string, reason: string) =>
    api
      .patch<Policy>(`${POLICIES}/${policyId}/cancel`, { reason })
      .then((r) => r.data),

  renewPolicy: (policyId: string) =>
    api.post<Policy>(`${POLICIES}/${policyId}/renewals`).then((r) => r.data),

  checkCoverage: (
    policyId: string,
    args: {
      procedureCode: string;
      claimAmount: number;
      serviceDate: string;
      claimId?: string;
    }
  ) =>
    api
      .post<CoverageResponse>(`${POLICIES}/${policyId}/coverage-checks`, {
        claimId: args.claimId ?? null,
        serviceDate: args.serviceDate,
        procedureCode: args.procedureCode,
        claimAmount: args.claimAmount,
      })
      .then((r) => r.data),

  // ── Admin ────────────────────────────────────────────────────────────────
  adminListPolicies: (page = 0, size = 20) =>
    api
      .get<Policy[] | SpringPage<Policy>>(
        `${ADMIN}/policies?page=${page}&size=${size}`
      )
      .then((r) => unwrap(r.data)),

  adminMemberPolicies: (memberId: string) =>
    api
      .get<Policy[]>(`${ADMIN}/members/${memberId}/policies`)
      .then((r) => r.data),

  adminPurchaseForMember: (
    memberId: string,
    body: { planId: string; startDate: string }
  ) =>
    api
      .post<Policy>(`${ADMIN}/members/${memberId}/policies`, {
        ...body,
        memberId,
      })
      .then((r) => r.data),

  adminStatistics: () =>
    api
      .get<Record<string, unknown>>(`${ADMIN}/policies/statistics`)
      .then((r) => r.data),
};
