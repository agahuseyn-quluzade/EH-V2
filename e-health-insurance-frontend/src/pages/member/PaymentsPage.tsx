import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { extractError, paymentApi, policyApi } from "../../api";
import { Payment, PaymentProvider, Policy } from "../../types";
import { fmtDateTime, fmtMoneyFine } from "../../utils/format";
import { StatusBadge } from "../../components/Badge";
import { Empty } from "../../components/Empty";
import { Loader } from "../../components/Loader";
import { Modal } from "../../components/Modal";
import { useToast } from "../../context/ToastContext";

const PROVIDERS: PaymentProvider[] = ["MOCK", "STRIPE", "MANUAL", "BANK_TRANSFER"];

export function PaymentsPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [policy, setPolicy] = useState<Policy | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [payOpen, setPayOpen] = useState(false);
  const toast = useToast();

  async function load() {
    setLoading(true);
    try {
      const [list, pol] = await Promise.all([
        paymentApi.mine().catch(() => [] as Payment[]),
        policyApi.myPolicy().catch(() => null),
      ]);
      setPayments(list || []);
      setPolicy(pol);
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function confirm(id: string) {
    try {
      await paymentApi.confirm(id, `manual-${Date.now()}`);
      toast.success("Payment confirmed.");
      load();
    } catch (e) {
      toast.error(extractError(e));
    }
  }

  if (loading) return <Loader />;

  return (
    <>
      <div className="split">
        <div>
          <h1 className="page-title">Payments</h1>
          <p className="page-sub">Pay your premium and track every transaction.</p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => setPayOpen(true)}
          disabled={!policy}
          title={policy ? undefined : "You need a policy before making a payment"}
        >
          + Make a payment
        </button>
      </div>

      {!policy && (
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="muted">
            You don't have a policy yet.{" "}
            <Link to="/plans">Browse plans</Link> to get covered, then come back to pay.
          </div>
        </div>
      )}

      {err ? (
        <Empty title="Could not load payments">
          <div className="tiny muted">{err}</div>
        </Empty>
      ) : payments.length === 0 ? (
        <Empty title="No payments yet" />
      ) : (
        <div className="table-wrap">
          <table className="t">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Amount</th>
                <th>Provider</th>
                <th>Status</th>
                <th>Created</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.id}>
                  <td>
                    <div style={{ fontWeight: 500 }}>{p.id.slice(0, 8)}</div>
                    <div className="tiny muted">{p.providerReference || "—"}</div>
                  </td>
                  <td>{fmtMoneyFine(p.amount)}</td>
                  <td>{p.provider}</td>
                  <td><StatusBadge status={p.status} /></td>
                  <td>{fmtDateTime(p.createdAt)}</td>
                  <td>
                    {(p.status === "PENDING" || p.status === "PROCESSING") && (
                      <button className="btn btn-secondary btn-sm" onClick={() => confirm(p.id)}>
                        Confirm
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {policy && (
        <PayModal
          open={payOpen}
          onClose={() => setPayOpen(false)}
          policy={policy}
          onPaid={() => {
            setPayOpen(false);
            toast.success("Payment submitted.");
            load();
          }}
        />
      )}
    </>
  );
}

function PayModal({
  open,
  onClose,
  policy,
  onPaid,
}: {
  open: boolean;
  onClose: () => void;
  policy: Policy;
  onPaid: () => void;
}) {
  const defaultAmount = policy.plan?.monthlyPremium ?? 0;
  const [amount, setAmount] = useState(String(defaultAmount));
  const [provider, setProvider] = useState<PaymentProvider>("MOCK");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setAmount(String(policy.plan?.monthlyPremium ?? 0));
      setProvider("MOCK");
      setErr(null);
    }
  }, [open, policy]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      await paymentApi.create(
        { policyId: policy.id, amount: Number(amount), provider },
        // Stable idempotency key per policy+amount avoids accidental double charge.
        `pay-${policy.id}-${amount}`
      );
      onPaid();
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
      title="Make a payment"
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="pay-form" className="btn btn-primary" disabled={busy}>
            {busy ? <span className="spinner" /> : "Pay now"}
          </button>
        </>
      }
    >
      <form id="pay-form" onSubmit={submit}>
        <div className="field">
          <label>Amount (USD)</label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
          {policy.plan && (
            <div className="hint">Monthly premium: {fmtMoneyFine(policy.plan.monthlyPremium)}</div>
          )}
        </div>
        <div className="field">
          <label>Payment provider</label>
          <select
            value={provider}
            onChange={(e) => setProvider(e.target.value as PaymentProvider)}
            style={{ padding: "10px 12px", borderRadius: "var(--radius)", border: "1px solid var(--border-strong)" }}
          >
            {PROVIDERS.map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </div>
        {err && <div className="error">{err}</div>}
      </form>
    </Modal>
  );
}
