import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi } from "../../api/claim";
import { extractError } from "../../api/client";
import { Badge, ErrorState, Spinner, StatCard } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { Claim } from "../../types";
import { claimStatusLabels, claimTypeLabels, formatDateTime, money } from "../../utils/format";

export function StaffDashboardPage() {
  const { user } = useAuth();
  const [queue, setQueue] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    claimApi
      .getAllClaims("UNDER_REVIEW", 0, 20)
      .then((page) => setQueue(page.content ?? []))
      .catch((e: unknown) => setError(extractError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  if (loading) return <Spinner />;
  if (error) return <ErrorState message={error} onRetry={load} />;

  const totalAmount = queue.reduce((sum, c) => sum + Number(c.amount || 0), 0);
  const oldest = queue[0];

  return (
    <>
      <div className="page-header">
        <div>
          <h1>İdarə paneli</h1>
          <p>Xoş gəlmisiniz, {user?.firstName}! Baxış gözləyən iddiaların icmalı.</p>
        </div>
        <Link to="/staff/queue" className="btn btn-primary">
          Növbəyə keç
        </Link>
      </div>

      <div className="grid grid-3">
        <StatCard
          label="Baxış gözləyən iddialar"
          value={queue.length}
          tone={queue.length > 0 ? "warning" : "success"}
        />
        <StatCard label="Ümumi məbləğ" value={money(totalAmount)} tone="info" />
        <StatCard
          label="Ən köhnə iddia"
          value={oldest ? formatDateTime(oldest.createdAt) : "—"}
          tone="neutral"
        />
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <div className="card-title">
          <h2>Növbədəki son iddialar</h2>
          <Link to="/staff/queue" className="btn btn-secondary btn-sm">
            Hamısı
          </Link>
        </div>
        {queue.length === 0 ? (
          <p className="muted">Baxış gözləyən iddia yoxdur. 🎉</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Təqdim edilib</th>
                  <th>İddia növü</th>
                  <th>Məbləğ</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {queue.slice(0, 8).map((c) => (
                  <tr key={c.id}>
                    <td>{formatDateTime(c.createdAt)}</td>
                    <td>{claimTypeLabels[c.claimType] ?? c.claimType}</td>
                    <td>{money(c.amount)}</td>
                    <td>
                      <Badge status={c.status} label={claimStatusLabels[c.status]} />
                    </td>
                    <td>
                      <Link
                        to={`/staff/claims/${c.id}`}
                        className="btn btn-primary btn-sm"
                      >
                        Bax
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
