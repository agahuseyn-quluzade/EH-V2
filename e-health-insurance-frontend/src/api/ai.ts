import { api } from "./client";
import {
  ChatConversation,
  ChatMessage,
  ChatRole,
  ClaimAnalysis,
  PlanRecommendation,
  RecommendedAction,
  RecommendInputs,
} from "../types";

const ROOT = "/api/ai";

export interface SendMessageResponse {
  conversationId: string;
  message: ChatMessage;
}

export interface RecommendResponse {
  id?: string;
  inputsSnapshot: RecommendInputs;
  recommendations: PlanRecommendation[];
}

// Backend ChatController returns a flat record:
//   { conversationId, messageId, role, content, createdAt }
// The UI expects { conversationId, message: ChatMessage } so we adapt here.
interface BackendSendMessageResponse {
  conversationId: number | string;
  messageId: number | string;
  role: ChatRole;
  content: string;
  createdAt: string;
}

// Backend ScoreClaimResponse: { id, claimId, score, recommendedAction, flags,
// createdAt } — note `score` (not fraudScore) and no explanationText.
interface BackendScoreResponse {
  id: number | string;
  claimId: string;
  score: number;
  recommendedAction: RecommendedAction;
  flags: string[];
  createdAt: string;
}

// Backend ExplainClaimResponse: { id, claimId, explanationText, createdAt }.
interface BackendExplainResponse {
  id: number | string;
  claimId: string;
  explanationText: string;
  createdAt: string;
}

export const aiApi = {
  scoreClaim: (claimId: string) =>
    api
      .post<BackendScoreResponse>(`${ROOT}/claims/score`, { claimId })
      .then<ClaimAnalysis>((r) => ({
        id: String(r.data.id),
        claimId: r.data.claimId,
        fraudScore: r.data.score,
        recommendedAction: r.data.recommendedAction,
        flags: r.data.flags || [],
        createdAt: r.data.createdAt,
      })),

  explainClaim: (claimId: string) =>
    api
      .post<BackendExplainResponse>(`${ROOT}/claims/explain`, { claimId })
      .then<Partial<ClaimAnalysis>>((r) => ({
        id: String(r.data.id),
        claimId: r.data.claimId,
        explanationText: r.data.explanationText,
        createdAt: r.data.createdAt,
      })),

  extractDocument: (documentId: string) =>
    api
      .post(`${ROOT}/claims/extract`, { documentId })
      .then((r) => r.data),

  // Chat
  listConversations: () =>
    api
      .get<ChatConversation[]>(`${ROOT}/chat/conversations`)
      .then((r) => r.data),

  sendMessage: (content: string, conversationId?: string) =>
    api
      .post<BackendSendMessageResponse>(`${ROOT}/chat/message`, {
        content,
        conversationId,
      })
      .then<SendMessageResponse>((r) => ({
        conversationId: String(r.data.conversationId),
        message: {
          id: String(r.data.messageId),
          conversationId: String(r.data.conversationId),
          role: r.data.role,
          content: r.data.content,
          createdAt: r.data.createdAt,
        },
      })),

  // Recommendation — adapt frontend input/output shapes to the AI service.
  recommend: (inputs: RecommendInputs) =>
    api
      .post<{
        id?: number | string;
        userId?: string;
        recommendations: Array<{
          planId: string;
          planName: string;
          price: number;
          reason: string;
        }>;
        createdAt?: string;
      }>(`${ROOT}/policies/recommend`, {
        age: inputs.age,
        monthlyBudget: inputs.budget,
        preExistingConditions: inputs.conditions,
      })
      .then<RecommendResponse>((r) => ({
        id: r.data.id !== undefined ? String(r.data.id) : undefined,
        inputsSnapshot: inputs,
        recommendations: (r.data.recommendations || []).map((rec) => ({
          planId: rec.planId,
          planName: rec.planName,
          reason: rec.reason,
        })),
      })),
};
