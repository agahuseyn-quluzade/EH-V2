import { FormEvent, useEffect, useState } from "react";
import { extractError } from "../../api/client";
import { iamApi } from "../../api/iam";
import { Field, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { formatDateTime } from "../../utils/format";

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
      toast.success("Profile updated");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setSaving(false);
    }
  };

  const onChangePassword = async (e: FormEvent) => {
    e.preventDefault();
    if (pwForm.newPassword.length < 8) {
      toast.error("New password must be at least 8 characters");
      return;
    }
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      toast.error("New passwords do not match");
      return;
    }
    setPwSaving(true);
    try {
      await iamApi.changePassword({
        currentPassword: pwForm.currentPassword,
        newPassword: pwForm.newPassword,
      });
      toast.success("Password updated");
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
          <h1>Profile</h1>
          <p>Your personal information</p>
        </div>
      </div>

      <div className="grid grid-2" style={{ alignItems: "start" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
          <div className="card">
            <h2>Edit information</h2>
            <form onSubmit={onSave}>
              <div className="form-row">
                <Field label="First name" required>
                  <input
                    value={form.firstName}
                    onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
                    required
                    maxLength={100}
                  />
                </Field>
                <Field label="Last name" required>
                  <input
                    value={form.lastName}
                    onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
                    required
                    maxLength={100}
                  />
                </Field>
              </div>
              <button className="btn btn-primary" disabled={saving}>
                {saving ? "Saving..." : "Save"}
              </button>
            </form>
          </div>

          <div className="card">
            <h2>Change password</h2>
            <form onSubmit={onChangePassword}>
              <Field label="Current password" required>
                <input
                  type="password"
                  value={pwForm.currentPassword}
                  onChange={(e) => setPwForm((f) => ({ ...f, currentPassword: e.target.value }))}
                  required
                />
              </Field>
              <Field label="New password" required>
                <input
                  type="password"
                  value={pwForm.newPassword}
                  onChange={(e) => setPwForm((f) => ({ ...f, newPassword: e.target.value }))}
                  required
                  minLength={8}
                />
              </Field>
              <Field label="Confirm new password" required>
                <input
                  type="password"
                  value={pwForm.confirmPassword}
                  onChange={(e) => setPwForm((f) => ({ ...f, confirmPassword: e.target.value }))}
                  required
                />
              </Field>
              <button className="btn btn-primary" disabled={pwSaving}>
                {pwSaving ? "Updating..." : "Update password"}
              </button>
            </form>
          </div>
        </div>

        <div className="card">
          <h2>Account information</h2>
          <dl className="detail-list" style={{ gridTemplateColumns: "1fr" }}>
            <div>
              <dt>Email</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Registered on</dt>
              <dd>{formatDateTime(user.createdAt)}</dd>
            </div>
          </dl>
        </div>
      </div>
    </>
  );
}
