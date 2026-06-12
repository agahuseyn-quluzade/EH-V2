import { FormEvent, useState } from "react";
import { extractError } from "../../api/client";
import { iamApi } from "../../api/iam";
import { Badge, Field } from "../../components/ui";
import { useToast } from "../../context/ToastContext";
import { UserProfile } from "../../types";
import { formatDateTime, roleLabels } from "../../utils/format";

export function StaffMembersPage() {
  const toast = useToast();

  const [memberId, setMemberId] = useState("");
  const [member, setMember] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const onSearch = async (e: FormEvent) => {
    e.preventDefault();
    const id = memberId.trim();
    if (!id) return;
    setLoading(true);
    setMember(null);
    try {
      const profile = await iamApi.getUser(id);
      setMember(profile);
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
          <h1>Üzv axtarışı</h1>
          <p>İstifadəçi ID ilə üzv məlumatlarını tapın</p>
        </div>
      </div>

      <div className="card">
        <form onSubmit={onSearch} style={{ display: "flex", gap: 10, alignItems: "flex-end" }}>
          <div style={{ flex: 1 }}>
            <Field label="Üzv ID (UUID)">
              <input
                value={memberId}
                onChange={(e) => setMemberId(e.target.value)}
                placeholder="məs: 4f8a2c3e-...-..."
                className="mono"
              />
            </Field>
          </div>
          <button
            className="btn btn-primary"
            disabled={loading || !memberId.trim()}
            style={{ marginBottom: 14 }}
          >
            {loading ? "Axtarılır..." : "Axtar"}
          </button>
        </form>
      </div>

      {member && (
        <div className="card">
          <div className="card-title">
            <h2>
              {member.firstName} {member.lastName}
            </h2>
            <Badge status="ACTIVE" label={roleLabels[member.role]} />
          </div>
          <dl className="detail-list">
            <div>
              <dt>E-poçt</dt>
              <dd>{member.email}</dd>
            </div>
            <div>
              <dt>Rol</dt>
              <dd>{roleLabels[member.role]}</dd>
            </div>
            <div>
              <dt>Qeydiyyat</dt>
              <dd>{formatDateTime(member.createdAt)}</dd>
            </div>
            <div>
              <dt>ID</dt>
              <dd className="mono">{member.id}</dd>
            </div>
          </dl>
        </div>
      )}

      {searched && !member && !loading && (
        <div className="alert alert-warning">Bu ID ilə üzv tapılmadı.</div>
      )}
    </>
  );
}
