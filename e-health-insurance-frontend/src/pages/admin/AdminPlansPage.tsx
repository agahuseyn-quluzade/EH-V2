import { FormEvent, useEffect, useState } from "react";
import { extractError } from "../../api/client";
import { policyApi } from "../../api/policy";
import { Badge, EmptyState, Field, Modal, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { Plan, PlanRequest } from "../../types";
import { money } from "../../utils/format";

interface PlanFormState {
  name: string;
  description: string;
  premiumAmount: string;
  coverageAmount: string;
  durationMonths: string;
}

const emptyForm = (): PlanFormState => ({
  name: "",
  description: "",
  premiumAmount: "",
  coverageAmount: "",
  durationMonths: "12",
});

function toRequest(form: PlanFormState): PlanRequest {
  return {
    name: form.name.trim(),
    description: form.description.trim(),
    premiumAmount: Number(form.premiumAmount),
    coverageAmount: Number(form.coverageAmount),
    durationMonths: Number(form.durationMonths),
  };
}

export function AdminPlansPage() {
  const toast = useToast();

  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<PlanFormState>(emptyForm());
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    policyApi
      .listPlans()
      .then(setPlans)
      .catch((e) => toast.error(extractError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const set =
    (key: keyof PlanFormState) =>
    (e: { target: { value: string } }) =>
      setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSave = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await policyApi.createPlan(toRequest(form));
      toast.success("Plan created");
      setShowCreate(false);
      setForm(emptyForm());
      load();
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Spinner />;

  const visiblePlans = plans.filter((p) => p.name !== "Smoke Test Plan");

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Plan Management</h1>
          <p>Create insurance plans</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setForm(emptyForm()); setShowCreate(true); }}>
          + New plan
        </button>
      </div>

      <div className="alert alert-info">
        Plan editing, archiving, and activation are not supported by the backend.
      </div>

      {visiblePlans.length === 0 ? (
        <EmptyState title="No plans" hint="Create your first plan." />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Premium</th>
                <th>Coverage amount</th>
                <th>Duration</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {visiblePlans.map((p) => (
                <tr key={p.id}>
                  <td>
                    <strong>{p.name}</strong>
                    {p.description && (
                      <div className="muted" style={{ fontSize: "0.78rem" }}>
                        {p.description}
                      </div>
                    )}
                  </td>
                  <td>{money(p.premiumAmount)}</td>
                  <td>{money(p.coverageAmount)}</td>
                  <td>{p.durationMonths} months</td>
                  <td>
                    <Badge
                      status={p.active ? "ACTIVE" : "CANCELLED"}
                      label={p.active ? "Active" : "Inactive"}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <Modal title="New plan" onClose={() => setShowCreate(false)}>
          <form onSubmit={onSave}>
            <Field label="Plan name" required>
              <input value={form.name} onChange={set("name")} required maxLength={120} />
            </Field>
            <Field label="Description" required>
              <textarea
                value={form.description}
                onChange={set("description")}
                required
                maxLength={1000}
              />
            </Field>
            <div className="form-row">
              <Field label="Yearly premium (USD)" required>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.premiumAmount}
                  onChange={set("premiumAmount")}
                  required
                />
              </Field>
              <Field label="Coverage amount (USD)" required>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.coverageAmount}
                  onChange={set("coverageAmount")}
                  required
                />
              </Field>
            </div>
            <Field label="Duration (months)" required>
              <input
                type="number"
                min="1"
                value={form.durationMonths}
                onChange={set("durationMonths")}
                required
              />
            </Field>
            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowCreate(false)}
              >
                Cancel
              </button>
              <button className="btn btn-primary" disabled={saving}>
                {saving ? "Saving..." : "Create"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
