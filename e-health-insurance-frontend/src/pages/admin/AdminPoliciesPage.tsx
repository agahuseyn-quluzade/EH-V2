import { useCallback, useEffect, useState } from "react";
import { extractError } from "../../api/client";
import { policyApi } from "../../api/policy";
import { Badge, EmptyState, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { Policy } from "../../types";
import { formatDate, money, policyStatusLabels } from "../../utils/format";

export function AdminPoliciesPage() {
  const toast = useToast();

  const [policies, setPolicies] = useState<Policy[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  const load = useCallback(
    (p = page) => {
      setLoading(true);
      policyApi
        .getAllPolicies(p, 20)
        .then((res) => {
          setPolicies(res.content ?? []);
          setTotalPages(res.totalPages ?? 0);
        })
        .catch((e) => toast.error(extractError(e)))
        .finally(() => setLoading(false));
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [page]
  );

  useEffect(() => {
    load(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Sığorta müqavilələri</h1>
          <p>Bütün müqavilələrin siyahısı</p>
        </div>
      </div>

      <div className="alert alert-info">
        Status filtrəsi və üzv üçün sığorta alma backend tərəfindən dəstəklənmir.
      </div>

      {loading ? (
        <Spinner />
      ) : policies.length === 0 ? (
        <EmptyState title="Müqavilə tapılmadı" hint="Hələ heç bir sığorta müqaviləsi yoxdur." />
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Müqavilə №</th>
                  <th>İstifadəçi ID</th>
                  <th>Plan</th>
                  <th>Müddət</th>
                  <th>Status</th>
                  <th>Aylıq haqq</th>
                </tr>
              </thead>
              <tbody>
                {policies.map((p) => (
                  <tr key={p.id}>
                    <td className="mono">{p.policyNumber}</td>
                    <td className="mono">{p.userId.slice(0, 8)}…</td>
                    <td>{p.planName}</td>
                    <td>
                      {formatDate(p.startDate)} — {formatDate(p.endDate)}
                    </td>
                    <td>
                      <Badge status={p.status} label={policyStatusLabels[p.status]} />
                    </td>
                    <td>{money(p.premiumAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div
              style={{
                display: "flex",
                gap: 8,
                justifyContent: "center",
                marginTop: 16,
                alignItems: "center",
              }}
            >
              <button
                className="btn btn-secondary btn-sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                ← Əvvəlki
              </button>
              <span className="muted">
                Səhifə {page + 1} / {totalPages}
              </span>
              <button
                className="btn btn-secondary btn-sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Növbəti →
              </button>
            </div>
          )}
        </>
      )}
    </>
  );
}
