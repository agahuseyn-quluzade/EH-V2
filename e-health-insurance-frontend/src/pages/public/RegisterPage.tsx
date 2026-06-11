import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { extractError } from "../../api";

export function RegisterPage() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const { register } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (password.length < 8) {
      setErr("Password must be at least 8 characters.");
      return;
    }
    setBusy(true);
    setErr(null);
    try {
      await register({ firstName, lastName, email, phone, password });
      toast.success("Account created. Welcome to HealthAssure!");
      navigate("/", { replace: true });
    } catch (e) {
      setErr(extractError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={onSubmit}>
      <h2>Create your account</h2>
      <div className="sub">Start managing your health insurance in minutes.</div>

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

      <div className="grid-2">
        <div className="field">
          <label htmlFor="fn">First name</label>
          <input
            id="fn"
            required
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="ln">Last name</label>
          <input
            id="ln"
            required
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
          />
        </div>
      </div>

      <div className="field">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="phone">Phone (for SMS notifications)</label>
        <input
          id="phone"
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="pw">Password</label>
        <input
          id="pw"
          type="password"
          required
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <div className="hint">At least 8 characters.</div>
      </div>

      <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={busy}>
        {busy ? <span className="spinner" /> : "Create account"}
      </button>

      <div style={{ marginTop: 20, fontSize: 13, color: "var(--text-secondary)", textAlign: "center" }}>
        Already have an account? <Link to="/login">Sign in</Link>
      </div>
    </form>
  );
}
