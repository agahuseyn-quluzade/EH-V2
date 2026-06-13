import { api } from "./client";
import { ChatMessageDto, ChatResponse, FraudAiResponse, RiskAiResponse } from "../types";

const ROOT = "/api/v1/ai";

export const aiApi = {
  chat: (body: { message: string; sessionId?: string | null }) =>
    api.post<ChatResponse>(`${ROOT}/chatbot`, body).then((r) => r.data),

  // GET /api/v1/ai/chatbot/history?sessionId= — chat history for a session (CUSTOMER)
  getChatHistory: (sessionId: string) =>
    api
      .get<ChatMessageDto[]>(`${ROOT}/chatbot/history?sessionId=${sessionId}`)
      .then((r) => r.data),

  // GET /api/v1/ai/fraud-checks/{claimId} — fraud check result (STAFF, ADMIN)
  getFraudCheck: (claimId: string) =>
    api.get<FraudAiResponse>(`${ROOT}/fraud-checks/${claimId}`).then((r) => r.data),

  // POST /api/v1/ai/claims/{claimId}/analyze — manual re-analysis (STAFF, ADMIN)
  analyzeClaim: (claimId: string) =>
    api
      .post<FraudAiResponse>(`${ROOT}/claims/${claimId}/analyze`)
      .then((r) => r.data),

  // GET /api/v1/ai/risk-profile/{userId} — user risk profile (STAFF, ADMIN)
  getRiskProfile: (userId: string) =>
    api.get<RiskAiResponse>(`${ROOT}/risk-profile/${userId}`).then((r) => r.data),
};
