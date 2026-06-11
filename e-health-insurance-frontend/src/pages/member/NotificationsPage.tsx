import { useEffect, useState } from "react";
import { extractError, notificationApi } from "../../api";
import { Notification } from "../../types";
import { fmtDateTime } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Empty } from "../../components/Empty";
import { Loader } from "../../components/Loader";

export function NotificationsPage() {
  const [notifs, setNotifs] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    notificationApi
      .mine()
      .then((n) => mounted && setNotifs(n || []))
      .catch((e) => mounted && setErr(extractError(e)))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <>
      <h1 className="page-title">Notifications</h1>
      <p className="page-sub">
        Every email or SMS we've sent you appears here.
      </p>

      {loading ? (
        <Loader />
      ) : err ? (
        <Empty title="Could not load notifications">
          <div className="tiny muted">{err}</div>
        </Empty>
      ) : notifs.length === 0 ? (
        <Empty title="No notifications yet" />
      ) : (
        <div className="stack">
          {notifs.map((n) => (
            <div key={n.id} className="card">
              <div className="split" style={{ marginBottom: 8 }}>
                <div>
                  <div style={{ fontWeight: 500 }}>{n.subject}</div>
                  <div className="tiny muted">
                    {n.channel} · {n.template} · {fmtDateTime(n.sentAt || n.createdAt)}
                  </div>
                </div>
                <StatusBadge status={n.status} />
              </div>
              {n.body && (
                <div
                  className="notif-body"
                  style={{
                    color: "var(--text-secondary)",
                    fontSize: 13.5,
                    lineHeight: 1.5,
                  }}
                  // Bodies come from server-controlled email templates in the
                  // notification service, never from user input.
                  dangerouslySetInnerHTML={{ __html: n.body }}
                />
              )}
            </div>
          ))}
        </div>
      )}
    </>
  );
}
