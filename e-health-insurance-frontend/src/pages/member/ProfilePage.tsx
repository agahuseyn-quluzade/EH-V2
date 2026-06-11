import { FormEvent, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { extractError, iamApi } from "../../api";
import { useToast } from "../../context/ToastContext";
import { fmtDateTime } from "../../utils/format";

export function ProfilePage() {
  const { user, refreshMe } = useAuth();
  const toast = useToast();
  const [firstName, setFirstName] = useState(user?.firstName ?? "");
  const [lastName, setLastName] = useState(user?.lastName ?? "");
  const [phone, setPhone] = useState(user?.phone ?? "");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  if (!user) return null;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      await iamApi.updateMe({ firstName, lastName, phone });
      await refreshMe();
      toast.success("Profile updated.");
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Profile</h1>
      <p className="page-sub">Update your personal details.</p>

      <div className="grid-2" style={{ alignItems: "flex-start" }}>
        <form className="card" onSubmit={submit}>
          <h3 style={{ fontSize: 15, marginBottom: 14 }}>Personal info</h3>

          <div className="grid-2">
            <div className="field">
              <label>First name</label>
              <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
            </div>
            <div className="field">
              <label>Last name</label>
              <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
            </div>
          </div>

          <div className="field">
            <label>Phone</label>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} />
          </div>

          {err && <div className="error" style={{ marginBottom: 12 }}>{err}</div>}

          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? <span className="spinner" /> : "Save changes"}
          </button>
        </form>

        <div className="card">
          <h3 style={{ fontSize: 15, marginBottom: 14 }}>Account</h3>
          <dl className="kv">
            <dt>Email</dt>
            <dd>{user.email}</dd>
            <dt>Role</dt>
            <dd>
              <span className={`role-pill ${user.role.toLowerCase()}`}>{user.role}</span>
            </dd>
            <dt>Status</dt>
            <dd>{user.status}</dd>
            <dt>Joined</dt>
            <dd>{fmtDateTime(user.createdAt)}</dd>
            <dt>Last login</dt>
            <dd>{fmtDateTime(user.lastLoginAt)}</dd>
          </dl>
        </div>
      </div>
    </>
  );
}
