import { useEffect, useState } from "react";
import { extractError } from "../../api/client";
import { notificationApi } from "../../api/notification";
import { Badge, EmptyState, ErrorState, Spinner } from "../../components/ui";
import { Notification } from "../../types";
import { formatDateTime, notificationStatusLabels } from "../../utils/format";

const notificationTypeLabels: Record<string, string> = {
  WELCOME: "Xoş gəlmisiniz",
  POLICY_ACTIVATED: "Sığorta aktivləşdi",
  POLICY_PENDING: "Sığorta gözləmədə",
  CLAIM_SUBMITTED: "İddia təqdim edildi",
  CLAIM_APPROVED: "İddia təsdiqləndi",
  CLAIM_REJECTED: "İddia rədd edildi",
  PAYMENT_SUCCESS: "Ödəniş uğurlu",
  PAYMENT_FAILED: "Ödəniş uğursuz",
  FRAUD_ALERT: "Fırıldaqçılıq xəbərdarlığı",
};

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
          <h1>Bildirişlər</h1>
          <p>Sizə göndərilən bildirişlər</p>
        </div>
      </div>

      {notifications.length === 0 ? (
        <EmptyState title="Bildiriş yoxdur" hint="Hələ heç bir bildiriş almamısınız." />
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
                  <strong>{n.subject || notificationTypeLabels[n.type] || n.type}</strong>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <Badge
                      status={n.status}
                      label={notificationStatusLabels[n.status]}
                    />
                    <span className="muted" style={{ fontSize: "0.78rem" }}>
                      {formatDateTime(n.createdAt)}
                    </span>
                  </div>
                </div>
                <div className="muted" style={{ fontSize: "0.8rem" }}>
                  {n.channel === "EMAIL" ? "📧" : "📱"} {n.recipient} •{" "}
                  {notificationTypeLabels[n.type] || n.type}
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
                    {n.body.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim()}
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
