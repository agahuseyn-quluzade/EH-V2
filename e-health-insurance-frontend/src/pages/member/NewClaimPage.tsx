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
      toast.error("Yalnız PDF, JPEG, PNG və WebP faylları qəbul edilir");
      return;
    }
    if (f.size > MAX_SIZE) {
      toast.error("Fayl 10 MB-dan böyük ola bilməz");
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
          toast.error(`İddia yaradıldı, lakin sənəd yüklənmədi: ${extractError(err)}`);
          navigate(`/claims/${claim.id}`);
          return;
        }
      }

      toast.success("İddia uğurla təqdim edildi!");
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
        title="Aktiv sığortanız yoxdur"
        hint="İddia təqdim etmək üçün əvvəlcə sığorta almalısınız."
        action={
          <Link to="/plans" className="btn btn-primary">
            Planlara bax
          </Link>
        }
      />
    );
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Yeni iddia</h1>
          <p>Tibbi xərclərinizin ödənilməsi üçün iddia təqdim edin</p>
        </div>
      </div>

      <div className="grid grid-2" style={{ alignItems: "start" }}>
        <div className="card">
          <h2>İddia məlumatları</h2>
          <form onSubmit={onSubmit}>
            <Field label="İddia növü" required>
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
            <Field label="Məbləğ (AZN)" required>
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={form.amount}
                onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))}
                required
              />
            </Field>
            <Field label="Təsvir" required hint="Müalicənin qısa təsviri">
              <textarea
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                required
                maxLength={1000}
                placeholder="Məs: Stomatoloji müayinə və diş dolgusu..."
              />
            </Field>

            <hr className="divider" />
            <h3>Sübut sənədi (opsional)</h3>
            <p className="muted" style={{ fontSize: "0.82rem" }}>
              PDF, JPEG, PNG və ya WebP — maksimum 10 MB.
            </p>
            <Field label="Fayl">
              <input
                type="file"
                accept=".pdf,.jpg,.jpeg,.png,.webp"
                onChange={(e) => onFileChange(e.target.files?.[0] ?? null)}
              />
            </Field>

            <div className="form-actions">
              <Link to="/claims" className="btn btn-secondary">
                İmtina
              </Link>
              <button className="btn btn-primary" disabled={submitting}>
                {submitting ? "Təqdim edilir..." : "İddianı təqdim et"}
              </button>
            </div>
          </form>
        </div>

        <div className="card">
          <h2>Sığortanız</h2>
          <dl className="detail-list" style={{ gridTemplateColumns: "1fr" }}>
            <div>
              <dt>Plan</dt>
              <dd>{policy.planName}</dd>
            </div>
            <div>
              <dt>Aylıq haqq</dt>
              <dd>{money(policy.premiumAmount)}</dd>
            </div>
            <div>
              <dt>Müqavilə №</dt>
              <dd className="mono">{policy.policyNumber}</dd>
            </div>
          </dl>
          <div className="alert alert-info" style={{ marginTop: 14 }}>
            İddianız avtomatik qiymətləndirilir: fırıldaqçılıq analizi aparılır.
            Nəticə bir neçə saniyəyə hazır olur, bəzi hallarda əməkdaş baxışı
            tələb oluna bilər.
          </div>
        </div>
      </div>
    </>
  );
}
