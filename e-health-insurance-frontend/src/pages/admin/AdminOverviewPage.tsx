import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi, extractError, policyApi } from "../../api";
import { Claim, Plan } from "../../types";
import { fmtMoney } from "../../utils/format";
import { Loader } from "../../components/Loader";

export function AdminOverviewPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [claims, setClaims] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [errs, setErrs] = useState<string[]>([]);

  useEffect(() => {
    let mounted = true;
    (async () => {
      const errors: string[] = [];
      await Promise.allSettled([
        policyApi
          .listPlans()
          .then((p) => mounted && setPlans(p || []))
          .catch((e) => errors.push(`Plans: ${extractError(e)}`)),
        claimApi
          .manualReviewQueue()
          .then((c) => mounted && setClaims(c || []))
          .catch((e) => errors.push(`Claims: ${extractError(e)}`)),
      ]);
      if (mounted) {
        setErrs(errors);
        setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  if (loading) return <Loader />;

  const activePlans = plans.filter((p) => p.status === "ACTIVE").length;
  const flagged = claims.length;
  const flaggedValue = claims.reduce((s, c) => s + c.amount, 0);

  return (
    <>
      <h1 className="page-title">Operations overview</h1>
      <p className="page-sub">A pulse check on the platform.</p>

      <div className="grid-4" style={{ marginBottom: 24 }}>
        <div className="metric">
          <div className="metric-label">Active plans</div>
          <div className="metric-value">{activePlans}</div>
          <div className="metric-hint">{plans.length} total in catalog</div>
        </div>
        <div className="metric">
          <div className="metric-label">Flagged claims</div>
          <div className="metric-value">{flagged}</div>
          <div className="metric-hint">Awaiting manual review</div>
        </div>
        <div className="metric">
          <div className="metric-label">Flagged value</div>
          <div className="metric-value">{fmtMoney(flaggedValue)}</div>
          <div className="metric-hint">Sum of claim amounts under review</div>
        </div>
        <div className="metric">
          <div className="metric-label">Avg. premium</div>
          <div className="metric-value">
            {plans.length
              ? fmtMoney(
                  plans.reduce((s, p) => s + p.monthlyPremium, 0) / plans.length
                )
              : "—"}
          </div>
          <div className="metric-hint">Across active plans</div>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <h3 style={{ fontSize: 15, marginBottom: 12 }}>Catalog snapshot</h3>
          {plans.length === 0 ? (
            <div className="muted">No plans yet. <Link to="/admin/plans">Create the first one</Link>.</div>
          ) : (
            <div className="stack" style={{ gap: 10 }}>
              {plans.slice(0, 4).map((p) => (
                <div key={p.id} style={{ display: "flex", justifyContent: "space-between", padding: 10, background: "var(--bg-muted)", borderRadius: "var(--radius)" }}>
                  <div>
                    <div style={{ fontWeight: 500 }}>{p.name}</div>
                    <div className="tiny muted">{p.status}</div>
                  </div>
                  <div>{fmtMoney(p.monthlyPremium)}/mo</div>
                </div>
              ))}
              <Link to="/admin/plans" className="btn btn-ghost btn-sm">Manage catalog →</Link>
            </div>
          )}
        </div>

        <div className="card">
          <h3 style={{ fontSize: 15, marginBottom: 12 }}>Action needed</h3>
          {flagged === 0 ? (
            <div className="muted">No claims need attention. </div>
          ) : (
            <>
              <div className="muted" style={{ marginBottom: 12 }}>
                {flagged} flagged claims totaling {fmtMoney(flaggedValue)} are
                waiting for human review.
              </div>
              <Link to="/staff" className="btn btn-primary btn-sm">Open review queue</Link>
            </>
          )}
        </div>
      </div>

      {errs.length > 0 && (
        <div className="card" style={{ marginTop: 16, background: "var(--amber-50)", borderColor: "var(--amber-200)" }}>
          <strong style={{ color: "var(--amber-800)" }}>Some services are not reachable.</strong>
          <ul style={{ marginTop: 6, paddingLeft: 18 }}>
            {errs.map((e, i) => (
              <li key={i} className="tiny">{e}</li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
