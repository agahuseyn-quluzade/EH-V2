import { FormEvent, useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { aiApi } from "../../api/ai";
import { claimApi } from "../../api/claim";
import { extractError } from "../../api/client";
import { Badge, ErrorState, Field, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { Claim, FraudAiResponse } from "../../types";
import {
  claimStatusLabels,
  claimTypeLabels,
  formatDateTime,
  money,
} from "../../utils/format";

export function StaffClaimReviewPage() {
  const { id } = useParams<{ id: string }>();
  const toast = useToast();
  const navigate = useNavigate();

  const [claim, setClaim] = useState<Claim | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [decision, setDecision] = useState<"APPROVED" | "REJECTED">("APPROVED");
  const [rejectionReason, setRejectionReason] = useState("");
  const [approvedAmount, setApprovedAmount] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [fraudResult, setFraudResult] = useState<FraudAiResponse | null>(null);
  const [analyzing, setAnalyzing] = useState(false);

  const load = useCallback(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    claimApi.get(id).then((c) => {
      setClaim(c);
      if (c.amount) setApprovedAmount(String(c.amount));
      setLoading(false);
    }).catch((e) => {
      setError(extractError(e));
      setLoading(false);
    });
  }, [id]);

  useEffect(load, [load]);

  const onAnalyze = async () => {
    if (!id) return;
    setAnalyzing(true);
    try {
      setFraudResult(await aiApi.analyzeClaim(id));
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setAnalyzing(false);
    }
  };

  const onReview = async (e: FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setSubmitting(true);
    try {
      await claimApi.review(id, {
        decision,
        approvedAmount:
          decision === "APPROVED" && approvedAmount ? Number(approvedAmount) : undefined,
        rejectionReason:
          decision === "REJECTED" ? rejectionReason.trim() : undefined,
      });
      toast.success(decision === "APPROVED" ? "İddia təsdiqləndi" : "İddia rədd edildi");
      navigate("/staff/queue");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Spinner />;
  if (error || !claim)
    return <ErrorState message={error ?? "İddia tapılmadı"} onRetry={load} />;

  const reviewable =
    claim.status === "SUBMITTED" || claim.status === "UNDER_REVIEW";

  return (
    <>
      <div className="page-header">
        <div>
          <h1>İddia baxışı</h1>
          <p className="mono">{claim.claimNumber}</p>
        </div>
        <Link to="/staff/queue" className="btn btn-secondary">
          ← Növbəyə qayıt
        </Link>
      </div>

      <div className="grid grid-2" style={{ alignItems: "start" }}>
        <div>
          <div className="card">
            <div className="card-title">
              <h2>İddia məlumatları</h2>
              <Badge status={claim.status} label={claimStatusLabels[claim.status]} />
            </div>
            <dl className="detail-list">
              <div>
                <dt>İstifadəçi ID</dt>
                <dd className="mono">{claim.userId}</dd>
              </div>
              <div>
                <dt>Sığorta ID</dt>
                <dd className="mono">{claim.policyId}</dd>
              </div>
              <div>
                <dt>İddia növü</dt>
                <dd>{claimTypeLabels[claim.claimType] ?? claim.claimType}</dd>
              </div>
              <div>
                <dt>Məbləğ</dt>
                <dd>
                  <strong>{money(claim.amount)}</strong>
                </dd>
              </div>
              <div>
                <dt>Təsvir</dt>
                <dd>{claim.description}</dd>
              </div>
              <div>
                <dt>Tarix</dt>
                <dd>{formatDateTime(claim.createdAt)}</dd>
              </div>
              {claim.riskScore != null && (
                <div>
                  <dt>Risk balı</dt>
                  <dd>{claim.riskScore}/100</dd>
                </div>
              )}
            </dl>

            {claim.fraudFlags && claim.fraudFlags.length > 0 && (
              <>
                <hr className="divider" />
                <h3>Fırıldaqçılıq siqnalları</h3>
                <div className="chip-row">
                  {claim.fraudFlags.map((f) => (
                    <span key={f} className="chip">
                      {f}
                    </span>
                  ))}
                </div>
                {claim.aiExplanation && (
                  <div className="alert alert-info" style={{ marginTop: 10 }}>
                    {claim.aiExplanation}
                  </div>
                )}
              </>
            )}
          </div>

          <div className="card">
            <div className="card-title">
              <h2>AI fırıldaqçılıq analizi</h2>
              <button
                className="btn btn-secondary btn-sm"
                onClick={onAnalyze}
                disabled={analyzing}
              >
                {analyzing ? "Analiz edilir..." : "🤖 Yenidən analiz et"}
              </button>
            </div>
            {!fraudResult ? (
              <p className="muted">
                Yenidən analiz üçün düyməyə basın.
              </p>
            ) : (
              <>
                <dl className="detail-list">
                  <div>
                    <dt>Yekun bal</dt>
                    <dd>{fraudResult.finalScore}/100</dd>
                  </div>
                  {fraudResult.aiScore != null && (
                    <div>
                      <dt>AI balı</dt>
                      <dd>{fraudResult.aiScore}/100</dd>
                    </div>
                  )}
                  <div>
                    <dt>Qayda balı</dt>
                    <dd>{fraudResult.ruleScore}/100</dd>
                  </div>
                </dl>
                {fraudResult.flags && fraudResult.flags.length > 0 && (
                  <div className="chip-row" style={{ marginTop: 10 }}>
                    {fraudResult.flags.map((f) => (
                      <span key={f} className="chip">
                        {f}
                      </span>
                    ))}
                  </div>
                )}
                {fraudResult.aiExplanation && (
                  <div className="alert alert-info" style={{ marginTop: 10 }}>
                    {fraudResult.aiExplanation}
                  </div>
                )}
              </>
            )}
          </div>
        </div>

        <div className="card">
          <h2>Qərar ver</h2>
          {!reviewable ? (
            <div className="alert alert-warning">
              Bu iddia üzrə qərar artıq verilib ({claimStatusLabels[claim.status]}).
            </div>
          ) : (
            <form onSubmit={onReview}>
              <Field label="Qərar" required>
                <select
                  value={decision}
                  onChange={(e) =>
                    setDecision(e.target.value as "APPROVED" | "REJECTED")
                  }
                >
                  <option value="APPROVED">Təsdiqlə</option>
                  <option value="REJECTED">Rədd et</option>
                </select>
              </Field>
              {decision === "APPROVED" && (
                <Field
                  label="Təsdiqlənən məbləğ (AZN)"
                  hint="Boş saxlasanız tam məbləğ təsdiqlənəcək"
                >
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={approvedAmount}
                    onChange={(e) => setApprovedAmount(e.target.value)}
                  />
                </Field>
              )}
              {decision === "REJECTED" && (
                <Field label="Rədd səbəbi" required>
                  <textarea
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    required
                    placeholder="Rədd etmənin əsaslandırılması..."
                  />
                </Field>
              )}
              <button
                className={`btn btn-block ${decision === "APPROVED" ? "btn-success" : "btn-danger"}`}
                disabled={submitting || (decision === "REJECTED" && !rejectionReason.trim())}
              >
                {submitting
                  ? "Göndərilir..."
                  : decision === "APPROVED"
                    ? "✓ Təsdiqlə"
                    : "✕ Rədd et"}
              </button>
            </form>
          )}
        </div>
      </div>
    </>
  );
}
