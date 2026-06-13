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
  const [detailsPlan, setDetailsPlan] = useState<Plan | null>(null);

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
      toast.success("Insurance purchased successfully!");
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
          <h1>Insurance Plans</h1>
          <p>Choose the health insurance plan that suits you</p>
        </div>
      </div>

      {activePlans.length === 0 ? (
        <EmptyState title="No active plans found" hint="There are no plans available for purchase right now." />
      ) : (
        <div className="grid grid-3">
          {activePlans.map((plan) => (
            <div
              key={plan.id}
              className="card plan-card"
              style={{ cursor: "pointer" }}
              onClick={() => setDetailsPlan(plan)}
            >
              <h3 className="mb-0">{plan.name}</h3>
              {plan.description && (
                <p className="muted" style={{ fontSize: "0.83rem" }}>
                  {plan.description}
                </p>
              )}
              <div className="plan-price">
                {money(plan.premiumAmount)} <small>/ year</small>
              </div>
              <ul className="plan-features">
                <li>
                  <span>Coverage amount</span>
                  <span>{money(plan.coverageAmount)}</span>
                </li>
                <li>
                  <span>Duration</span>
                  <span>{plan.durationMonths} months</span>
                </li>
              </ul>
              <div className="plan-actions">
                <button
                  className="btn btn-primary"
                  onClick={(e) => {
                    e.stopPropagation();
                    setPurchasePlan(plan);
                  }}
                >
                  Buy insurance
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {detailsPlan && (
        <Modal title={detailsPlan.name} onClose={() => setDetailsPlan(null)}>
          {detailsPlan.description && (
            <p className="muted" style={{ fontSize: "0.85rem" }}>{detailsPlan.description}</p>
          )}
          <ul className="plan-features">
            <li>
              <span>Yearly premium</span>
              <span>{money(detailsPlan.premiumAmount)}</span>
            </li>
            <li>
              <span>Coverage amount</span>
              <span>{money(detailsPlan.coverageAmount)}</span>
            </li>
            <li>
              <span>Duration</span>
              <span>{detailsPlan.durationMonths} months</span>
            </li>
          </ul>
          <div className="form-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setDetailsPlan(null)}
            >
              Close
            </button>
            <button
              className="btn btn-primary"
              onClick={() => {
                setPurchasePlan(detailsPlan);
                setDetailsPlan(null);
              }}
            >
              Buy insurance
            </button>
          </div>
        </Modal>
      )}

      {purchasePlan && (
        <Modal title={`Buy "${purchasePlan.name}" plan`} onClose={() => setPurchasePlan(null)}>
          <form onSubmit={onPurchase}>
            <div className="alert alert-info">
              Yearly premium: <strong>{money(purchasePlan.premiumAmount)}</strong> •
              Duration: <strong>{purchasePlan.durationMonths} months</strong>
            </div>
            <p className="muted" style={{ fontSize: "0.85rem" }}>
              Payment processing will begin after you purchase this plan. Your policy
              will become active once the payment completes.
            </p>
            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setPurchasePlan(null)}
              >
                Cancel
              </button>
              <button className="btn btn-primary" disabled={purchasing}>
                {purchasing ? "Purchasing..." : "Confirm and buy"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
