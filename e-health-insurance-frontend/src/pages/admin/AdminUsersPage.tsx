import { FormEvent, useState } from "react";
import { extractError } from "../../api/client";
import { iamApi } from "../../api/iam";
import { Badge, Field } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { UserProfile } from "../../types";
import { formatDateTime, roleLabels } from "../../utils/format";

export function AdminUsersPage() {
  const toast = useToast();

  const [userId, setUserId] = useState("");
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const onSearch = async (e: FormEvent) => {
    e.preventDefault();
    const id = userId.trim();
    if (!id) return;
    setLoading(true);
    setUser(null);
    try {
      const profile = await iamApi.getUser(id);
      setUser(profile);
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setSearched(true);
      setLoading(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>İstifadəçi idarəetməsi</h1>
          <p>İstifadəçi axtarışı (ID ilə)</p>
        </div>
      </div>

      <div className="alert alert-info">
        Backend istifadəçi siyahısı endpoint-i yoxdur — axtarış yalnız istifadəçi ID
        (UUID) ilə mümkündür. Rol dəyişikliyi endpoint-i yoxdur.
      </div>

      <div className="card">
        <form
          onSubmit={onSearch}
          style={{ display: "flex", gap: 10, alignItems: "flex-end" }}
        >
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
            disabled={loading || !userId.trim()}
            style={{ marginBottom: 14 }}
          >
            {loading ? "Axtarılır..." : "Axtar"}
          </button>
        </form>
      </div>

      {user && (
        <div className="card">
          <div className="card-title">
            <h2>
              {user.firstName} {user.lastName}
            </h2>
            <Badge status="ACTIVE" label={roleLabels[user.role]} />
          </div>
          <dl className="detail-list" style={{ gridTemplateColumns: "1fr" }}>
            <div>
              <dt>E-poçt</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Hazırkı rol</dt>
              <dd>
                <Badge status="ACTIVE" label={roleLabels[user.role]} />
              </dd>
            </div>
            <div>
              <dt>Qeydiyyat</dt>
              <dd>{formatDateTime(user.createdAt)}</dd>
            </div>
            <div>
              <dt>ID</dt>
              <dd className="mono">{user.id}</dd>
            </div>
          </dl>
        </div>
      )}

      {searched && !user && !loading && (
        <div className="alert alert-warning">Bu ID ilə istifadəçi tapılmadı.</div>
      )}
    </>
  );
}
