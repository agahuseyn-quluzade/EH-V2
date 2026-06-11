import { FormEvent, useState } from "react";
import { extractError, iamApi } from "../../api";
import { Role, UserProfile } from "../../types";
import { fmtDateTime, initials } from "../../utils/format";
import { Modal } from "../../components/Modal";
import { useToast } from "../../context/ToastContext";

// The IAM service exposes /users/{id} and PATCH /users/{id}/role but no list
// endpoint — admins look users up by ID. A future list endpoint would slot in
// here as a paginated table above this card.
export function AdminUsersPage() {
  const [userId, setUserId] = useState("");
  const [user, setUser] = useState<UserProfile | null>(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [roleOpen, setRoleOpen] = useState(false);
  const toast = useToast();

  async function lookup(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    setUser(null);
    try {
      const u = await iamApi.getUser(userId.trim());
      setUser(u);
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Users</h1>
      <p className="page-sub">Look up users and change their role.</p>

      <form className="card" onSubmit={lookup} style={{ marginBottom: 20 }}>
        <div className="row">
          <input
            placeholder="User ID (UUID)"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            required
            style={{
              flex: 1,
              padding: "10px 12px",
              border: "1px solid var(--border-strong)",
              borderRadius: "var(--radius)",
            }}
          />
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? <span className="spinner" /> : "Find"}
          </button>
        </div>
        {err && <div className="error" style={{ marginTop: 10 }}>{err}</div>}
      </form>

      {user && (
        <div className="card">
          <div className="split" style={{ marginBottom: 16 }}>
            <div className="row" style={{ gap: 16 }}>
              <div className="avatar" style={{ width: 56, height: 56, fontSize: 18 }}>
                {initials(user.firstName, user.lastName)}
              </div>
              <div>
                <h3 style={{ fontSize: 18 }}>
                  {user.firstName} {user.lastName}
                </h3>
                <div className="tiny muted">{user.email}</div>
                <div className="row" style={{ marginTop: 6 }}>
                  <span className={`role-pill ${user.role.toLowerCase()}`}>{user.role}</span>
                  <span className="badge badge-neutral">{user.status}</span>
                </div>
              </div>
            </div>
            <button className="btn btn-secondary" onClick={() => setRoleOpen(true)}>
              Change role
            </button>
          </div>

          <dl className="kv">
            <dt>User ID</dt>
            <dd>{user.id}</dd>
            <dt>Phone</dt>
            <dd>{user.phone || "—"}</dd>
            <dt>Created</dt>
            <dd>{fmtDateTime(user.createdAt)}</dd>
            <dt>Last login</dt>
            <dd>{fmtDateTime(user.lastLoginAt)}</dd>
          </dl>
        </div>
      )}

      {user && (
        <RoleModal
          open={roleOpen}
          onClose={() => setRoleOpen(false)}
          user={user}
          onChanged={(u) => {
            setUser(u);
            setRoleOpen(false);
            toast.success(`Role updated to ${u.role}.`);
          }}
        />
      )}
    </>
  );
}

function RoleModal({
  open,
  onClose,
  user,
  onChanged,
}: {
  open: boolean;
  onClose: () => void;
  user: UserProfile;
  onChanged: (u: UserProfile) => void;
}) {
  const [role, setRole] = useState<Role>(user.role);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      const updated = await iamApi.changeRole(user.id, role);
      onChanged(updated);
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Change role for ${user.firstName} ${user.lastName}`}
      footer={
        <>
          <button className="btn btn-ghost" type="button" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" type="submit" form="role-form" disabled={busy || role === user.role}>
            {busy ? <span className="spinner" /> : "Save"}
          </button>
        </>
      }
    >
      <form id="role-form" onSubmit={submit}>
        <div className="field">
          <label>Role</label>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value as Role)}
            style={{
              padding: "10px 12px",
              border: "1px solid var(--border-strong)",
              borderRadius: "var(--radius)",
              background: "var(--bg-surface)",
            }}
          >
            <option value="MEMBER">Member</option>
            <option value="STAFF">Staff</option>
            <option value="ADMIN">Admin</option>
          </select>
          <div className="hint">
            Admins have full access. Staff can review and decide claims. Members access their own data only.
          </div>
        </div>
        {err && <div className="error">{err}</div>}
      </form>
    </Modal>
  );
}
