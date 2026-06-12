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
      toast.success("Xoş gəlmisiniz!");
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
        <h1>🏥 E-Sağlamlıq Sığortası</h1>
        <p>
          Sağlamlığınız üçün etibarlı rəqəmsal sığorta platforması. Planları
          müqayisə edin, sığorta alın, iddialarınızı onlayn izləyin.
        </p>
        <ul>
          <li>✅ Onlayn sığorta planı seçimi və müqayisəsi</li>
          <li>✅ İddiaların avtomatik qiymətləndirilməsi</li>
          <li>✅ Süni intellekt əsaslı köməkçi və tövsiyələr</li>
          <li>✅ Sənədlərin təhlükəsiz yüklənməsi</li>
        </ul>
      </div>
      <div className="auth-form-side">
        <div className="auth-card">
          <h2>Daxil ol</h2>
          <p className="auth-sub">Hesabınıza daxil olun</p>
          {error && <div className="alert alert-danger">{error}</div>}
          <form onSubmit={onSubmit}>
            <Field label="E-poçt" required>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="ad@nümunə.az"
                required
                autoComplete="email"
              />
            </Field>
            <Field label="Şifrə" required>
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
              {loading ? "Daxil olunur..." : "Daxil ol"}
            </button>
          </form>
          <div className="auth-switch">
            Hesabınız yoxdur? <Link to="/register">Qeydiyyatdan keçin</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
