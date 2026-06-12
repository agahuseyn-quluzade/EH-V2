import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { claimApi } from "../../api/claim";
import { extractError } from "../../api/client";
import { Badge, EmptyState, ErrorState, Spinner } from "../../components/ui";
import { Claim } from "../../types";
import { claimStatusLabels, claimTypeLabels, formatDateTime, money } from "../../utils/format";

export function StaffQueuePage() {
  const navigate = useNavigate();
  const [queue, setQueue] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    // GET /api/v1/claims?status=UNDER_REVIEW — claims awaiting staff review
    claimApi
      .getAllClaims("UNDER_REVIEW", 0, 50)
      .then((page) => setQueue(page.content ?? []))
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
          <h1>Baxış növbəsi</h1>
          <p>Baxış tələb edən iddialar</p>
        </div>
        <button className="btn btn-secondary" onClick={load}>
          ↻ Yenilə
        </button>
      </div>

      {queue.length === 0 ? (
        <EmptyState
          title="Növbə boşdur"
          hint="Hazırda baxış tələb edən iddia yoxdur."
        />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>№</th>
                <th>İstifadəçi ID</th>
                <th>Növ</th>
                <th>Məbləğ</th>
                <th>Risk balı</th>
                <th>Status</th>
                <th>Tarix</th>
              </tr>
            </thead>
            <tbody>
              {queue.map((c) => (
                <tr
                  key={c.id}
                  className="row-click"
                  onClick={() => navigate(`/staff/claims/${c.id}`)}
                >
                  <td className="mono">{c.claimNumber}</td>
                  <td className="mono">{c.userId.slice(0, 8)}…</td>
                  <td>{claimTypeLabels[c.claimType] ?? c.claimType}</td>
                  <td>
                    <strong>{money(c.amount)}</strong>
                  </td>
                  <td>
                    {c.riskScore != null ? `${c.riskScore}/100` : "—"}
                  </td>
                  <td>
                    <Badge status={c.status} label={claimStatusLabels[c.status]} />
                  </td>
                  <td>{formatDateTime(c.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
