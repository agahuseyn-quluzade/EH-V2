import { FormEvent, useState } from "react";
import { extractError } from "../../api/client";
import { iamApi } from "../../api/iam";
import { Badge, EmptyState, Field, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Role, SpringPage, UserProfile } from "../../types";
import { roleLabels } from "../../utils/format";

export function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const toast = useToast();

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SpringPage<UserProfile> | null>(null);
  const [page, setPage] = useState(0);
  const [searching, setSearching] = useState(false);

  const [userId, setUserId] = useState("");
  const [lookedUp, setLookedUp] = useState<UserProfile | null>(null);
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupSearched, setLookupSearched] = useState(false);

  const [updating, setUpdating] = useState<string | null>(null);

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

  const onLookup = async (e: FormEvent) => {
    e.preventDefault();
    const id = userId.trim();
    if (!id) return;
    setLookupLoading(true);
    setLookedUp(null);
    try {
      const profile = await iamApi.getUser(id);
      setLookedUp(profile);
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setLookupSearched(true);
      setLookupLoading(false);
    }
  };

  const handleRoleChange = async (id: string, role: Role) => {
    setUpdating(id);
    try {
      const updated = await iamApi.changeRole(id, { role });
      syncUser(updated);
      toast.success("Rol yeniləndi");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setUpdating(null);
    }
  };

  const handleStatusToggle = async (u: UserProfile) => {
    setUpdating(u.id);
    try {
      const updated = await iamApi.changeStatus(u.id, { active: u.active === false });
      syncUser(updated);
      toast.success(updated.active === false ? "İstifadəçi bloklandı" : "İstifadəçi aktivləşdirildi");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setUpdating(null);
    }
  };

  const syncUser = (updated: UserProfile) => {
    setResults((prev) =>
      prev ? { ...prev, content: prev.content.map((u) => (u.id === updated.id ? updated : u)) } : prev
    );
    setLookedUp((prev) => (prev?.id === updated.id ? updated : prev));
  };

  const isSelf = (id: string) => currentUser?.id === id;

  const renderRow = (u: UserProfile) => (
    <tr key={u.id}>
      <td>
        {u.firstName} {u.lastName}
      </td>
      <td>{u.email}</td>
      <td>
        {isSelf(u.id) ? (
          <Badge status="ACTIVE" label={roleLabels[u.role]} />
        ) : (
          <select
            value={u.role}
            disabled={updating === u.id}
            onChange={(e) => handleRoleChange(u.id, e.target.value as Role)}
            style={{ fontSize: "0.85rem" }}
          >
            <option value="CUSTOMER">{roleLabels["CUSTOMER"]}</option>
            <option value="AGENT">{roleLabels["AGENT"]}</option>
            <option value="ADMIN">{roleLabels["ADMIN"]}</option>
          </select>
        )}
      </td>
      <td>
        <Badge
          status={u.active === false ? "CANCELLED" : "ACTIVE"}
          label={u.active === false ? "Blok" : "Aktiv"}
        />
      </td>
      <td>
        {!isSelf(u.id) && (
          <button
            className={`btn btn-sm ${u.active === false ? "btn-primary" : "btn-danger"}`}
            disabled={updating === u.id}
            onClick={() => handleStatusToggle(u)}
          >
            {updating === u.id ? "..." : u.active === false ? "Aktiv et" : "Blokla"}
          </button>
        )}
      </td>
    </tr>
  );

  const renderTable = (rows: UserProfile[]) => (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Ad Soyad</th>
            <th>E-poçt</th>
            <th>Rol</th>
            <th>Status</th>
            <th>Əməliyyat</th>
          </tr>
        </thead>
        <tbody>{rows.map(renderRow)}</tbody>
      </table>
    </div>
  );

  return (
    <>
      <div className="page-header">
        <div>
          <h1>İstifadəçi idarəetməsi</h1>
          <p>Axtarış, rol və status idarəetməsi</p>
        </div>
      </div>

      <div className="card">
        <h2>Ad / E-poçt ilə axtarış</h2>
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
          <EmptyState title="İstifadəçi tapılmadı" hint="Başqa sorğu ilə cəhd edin." />
        ) : (
          <>
            {renderTable(results.content)}
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

      <div className="card" style={{ marginTop: "1.5rem" }}>
        <h2>ID ilə axtarış</h2>
        <form onSubmit={onLookup} style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
          <div style={{ flex: 1 }}>
            <Field label="İstifadəçi ID (UUID)">
              <input
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                placeholder="məs: 4f8a2c3e-...-..."
                className="mono"
              />
            </Field>
          </div>
          <button
            className="btn btn-primary"
            disabled={lookupLoading || !userId.trim()}
            style={{ marginBottom: 14 }}
          >
            {lookupLoading ? "Axtarılır..." : "Tap"}
          </button>
        </form>
        {lookedUp && renderTable([lookedUp])}
        {lookupSearched && !lookedUp && !lookupLoading && (
          <div className="alert alert-warning" style={{ marginTop: "1rem" }}>
            Bu ID ilə istifadəçi tapılmadı.
          </div>
        )}
      </div>
    </>
  );
}
