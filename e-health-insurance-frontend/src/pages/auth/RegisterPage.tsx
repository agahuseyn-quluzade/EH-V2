import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { extractError } from "../../api/client";
import { Field } from "../../components/ui";
import { homePathForRole, useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";

export function RegisterPage() {
  const { register } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    password: "",
    passwordConfirm: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set = (key: keyof typeof form) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (form.password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    if (form.password !== form.passwordConfirm) {
      setError("Passwords do not match");
      return;
    }
    setLoading(true);
    try {
      const role = await register({
        email: form.email,
        password: form.password,
        firstName: form.firstName,
        lastName: form.lastName,
        phone: form.phone || undefined,
      });
      toast.success("Registration completed successfully!");
      navigate(homePathForRole(role), { replace: true });
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
          Sign up in a few minutes and manage your health insurance
          entirely online.
        </p>
        <ul>
          <li>✅ Free account creation</li>
          <li>✅ Plan recommendations that fit your budget</li>
          <li>✅ Real-time claim status tracking</li>
          <li>✅ Email notifications</li>
        </ul>
      </div>
      <div className="auth-form-side">
        <div className="auth-card">
          <h2>Register</h2>
          <p className="auth-sub">Create a new account</p>
          {error && <div className="alert alert-danger">{error}</div>}
          <form onSubmit={onSubmit}>
            <div className="form-row">
              <Field label="First name" required>
                <input value={form.firstName} onChange={set("firstName")} required />
              </Field>
              <Field label="Last name" required>
                <input value={form.lastName} onChange={set("lastName")} required />
              </Field>
            </div>
            <Field label="Email" required>
              <input
                type="email"
                value={form.email}
                onChange={set("email")}
                placeholder="name@example.com"
                required
                autoComplete="email"
              />
            </Field>
            <Field label="Phone">
              <input
                type="tel"
                value={form.phone}
                onChange={set("phone")}
                placeholder="+994 50 000 00 00"
              />
            </Field>
            <Field label="Password" required hint="At least 8 characters">
              <input
                type="password"
                value={form.password}
                onChange={set("password")}
                required
                minLength={8}
                autoComplete="new-password"
              />
            </Field>
            <Field label="Confirm password" required>
              <input
                type="password"
                value={form.passwordConfirm}
                onChange={set("passwordConfirm")}
                required
                autoComplete="new-password"
              />
            </Field>
            <button className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Registering..." : "Sign up"}
            </button>
          </form>
          <div className="auth-switch">
            Already have an account? <Link to="/login">Log in</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
