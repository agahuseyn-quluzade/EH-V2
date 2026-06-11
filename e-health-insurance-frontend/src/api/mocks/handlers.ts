// Mock route table. Each handler receives the parsed URL, query params, JSON
// body (when applicable), and the authenticated user (when the route
// requires auth).

import {
  AuthResponse,
  Claim,
  ClaimReviewRequest,
  ClaimSubmitRequest,
  Evidence,
  Plan,
  PlanRequest,
  Policy,
  PurchaseRequest,
  TokenResponse,
  UpdateUserRequest,
  UserProfile,
} from "../../types";
import {
  claims,
  evidence,
  makeToken,
  nextChatReply,
  notifications,
  now,
  plans,
  policies,
  roleForEmail,
  sessions,
  userFromToken,
  users,
  uuid,
} from "./db";

export interface MockRequest {
  method: string;
  url: string; // path with query string
  pathname: string;
  search: URLSearchParams;
  params: Record<string, string>; // captured path params
  headers: Record<string, string>;
  body: unknown;
  auth: UserProfile | null;
}

export type MockResponse =
  | { status: number; data?: unknown }
  | { status: number; error: string };

type Handler = (req: MockRequest) => Promise<MockResponse> | MockResponse;

interface Route {
  method: string;
  // pattern uses {paramName} placeholders, e.g. /users/{id}/role
  pattern: string;
  handler: Handler;
  auth?: boolean;
  roles?: ("MEMBER" | "STAFF" | "ADMIN")[];
}

// ─── matcher ─────────────────────────────────────────────────────────────────

function compile(pattern: string): RegExp {
  const escaped = pattern
    .replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
    .replace(/\\\{([^}]+)\\\}/g, "(?<$1>[^/]+)");
  return new RegExp(`^${escaped}$`);
}

const compiled: Array<Route & { regex: RegExp }> = [];

function add(
  method: string,
  pattern: string,
  handler: Handler,
  opts: { auth?: boolean; roles?: Route["roles"] } = {}
) {
  compiled.push({
    method,
    pattern,
    handler,
    regex: compile(pattern),
    ...opts,
  });
}

export function match(method: string, pathname: string) {
  for (const r of compiled) {
    if (r.method !== method) continue;
    const m = r.regex.exec(pathname);
    if (m) {
      return { route: r, params: { ...(m.groups || {}) } };
    }
  }
  return null;
}

const ok = (data: unknown, status = 200): MockResponse => ({ status, data });
const fail = (status: number, error: string): MockResponse => ({
  status,
  error,
});

// ─── IAM ─────────────────────────────────────────────────────────────────────

add("POST", "/api/iam/auth/register", (req) => {
  const body = req.body as {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone?: string;
  };
  if (!body?.email || !body?.password) {
    return fail(400, "Email and password are required.");
  }
  if (users.find((u) => u.email.toLowerCase() === body.email.toLowerCase())) {
    return fail(409, "An account with that email already exists.");
  }
  const user: UserProfile = {
    id: uuid(),
    email: body.email,
    firstName: body.firstName,
    lastName: body.lastName,
    phone: body.phone || null,
    role: roleForEmail(body.email),
    status: "ACTIVE",
    createdAt: now(),
    lastLoginAt: now(),
  };
  users.push(user);
  const accessToken = makeToken(user.id, "access");
  const refreshToken = makeToken(user.id, "refresh");
  const res: AuthResponse = {
    accessToken,
    refreshToken,
  };
  return ok(res, 201);
});

add("POST", "/api/iam/auth/login", (req) => {
  const body = req.body as { email: string; password: string };
  if (!body?.email) return fail(400, "Email is required.");
  let user = users.find(
    (u) => u.email.toLowerCase() === body.email.toLowerCase()
  );
  // Convenience: auto-create a user for any new email so reviewers can poke
  // around without registering first. Role is derived from the email.
  if (!user) {
    user = {
      id: uuid(),
      email: body.email,
      firstName: body.email.split("@")[0],
      lastName: "Demo",
      phone: null,
      role: roleForEmail(body.email),
      status: "ACTIVE",
      createdAt: now(),
      lastLoginAt: now(),
    };
    users.push(user);
  }
  user.lastLoginAt = now();
  const accessToken = makeToken(user.id, "access");
  const refreshToken = makeToken(user.id, "refresh");
  const res: AuthResponse = {
    accessToken,
    refreshToken,
  };
  return ok(res);
});

add("POST", "/api/iam/auth/refresh", (req) => {
  const body = req.body as { refreshToken: string };
  const uid = body?.refreshToken && sessions.get(body.refreshToken);
  if (!uid) return fail(401, "Invalid refresh token.");
  const accessToken = makeToken(uid, "access");
  const refreshToken = makeToken(uid, "refresh");
  const res: TokenResponse = { accessToken, refreshToken };
  return ok(res);
});

add(
  "GET",
  "/api/iam/users/me",
  (req) => ok(req.auth),
  { auth: true }
);

add(
  "PUT",
  "/api/iam/users/me",
  (req) => {
    const body = req.body as UpdateUserRequest;
    const u = req.auth!;
    u.firstName = body.firstName;
    u.lastName = body.lastName;
    u.phone = body.phone ?? null;
    return ok(u);
  },
  { auth: true }
);

add(
  "GET",
  "/api/iam/users/{id}",
  (req) => {
    const u = users.find((x) => x.id === req.params.id);
    if (!u) return fail(404, "User not found.");
    return ok(u);
  },
  { auth: true, roles: ["STAFF", "ADMIN"] }
);

add(
  "PATCH",
  "/api/iam/users/{id}/role",
  (req) => {
    const body = req.body as { role: "MEMBER" | "STAFF" | "ADMIN" };
    const u = users.find((x) => x.id === req.params.id);
    if (!u) return fail(404, "User not found.");
    u.role = body.role;
    return ok(u);
  },
  { auth: true, roles: ["ADMIN"] }
);

// ─── Policy ──────────────────────────────────────────────────────────────────

add("GET", "/api/policy/policies/plans", () => ok(plans), { auth: true });

add(
  "GET",
  "/api/policy/policies/plans/{id}",
  (req) => {
    const p = plans.find((x) => x.id === req.params.id);
    if (!p) return fail(404, "Plan not found.");
    return ok(p);
  },
  { auth: true }
);

add(
  "POST",
  "/api/policy/policies/plans",
  (req) => {
    const body = req.body as PlanRequest;
    const plan: Plan = {
      id: `plan-${uuid().slice(0, 8)}`,
      ...body,
      status: "ACTIVE",
      createdAt: now(),
    };
    plans.push(plan);
    return ok(plan, 201);
  },
  { auth: true, roles: ["ADMIN"] }
);

add(
  "PUT",
  "/api/policy/policies/plans/{id}",
  (req) => {
    const body = req.body as PlanRequest;
    const p = plans.find((x) => x.id === req.params.id);
    if (!p) return fail(404, "Plan not found.");
    Object.assign(p, body);
    return ok(p);
  },
  { auth: true, roles: ["ADMIN"] }
);

add(
  "POST",
  "/api/policy/policies/purchase",
  (req) => {
    const body = req.body as PurchaseRequest;
    const plan = plans.find((x) => x.id === body.planId);
    if (!plan) return fail(404, "Plan not found.");
    // Replace existing policy for this member if there is one.
    const idx = policies.findIndex((p) => p.memberId === req.auth!.id);
    const start = body.startDate;
    const endDate = new Date(start);
    endDate.setFullYear(endDate.getFullYear() + 1);
    const policy: Policy = {
      id: `pol-${uuid().slice(0, 8)}`,
      memberId: req.auth!.id,
      planId: plan.id,
      startDate: start,
      endDate: endDate.toISOString().slice(0, 10),
      status: "ACTIVE",
      deductiblePaid: 0,
      claimsTotal: 0,
      createdAt: now(),
    };
    if (idx >= 0) policies[idx] = policy;
    else policies.push(policy);
    return ok(policy, 201);
  },
  { auth: true }
);

add(
  "GET",
  "/api/policy/policies/me",
  (req) => {
    const p = policies.find((x) => x.memberId === req.auth!.id);
    if (!p) return fail(404, "No active policy.");
    return ok(p);
  },
  { auth: true }
);

// ─── Claim ───────────────────────────────────────────────────────────────────

add(
  "POST",
  "/api/claim/claims",
  (req) => {
    const body = req.body as ClaimSubmitRequest;
    const policy = policies.find((p) => p.id === body.policyId);
    if (!policy || policy.memberId !== req.auth!.id) {
      return fail(400, "Policy not found or not yours.");
    }
    const plan = plans.find((p) => p.id === policy.planId);
    // Simulate decisioning:
    const covered = plan?.coveredProcedures.includes(body.procedureCode);
    const score = body.amount > 4000 ? 0.7 : body.amount > 1500 ? 0.35 : 0.1;
    let status: Claim["status"];
    let approvedAmount: number | undefined;
    let reason: string;
    let decidedAt: string | undefined;

    if (!covered) {
      status = "REJECTED";
      reason = `Procedure ${body.procedureCode} is not covered by your plan.`;
      approvedAmount = 0;
      decidedAt = now();
    } else if (score > 0.6) {
      status = "MANUAL_REVIEW";
      reason = `Elevated risk (fraud score ${score.toFixed(2)}). A reviewer will take a look.`;
    } else {
      status = "APPROVED";
      approvedAmount = Math.round(body.amount * (plan?.coveragePercent ?? 0.8));
      reason = `Auto-approved. Fraud score ${score.toFixed(2)} (low).`;
      decidedAt = now();
      // Update policy aggregates.
      policy.claimsTotal += approvedAmount;
    }

    const claim: Claim = {
      id: `clm-${uuid().slice(0, 6)}`,
      memberId: req.auth!.id,
      policyId: body.policyId,
      amount: body.amount,
      procedureCode: body.procedureCode,
      diagnosisCode: body.diagnosisCode,
      providerName: body.providerName,
      serviceDate: body.serviceDate,
      status,
      decisionReason: reason,
      approvedAmount,
      submittedAt: now(),
      decidedAt,
      fraudScore: score,
    };
    claims.unshift(claim);
    return ok(claim, 201);
  },
  { auth: true }
);

add(
  "GET",
  "/api/claim/claims/me",
  (req) => ok(claims.filter((c) => c.memberId === req.auth!.id)),
  { auth: true }
);

add(
  "GET",
  "/api/claim/claims/flagged",
  () => ok(claims.filter((c) => c.status === "MANUAL_REVIEW")),
  { auth: true, roles: ["STAFF", "ADMIN"] }
);

add(
  "GET",
  "/api/claim/claims/{id}",
  (req) => {
    const c = claims.find((x) => x.id === req.params.id);
    if (!c) return fail(404, "Claim not found.");
    // Members can only see their own claims.
    if (req.auth!.role === "MEMBER" && c.memberId !== req.auth!.id) {
      return fail(403, "Not allowed.");
    }
    return ok(c);
  },
  { auth: true }
);

add(
  "GET",
  "/api/claim/claims/{id}/evidence",
  (req) => ok(evidence.filter((e) => e.claimId === req.params.id)),
  { auth: true }
);

add(
  "POST",
  "/api/claim/claims/{id}/evidence",
  (req) => {
    const body = req.body as { file?: { name?: string; size?: number; type?: string }; fileType?: string };
    const ev: Evidence = {
      id: `ev-${uuid().slice(0, 6)}`,
      claimId: req.params.id,
      filePath: `/uploads/${body.file?.name || "upload"}`,
      originalFileName: body.file?.name || "upload",
      contentType: body.file?.type,
      fileType: (body.fileType as Evidence["fileType"]) || "OTHER",
      fileSize: body.file?.size || 0,
      uploadedAt: now(),
    };
    evidence.push(ev);
    return ok(ev, 201);
  },
  { auth: true }
);

add(
  "PATCH",
  "/api/claim/claims/{id}/review",
  (req) => {
    const body = req.body as ClaimReviewRequest;
    const c = claims.find((x) => x.id === req.params.id);
    if (!c) return fail(404, "Claim not found.");
    c.status = body.decision;
    c.decisionReason = body.reason;
    c.approvedAmount = body.decision === "APPROVED" ? body.approvedAmount || 0 : 0;
    c.decidedAt = now();
    return ok(c);
  },
  { auth: true, roles: ["STAFF", "ADMIN"] }
);

// ─── AI ──────────────────────────────────────────────────────────────────────

add(
  "POST",
  "/api/ai/chat/message",
  (req) => {
    const body = req.body as { content: string; conversationId?: string };
    const conversationId = body.conversationId || `conv-${uuid().slice(0, 8)}`;
    return ok({
      conversationId,
      message: {
        role: "ASSISTANT",
        content: nextChatReply(),
        createdAt: now(),
      },
    });
  },
  { auth: true }
);

add(
  "POST",
  "/api/ai/policies/recommend",
  (req) => {
    const inputs = req.body as { budget?: number; age?: number };
    const ranked = [...plans]
      .filter((p) => p.status === "ACTIVE")
      .sort((a, b) => {
        if (inputs.budget) {
          return (
            Math.abs(a.monthlyPremium - inputs.budget) -
            Math.abs(b.monthlyPremium - inputs.budget)
          );
        }
        return a.monthlyPremium - b.monthlyPremium;
      })
      .slice(0, 3)
      .map((p) => ({
        planId: p.id,
        planName: p.name,
        reason: inputs.budget
          ? `Closest to your ${inputs.budget} monthly budget, with ${Math.round(p.coveragePercent * 100)}% reimbursement.`
          : `Strong balance of price (${p.monthlyPremium}/mo) and coverage (${Math.round(p.coveragePercent * 100)}%).`,
      }));
    return ok({ inputsSnapshot: inputs, recommendations: ranked });
  },
  { auth: true }
);

add(
  "POST",
  "/api/ai/claims/score",
  (req) => {
    const body = req.body as { claimId: string };
    const c = claims.find((x) => x.id === body.claimId);
    if (!c) return fail(404, "Claim not found.");
    return ok({
      id: `ana-${uuid().slice(0, 6)}`,
      claimId: c.id,
      fraudScore: c.fraudScore || 0,
      recommendedAction:
        (c.fraudScore || 0) > 0.3 ? "HUMAN_REVIEW" : "APPROVE",
      flags:
        (c.fraudScore || 0) > 0.6
          ? ["amount_too_high", "unusual_provider"]
          : [],
      explanationText: c.decisionReason || "No notable risk indicators.",
      createdAt: c.submittedAt,
    });
  },
  { auth: true }
);

// ─── Notification ────────────────────────────────────────────────────────────

add(
  "GET",
  "/api/notification/notifications/me",
  (req) =>
    ok(
      notifications.filter(
        (n) =>
          n.recipientId === req.auth!.id ||
          n.recipientEmail.toLowerCase() === req.auth!.email.toLowerCase()
      )
    ),
  { auth: true }
);

// ─── Route entrypoint used by the adapter ────────────────────────────────────

export async function handle(req: MockRequest): Promise<MockResponse> {
  const m = match(req.method, req.pathname);
  if (!m) {
    return fail(404, `No mock handler for ${req.method} ${req.pathname}`);
  }
  req.params = m.params;

  // Auth checks
  if (m.route.auth) {
    const auth = req.headers["authorization"] || req.headers["Authorization"];
    const token = auth?.startsWith("Bearer ") ? auth.slice(7) : null;
    const user = token ? userFromToken(token) : null;
    if (!user) return fail(401, "Authentication required.");
    if (m.route.roles && !m.route.roles.includes(user.role)) {
      return fail(403, "Your role does not allow this action.");
    }
    req.auth = user;
  }

  try {
    return await m.route.handler(req);
  } catch (e) {
    return fail(500, (e as Error).message);
  }
}
