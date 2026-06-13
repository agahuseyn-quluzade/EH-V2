import { api } from "./client";
import { Plan, PlanRequest, Policy, PurchaseRequest, SpringPage } from "../types";

const PLANS = "/api/v1/plans";
const POLICIES = "/api/v1/policies";

export const policyApi = {
  listPlans: () =>
    api.get<Plan[]>(PLANS).then((r) => r.data),

  getPlan: (id: string) => api.get<Plan>(`${PLANS}/${id}`).then((r) => r.data),

  createPlan: (body: PlanRequest) => api.post<Plan>(PLANS, body).then((r) => r.data),

  deletePlan: (id: string) => api.delete(`${PLANS}/${id}`).then((r) => r.data),

  purchase: (body: PurchaseRequest) =>
    api.post<Policy>(POLICIES, body).then((r) => r.data),

  myPolicies: () => api.get<Policy[]>(`${POLICIES}/me`).then((r) => r.data),

  getPolicyById: (id: string) =>
    api.get<Policy>(`${POLICIES}/${id}`).then((r) => r.data),

  cancelPolicy: (policyId: string) =>
    api.put<Policy>(`${POLICIES}/${policyId}/cancel`).then((r) => r.data),

  getAllPolicies: (page = 0, size = 20) =>
    api
      .get<SpringPage<Policy>>(`${POLICIES}?page=${page}&size=${size}`)
      .then((r) => r.data),
};
