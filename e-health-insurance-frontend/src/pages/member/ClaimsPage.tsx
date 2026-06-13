import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { claimApi } from "../../api/claim";
import { extractError } from "../../api/client";
import { Badge, EmptyState, ErrorState, Spinner } from "../../components/ui";
import { Claim, ClaimStatus } from "../../types";
import { claimStatusLabels, claimTypeLabels, formatDateTime, money } from "../../utils/format";

const FILTERS: Array<{ value: ClaimStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "All" },
  { value: "UNDER_REVIEW", label: "Under review" },
  { value: "APPROVED", label: "Approved" },
  { value: "REJECTED", label: "Rejected" },
];

export function ClaimsPage() {
  const navigate = useNavigate();
  const [claims, setClaims] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<ClaimStatus | "ALL">("ALL");

  const load = () => {
    setLoading(true);
    setError(null);
    claimApi
      .mine()
      .then(setClaims)
      .catch((e) => setError(extractError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  if (loading) return <Spinner />;
  if (error) return <ErrorState message={error} onRetry={load} />;

  const filtered =
    filter === "ALL" ? claims : claims.filter((c) => c.status === filter);

  return (
    <>
      <div className="page-header">
        <div>
          <h1>My Claims</h1>
          <p>Insurance claims you have submitted</p>
        </div>
        <Link to="/claims/new" className="btn btn-primary">
          + New claim
        </Link>
      </div>

      <div style={{ display: "flex", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
        {FILTERS.map((f) => (
          <button
            key={f.value}
            className={`btn btn-sm ${filter === f.value ? "btn-primary" : "btn-secondary"}`}
            onClick={() => setFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          title="No claims found"
          hint={
            filter === "ALL"
              ? "You haven't submitted any claims yet."
              : "There are no claims with this status."
          }
        />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>No.</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Approved</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr
                  key={c.id}
                  className="row-click"
                  onClick={() => navigate(`/claims/${c.id}`)}
                >
                  <td className="mono">{c.claimNumber}</td>
                  <td>{claimTypeLabels[c.claimType] ?? c.claimType}</td>
                  <td>{money(c.amount)}</td>
                  <td>
                    {c.approvedAmount != null ? money(c.approvedAmount) : "—"}
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
