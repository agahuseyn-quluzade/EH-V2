import { FormEvent, useEffect, useState } from "react";
import { aiApi, extractError, policyApi } from "../../api";
import { Plan, PlanRecommendation, RecommendInputs } from "../../types";
import { fmtMoney, fmtPercent } from "../../utils/format";
import { Loader } from "../../components/Loader";
import { Modal } from "../../components/Modal";
import { useToast } from "../../context/ToastContext";

export function PlansPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [purchasing, setPurchasing] = useState<string | null>(null);
  const [recommendOpen, setRecommendOpen] = useState(false);
  const [recommendations, setRecommendations] = useState<PlanRecommendation[]>([]);
  const toast = useToast();

  useEffect(() => {
    let mounted = true;
    policyApi
      .listPlans()
      .then((data) => mounted && setPlans(data || []))
      .catch((e) => mounted && setErr(extractError(e)))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  async function purchase(plan: Plan) {
    setPurchasing(plan.id);
    try {
      const today = new Date().toISOString().slice(0, 10);
      await policyApi.purchase({ planId: plan.id, startDate: today });
      toast.success(`Welcome to ${plan.name}. Your policy is active.`);
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setPurchasing(null);
    }
  }

  if (loading) return <Loader />;

  return (
    <>
      <div className="split">
        <div>
          <h1 className="page-title">Choose a plan</h1>
          <p className="page-sub">
            All plans cover preventive care, emergencies, and prescriptions.
            Higher tiers raise the reimbursement rate and annual limit.
          </p>
        </div>
        <button className="btn btn-secondary" onClick={() => setRecommendOpen(true)}>
          ✦ Ask the AI to recommend
        </button>
      </div>

      {err && (
        <div className="card" style={{ marginBottom: 16, background: "var(--red-50)" }}>
          <strong style={{ color: "var(--red-800)" }}>Could not load plans.</strong>
          <div className="tiny muted">{err}</div>
        </div>
      )}

      {recommendations.length > 0 && (
        <div
          className="card"
          style={{
            background: "var(--accent-soft)",
            borderColor: "var(--teal-200)",
            marginBottom: 20,
          }}
        >
          <div className="section-title">AI recommendation</div>
          <div className="stack">
            {recommendations.map((r, i) => (
              <div key={i}>
                <strong>{i + 1}. {r.planName}</strong>
                <div className="tiny muted">{r.reason}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="grid-3">
        {plans.map((plan) => (
          <div key={plan.id} className="plan-card">
            <div>
              <div className="section-title" style={{ margin: 0 }}>{plan.status === "ACTIVE" ? "Available" : "Retired"}</div>
              <h3 style={{ fontSize: 20, margin: "4px 0" }}>{plan.name}</h3>
              {plan.description && (
                <div className="tiny muted">{plan.description}</div>
              )}
            </div>

            <div className="plan-price">
              {fmtMoney(plan.monthlyPremium)}
              <span> /month</span>
            </div>

            <div className="stack" style={{ gap: 6 }}>
              <div className="plan-feature">
                Reimburses {fmtPercent(plan.coveragePercent)} of covered care
              </div>
              <div className="plan-feature">
                {fmtMoney(plan.annualLimit)} annual limit
              </div>
              <div className="plan-feature">
                {fmtMoney(plan.deductible)} deductible
              </div>
              <div className="plan-feature">
                {plan.coveredProcedures?.length || 0} covered procedures
              </div>
            </div>

            <button
              className="btn btn-primary btn-block"
              disabled={purchasing === plan.id || plan.status !== "ACTIVE"}
              onClick={() => purchase(plan)}
            >
              {purchasing === plan.id ? <span className="spinner" /> : "Select this plan"}
            </button>
          </div>
        ))}
      </div>

      <RecommendModal
        open={recommendOpen}
        onClose={() => setRecommendOpen(false)}
        onResult={(recs) => {
          setRecommendations(recs);
          setRecommendOpen(false);
          toast.success("AI recommendation ready.");
        }}
      />
    </>
  );
}

function RecommendModal({
  open,
  onClose,
  onResult,
}: {
  open: boolean;
  onClose: () => void;
  onResult: (r: PlanRecommendation[]) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [age, setAge] = useState("");
  const [budget, setBudget] = useState("");
  const [conditions, setConditions] = useState("");
  const [family, setFamily] = useState("");
  const [notes, setNotes] = useState("");
  const toast = useToast();

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      const inputs: RecommendInputs = {
        age: age ? Number(age) : undefined,
        budget: budget ? Number(budget) : undefined,
        conditions: conditions
          ? conditions.split(",").map((s) => s.trim()).filter(Boolean)
          : undefined,
        family: family ? Number(family) : undefined,
        notes: notes || undefined,
      };
      const res = await aiApi.recommend(inputs);
      onResult(res.recommendations || []);
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Tell us about yourself"
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" form="recommend-form" className="btn btn-primary" disabled={busy}>
            {busy ? <span className="spinner" /> : "Recommend"}
          </button>
        </>
      }
    >
      <form id="recommend-form" onSubmit={submit}>
        <div className="grid-2">
          <div className="field">
            <label>Age</label>
            <input type="number" value={age} onChange={(e) => setAge(e.target.value)} />
          </div>
          <div className="field">
            <label>Family size</label>
            <input type="number" value={family} onChange={(e) => setFamily(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label>Monthly budget ($)</label>
          <input type="number" value={budget} onChange={(e) => setBudget(e.target.value)} />
        </div>
        <div className="field">
          <label>Known conditions (comma-separated)</label>
          <input value={conditions} onChange={(e) => setConditions(e.target.value)} />
        </div>
        <div className="field">
          <label>Anything else?</label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
            style={{ padding: "10px 12px", borderRadius: "var(--radius)", border: "1px solid var(--border-strong)" }}
          />
        </div>
      </form>
    </Modal>
  );
}
