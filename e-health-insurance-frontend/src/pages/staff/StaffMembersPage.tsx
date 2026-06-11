import { FormEvent, useState } from "react";
import { extractError, iamApi } from "../../api";
import { UserProfile } from "../../types";
import { fmtDateTime, initials } from "../../utils/format";
import { useToast } from "../../context/ToastContext";

export function StaffMembersPage() {
  const [memberId, setMemberId] = useState("");
  const [user, setUser] = useState<UserProfile | null>(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const toast = useToast();

  async function lookup(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    setUser(null);
    try {
      const u = await iamApi.getUser(memberId.trim());
      setUser(u);
    } catch (e) {
      const msg = extractError(e);
      setErr(msg);
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Member lookup</h1>
      <p className="page-sub">
        Look up a member by user ID to see their profile and contact details.
      </p>

      <form className="card" onSubmit={lookup} style={{ marginBottom: 20 }}>
        <div className="row">
          <input
            placeholder="Member ID (UUID)"
            value={memberId}
            onChange={(e) => setMemberId(e.target.value)}
            required
            style={{
              flex: 1,
              padding: "10px 12px",
              border: "1px solid var(--border-strong)",
              borderRadius: "var(--radius)",
            }}
          />
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? <span className="spinner" /> : "Look up"}
          </button>
        </div>
        {err && <div className="error" style={{ marginTop: 10 }}>{err}</div>}
      </form>

      {user && (
        <div className="card">
          <div className="row" style={{ gap: 16, marginBottom: 16 }}>
            <div
              className="avatar"
              style={{ width: 56, height: 56, fontSize: 18 }}
            >
              {initials(user.firstName, user.lastName)}
            </div>
            <div>
              <h3 style={{ fontSize: 18 }}>
                {user.firstName} {user.lastName}
              </h3>
              <div className="muted tiny">{user.email}</div>
              <div className="row" style={{ marginTop: 6 }}>
                <span className={`role-pill ${user.role.toLowerCase()}`}>{user.role}</span>
                <span className="badge badge-neutral">{user.status}</span>
              </div>
            </div>
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
    </>
  );
}
