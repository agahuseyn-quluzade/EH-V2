import { FormEvent, useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { claimApi } from "../../api/claim";
import { extractError } from "../../api/client";
import { Badge, ErrorState, Field, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { Claim } from "../../types";
import {
  claimStatusLabels,
  claimTypeLabels,
  formatDateTime,
  money,
} from "../../utils/format";

export function ClaimDetailPage() {
  const { id } = useParams<{ id: string }>();
  const toast = useToast();

  const [claim, setClaim] = useState<Claim | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  const load = useCallback(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    claimApi.get(id).then((c) => {
      setClaim(c);
      setLoading(false);
    }).catch((e) => {
      setError(extractError(e));
      setLoading(false);
    });
  }, [id]);

  useEffect(load, [load]);

  const onUpload = async (e: FormEvent) => {
    e.preventDefault();
    if (!id || !file) return;
    setUploading(true);
    try {
      await claimApi.uploadEvidence(id, file);
      toast.success("Sənəd yükləndi");
      setFile(null);
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setUploading(false);
    }
  };

  if (loading) return <Spinner />;
  if (error || !claim)
    return <ErrorState message={error ?? "İddia tapılmadı"} onRetry={load} />;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>İddia detalları</h1>
          <p className="mono">{claim.claimNumber}</p>
        </div>
        <Link to="/claims" className="btn btn-secondary">
          ← Geri
        </Link>
      </div>

      <div className="card">
        <div className="card-title">
          <h2>Ümumi məlumat</h2>
          <Badge status={claim.status} label={claimStatusLabels[claim.status]} />
        </div>
        <dl className="detail-list">
          <div>
            <dt>İddia növü</dt>
            <dd>{claimTypeLabels[claim.claimType] ?? claim.claimType}</dd>
          </div>
          <div>
            <dt>Məbləğ</dt>
            <dd>{money(claim.amount)}</dd>
          </div>
          <div>
            <dt>Təsvir</dt>
            <dd>{claim.description}</dd>
          </div>
          <div>
            <dt>Təqdim edilib</dt>
            <dd>{formatDateTime(claim.createdAt)}</dd>
          </div>
          {claim.riskScore != null && (
            <div>
              <dt>Risk balı</dt>
              <dd>{claim.riskScore}/100</dd>
            </div>
          )}
        </dl>
      </div>

      {(claim.approvedAmount != null || claim.rejectionReason) && (
        <div className="card">
          <h2>Qərar</h2>
          <dl className="detail-list">
            {claim.approvedAmount != null && (
              <div>
                <dt>Təsdiqlənən məbləğ</dt>
                <dd>{money(claim.approvedAmount)}</dd>
              </div>
            )}
            {claim.rejectionReason && (
              <div>
                <dt>Rədd səbəbi</dt>
                <dd>{claim.rejectionReason}</dd>
              </div>
            )}
            {claim.reviewedBy && (
              <div>
                <dt>Baxan əməkdaş</dt>
                <dd className="mono">{claim.reviewedBy}</dd>
              </div>
            )}
          </dl>
        </div>
      )}

      {claim.fraudFlags && claim.fraudFlags.length > 0 && (
        <div className="card">
          <h2>Fırıldaqçılıq analizi</h2>
          <dl className="detail-list">
            {claim.riskScore != null && (
              <div>
                <dt>Risk balı</dt>
                <dd>{claim.riskScore}/100</dd>
              </div>
            )}
          </dl>
          <div className="chip-row" style={{ marginTop: 10 }}>
            {claim.fraudFlags.map((f) => (
              <span key={f} className="chip">
                {f}
              </span>
            ))}
          </div>
          {claim.aiExplanation && (
            <div className="alert alert-info" style={{ marginTop: 12 }}>
              {claim.aiExplanation}
            </div>
          )}
        </div>
      )}

      <div className="card">
        <h2>Sübut sənədi yüklə</h2>
        <form onSubmit={onUpload}>
          <Field label="Fayl" hint="PDF, JPEG, PNG, WebP — maks 10 MB">
            <input
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.webp"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </Field>
          <button className="btn btn-primary" disabled={!file || uploading}>
            {uploading ? "Yüklənir..." : "Sənəd yüklə"}
          </button>
        </form>
      </div>
    </>
  );
}
