import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { claimApi } from "../../api/claim";
import { extractError } from "../../api/client";
import { policyApi } from "../../api/policy";
import { EmptyState, Field, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { ClaimType, Policy } from "../../types";
import { claimTypeLabels, money } from "../../utils/format";

const ALLOWED_TYPES = ["application/pdf", "image/jpeg", "image/png", "image/webp"];
const MAX_SIZE = 10 * 1024 * 1024;

const CLAIM_TYPES: ClaimType[] = ["HOSPITALIZATION", "MEDICATION", "DENTAL", "CONSULTATION"];

export function NewClaimPage() {
  const toast = useToast();
  const navigate = useNavigate();

  const [policy, setPolicy] = useState<Policy | null>(null);
  const [loading, setLoading] = useState(true);

  const [form, setForm] = useState({
    claimType: "CONSULTATION" as ClaimType,
    amount: "",
    description: "",
  });
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    policyApi
      .myPolicies()
      .then((policies) => {
        const active = policies.find((p) => p.status === "ACTIVE");
        setPolicy(active ?? null);
      })
      .catch(() => setPolicy(null))
      .finally(() => setLoading(false));
  }, []);

  const onFileChange = (f: File | null) => {
    if (!f) return setFile(null);
    if (!ALLOWED_TYPES.includes(f.type)) {
      toast.error("Only PDF, JPEG, PNG, and WebP files are accepted");
      return;
    }
    if (f.size > MAX_SIZE) {
      toast.error("File cannot be larger than 10 MB");
      return;
    }
    setFile(f);
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!policy) return;
    setSubmitting(true);
    try {
      const claim = await claimApi.submit({
        policyId: policy.id,
        claimType: form.claimType,
        amount: Number(form.amount),
        description: form.description.trim(),
      });

      if (file) {
        try {
          await claimApi.uploadEvidence(claim.id, file);
        } catch (err) {
          toast.error(`Claim created, but the document failed to upload: ${extractError(err)}`);
          navigate(`/claims/${claim.id}`);
          return;
        }
      }

      toast.success("Claim submitted successfully!");
      navigate(`/claims/${claim.id}`);
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Spinner />;

  if (!policy) {
    return (
      <EmptyState
        title="You don't have an active policy"
        hint="You need to purchase insurance before you can submit a claim."
        action={
          <Link to="/plans" className="btn btn-primary">
            View plans
          </Link>
        }
      />
    );
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>New Claim</h1>
          <p>Submit a claim to get reimbursed for your medical expenses</p>
        </div>
      </div>

      <div className="grid grid-2" style={{ alignItems: "start" }}>
        <div className="card">
          <h2>Claim information</h2>
          <form onSubmit={onSubmit}>
            <Field label="Claim type" required>
              <select
                value={form.claimType}
                onChange={(e) =>
                  setForm((f) => ({ ...f, claimType: e.target.value as ClaimType }))
                }
              >
                {CLAIM_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {claimTypeLabels[t]}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Amount (USD)" required>
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={form.amount}
                onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))}
                required
              />
            </Field>
            <Field label="Description" required hint="A short description of the treatment">
              <textarea
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                required
                maxLength={1000}
                placeholder="e.g. Dental check-up and filling..."
              />
            </Field>

            <hr className="divider" />
            <h3>Evidence document (optional)</h3>
            <p className="muted" style={{ fontSize: "0.82rem" }}>
              PDF, JPEG, PNG, or WebP — max 10 MB.
            </p>
            <Field label="File">
              <input
                type="file"
                accept=".pdf,.jpg,.jpeg,.png,.webp"
                onChange={(e) => onFileChange(e.target.files?.[0] ?? null)}
              />
            </Field>

            <div className="form-actions">
              <Link to="/claims" className="btn btn-secondary">
                Cancel
              </Link>
              <button className="btn btn-primary" disabled={submitting}>
                {submitting ? "Submitting..." : "Submit claim"}
              </button>
            </div>
          </form>
        </div>

        <div className="card">
          <h2>Your Policy</h2>
          <dl className="detail-list" style={{ gridTemplateColumns: "1fr" }}>
            <div>
              <dt>Plan</dt>
              <dd>{policy.planName}</dd>
            </div>
            <div>
              <dt>Monthly premium</dt>
              <dd>{money(policy.premiumAmount)}</dd>
            </div>
            <div>
              <dt>Policy No.</dt>
              <dd className="mono">{policy.policyNumber}</dd>
            </div>
          </dl>
          <div className="alert alert-info" style={{ marginTop: 14 }}>
            Your claim is evaluated automatically: a fraud analysis is run.
            The result is ready within a few seconds; in some cases staff
            review may be required.
          </div>
        </div>
      </div>
    </>
  );
}
