import { api } from "./client";
import {
  Claim,
  ClaimReviewRequest,
  ClaimStatus,
  ClaimSubmitRequest,
  Evidence,
  SpringPage,
} from "../types";

const ROOT = "/api/v1/claims";

export const claimApi = {
  submit: (body: ClaimSubmitRequest) => api.post<Claim>(ROOT, body).then((r) => r.data),

  mine: () => api.get<Claim[]>(`${ROOT}/me`).then((r) => r.data),

  get: (id: string) => api.get<Claim>(`${ROOT}/${id}`).then((r) => r.data),

  getAllClaims: (status?: ClaimStatus, page = 0, size = 20) => {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    params.set("page", String(page));
    params.set("size", String(size));
    return api
      .get<SpringPage<Claim>>(`${ROOT}?${params.toString()}`)
      .then((r) => r.data);
  },

  review: (id: string, body: ClaimReviewRequest) =>
    api.put<Claim>(`${ROOT}/${id}/review`, body).then((r) => r.data),

  uploadEvidence: (claimId: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api
      .post<Evidence>(`${ROOT}/${claimId}/evidence`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },
};
