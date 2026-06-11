import { api } from "./client";
import {
  CreatePaymentRequest,
  CreateRefundRequest,
  Invoice,
  Payment,
  Refund,
} from "../types";

// Gateway rewrites /api/payment/(.*) → /api/v1/$1.
// Upstream PaymentController is mounted at /api/v1 (payments, invoices, refunds).
const ROOT = "/api/payment";

export const paymentApi = {
  // Idempotency-Key lets the member safely retry a charge without double-paying.
  create: (body: CreatePaymentRequest, idempotencyKey?: string) =>
    api
      .post<Payment>(`${ROOT}/payments`, body, {
        headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
      })
      .then((r) => r.data),

  mine: () =>
    api.get<Payment[]>(`${ROOT}/payments/me`).then((r) => r.data),

  get: (id: string) =>
    api.get<Payment>(`${ROOT}/payments/${id}`).then((r) => r.data),

  confirm: (id: string, providerReference?: string) =>
    api
      .post<Payment>(`${ROOT}/payments/${id}/confirm`, { providerReference })
      .then((r) => r.data),

  invoicesByPolicy: (policyId: string) =>
    api.get<Invoice[]>(`${ROOT}/invoices/${policyId}`).then((r) => r.data),

  createRefund: (body: CreateRefundRequest) =>
    api.post<Refund>(`${ROOT}/refunds`, body).then((r) => r.data),

  myRefunds: () =>
    api.get<Refund[]>(`${ROOT}/refunds/me`).then((r) => r.data),
};
