import { ReactNode } from "react";

interface BadgeProps {
  children: ReactNode;
  variant?: "success" | "warn" | "info" | "danger" | "neutral" | "accent";
}

export function Badge({ children, variant = "neutral" }: BadgeProps) {
  return <span className={`badge badge-${variant}`}>{children}</span>;
}

// Status → visual variant mapping.
const STATUS_VARIANT: Record<string, BadgeProps["variant"]> = {
  APPROVED: "success",
  PENDING: "warn",
  MANUAL_REVIEW: "info",
  REJECTED: "danger",
  PENDING_PAYMENT: "warn",
  PAYMENT_FAILED: "danger",
  SCHEDULED: "info",
  ACTIVE: "success",
  GRACE_PERIOD: "warn",
  DISABLED: "danger",
  SENT: "success",
  FAILED: "danger",
  EXPIRED: "neutral",
  CANCELLED: "neutral",
  RETIRED: "neutral",
  // Payment / refund / invoice statuses
  COMPLETED: "success",
  PROCESSING: "info",
  REFUNDED: "neutral",
  PAID: "success",
  OVERDUE: "danger",
  // Health-record statuses
  ARCHIVED: "neutral",
  AWAITING_EVIDENCE: "warn",
};

const STATUS_LABEL: Record<string, string> = {
  APPROVED: "Approved",
  PENDING: "Pending",
  MANUAL_REVIEW: "Under Review",
  REJECTED: "Rejected",
  PENDING_PAYMENT: "Pending Payment",
  PAYMENT_FAILED: "Payment Failed",
  SCHEDULED: "Scheduled",
  ACTIVE: "Active",
  GRACE_PERIOD: "Grace Period",
  DISABLED: "Disabled",
  SENT: "Sent",
  FAILED: "Failed",
  EXPIRED: "Expired",
  CANCELLED: "Cancelled",
  RETIRED: "Retired",
  COMPLETED: "Completed",
  PROCESSING: "Processing",
  REFUNDED: "Refunded",
  PAID: "Paid",
  OVERDUE: "Overdue",
  ARCHIVED: "Archived",
  AWAITING_EVIDENCE: "Awaiting Evidence",
};

export function StatusBadge({ status }: { status: string }) {
  const variant = STATUS_VARIANT[status] || "neutral";
  const label = STATUS_LABEL[status] || status;
  return <Badge variant={variant}>{label}</Badge>;
}
