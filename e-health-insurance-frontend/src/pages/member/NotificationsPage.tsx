import { useEffect, useState } from "react";
import { extractError } from "../../api/client";
import { notificationApi } from "../../api/notification";
import { EmptyState, ErrorState, Spinner } from "../../components/ui";
import { Notification } from "../../types";
import { claimTypeLabels, formatDateTime, money } from "../../utils/format";

const notificationTypeLabels: Record<string, string> = {
  WELCOME: "Welcome",
  POLICY_ACTIVATED: "Policy activated",
  POLICY_PENDING: "Policy pending",
  CLAIM_SUBMITTED: "Claim submitted",
  CLAIM_APPROVED: "Claim approved",
  CLAIM_REJECTED: "Claim rejected",
  PAYMENT_SUCCESS: "Payment successful",
  PAYMENT_FAILED: "Payment failed",
  FRAUD_ALERT: "Fraud alert",
};

const notificationTypeIcons: Record<string, string> = {
  WELCOME: "👋",
  POLICY_ACTIVATED: "✅",
  POLICY_PENDING: "⏳",
  CLAIM_SUBMITTED: "📝",
  CLAIM_APPROVED: "✅",
  CLAIM_REJECTED: "❌",
  PAYMENT_SUCCESS: "💳",
  PAYMENT_FAILED: "⚠️",
  FRAUD_ALERT: "🚨",
};

function formatNotificationBody(body: string): string {
  let text = body.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();

  text = text.replace(/\s*\(transaction\s+[^)]+\)/gi, "");

  for (const [type, label] of Object.entries(claimTypeLabels)) {
    text = text.replace(new RegExp(`\\b${type}\\b`, "g"), label);
  }

  text = text.replace(/\b(of|for)\s+(\d+(?:\.\d+)?)\b/gi, (_match, keyword, amount) => {
    return `${keyword} ${money(Number(amount))}`;
  });

  return text;
}

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    notificationApi
      .mine()
      .then(setNotifications)
      .catch((e) => setError(extractError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  if (loading) return <Spinner />;
  if (error) return <ErrorState message={error} onRetry={load} />;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Notifications</h1>
          <p>Notifications sent to you</p>
        </div>
      </div>

      {notifications.length === 0 ? (
        <EmptyState title="No notifications" hint="You haven't received any notifications yet." />
      ) : (
        <div className="card" style={{ padding: 0 }}>
          <ul style={{ listStyle: "none", margin: 0, padding: 0 }}>
            {notifications.map((n) => (
              <li
                key={n.id}
                style={{
                  padding: "14px 20px",
                  borderBottom: "1px solid var(--color-border)",
                  cursor: "pointer",
                }}
                onClick={() => setExpanded(expanded === n.id ? null : n.id)}
              >
                <div className="flex-between">
                  <strong>
                    {notificationTypeIcons[n.type] ?? "🔔"}{" "}
                    {n.subject || notificationTypeLabels[n.type] || n.type}
                  </strong>
                  <span className="muted" style={{ fontSize: "0.78rem" }}>
                    {formatDateTime(n.createdAt)}
                  </span>
                </div>
                {expanded === n.id && n.body && (
                  <div
                    style={{
                      marginTop: 10,
                      padding: 12,
                      background: "var(--color-neutral-bg)",
                      borderRadius: 8,
                      fontSize: "0.83rem",
                      whiteSpace: "pre-wrap",
                      maxHeight: 280,
                      overflow: "auto",
                    }}
                  >
                    {formatNotificationBody(n.body)}
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
