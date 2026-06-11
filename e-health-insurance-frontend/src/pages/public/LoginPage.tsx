import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { extractError } from "../../api";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      await login({ email, password });
      toast.success("Welcome back!");
      const from = (location.state as { from?: { pathname: string } } | null)?.from
        ?.pathname;
      navigate(from || "/", { replace: true });
    } catch (e) {
      const msg = extractError(e);
      setErr(msg);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={onSubmit}>
      <h2>Sign in</h2>
      <div className="sub">Access your member, staff, or admin account.</div>

      {err && (
        <div
          style={{
            background: "var(--red-50)",
            color: "var(--red-800)",
            padding: "10px 14px",
            borderRadius: "var(--radius)",
            fontSize: 13,
            marginBottom: 14,
          }}
        >
          {err}
        </div>
      )}

      <div className="field">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          required
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          required
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>

      <button
        type="submit"
        className="btn btn-primary btn-block btn-lg"
        disabled={busy}
      >
        {busy ? <span className="spinner" /> : "Sign in"}
      </button>

      <div style={{ marginTop: 20, fontSize: 13, color: "var(--text-secondary)", textAlign: "center" }}>
        No account yet? <Link to="/register">Create one</Link>
      </div>
    </form>
  );
}
