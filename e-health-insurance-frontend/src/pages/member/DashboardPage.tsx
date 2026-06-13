import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi } from "../../api/claim";
import { notificationApi } from "../../api/notification";
import { policyApi } from "../../api/policy";
import { Badge, Spinner, StatCard } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { Claim, Notification, Policy } from "../../types";
import {
  claimStatusLabels,
  claimTypeLabels,
  formatDate,
  formatDateTime,
  money,
  policyStatusLabels,
} from "../../utils/format";

export function DashboardPage() {
  const { user } = useAuth();
  const [policy, setPolicy] = useState<Policy | null>(null);
  const [claims, setClaims] = useState<Claim[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      policyApi.myPolicies(),
      claimApi.mine(),
      notificationApi.mine(),
    ]).then(([p, c, n]) => {
      if (p.status === "fulfilled") {
        const policies = p.value;
        const current =
          policies.find((x) => x.status === "ACTIVE") ??
          policies.find((x) => x.status === "PENDING") ??
          null;
        setPolicy(current);
      }
      if (c.status === "fulfilled") setClaims(c.value);
      if (n.status === "fulfilled") setNotifications(n.value);
      setLoading(false);
    });
  }, []);

  if (loading) return <Spinner />;

  const pendingClaims = claims.filter(
    (c) => c.status === "SUBMITTED" || c.status === "UNDER_REVIEW"
  ).length;
  const approvedClaims = claims.filter((c) => c.status === "APPROVED").length;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Welcome, {user?.firstName}!</h1>
          <p>Overview of your insurance</p>
        </div>
        <Link to="/claims/new" className="btn btn-primary">
          + New claim
        </Link>
      </div>

      <div className="grid grid-4">
        <StatCard
          label="Insurance status"
          value={
            policy ? (
              <Badge status={policy.status} label={policyStatusLabels[policy.status]} />
            ) : (
              "None"
            )
          }
          hint={policy ? policy.planName : "You haven't purchased insurance yet"}
          tone={policy?.status === "ACTIVE" ? "success" : "warning"}
        />
        <StatCard
          label="Yearly premium"
          value={policy ? money(policy.premiumAmount) : "—"}
          hint={policy ? `Plan: ${policy.planName}` : undefined}
          tone="info"
        />
        <StatCard
          label="Pending claims"
          value={pendingClaims}
          hint={`${claims.length} claims total`}
          tone={pendingClaims > 0 ? "warning" : "neutral"}
        />
        <StatCard
          label="Approved claims"
          value={approvedClaims}
          tone="success"
        />
      </div>

      {policy && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-title">
            <h2>My active policy</h2>
            <Link to="/policy" className="btn btn-secondary btn-sm">
              Details
            </Link>
          </div>
          <dl className="detail-list">
            <div>
              <dt>Plan</dt>
              <dd>{policy.planName}</dd>
            </div>
            <div>
              <dt>Policy No.</dt>
              <dd className="mono">{policy.policyNumber}</dd>
            </div>
            <div>
              <dt>Yearly premium</dt>
              <dd>{money(policy.premiumAmount)}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>
                {formatDate(policy.startDate)} — {formatDate(policy.endDate)}
              </dd>
            </div>
          </dl>
        </div>
      )}

      {!policy && (
        <div className="card" style={{ marginTop: 16 }}>
          <h2>You don't have an active policy</h2>
          <p className="muted">
            Browse our health insurance plans and choose the one that suits you.
          </p>
          <Link to="/plans" className="btn btn-primary">
            View plans
          </Link>
        </div>
      )}

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <div className="card">
          <div className="card-title">
            <h2>Recent claims</h2>
            <Link to="/claims" className="btn btn-secondary btn-sm">
              View all
            </Link>
          </div>
          {claims.length === 0 ? (
            <p className="muted">You haven't submitted any claims yet.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {claims.slice(0, 5).map((c) => (
                    <tr key={c.id}>
                      <td>
                        <Link to={`/claims/${c.id}`}>
                          {claimTypeLabels[c.claimType] ?? c.claimType}
                        </Link>
                      </td>
                      <td>{money(c.amount)}</td>
                      <td>
                        <Badge status={c.status} label={claimStatusLabels[c.status]} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-title">
            <h2>Recent notifications</h2>
            <Link to="/notifications" className="btn btn-secondary btn-sm">
              View all
            </Link>
          </div>
          {notifications.length === 0 ? (
            <p className="muted">No notifications.</p>
          ) : (
            <ul style={{ listStyle: "none", margin: 0, padding: 0 }}>
              {notifications.slice(0, 5).map((n) => (
                <li
                  key={n.id}
                  style={{
                    padding: "8px 0",
                    borderBottom: "1px solid var(--color-border)",
                  }}
                >
                  <strong>{n.subject}</strong>
                  <div className="muted" style={{ fontSize: "0.78rem" }}>
                    {formatDateTime(n.createdAt)}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </>
  );
}
