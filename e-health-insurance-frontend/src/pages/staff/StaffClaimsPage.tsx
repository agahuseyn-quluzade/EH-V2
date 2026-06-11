import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi, extractError } from "../../api";
import { Claim, ClaimStatus } from "../../types";
import { fmtDate, fmtMoney } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Loader } from "../../components/Loader";
import { Empty } from "../../components/Empty";

// Staff-wide list. With the current backend this falls back to the flagged
// endpoint; a future /api/claim/claims endpoint with a staff role would
// return everything.
export function StaffClaimsPage() {
  const [claims, setClaims] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [filter, setFilter] = useState<ClaimStatus | "ALL">("ALL");
  const [search, setSearch] = useState("");

  useEffect(() => {
    let mounted = true;
    claimApi
      .manualReviewQueue()
      .then((c) => mounted && setClaims(c || []))
      .catch((e) => mounted && setErr(extractError(e)))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  const filtered = useMemo(() => {
    let list = claims;
    if (filter !== "ALL") list = list.filter((c) => c.status === filter);
    if (search) {
      const q = search.toLowerCase();
      list = list.filter(
        (c) =>
          c.providerName.toLowerCase().includes(q) ||
          c.procedureCode.toLowerCase().includes(q) ||
          c.id.toLowerCase().includes(q)
      );
    }
    return list;
  }, [claims, filter, search]);

  return (
    <>
      <h1 className="page-title">All claims</h1>
      <p className="page-sub">Search and filter every claim in the system.</p>

      <div className="row" style={{ marginBottom: 16, gap: 12 }}>
        <input
          placeholder="Search by ID, procedure, provider…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{
            flex: 1,
            padding: "10px 12px",
            border: "1px solid var(--border-strong)",
            borderRadius: "var(--radius)",
            background: "var(--bg-surface)",
          }}
        />
        <select
          value={filter}
          onChange={(e) => setFilter(e.target.value as ClaimStatus | "ALL")}
          style={{
            padding: "10px 12px",
            border: "1px solid var(--border-strong)",
            borderRadius: "var(--radius)",
            background: "var(--bg-surface)",
          }}
        >
          <option value="ALL">All statuses</option>
          <option value="PENDING">Pending</option>
          <option value="MANUAL_REVIEW">Under review</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      {loading ? (
        <Loader />
      ) : err ? (
        <Empty title="Could not load claims">
          <div className="tiny muted">{err}</div>
        </Empty>
      ) : filtered.length === 0 ? (
        <Empty title="No claims match your filters" />
      ) : (
        <div className="table-wrap">
          <table className="t">
            <thead>
              <tr>
                <th>Claim</th>
                <th>Provider</th>
                <th>Service date</th>
                <th>Amount</th>
                <th>Approved</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td>
                    <Link to={`/staff/claims/${c.id}`} style={{ fontWeight: 500 }}>
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
