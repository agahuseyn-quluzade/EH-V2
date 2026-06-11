import { FormEvent, useEffect, useState } from "react";
import { extractError, healthRecordApi } from "../../api";
import {
  AddMedicalEntryRequest,
  EntryType,
  HealthSummary,
  LabResult,
  MedicalEntry,
  Prescription,
} from "../../types";
import { fmtDate } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Empty } from "../../components/Empty";
import { Loader } from "../../components/Loader";
import { Modal } from "../../components/Modal";
import { useToast } from "../../context/ToastContext";

const ENTRY_TYPES: EntryType[] = [
  "DIAGNOSIS",
  "PROCEDURE",
  "VISIT",
  "SURGERY",
  "HOSPITALIZATION",
  "IMAGING",
  "LAB_ORDER",
];

export function HealthRecordPage() {
  const [summary, setSummary] = useState<HealthSummary | null>(null);
  const [entries, setEntries] = useState<MedicalEntry[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [labs, setLabs] = useState<LabResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [missing, setMissing] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [entryOpen, setEntryOpen] = useState(false);
  const toast = useToast();

  async function load() {
    setLoading(true);
    setErr(null);
    try {
      const s = await healthRecordApi.myRecord();
      setSummary(s);
      setMissing(false);
      const [e, p, l] = await Promise.all([
        healthRecordApi.listEntries().catch(() => s.recentEntries || []),
        healthRecordApi.listPrescriptions().catch(() => s.activePrescriptions || []),
        healthRecordApi.listLabResults().catch(() => s.recentLabResults || []),
      ]);
      setEntries(e);
      setPrescriptions(p);
      setLabs(l);
    } catch (e: unknown) {
      // 404 → the member simply hasn't started a record yet.
      const status =
        typeof e === "object" && e && "response" in e
          ? (e as { response?: { status?: number } }).response?.status
          : undefined;
      if (status === 404) {
        setMissing(true);
        setSummary(null);
      } else {
        setErr(extractError(e));
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function createRecord() {
    try {
      await healthRecordApi.createMyRecord();
      toast.success("Health record created.");
      load();
    } catch (e) {
      toast.error(extractError(e));
    }
  }

  if (loading) return <Loader />;

  if (missing) {
    return (
      <>
        <h1 className="page-title">Health record</h1>
        <Empty
          title="No health record yet"
          action={
            <button className="btn btn-primary" onClick={createRecord}>
              Create my record
            </button>
          }
        >
          <div className="tiny muted">
            Start a record to keep your diagnoses, prescriptions and lab results in one place.
          </div>
        </Empty>
      </>
    );
  }

  if (err || !summary) {
    return (
      <>
        <h1 className="page-title">Health record</h1>
        <Empty title="Could not load your health record">
          {err && <div className="tiny muted">{err}</div>}
        </Empty>
      </>
    );
  }

  return (
    <>
      <div className="split">
        <div>
          <h1 className="page-title">Health record</h1>
          <p className="page-sub">Your medical history, prescriptions and lab results.</p>
        </div>
        <div className="row">
          <StatusBadge status={summary.status} />
          <button className="btn btn-primary" onClick={() => setEntryOpen(true)}>
            + Add entry
          </button>
        </div>
      </div>

      <div className="grid-3" style={{ marginBottom: 20 }}>
        <div className="card">
          <div className="tiny muted">Medical entries</div>
          <div style={{ fontSize: 28, fontWeight: 600 }}>{summary.totalEntries}</div>
        </div>
        <div className="card">
          <div className="tiny muted">Prescriptions</div>
          <div style={{ fontSize: 28, fontWeight: 600 }}>{summary.totalPrescriptions}</div>
        </div>
        <div className="card">
          <div className="tiny muted">Lab results</div>
          <div style={{ fontSize: 28, fontWeight: 600 }}>{summary.totalLabResults}</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h3 style={{ fontSize: 15, marginBottom: 14 }}>Medical entries</h3>
        {entries.length === 0 ? (
          <div className="muted">No entries recorded.</div>
        ) : (
          <div className="table-wrap">
            <table className="t">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Diagnosis</th>
                  <th>Description</th>
                  <th>Provider</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((e) => (
                  <tr key={e.id}>
                    <td><span className="badge badge-neutral">{e.entryType}</span></td>
                    <td>{e.diagnosisCode || "—"}</td>
                    <td>{e.description || "—"}</td>
                    <td>{e.providerName || "—"}</td>
                    <td>{fmtDate(e.entryDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="grid-2">
        <div className="card">
          <h3 style={{ fontSize: 15, marginBottom: 14 }}>Prescriptions</h3>
          {prescriptions.length === 0 ? (
            <div className="muted">No prescriptions.</div>
          ) : (
            <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
              {prescriptions.map((p) => (
                <li key={p.id} style={{ marginBottom: 10 }}>
                  <div style={{ fontWeight: 500 }}>{p.medicationName}</div>
                  <div className="tiny muted">
                    {[p.dosage, p.frequency].filter(Boolean).join(" · ") || "—"}
                    {" · since "}
                    {fmtDate(p.prescribedDate)}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <h3 style={{ fontSize: 15, marginBottom: 14 }}>Lab results</h3>
          {labs.length === 0 ? (
            <div className="muted">No lab results.</div>
          ) : (
            <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
              {labs.map((l) => (
                <li key={l.id} style={{ marginBottom: 10 }}>
                  <div style={{ fontWeight: 500 }}>{l.testName}</div>
                  <div className="tiny muted">
                    {[l.resultValue, l.unit].filter(Boolean).join(" ")}
                    {l.referenceRange ? ` (ref ${l.referenceRange})` : ""}
                    {" · "}
                    {fmtDate(l.testDate)}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <AddEntryModal
        open={entryOpen}
        onClose={() => setEntryOpen(false)}
        onAdded={() => {
          setEntryOpen(false);
          toast.success("Entry added.");
          load();
        }}
      />
    </>
  );
}

function AddEntryModal({
  open,
  onClose,
  onAdded,
}: {
  open: boolean;
  onClose: () => void;
  onAdded: () => void;
}) {
  const empty: AddMedicalEntryRequest = {
    entryType: "VISIT",
    diagnosisCode: "",
    description: "",
    providerName: "",
    entryDate: new Date().toISOString().slice(0, 10),
  };
  const [form, setForm] = useState<AddMedicalEntryRequest>(empty);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setForm({ ...empty, entryDate: new Date().toISOString().slice(0, 10) });
      setErr(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function set<K extends keyof AddMedicalEntryRequest>(k: K, v: AddMedicalEntryRequest[K]) {
    setForm((f) => ({ ...f, [k]: v }));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      await healthRecordApi.addEntry({
        entryType: form.entryType,
        diagnosisCode: form.diagnosisCode || undefined,
        description: form.description || undefined,
        providerName: form.providerName || undefined,
        entryDate: form.entryDate,
      });
      onAdded();
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
      title="Add medical entry"
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="entry-form" className="btn btn-primary" disabled={busy}>
            {busy ? <span className="spinner" /> : "Add entry"}
          </button>
        </>
      }
    >
      <form id="entry-form" onSubmit={submit}>
        <div className="field">
          <label>Type</label>
          <select
            value={form.entryType}
            onChange={(e) => set("entryType", e.target.value as EntryType)}
            style={{ padding: "10px 12px", borderRadius: "var(--radius)", border: "1px solid var(--border-strong)" }}
          >
            {ENTRY_TYPES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Diagnosis code</label>
            <input value={form.diagnosisCode} onChange={(e) => set("diagnosisCode", e.target.value)} />
          </div>
          <div className="field">
            <label>Entry date</label>
            <input
              type="date"
              value={form.entryDate}
              onChange={(e) => set("entryDate", e.target.value)}
              required
            />
          </div>
        </div>
        <div className="field">
          <label>Provider</label>
          <input value={form.providerName} onChange={(e) => set("providerName", e.target.value)} />
        </div>
        <div className="field">
          <label>Description</label>
          <textarea
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            rows={3}
            style={{ padding: "10px 12px", borderRadius: "var(--radius)", border: "1px solid var(--border-strong)" }}
          />
        </div>
        {err && <div className="error">{err}</div>}
      </form>
    </Modal>
  );
}
