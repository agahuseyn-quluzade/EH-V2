import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extractError } from "../../api/client";
import { policyApi } from "../../api/policy";
import { EmptyState, Modal, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { Plan } from "../../types";
import { money } from "../../utils/format";

export function PlansPage() {
  const toast = useToast();
  const navigate = useNavigate();

  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [purchasePlan, setPurchasePlan] = useState<Plan | null>(null);
  const [purchasing, setPurchasing] = useState(false);

  useEffect(() => {
    policyApi
      .listPlans()
      .then(setPlans)
      .catch((e) => toast.error(extractError(e)))
      .finally(() => setLoading(false));
  }, [toast]);

  const activePlans = useMemo(() => plans.filter((p) => p.active), [plans]);

  const onPurchase = async (e: FormEvent) => {
    e.preventDefault();
    if (!purchasePlan) return;
    setPurchasing(true);
    try {
      await policyApi.purchase({ planId: purchasePlan.id });
      toast.success("Sığorta uğurla alındı!");
      setPurchasePlan(null);
      navigate("/policy");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setPurchasing(false);
    }
  };

  if (loading) return <Spinner />;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Sığorta planları</h1>
          <p>Sizə uyğun sağlamlıq sığortası planını seçin</p>
        </div>
      </div>

      {activePlans.length === 0 ? (
        <EmptyState title="Aktiv plan tapılmadı" hint="Hazırda satışda plan yoxdur." />
      ) : (
        <div className="grid grid-3">
          {activePlans.map((plan) => (
            <div key={plan.id} className="card plan-card">
              <h3 className="mb-0">{plan.name}</h3>
              {plan.description && (
                <p className="muted" style={{ fontSize: "0.83rem" }}>
                  {plan.description}
                </p>
              )}
              <div className="plan-price">
                {money(plan.premiumAmount)} <small>/ ay</small>
              </div>
              <ul className="plan-features">
                <li>
                  <span>Əhatə məbləği</span>
                  <span>{money(plan.coverageAmount)}</span>
                </li>
                <li>
                  <span>Müddət</span>
                  <span>{plan.durationMonths} ay</span>
                </li>
              </ul>
              <div className="plan-actions">
                <button
                  className="btn btn-primary"
                  onClick={() => setPurchasePlan(plan)}
                >
                  Sığorta al
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {purchasePlan && (
        <Modal title={`"${purchasePlan.name}" planını al`} onClose={() => setPurchasePlan(null)}>
          <form onSubmit={onPurchase}>
            <div className="alert alert-info">
              Aylıq haqq: <strong>{money(purchasePlan.premiumAmount)}</strong> •
              Müddət: <strong>{purchasePlan.durationMonths} ay</strong>
            </div>
            <p className="muted" style={{ fontSize: "0.85rem" }}>
              Sığorta alındıqdan sonra ödəniş emalı başlayacaq. Ödəniş tamamlandıqdan
              sonra sığortanız aktiv olacaq.
            </p>
            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setPurchasePlan(null)}
              >
                İmtina
              </button>
              <button className="btn btn-primary" disabled={purchasing}>
                {purchasing ? "Alınır..." : "Təsdiq et və al"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
