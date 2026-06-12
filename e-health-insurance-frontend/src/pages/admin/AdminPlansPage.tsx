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

  useEffect(load, []); // eslint-disable-line react-hooks/exhaustive-deps

  const set =
    (key: keyof PlanFormState) =>
    (e: { target: { value: string } }) =>
      setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSave = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await policyApi.createPlan(toRequest(form));
      toast.success("Plan yaradıldı");
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

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Plan idarəetməsi</h1>
          <p>Sığorta planlarının yaradılması</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setForm(emptyForm()); setShowCreate(true); }}>
          + Yeni plan
        </button>
      </div>

      <div className="alert alert-info">
        Plan redaktəsi, arxivləşdirmə və aktivləşdirmə backend tərəfindən dəstəklənmir.
      </div>

      {plans.length === 0 ? (
        <EmptyState title="Plan yoxdur" hint="İlk planı yaradın." />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Ad</th>
                <th>Haqq</th>
                <th>Əhatə məbləği</th>
                <th>Müddət</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {plans.map((p) => (
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
                  <td>{p.durationMonths} ay</td>
                  <td>
                    <Badge
                      status={p.active ? "ACTIVE" : "CANCELLED"}
                      label={p.active ? "Aktiv" : "Deaktiv"}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <Modal title="Yeni plan" onClose={() => setShowCreate(false)}>
          <form onSubmit={onSave}>
            <Field label="Plan adı" required>
              <input value={form.name} onChange={set("name")} required maxLength={120} />
            </Field>
            <Field label="Təsvir" required>
              <textarea
                value={form.description}
                onChange={set("description")}
                required
                maxLength={1000}
              />
            </Field>
            <div className="form-row">
              <Field label="Aylıq haqq (AZN)" required>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.premiumAmount}
                  onChange={set("premiumAmount")}
                  required
                />
              </Field>
              <Field label="Əhatə məbləği (AZN)" required>
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
            <Field label="Müddət (ay)" required>
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
                İmtina
              </button>
              <button className="btn btn-primary" disabled={saving}>
                {saving ? "Yadda saxlanılır..." : "Yarat"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
