import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { claimApi, extractError } from "../../api";
import { Claim, Evidence } from "../../types";
import { fmtDate, fmtDateTime, fmtMoney } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Loader } from "../../components/Loader";
import { Modal } from "../../components/Modal";
import { useToast } from "../../context/ToastContext";
import { useAuth } from "../../context/AuthContext";

interface Props {
  staffView?: boolean;
}

export function ClaimDetailPage({ staffView }: Props) {
  const { id = "" } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const toast = useToast();

  const [claim, setClaim] = useState<Claim | null>(null);
  const [evidence, setEvidence] = useState<Evidence[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const [reviewOpen, setReviewOpen] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const [c, ev] = await Promise.all([
        claimApi.get(id),
        claimApi.listEvidence(id).catch(() => []),
      ]);
      setClaim(c);
      setEvidence(ev);
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (id) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (loading) return <Loader />;
  if (err || !claim) {
    return (
      <>
        <Link to={staffView ? "/staff" : "/claims"} className="muted tiny">
          ← Back
        </Link>
        <h1 className="page-title" style={{ marginTop: 8 }}>Claim not found</h1>
        {err && <div className="tiny muted">{err}</div>}
      </>
    );
  }

  const isStaff = user?.role === "STAFF" || user?.role === "ADMIN";

  return (
    <>
      <Link to={staffView ? "/staff" : "/claims"} className="muted tiny">
        ← Back to {staffView ? "review queue" : "claims"}
      </Link>

      <div className="split" style={{ marginTop: 8 }}>
        <div>
          <h1 className="page-title">Claim {claim.id.slice(0, 8)}</h1>
          <p className="page-sub">
            {claim.procedureCode} · {fmtDate(claim.serviceDate)} · {claim.providerName}
          </p>
        </div>
        <div className="row">
          <StatusBadge status={claim.status} />
          {staffView && isStaff && claim.status === "MANUAL_REVIEW" && (
            <button className="btn btn-primary" onClick={() => setReviewOpen(true)}>
              Review claim
            </button>
          )}
        </div>
      </div>

      <div className="grid-2" style={{ marginBottom: 20 }}>
        <div className="card">
          <h3 style={{ fontSize: 15, marginBottom: 14 }}>Claim details</h3>
          <dl className="kv">
            <dt>Amount claimed</dt>
            <dd>{fmtMoney(claim.amount)}</dd>
            <dt>Amount approved</dt>
            <dd>{fmtMoney(claim.approvedAmount ?? null)}</dd>
            <dt>Procedure code</dt>
            <dd>{claim.procedureCode}</dd>
            <dt>Diagnosis code</dt>
            <dd>{claim.diagnosisCode || "—"}</dd>
            <dt>Provider</dt>
            <dd>{claim.providerName}</dd>
            <dt>Service date</dt>
            <dd>{fmtDate(claim.serviceDate)}</dd>
            <dt>Submitted</dt>
            <dd>{fmtDateTime(claim.submittedAt)}</dd>
            <dt>Decided</dt>
            <dd>{fmtDateTime(claim.decidedAt)}</dd>
          </dl>
        </div>

        <div className="card">
          <h3 style={{ fontSize: 15, marginBottom: 14 }}>Decision</h3>
          {claim.status === "PENDING" ? (
            <div className="muted">
              We're processing your claim. You'll get an email when it's decided.
            </div>
          ) : (
            <>
              <div style={{ marginBottom: 12 }}>
                <StatusBadge status={claim.status} />
              </div>
              {claim.fraudScore != null && (
                <div className="kv" style={{ marginBottom: 12 }}>
                  <dt>Fraud score</dt>
                  <dd>
                    <span
                      className={
                        "badge badge-" +
                        (claim.fraudScore > 0.6
                          ? "danger"
                          : claim.fraudScore > 0.3
                          ? "warn"
                          : "success")
                      }
                    >
                      {claim.fraudScore.toFixed(2)}
                    </span>
                  </dd>
                </div>
              )}
              <div style={{ fontSize: 13.5, lineHeight: 1.55 }}>
                {claim.decisionReason || "No detailed reason provided."}
              </div>
            </>
          )}
        </div>
      </div>

      <div className="card">
        <h3 style={{ fontSize: 15, marginBottom: 14 }}>Supporting evidence</h3>
        {evidence.length === 0 ? (
          <div className="muted">No documents attached.</div>
        ) : (
          <div className="table-wrap">
            <table className="t">
              <thead>
                <tr>
                  <th>File</th>
                  <th>Type</th>
                  <th>Size</th>
                  <th>Uploaded</th>
                </tr>
              </thead>
              <tbody>
                {evidence.map((e) => (
                  <tr key={e.id}>
                    <td>{e.originalFileName}</td>
                    <td><span className="badge badge-neutral">{e.fileType}</span></td>
                    <td>{(e.fileSize / 1024).toFixed(0)} KB</td>
                    <td>{fmtDateTime(e.uploadedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ReviewModal
        open={reviewOpen}
        onClose={() => setReviewOpen(false)}
        claim={claim}
        onReviewed={() => {
          setReviewOpen(false);
          toast.success("Decision recorded.");
          load();
          if (staffView) navigate("/staff");
        }}
      />
    </>
  );
}

function ReviewModal({
  open,
  onClose,
  claim,
  onReviewed,
}: {
  open: boolean;
  onClose: () => void;
  claim: Claim;
  onReviewed: () => void;
}) {
  const [status, setStatus] = useState<"APPROVED" | "REJECTED">("APPROVED");
  const [reason, setReason] = useState("");
  const [approvedAmount, setApprovedAmount] = useState(String(claim.amount));
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      await claimApi.review(claim.id, {
        decision: status,
        approvedAmount:
          status === "APPROVED" ? Number(approvedAmount) : undefined,
        reason,
      });
      onReviewed();
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
      title="Review claim"
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="review-form" className="btn btn-primary" disabled={busy}>
            {busy ? <span className="spinner" /> : "Record decision"}
          </button>
        </>
      }
    >
      <form id="review-form" onSubmit={submit}>
        <div className="field">
          <label>Decision</label>
          <div className="row">
            <label className="row" style={{ gap: 6 }}>
              <input
                type="radio"
                checked={status === "APPROVED"}
                onChange={() => setStatus("APPROVED")}
              />
              Approve
            </label>
            <label className="row" style={{ gap: 6 }}>
              <input
                type="radio"
                checked={status === "REJECTED"}
                onChange={() => setStatus("REJECTED")}
              />
              Reject
            </label>
          </div>
        </div>

        {status === "APPROVED" && (
          <div className="field">
            <label>Approved amount (USD)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={approvedAmount}
              onChange={(e) => setApprovedAmount(e.target.value)}
              required
            />
            <div className="hint">Claimed: {fmtMoney(claim.amount)}</div>
          </div>
        )}

        <div className="field">
          <label>Reason</label>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            required
            style={{ padding: "10px 12px", borderRadius: "var(--radius)", border: "1px solid var(--border-strong)" }}
          />
          <div className="hint">This will be shown to the member.</div>
        </div>

        {err && <div className="error">{err}</div>}
      </form>
    </Modal>
  );
}
