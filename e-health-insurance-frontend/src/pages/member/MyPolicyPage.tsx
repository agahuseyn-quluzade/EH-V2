import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { extractError, policyApi } from "../../api";
import { Plan, Policy } from "../../types";
import { fmtDate, fmtMoney, fmtPercent } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Empty } from "../../components/Empty";
import { Loader } from "../../components/Loader";

export function MyPolicyPage() {
  const [policy, setPolicy] = useState<Policy | null>(null);
  const [plan, setPlan] = useState<Plan | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
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
      .catch((e) => mounted && setErr(extractError(e)))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  if (loading) return <Loader />;
  if (!policy) {
    return (
      <>
        <h1 className="page-title">My policy</h1>
        <Empty
          title="You don't have an active policy"
          action={<Link to="/plans" className="btn btn-primary">Browse plans</Link>}
        >
          {err && <div className="tiny muted" style={{ marginTop: 8 }}>{err}</div>}
        </Empty>
      </>
    );
  }

  const utilizationPct =
    plan && plan.annualLimit ? Math.min(1, policy.claimsTotal / plan.annualLimit) : 0;
  const deductiblePct =
    plan && plan.deductible
      ? Math.min(1, policy.deductiblePaid / plan.deductible)
      : 0;

  return (
    <>
      <div className="split">
        <div>
          <h1 className="page-title">My policy</h1>
          <p className="page-sub">{plan?.name || "Plan"} · Effective {fmtDate(policy.startDate)} – {fmtDate(policy.endDate)}</p>
        </div>
        <StatusBadge status={policy.status} />
      </div>

      <div className="grid-2" style={{ marginBottom: 24 }}>
        <div className="card">
          <div className="section-title">Annual reimbursement</div>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <strong>{fmtMoney(policy.claimsTotal)}</strong>
            <span className="muted">of {fmtMoney(plan?.annualLimit)}</span>
          </div>
          <ProgressBar pct={utilizationPct} />
        </div>
        <div className="card">
          <div className="section-title">Deductible</div>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <strong>{fmtMoney(policy.deductiblePaid)}</strong>
            <span className="muted">of {fmtMoney(plan?.deductible)}</span>
          </div>
          <ProgressBar pct={deductiblePct} />
        </div>
      </div>

      <div className="card">
        <h3 style={{ fontSize: 16, marginBottom: 14 }}>Plan details</h3>
        <dl className="kv">
          <dt>Plan name</dt>
          <dd>{plan?.name || "—"}</dd>
          <dt>Description</dt>
          <dd>{plan?.description || "—"}</dd>
          <dt>Monthly premium</dt>
          <dd>{fmtMoney(plan?.monthlyPremium)}</dd>
          <dt>Coverage rate</dt>
          <dd>{fmtPercent(plan?.coveragePercent)}</dd>
          <dt>Annual limit</dt>
          <dd>{fmtMoney(plan?.annualLimit)}</dd>
          <dt>Deductible</dt>
          <dd>{fmtMoney(plan?.deductible)}</dd>
          <dt>Covered procedures</dt>
          <dd>
            {plan?.coveredProcedures && plan.coveredProcedures.length > 0 ? (
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                {plan.coveredProcedures.map((c) => (
                  <span key={c} className="badge badge-accent">{c}</span>
                ))}
              </div>
            ) : "—"}
          </dd>
        </dl>
      </div>
    </>
  );
}

function ProgressBar({ pct }: { pct: number }) {
  return (
    <div
      style={{
        background: "var(--bg-muted)",
        borderRadius: 999,
        height: 10,
        overflow: "hidden",
      }}
    >
      <div
        style={{
          width: `${Math.round(pct * 100)}%`,
          height: "100%",
          background: pct > 0.8 ? "var(--red-400)" : "var(--teal-400)",
          transition: "width 0.3s ease",
        }}
      />
    </div>
  );
}
