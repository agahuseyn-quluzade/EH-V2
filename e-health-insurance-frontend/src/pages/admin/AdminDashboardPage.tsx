import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi } from "../../api/claim";
import { policyApi } from "../../api/policy";
import { Spinner, StatCard } from "../../components/ui";
import { Claim, Plan } from "../../types";
import { money } from "../../utils/format";

export function AdminDashboardPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [queue, setQueue] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      policyApi.listPlans(),
      claimApi.getAllClaims("UNDER_REVIEW", 0, 50).then((p) => p.content ?? []),
    ]).then(([p, q]) => {
      if (p.status === "fulfilled") setPlans(p.value);
      if (q.status === "fulfilled") setQueue(q.value);
      setLoading(false);
    });
  }, []);

  if (loading) return <Spinner />;

  const activePlans = plans.filter((p) => p.active === true).length;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Admin idarə paneli</h1>
          <p>Platformanın ümumi göstəriciləri</p>
        </div>
      </div>

      <div className="alert alert-info">
        Statistika endpoint-i backend tərəfindən dəstəklənmir — yalnız plan sayı
        və baxış növbəsi göstərilir.
      </div>

      <div className="grid grid-3">
        <StatCard
          label="Aktiv planlar"
          value={activePlans}
          hint={`Cəmi ${plans.length} plan`}
          tone="success"
        />
        <StatCard
          label="Baxış gözləyən iddialar"
          value={queue.length}
          tone={queue.length > 0 ? "warning" : "success"}
        />
        <StatCard
          label="Ümumi iddia məbləği"
          value={money(queue.reduce((s, c) => s + Number(c.amount || 0), 0))}
          tone="info"
        />
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <h2>Sürətli keçidlər</h2>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Link to="/admin/plans" className="btn btn-primary">
            📋 Plan idarəetməsi
          </Link>
          <Link to="/admin/policies" className="btn btn-secondary">
            🛡️ Sığorta müqavilələri
          </Link>
          <Link to="/admin/users" className="btn btn-secondary">
            👥 İstifadəçilər
          </Link>
          <Link to="/staff/queue" className="btn btn-secondary">
            🗂️ Baxış növbəsi ({queue.length})
          </Link>
        </div>
      </div>
    </>
  );
}
