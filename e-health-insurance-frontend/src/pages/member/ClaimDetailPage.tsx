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
      toast.success("Document uploaded");
      setFile(null);
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setUploading(false);
    }
  };

  if (loading) return <Spinner />;
  if (error || !claim)
    return <ErrorState message={error ?? "Claim not found"} onRetry={load} />;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Claim Details</h1>
          <p className="mono">{claim.claimNumber}</p>
        </div>
        <Link to="/claims" className="btn btn-secondary">
          ← Back
        </Link>
      </div>

      <div className="card">
        <div className="card-title">
          <h2>General information</h2>
          <Badge status={claim.status} label={claimStatusLabels[claim.status]} />
        </div>
        <dl className="detail-list">
          <div>
            <dt>Claim type</dt>
            <dd>{claimTypeLabels[claim.claimType] ?? claim.claimType}</dd>
          </div>
          <div>
            <dt>Amount</dt>
            <dd>{money(claim.amount)}</dd>
          </div>
          <div>
            <dt>Description</dt>
            <dd>{claim.description}</dd>
          </div>
          <div>
            <dt>Submitted</dt>
            <dd>{formatDateTime(claim.createdAt)}</dd>
          </div>
          {claim.riskScore != null && (
            <div>
              <dt>Risk score</dt>
              <dd>{claim.riskScore}/100</dd>
            </div>
          )}
        </dl>
      </div>

      {(claim.approvedAmount != null || claim.rejectionReason) && (
        <div className="card">
          <h2>Decision</h2>
          <dl className="detail-list">
            {claim.approvedAmount != null && (
              <div>
                <dt>Approved amount</dt>
                <dd>{money(claim.approvedAmount)}</dd>
              </div>
            )}
            {claim.rejectionReason && (
              <div>
                <dt>Rejection reason</dt>
                <dd>{claim.rejectionReason}</dd>
              </div>
            )}
            {claim.reviewedBy && (
              <div>
                <dt>Reviewed by</dt>
                <dd className="mono">{claim.reviewedBy}</dd>
              </div>
            )}
          </dl>
        </div>
      )}

      {claim.fraudFlags && claim.fraudFlags.length > 0 && (
        <div className="card">
          <h2>Fraud analysis</h2>
          <dl className="detail-list">
            {claim.riskScore != null && (
              <div>
                <dt>Risk score</dt>
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
        <h2>Upload evidence</h2>
        <form onSubmit={onUpload}>
          <Field label="File" hint="PDF, JPEG, PNG, WebP — max 10 MB">
            <input
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.webp"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </Field>
          <button className="btn btn-primary" disabled={!file || uploading}>
            {uploading ? "Uploading..." : "Upload document"}
          </button>
        </form>
      </div>
    </>
  );
}
