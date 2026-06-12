import { FormEvent, useEffect, useState } from "react";
import { extractError } from "../../api/client";
import { iamApi } from "../../api/iam";
import { Badge, Field, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { formatDateTime, roleLabels } from "../../utils/format";

export function ProfilePage() {
  const { user, refreshProfile } = useAuth();
  const toast = useToast();

  const [form, setForm] = useState({ firstName: "", lastName: "" });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setForm({ firstName: user.firstName, lastName: user.lastName });
    }
  }, [user]);

  if (!user) return <Spinner />;

  const onSave = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await iamApi.updateMe({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
      });
      await refreshProfile();
      toast.success("Profil yeniləndi");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Profil</h1>
          <p>Şəxsi məlumatlarınız</p>
        </div>
      </div>

      <div className="grid grid-2" style={{ alignItems: "start" }}>
        <div className="card">
          <h2>Məlumatları redaktə et</h2>
          <form onSubmit={onSave}>
            <div className="form-row">
              <Field label="Ad" required>
                <input
                  value={form.firstName}
                  onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
                  required
                  maxLength={100}
                />
              </Field>
              <Field label="Soyad" required>
                <input
                  value={form.lastName}
                  onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
                  required
                  maxLength={100}
                />
              </Field>
            </div>
            <button className="btn btn-primary" disabled={saving}>
              {saving ? "Yadda saxlanılır..." : "Yadda saxla"}
            </button>
          </form>
        </div>

        <div className="card">
          <h2>Hesab məlumatları</h2>
          <dl className="detail-list" style={{ gridTemplateColumns: "1fr" }}>
            <div>
              <dt>E-poçt</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Rol</dt>
              <dd>
                <Badge status="ACTIVE" label={roleLabels[user.role]} />
              </dd>
            </div>
            <div>
              <dt>Qeydiyyat tarixi</dt>
              <dd>{formatDateTime(user.createdAt)}</dd>
            </div>
            <div>
              <dt>İstifadəçi ID</dt>
              <dd className="mono">{user.id}</dd>
            </div>
          </dl>
        </div>
      </div>
    </>
  );
}
