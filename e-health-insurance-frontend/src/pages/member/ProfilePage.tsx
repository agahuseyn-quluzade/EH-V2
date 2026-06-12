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

  const [pwForm, setPwForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [pwSaving, setPwSaving] = useState(false);

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

  const onChangePassword = async (e: FormEvent) => {
    e.preventDefault();
    if (pwForm.newPassword.length < 8) {
      toast.error("Yeni şifrə ən az 8 simvol olmalıdır");
      return;
    }
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      toast.error("Yeni şifrələr uyğun gəlmir");
      return;
    }
    setPwSaving(true);
    try {
      await iamApi.changePassword({
        currentPassword: pwForm.currentPassword,
        newPassword: pwForm.newPassword,
      });
      toast.success("Şifrə yeniləndi");
      setPwForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setPwSaving(false);
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
        <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
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
            <h2>Şifrəni dəyiş</h2>
            <form onSubmit={onChangePassword}>
              <Field label="Cari şifrə" required>
                <input
                  type="password"
                  value={pwForm.currentPassword}
                  onChange={(e) => setPwForm((f) => ({ ...f, currentPassword: e.target.value }))}
                  required
                />
              </Field>
              <Field label="Yeni şifrə" required>
                <input
                  type="password"
                  value={pwForm.newPassword}
                  onChange={(e) => setPwForm((f) => ({ ...f, newPassword: e.target.value }))}
                  required
                  minLength={8}
                />
              </Field>
              <Field label="Yeni şifrəni təsdiqlə" required>
                <input
                  type="password"
                  value={pwForm.confirmPassword}
                  onChange={(e) => setPwForm((f) => ({ ...f, confirmPassword: e.target.value }))}
                  required
                />
              </Field>
              <button className="btn btn-primary" disabled={pwSaving}>
                {pwSaving ? "Yenilənir..." : "Şifrəni yenilə"}
              </button>
            </form>
          </div>
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
