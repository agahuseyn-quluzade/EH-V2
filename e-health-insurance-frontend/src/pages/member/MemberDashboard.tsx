import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { claimApi, extractError, policyApi } from "../../api";
import { Claim, Policy, Plan } from "../../types";
import { fmtDate, fmtMoney, fmtPercent } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Loader } from "../../components/Loader";

export function MemberDashboard() {
  const { user } = useAuth();
  const [policy, setPolicy] = useState<Policy | null>(null);
  const [plan, setPlan] = useState<Plan | null>(null);
  const [claims, setClaims] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [policyErr, setPolicyErr] = useState<string | null>(null);
  const [claimsErr, setClaimsErr] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    (async () => {
      // These can fail independently (e.g. member has no policy yet).
      await Promise.allSettled([
        policyApi
          .myPolicy()
          .then(async (p) => {
            if (!mounted) return;
            setPolicy(p);
            if (p?.planId) {
              try {
                const pl = await policyApi.getPlan(p.planId);
                if (mounted) setPlan(pl);
              } catch {
                /* ignore */
              }
            }
          })
          .catch((e) => mounted && setPolicyErr(extractError(e))),
        claimApi
          .mine()
          .then((c) => mounted && setClaims(c || []))
          .catch((e) => mounted && setClaimsErr(extractError(e))),
      ]);
      if (mounted) setLoading(false);
    })();
    return () => {
      mounted = false;
    };
  }, []);

  if (loading) return <Loader />;

  const totalApproved = claims
    .filter((c) => c.status === "APPROVED" && c.approvedAmount)
    .reduce((s, c) => s + (c.approvedAmount || 0), 0);

  const pendingCount = claims.filter(
    (c) => c.status === "PENDING" || c.status === "MANUAL_REVIEW"
  ).length;

  return (
    <>
      <div className="split">
        <div>
          <h1 className="page-title">Welcome back, {user?.firstName}.</h1>
          <p className="page-sub">Here's a quick snapshot of your coverage and recent activity.</p>
        </div>
        <div>
          <Link to="/claims/new" className="btn btn-primary">+ Submit a claim</Link>
        </div>
      </div>

      <div className="grid-4" style={{ marginBottom: 24 }}>
        <div className="metric">
          <div className="metric-label">Active plan</div>
          <div className="metric-value">{plan?.name || (policy ? "—" : "None")}</div>
          <div className="metric-hint">
            {plan
              ? `${fmtMoney(plan.monthlyPremium)}/mo · ${fmtPercent(plan.coveragePercent)} coverage`
              : "Browse plans to get covered."}
          </div>
        </div>
        <div className="metric">
          <div className="metric-label">Reimbursed YTD</div>
          <div className="metric-value">{fmtMoney(totalApproved)}</div>
          <div className="metric-hint">
            {policy ? `Annual limit ${fmtMoney(plan?.annualLimit)}` : "—"}
          </div>
        </div>
        <div className="metric">
          <div className="metric-label">Deductible paid</div>
          <div className="metric-value">{fmtMoney(policy?.deductiblePaid ?? 0)}</div>
          <div className="metric-hint">
            of {fmtMoney(plan?.deductible)} deductible
          </div>
        </div>
        <div className="metric">
          <div className="metric-label">Open claims</div>
          <div className="metric-value">{pendingCount}</div>
          <div className="metric-hint">
            {claims.length} total claims this year
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <div className="split">
          <h3 style={{ fontSize: 16 }}>Recent claims</h3>
          <Link to="/claims" className="btn btn-ghost btn-sm">View all</Link>
        </div>
        {claimsErr ? (
          <div className="muted tiny">{claimsErr}</div>
        ) : claims.length === 0 ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <h3>No claims yet</h3>
            <div className="muted tiny">When you submit a claim, it will appear here.</div>
          </div>
        ) : (
          <div className="table-wrap" style={{ marginTop: 12 }}>
            <table className="t">
              <thead>
                <tr>
                  <th>Claim</th>
                  <th>Service date</th>
                  <th>Amount</th>
                  <th>Reimbursed</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {claims.slice(0, 5).map((c) => (
                  <tr key={c.id}>
                    <td>
                      <Link to={`/claims/${c.id}`} style={{ fontWeight: 500 }}>
                        {c.procedureCode}
                      </Link>
                      <div className="tiny muted">{c.providerName}</div>
                    </td>
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
      </div>

      {!policy && !policyErr && (
        <div
          className="card"
          style={{
            background: "var(--accent-soft)",
            borderColor: "var(--teal-200)",
          }}
        >
          <h3 style={{ fontSize: 16, marginBottom: 6 }}>You're not covered yet</h3>
          <p className="muted" style={{ marginBottom: 14 }}>
            Choose a plan that fits your needs and start protecting your health
            today. Our AI assistant can help you decide.
          </p>
          <div className="row">
            <Link to="/plans" className="btn btn-primary">Browse plans</Link>
            <Link to="/chat" className="btn btn-secondary">Ask the AI</Link>
          </div>
        </div>
      )}
    </>
  );
}
