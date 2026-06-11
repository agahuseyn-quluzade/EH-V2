import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { claimApi, extractError, policyApi } from "../../api";
import { EvidenceFileType, Policy } from "../../types";
import { useToast } from "../../context/ToastContext";

interface PendingFile {
  file: File;
  fileType: EvidenceFileType;
}

const FILE_TYPES: { value: EvidenceFileType; label: string }[] = [
  { value: "BILL", label: "Medical bill" },
  { value: "PRESCRIPTION", label: "Prescription" },
  { value: "REPORT", label: "Medical report" },
  { value: "OTHER", label: "Other" },
];

export function NewClaimPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [policy, setPolicy] = useState<Policy | null>(null);
  const [policyLoading, setPolicyLoading] = useState(true);

  const [amount, setAmount] = useState("");
  const [procedureCode, setProcedureCode] = useState("");
  const [diagnosisCode, setDiagnosisCode] = useState("");
  const [providerName, setProviderName] = useState("");
  const [serviceDate, setServiceDate] = useState("");
  const [pending, setPending] = useState<PendingFile[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    policyApi
      .myPolicy()
      .then((p) => mounted && setPolicy(p))
      .catch(() => {})
      .finally(() => mounted && setPolicyLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  function handleFiles(files: FileList | null) {
    if (!files) return;
    const next: PendingFile[] = [];
    for (const f of Array.from(files)) {
      next.push({ file: f, fileType: guessFileType(f.name) });
    }
    setPending((p) => [...p, ...next]);
  }

  function guessFileType(name: string): EvidenceFileType {
    const n = name.toLowerCase();
    if (n.includes("bill") || n.includes("invoice")) return "BILL";
    if (n.includes("rx") || n.includes("prescription")) return "PRESCRIPTION";
    if (n.includes("report") || n.includes("result")) return "REPORT";
    return "OTHER";
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErr(null);
    if (!policy) {
      setErr("You must have an active policy to submit a claim.");
      return;
    }
    setSubmitting(true);
    try {
      const claim = await claimApi.submit({
        policyId: policy.id,
        amount: Number(amount),
        procedureCode,
        diagnosisCode: diagnosisCode || undefined,
        providerName,
        serviceDate,
      });

      // Upload evidence sequentially so we can surface individual failures.
      for (const p of pending) {
        try {
          await claimApi.uploadEvidence(claim.id, p.file, p.fileType);
        } catch (e) {
          toast.error(`Could not upload ${p.file.name}: ${extractError(e)}`);
        }
      }

      toast.success("Claim submitted. We'll let you know the decision shortly.");
      navigate(`/claims/${claim.id}`, { replace: true });
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Submit a claim</h1>
      <p className="page-sub">
        Tell us about the care you received. We'll check coverage and review
        for fraud automatically.
      </p>

      {!policyLoading && !policy && (
        <div
          className="card"
          style={{ background: "var(--amber-50)", borderColor: "var(--amber-200)", marginBottom: 20 }}
        >
          You don't have an active policy. <a href="/plans">Pick a plan</a> first to submit claims.
        </div>
      )}

      <form onSubmit={onSubmit}>
        <div className="grid-2" style={{ alignItems: "flex-start" }}>
          <div className="card">
            <h3 style={{ fontSize: 15, marginBottom: 14 }}>Care details</h3>

            <div className="field">
              <label>Provider name</label>
              <input
                value={providerName}
                onChange={(e) => setProviderName(e.target.value)}
                placeholder="e.g. City Medical Center"
                required
              />
            </div>
            <div className="grid-2">
              <div className="field">
                <label>Service date</label>
                <input
                  type="date"
                  value={serviceDate}
                  onChange={(e) => setServiceDate(e.target.value)}
                  required
                />
              </div>
              <div className="field">
                <label>Amount (USD)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="grid-2">
              <div className="field">
                <label>Procedure code</label>
                <input
                  value={procedureCode}
                  onChange={(e) => setProcedureCode(e.target.value)}
                  placeholder="e.g. SUR01"
                  required
                />
              </div>
              <div className="field">
                <label>Diagnosis code</label>
                <input
                  value={diagnosisCode}
                  onChange={(e) => setDiagnosisCode(e.target.value)}
                  placeholder="e.g. K35 (optional)"
                />
              </div>
            </div>
          </div>

          <div className="card">
            <h3 style={{ fontSize: 15, marginBottom: 14 }}>Supporting evidence</h3>
            <p className="tiny muted" style={{ marginBottom: 12 }}>
              Upload your bill, prescription, or medical report. Our AI will
              extract the details for faster processing.
            </p>

            <label className="dropzone">
              <input
                type="file"
                multiple
                style={{ display: "none" }}
                onChange={(e) => handleFiles(e.target.files)}
              />
              <div style={{ fontSize: 32, color: "var(--text-tertiary)" }}>↑</div>
              <div style={{ fontWeight: 500, marginTop: 6 }}>Click to upload files</div>
              <div className="tiny muted">PDF, JPG, PNG up to 10MB</div>
            </label>

            {pending.length > 0 && (
              <div className="stack" style={{ marginTop: 16, gap: 8 }}>
                {pending.map((p, i) => (
                  <div
                    key={i}
                    style={{
                      display: "flex",
                      gap: 10,
                      alignItems: "center",
                      padding: 10,
                      background: "var(--bg-muted)",
                      borderRadius: "var(--radius)",
                    }}
                  >
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 500, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                        {p.file.name}
                      </div>
                      <div className="tiny muted">{(p.file.size / 1024).toFixed(0)} KB</div>
                    </div>
                    <select
                      value={p.fileType}
                      onChange={(e) =>
                        setPending((arr) =>
                          arr.map((item, idx) =>
                            idx === i
                              ? { ...item, fileType: e.target.value as EvidenceFileType }
                              : item
                          )
                        )
                      }
                      style={{
                        padding: "6px 8px",
                        border: "1px solid var(--border-strong)",
                        borderRadius: 6,
                        background: "var(--bg-surface)",
                      }}
                    >
                      {FILE_TYPES.map((t) => (
                        <option key={t.value} value={t.value}>{t.label}</option>
                      ))}
                    </select>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      onClick={() => setPending((arr) => arr.filter((_, idx) => idx !== i))}
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {err && (
          <div
            className="card"
            style={{ background: "var(--red-50)", borderColor: "var(--red-200)", marginTop: 16, color: "var(--red-800)" }}
          >
            {err}
          </div>
        )}

        <div className="row-end" style={{ marginTop: 20 }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate(-1)}
          >
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting || !policy}>
            {submitting ? <span className="spinner" /> : "Submit claim"}
          </button>
        </div>
      </form>
    </>
  );
}
