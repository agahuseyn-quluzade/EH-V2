import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { extractError } from "../../api/client";
import { Field } from "../../components/ui";
import { homePathForRole, useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";

export function LoginPage() {
  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation() as { state?: { from?: string } };

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const role = await login({ email, password });
      toast.success("Welcome!");
      navigate(location.state?.from || homePathForRole(role), { replace: true });
    } catch (err) {
      setError(extractError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-hero">
        <h1>SaglamOl</h1>
        <p>
          A reliable digital insurance platform for your health. Compare
          plans, purchase insurance, and track your claims online.
        </p>
        <ul>
          <li>✅ Online insurance plan selection and comparison</li>
          <li>✅ Automatic claim evaluation</li>
          <li>✅ AI-powered assistant and recommendations</li>
          <li>✅ Secure document uploads</li>
        </ul>
      </div>
      <div className="auth-form-side">
        <div className="auth-card">
          <h2>Log in</h2>
          <p className="auth-sub">Sign in to your account</p>
          {error && <div className="alert alert-danger">{error}</div>}
          <form onSubmit={onSubmit}>
            <Field label="Email" required>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@example.com"
                required
                autoComplete="email"
              />
            </Field>
            <Field label="Password" required>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                autoComplete="current-password"
              />
            </Field>
            <button className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Logging in..." : "Log in"}
            </button>
          </form>
          <div className="auth-switch">
            Don't have an account? <Link to="/register">Sign up</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
