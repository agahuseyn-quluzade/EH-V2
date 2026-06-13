import { FormEvent, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { extractError } from "../../api/client";
import { policyApi } from "../../api/policy";
import { Badge, EmptyState, Modal, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { Policy } from "../../types";
import { formatDate, money, policyStatusLabels } from "../../utils/format";

export function PolicyPage() {
  const toast = useToast();

  const [policy, setPolicy] = useState<Policy | null>(null);
  const [loading, setLoading] = useState(true);

  const [showCancel, setShowCancel] = useState(false);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    policyApi
      .myPolicies()
      .then((policies) => {
        const active = policies.find((p) => p.status === "ACTIVE");
        setPolicy(active ?? policies[0] ?? null);
        setLoading(false);
      })
      .catch(() => {
        setPolicy(null);
        setLoading(false);
      });
  }, []);

  useEffect(load, [load]);

  const onCancel = async (e: FormEvent) => {
    e.preventDefault();
    if (!policy) return;
    setBusy(true);
    try {
      await policyApi.cancelPolicy(policy.id);
      toast.success("Insurance cancelled");
      setShowCancel(false);
      load();
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Spinner />;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>My Policy</h1>
          <p>Your active insurance</p>
        </div>
        {policy && policy.status !== "CANCELLED" && (
          <button className="btn btn-danger" onClick={() => setShowCancel(true)}>
            Cancel
          </button>
        )}
      </div>

      {!policy ? (
        <EmptyState
          title="You don't have an active policy"
          hint="You can purchase insurance by choosing a plan."
          action={
            <Link to="/plans" className="btn btn-primary">
              View plans
            </Link>
          }
        />
      ) : (
        <div className="card">
          <div className="card-title">
            <h2>{policy.planName}</h2>
            <Badge status={policy.status} label={policyStatusLabels[policy.status]} />
          </div>
          <dl className="detail-list">
            <div>
              <dt>Policy No.</dt>
              <dd className="mono">{policy.policyNumber}</dd>
            </div>
            <div>
              <dt>Yearly premium</dt>
              <dd>{money(policy.premiumAmount)}</dd>
            </div>
            <div>
              <dt>Start date</dt>
              <dd>{formatDate(policy.startDate)}</dd>
            </div>
            <div>
              <dt>End date</dt>
              <dd>{formatDate(policy.endDate)}</dd>
            </div>
          </dl>
        </div>
      )}

      {showCancel && policy && (
        <Modal title="Cancel insurance" onClose={() => setShowCancel(false)}>
          <div className="alert alert-warning">
            Warning: once cancelled, this policy cannot be restored.
          </div>
          <form onSubmit={onCancel}>
            <p>
              Are you sure you want to cancel your <strong>{policy.planName}</strong> policy?
            </p>
            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowCancel(false)}
              >
                Cancel
              </button>
              <button className="btn btn-danger" disabled={busy}>
                {busy ? "Cancelling..." : "Confirm cancellation"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
