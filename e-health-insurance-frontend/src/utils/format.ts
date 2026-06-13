import { ClaimStatus, NotificationStatus, PolicyStatus, Role } from "../types";

const USD = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
});

export function money(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return "—";
  return USD.format(Number(value));
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-US", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("en-US", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function fileSize(bytes: number | null | undefined): string {
  if (!bytes && bytes !== 0) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// ─── Status labels (English) ──────────────────────────────────

// Backend UserRole: CUSTOMER, STAFF, ADMIN
export const roleLabels: Record<Role, string> = {
  CUSTOMER: "Customer",
  STAFF: "Staff",
  ADMIN: "Administrator",
};

// Backend PolicyStatus: PENDING, ACTIVE, CANCELLED
export const policyStatusLabels: Record<PolicyStatus, string> = {
  PENDING: "Pending",
  ACTIVE: "Active",
  CANCELLED: "Cancelled",
};

// Backend ClaimStatus: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED
export const claimStatusLabels: Record<ClaimStatus, string> = {
  SUBMITTED: "Submitted",
  UNDER_REVIEW: "Under review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
};

export const notificationStatusLabels: Record<NotificationStatus, string> = {
  PENDING: "Pending",
  SENT: "Sent",
  FAILED: "Failed",
};

export const claimTypeLabels: Record<string, string> = {
  HOSPITALIZATION: "Hospitalization",
  MEDICATION: "Medication",
  DENTAL: "Dental",
  CONSULTATION: "Consultation",
};

/** Status → CSS badge class */
export function statusTone(
  status: string
): "success" | "warning" | "danger" | "info" | "neutral" {
  switch (status) {
    case "ACTIVE":
    case "APPROVED":
    case "SENT":
      return "success";
    case "PENDING":
    case "SUBMITTED":
    case "UNDER_REVIEW":
      return "warning";
    case "REJECTED":
    case "FAILED":
    case "CANCELLED":
      return "danger";
    default:
      return "info";
  }
}
