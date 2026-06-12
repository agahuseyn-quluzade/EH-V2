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
        const active = policies.find((x) => x.status === "ACTIVE");
        setPolicy(active ?? policies[0] ?? null);
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
          <h1>Xoş gəlmisiniz, {user?.firstName}!</h1>
          <p>Sığortanızın ümumi vəziyyəti</p>
        </div>
        <Link to="/claims/new" className="btn btn-primary">
          + Yeni iddia
        </Link>
      </div>

      <div className="grid grid-4">
        <StatCard
          label="Sığorta statusu"
          value={
            policy ? (
              <Badge status={policy.status} label={policyStatusLabels[policy.status]} />
            ) : (
              "Yoxdur"
            )
          }
          hint={policy ? policy.planName : "Hələ sığorta almamısınız"}
          tone={policy?.status === "ACTIVE" ? "success" : "warning"}
        />
        <StatCard
          label="Aylıq haqq"
          value={policy ? money(policy.premiumAmount) : "—"}
          hint={policy ? `Plan: ${policy.planName}` : undefined}
          tone="info"
        />
        <StatCard
          label="Gözləyən iddialar"
          value={pendingClaims}
          hint={`Cəmi ${claims.length} iddia`}
          tone={pendingClaims > 0 ? "warning" : "neutral"}
        />
        <StatCard
          label="Təsdiqlənmiş iddialar"
          value={approvedClaims}
          tone="success"
        />
      </div>

      {policy && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-title">
            <h2>Aktiv sığortam</h2>
            <Link to="/policy" className="btn btn-secondary btn-sm">
              Ətraflı
            </Link>
          </div>
          <dl className="detail-list">
            <div>
              <dt>Plan</dt>
              <dd>{policy.planName}</dd>
            </div>
            <div>
              <dt>Müqavilə №</dt>
              <dd className="mono">{policy.policyNumber}</dd>
            </div>
            <div>
              <dt>Aylıq haqq</dt>
              <dd>{money(policy.premiumAmount)}</dd>
            </div>
            <div>
              <dt>Müddət</dt>
              <dd>
                {formatDate(policy.startDate)} — {formatDate(policy.endDate)}
              </dd>
            </div>
          </dl>
        </div>
      )}

      {!policy && (
        <div className="card" style={{ marginTop: 16 }}>
          <h2>Sığortanız yoxdur</h2>
          <p className="muted">
            Sağlamlıq sığortası planlarını nəzərdən keçirin və sizə uyğun olanı seçin.
          </p>
          <Link to="/plans" className="btn btn-primary">
            Planlara bax
          </Link>
        </div>
      )}

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <div className="card">
          <div className="card-title">
            <h2>Son iddialar</h2>
            <Link to="/claims" className="btn btn-secondary btn-sm">
              Hamısı
            </Link>
          </div>
          {claims.length === 0 ? (
            <p className="muted">Hələ iddia təqdim etməmisiniz.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Növ</th>
                    <th>Məbləğ</th>
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
            <h2>Son bildirişlər</h2>
            <Link to="/notifications" className="btn btn-secondary btn-sm">
              Hamısı
            </Link>
          </div>
          {notifications.length === 0 ? (
            <p className="muted">Bildiriş yoxdur.</p>
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
