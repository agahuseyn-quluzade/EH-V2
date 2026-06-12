import { ClaimStatus, NotificationStatus, PolicyStatus, Role } from "../types";

const AZN = new Intl.NumberFormat("az-Latn-AZ", {
  style: "currency",
  currency: "AZN",
  minimumFractionDigits: 2,
});

export function money(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return "—";
  return AZN.format(Number(value));
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("az-Latn-AZ", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("az-Latn-AZ", {
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

// ─── Status etiketləri (Azərbaycan dilində) ──────────────────────────────────

// Backend UserRole: CUSTOMER, AGENT, ADMIN
export const roleLabels: Record<Role, string> = {
  CUSTOMER: "Müştəri",
  AGENT: "Əməkdaş",
  ADMIN: "Administrator",
};

// Backend PolicyStatus: PENDING, ACTIVE, CANCELLED
export const policyStatusLabels: Record<PolicyStatus, string> = {
  PENDING: "Gözləmədə",
  ACTIVE: "Aktiv",
  CANCELLED: "Ləğv edilib",
};

// Backend ClaimStatus: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED
export const claimStatusLabels: Record<ClaimStatus, string> = {
  SUBMITTED: "Təqdim edilib",
  UNDER_REVIEW: "Baxış altında",
  APPROVED: "Təsdiqlənib",
  REJECTED: "Rədd edilib",
};

export const notificationStatusLabels: Record<NotificationStatus, string> = {
  PENDING: "Gözləmədə",
  SENT: "Göndərilib",
  FAILED: "Uğursuz",
};

export const claimTypeLabels: Record<string, string> = {
  HOSPITALIZATION: "Xəstəxanaya yatış",
  MEDICATION: "Dərman",
  DENTAL: "Diş müalicəsi",
  CONSULTATION: "Konsultasiya",
};

/** Status → CSS badge sinfi */
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
