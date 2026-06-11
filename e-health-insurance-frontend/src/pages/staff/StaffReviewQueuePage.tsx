import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi, extractError } from "../../api";
import { Claim } from "../../types";
import { fmtDate, fmtDateTime, fmtMoney } from "../../utils/format";
import { Loader } from "../../components/Loader";
import { Empty } from "../../components/Empty";

export function StaffReviewQueuePage() {
  const [claims, setClaims] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

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

  const flaggedHigh = claims.filter((c) => (c.fraudScore ?? 0) > 0.6).length;
  const totalAmount = claims.reduce((s, c) => s + c.amount, 0);

  return (
    <>
      <h1 className="page-title">Review queue</h1>
      <p className="page-sub">
        Claims flagged by the AI as needing a human decision. Click a row to
        review the evidence and record your verdict.
      </p>

      <div className="grid-3" style={{ marginBottom: 24 }}>
        <div className="metric">
          <div className="metric-label">Awaiting review</div>
          <div className="metric-value">{claims.length}</div>
          <div className="metric-hint">Total flagged claims</div>
        </div>
        <div className="metric">
          <div className="metric-label">High-risk</div>
          <div className="metric-value">{flaggedHigh}</div>
          <div className="metric-hint">Fraud score &gt; 0.6</div>
        </div>
        <div className="metric">
          <div className="metric-label">Combined value</div>
          <div className="metric-value">{fmtMoney(totalAmount)}</div>
          <div className="metric-hint">Sum of claimed amounts</div>
        </div>
      </div>

      {loading ? (
        <Loader />
      ) : err ? (
        <Empty title="Could not load flagged claims">
          <div className="tiny muted">{err}</div>
        </Empty>
      ) : claims.length === 0 ? (
        <Empty title="The queue is clear" >
          <div>No claims need manual review right now.</div>
        </Empty>
      ) : (
        <div className="table-wrap">
          <table className="t">
            <thead>
              <tr>
                <th>Claim</th>
                <th>Member</th>
                <th>Provider</th>
                <th>Service date</th>
                <th>Amount</th>
                <th>Fraud score</th>
                <th>Submitted</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {claims.map((c) => (
                <tr key={c.id}>
                  <td>
                    <Link to={`/staff/claims/${c.id}`} style={{ fontWeight: 500 }}>
                      {c.procedureCode}
                    </Link>
                    <div className="tiny muted">{c.id.slice(0, 8)}</div>
                  </td>
                  <td>{c.memberId?.slice(0, 8) || "—"}</td>
                  <td>{c.providerName}</td>
                  <td>{fmtDate(c.serviceDate)}</td>
                  <td>{fmtMoney(c.amount)}</td>
                  <td>
                    {c.fraudScore != null ? (
                      <span
                        className={
                          "badge badge-" +
                          (c.fraudScore > 0.6 ? "danger" : c.fraudScore > 0.3 ? "warn" : "success")
                        }
                      >
                        {c.fraudScore.toFixed(2)}
                      </span>
                    ) : "—"}
                  </td>
                  <td className="tiny muted">{fmtDateTime(c.submittedAt)}</td>
                  <td>
                    <Link to={`/staff/claims/${c.id}`} className="btn btn-secondary btn-sm">
                      Review
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
