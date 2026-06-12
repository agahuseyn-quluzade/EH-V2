import { FormEvent, useState } from "react";
import { extractError } from "../../api/client";
import { iamApi } from "../../api/iam";
import { Badge, EmptyState, Field, Spinner } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { SpringPage, UserProfile } from "../../types";
import { formatDateTime, roleLabels } from "../../utils/format";

export function StaffMembersPage() {
  const toast = useToast();

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SpringPage<UserProfile> | null>(null);
  const [page, setPage] = useState(0);
  const [searching, setSearching] = useState(false);

  const doSearch = async (p = 0) => {
    const q = query.trim();
    if (!q) return;
    setSearching(true);
    try {
      const res = await iamApi.searchUsers(q, p, 20);
      setResults(res);
      setPage(p);
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setSearching(false);
    }
  };

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    doSearch(0);
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Üzv axtarışı</h1>
          <p>Ad, soyad və ya e-poçt ilə üzv tapın</p>
        </div>
      </div>

      <div className="card">
        <form onSubmit={onSearch} style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
          <div style={{ flex: 1 }}>
            <Field label="Axtarış sorğusu">
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Ad, soyad və ya e-poçt"
              />
            </Field>
          </div>
          <button
            className="btn btn-primary"
            disabled={searching || !query.trim()}
            style={{ marginBottom: 14 }}
          >
            {searching ? "Axtarılır..." : "Axtar"}
          </button>
        </form>
      </div>

      {searching && <Spinner />}

      {results && !searching && (
        results.content.length === 0 ? (
          <EmptyState title="Üzv tapılmadı" hint="Başqa sorğu ilə cəhd edin." />
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Ad Soyad</th>
                    <th>E-poçt</th>
                    <th>Rol</th>
                    <th>Status</th>
                    <th>Qeydiyyat</th>
                    <th>ID</th>
                  </tr>
                </thead>
                <tbody>
                  {results.content.map((u) => (
                    <tr key={u.id}>
                      <td>
                        {u.firstName} {u.lastName}
                      </td>
                      <td>{u.email}</td>
                      <td>
                        <Badge status="ACTIVE" label={roleLabels[u.role]} />
                      </td>
                      <td>
                        <Badge
                          status={u.active === false ? "CANCELLED" : "ACTIVE"}
                          label={u.active === false ? "Blok" : "Aktiv"}
                        />
                      </td>
                      <td>{formatDateTime(u.createdAt)}</td>
                      <td className="mono">{u.id.slice(0, 8)}…</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {(results.totalPages ?? 0) > 1 && (
              <div style={{ display: "flex", gap: 8, justifyContent: "center", marginTop: 16, alignItems: "center" }}>
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={page === 0}
                  onClick={() => doSearch(page - 1)}
                >
                  ← Əvvəlki
                </button>
                <span className="muted">
                  Səhifə {page + 1} / {results.totalPages}
                </span>
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={page + 1 >= (results.totalPages ?? 0)}
                  onClick={() => doSearch(page + 1)}
                >
                  Növbəti →
                </button>
              </div>
            )}
          </>
        )
      )}
    </>
  );
}
