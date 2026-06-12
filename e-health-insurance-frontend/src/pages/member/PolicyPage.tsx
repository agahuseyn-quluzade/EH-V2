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
        // Show the first active policy, or the first one if no active policy
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
      toast.success("Sığorta ləğv edildi");
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
          <h1>Mənim sığortam</h1>
          <p>Aktiv sığortanız</p>
        </div>
        {policy && policy.status !== "CANCELLED" && (
          <button className="btn btn-danger" onClick={() => setShowCancel(true)}>
            Ləğv et
          </button>
        )}
      </div>

      {!policy ? (
        <EmptyState
          title="Aktiv sığortanız yoxdur"
          hint="Plan seçərək sığorta ala bilərsiniz."
          action={
            <Link to="/plans" className="btn btn-primary">
              Planlara bax
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
              <dt>Müqavilə №</dt>
              <dd className="mono">{policy.policyNumber}</dd>
            </div>
            <div>
              <dt>Aylıq haqq</dt>
              <dd>{money(policy.premiumAmount)}</dd>
            </div>
            <div>
              <dt>Başlama</dt>
              <dd>{formatDate(policy.startDate)}</dd>
            </div>
            <div>
              <dt>Bitmə</dt>
              <dd>{formatDate(policy.endDate)}</dd>
            </div>
            <div>
              <dt>Qeydiyyat tarixi</dt>
              <dd>{formatDate(policy.createdAt)}</dd>
            </div>
          </dl>
        </div>
      )}

      {showCancel && policy && (
        <Modal title="Sığortanı ləğv et" onClose={() => setShowCancel(false)}>
          <div className="alert alert-warning">
            Diqqət: ləğv edildikdən sonra sığorta bərpa olunmur.
          </div>
          <form onSubmit={onCancel}>
            <p>
              <strong>{policy.planName}</strong> planı üzrə sığortanızı ləğv etmək
              istədiyinizə əminsiniz?
            </p>
            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowCancel(false)}
              >
                İmtina
              </button>
              <button className="btn btn-danger" disabled={busy}>
                {busy ? "Ləğv edilir..." : "Ləğvi təsdiqlə"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
