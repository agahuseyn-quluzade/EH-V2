import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi, extractError } from "../../api";
import { Claim, ClaimStatus } from "../../types";
import { fmtDate, fmtMoney } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Empty } from "../../components/Empty";
import { Loader } from "../../components/Loader";

const FILTERS: { label: string; value: ClaimStatus | "ALL" }[] = [
  { label: "All", value: "ALL" },
  { label: "Pending", value: "PENDING" },
  { label: "Under review", value: "MANUAL_REVIEW" },
  { label: "Approved", value: "APPROVED" },
  { label: "Rejected", value: "REJECTED" },
];

export function MyClaimsPage() {
  const [claims, setClaims] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [filter, setFilter] = useState<ClaimStatus | "ALL">("ALL");

  useEffect(() => {
    let mounted = true;
    claimApi
      .mine()
      .then((c) => mounted && setClaims(c || []))
      .catch((e) => mounted && setErr(extractError(e)))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  const filtered = useMemo(
    () => (filter === "ALL" ? claims : claims.filter((c) => c.status === filter)),
    [claims, filter]
  );

  return (
    <>
      <div className="split">
        <div>
          <h1 className="page-title">My claims</h1>
          <p className="page-sub">Track every claim you've submitted and see the decision.</p>
        </div>
        <Link to="/claims/new" className="btn btn-primary">+ Submit a claim</Link>
      </div>

      <div className="tabs">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            className={filter === f.value ? "active" : ""}
            onClick={() => setFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading ? (
        <Loader />
      ) : err ? (
        <Empty title="Could not load claims">
          <div className="tiny muted">{err}</div>
        </Empty>
      ) : filtered.length === 0 ? (
        <Empty
          title="No claims to show"
          action={<Link to="/claims/new" className="btn btn-primary">Submit your first claim</Link>}
        />
      ) : (
        <div className="table-wrap">
          <table className="t">
            <thead>
              <tr>
                <th>Claim</th>
                <th>Provider</th>
                <th>Service date</th>
                <th>Amount</th>
                <th>Reimbursed</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td>
                    <Link to={`/claims/${c.id}`} style={{ fontWeight: 500 }}>
                      {c.procedureCode}
                    </Link>
                    <div className="tiny muted">{c.id.slice(0, 8)}</div>
                  </td>
                  <td>{c.providerName}</td>
                  <td>{fmtDate(c.serviceDate)}</td>
                  <td>{fmtMoney(c.amount)}</td>
                  <td>{fmtMoney(c.approvedAmount ?? null)}</td>
                  <td><StatusBadge status={c.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
