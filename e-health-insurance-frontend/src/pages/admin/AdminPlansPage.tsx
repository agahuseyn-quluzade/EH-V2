import { FormEvent, useEffect, useState } from "react";
import { extractError, policyApi } from "../../api";
import { Plan, PlanRequest } from "../../types";
import { fmtMoney, fmtPercent } from "../../utils/format";
import { Loader } from "../../components/Loader";
import { Modal } from "../../components/Modal";
import { useToast } from "../../context/ToastContext";
import { StatusBadge } from "../../components/Badge";

const EMPTY: PlanRequest = {
  name: "",
  description: "",
  monthlyPremium: 0,
  coveragePercent: 0.8,
  annualLimit: 30000,
  deductible: 500,
  durationMonths: 12,
  gracePeriodDays: 15,
  waitingPeriodDays: 30,
  effectiveFrom: new Date().toISOString().slice(0, 10),
  coveredProcedures: [],
};

export function AdminPlansPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [editing, setEditing] = useState<Plan | null>(null);
  const [creating, setCreating] = useState(false);
  const toast = useToast();

  async function load() {
    setLoading(true);
    try {
      const data = await policyApi.listPlans();
      setPlans(data || []);
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <>
      <div className="split">
        <div>
          <h1 className="page-title">Plan catalog</h1>
          <p className="page-sub">Create, edit, and retire the plans offered to members.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>
          + New plan
        </button>
      </div>

      {loading ? (
        <Loader />
      ) : err ? (
        <div className="card" style={{ background: "var(--red-50)", borderColor: "var(--red-200)" }}>
          {err}
        </div>
      ) : plans.length === 0 ? (
        <div className="empty-state">
          <h3>No plans yet</h3>
          <div className="muted">Create the first plan to start selling insurance.</div>
        </div>
      ) : (
        <div className="table-wrap">
          <table className="t">
            <thead>
              <tr>
                <th>Name</th>
                <th>Premium</th>
                <th>Coverage</th>
                <th>Annual limit</th>
                <th>Deductible</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {plans.map((p) => (
                <tr key={p.id}>
                  <td>
                    <div style={{ fontWeight: 500 }}>{p.name}</div>
                    <div className="tiny muted" style={{ maxWidth: 280, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                      {p.description}
                    </div>
                  </td>
                  <td>{fmtMoney(p.monthlyPremium)}</td>
                  <td>{fmtPercent(p.coveragePercent)}</td>
                  <td>{fmtMoney(p.annualLimit)}</td>
                  <td>{fmtMoney(p.deductible)}</td>
                  <td><StatusBadge status={p.status} /></td>
                  <td>
                    <button className="btn btn-secondary btn-sm" onClick={() => setEditing(p)}>
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <PlanFormModal
        open={creating}
        onClose={() => setCreating(false)}
        initial={EMPTY}
        title="Create plan"
        onSubmit={async (body) => {
          await policyApi.createPlan(body);
          toast.success("Plan created.");
          setCreating(false);
          load();
        }}
      />

      <PlanFormModal
        open={!!editing}
        onClose={() => setEditing(null)}
        initial={
          editing
            ? {
                name: editing.name,
                description: editing.description ?? "",
                monthlyPremium: editing.monthlyPremium,
                coveragePercent: editing.coveragePercent,
                annualLimit: editing.annualLimit,
                deductible: editing.deductible,
                durationMonths: editing.durationMonths ?? EMPTY.durationMonths,
                gracePeriodDays: editing.gracePeriodDays ?? EMPTY.gracePeriodDays,
                waitingPeriodDays: editing.waitingPeriodDays ?? EMPTY.waitingPeriodDays,
                effectiveFrom: editing.effectiveFrom ?? EMPTY.effectiveFrom,
                effectiveTo: editing.effectiveTo ?? undefined,
                coveredProcedures: editing.coveredProcedures || [],
              }
            : EMPTY
        }
        title={editing ? `Edit ${editing.name}` : "Edit plan"}
        onSubmit={async (body) => {
          if (!editing) return;
          await policyApi.updatePlan(editing.id, body);
          toast.success("Plan updated.");
          setEditing(null);
          load();
        }}
      />
    </>
  );
}

function PlanFormModal({
  open,
  onClose,
  title,
  initial,
  onSubmit,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  initial: PlanRequest;
  onSubmit: (body: PlanRequest) => Promise<void>;
}) {
  const [form, setForm] = useState<PlanRequest>(initial);
  const [proc, setProc] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setForm(initial);
      setProc("");
      setErr(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function set<K extends keyof PlanRequest>(k: K, v: PlanRequest[K]) {
    setForm((f) => ({ ...f, [k]: v }));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      await onSubmit(form);
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      width={560}
      footer={
        <>
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" type="submit" form="plan-form" disabled={busy}>
            {busy ? <span className="spinner" /> : "Save plan"}
          </button>
        </>
      }
    >
      <form id="plan-form" onSubmit={submit}>
        <div className="field">
          <label>Plan name</label>
          <input value={form.name} onChange={(e) => set("name", e.target.value)} required />
        </div>
        <div className="field">
          <label>Description</label>
          <textarea
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            rows={2}
            required
            style={{ padding: "10px 12px", borderRadius: "var(--radius)", border: "1px solid var(--border-strong)" }}
          />
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Monthly premium (USD)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={form.monthlyPremium}
              onChange={(e) => set("monthlyPremium", Number(e.target.value))}
              required
            />
          </div>
          <div className="field">
            <label>Coverage (0–1)</label>
            <input
              type="number"
              step="0.05"
              min="0"
              max="1"
              value={form.coveragePercent}
              onChange={(e) => set("coveragePercent", Number(e.target.value))}
              required
            />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Annual limit (USD)</label>
            <input
              type="number"
              min="0"
              value={form.annualLimit}
              onChange={(e) => set("annualLimit", Number(e.target.value))}
              required
            />
          </div>
          <div className="field">
            <label>Deductible (USD)</label>
            <input
              type="number"
              min="0"
              value={form.deductible}
              onChange={(e) => set("deductible", Number(e.target.value))}
              required
            />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Duration (months)</label>
            <input
              type="number"
              min="1"
              value={form.durationMonths}
              onChange={(e) => set("durationMonths", Number(e.target.value))}
              required
            />
          </div>
          <div className="field">
            <label>Effective from</label>
            <input
              type="date"
              value={form.effectiveFrom}
              onChange={(e) => set("effectiveFrom", e.target.value)}
              required
            />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Grace period (days)</label>
            <input
              type="number"
              min="0"
              value={form.gracePeriodDays}
              onChange={(e) => set("gracePeriodDays", Number(e.target.value))}
              required
            />
          </div>
          <div className="field">
            <label>Waiting period (days)</label>
            <input
              type="number"
              min="0"
              value={form.waitingPeriodDays}
              onChange={(e) => set("waitingPeriodDays", Number(e.target.value))}
              required
            />
          </div>
        </div>

        <div className="field">
          <label>Covered procedures</label>
          <div className="row">
            <input
              placeholder="e.g. SUR01"
              value={proc}
              onChange={(e) => setProc(e.target.value)}
              style={{
                flex: 1,
                padding: "10px 12px",
                border: "1px solid var(--border-strong)",
                borderRadius: "var(--radius)",
              }}
            />
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                if (!proc) return;
                set("coveredProcedures", [...form.coveredProcedures, proc.trim()]);
                setProc("");
              }}
            >
              Add
            </button>
          </div>
          {form.coveredProcedures.length > 0 && (
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 8 }}>
              {form.coveredProcedures.map((c, i) => (
                <span
                  key={i}
                  className="badge badge-accent"
                  style={{ cursor: "pointer" }}
                  onClick={() =>
                    set(
                      "coveredProcedures",
                      form.coveredProcedures.filter((_, idx) => idx !== i)
                    )
                  }
                  title="Click to remove"
                >
                  {c} ×
                </span>
              ))}
            </div>
          )}
        </div>

        {err && <div className="error">{err}</div>}
      </form>
    </Modal>
  );
}
