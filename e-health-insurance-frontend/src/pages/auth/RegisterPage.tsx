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
      setError("Şifrə ən azı 8 simvoldan ibarət olmalıdır");
      return;
    }
    if (form.password !== form.passwordConfirm) {
      setError("Şifrələr uyğun gəlmir");
      return;
    }
    setLoading(true);
    try {
      const role = await register({
        email: form.email,
        password: form.password,
        firstName: form.firstName,
        lastName: form.lastName,
      });
      toast.success("Qeydiyyat uğurla tamamlandı!");
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
        <h1>🏥 E-Sağlamlıq Sığortası</h1>
        <p>
          Bir neçə dəqiqəyə qeydiyyatdan keçin və sağlamlıq sığortanızı tam
          onlayn idarə edin.
        </p>
        <ul>
          <li>✅ Pulsuz hesab yaradılması</li>
          <li>✅ Büdcənizə uyğun plan tövsiyələri</li>
          <li>✅ İddia statusunun real vaxtda izlənməsi</li>
          <li>✅ E-poçt bildirişləri</li>
        </ul>
      </div>
      <div className="auth-form-side">
        <div className="auth-card">
          <h2>Qeydiyyat</h2>
          <p className="auth-sub">Yeni hesab yaradın</p>
          {error && <div className="alert alert-danger">{error}</div>}
          <form onSubmit={onSubmit}>
            <div className="form-row">
              <Field label="Ad" required>
                <input value={form.firstName} onChange={set("firstName")} required />
              </Field>
              <Field label="Soyad" required>
                <input value={form.lastName} onChange={set("lastName")} required />
              </Field>
            </div>
            <Field label="E-poçt" required>
              <input
                type="email"
                value={form.email}
                onChange={set("email")}
                placeholder="ad@nümunə.az"
                required
                autoComplete="email"
              />
            </Field>
            <Field label="Telefon">
              <input
                type="tel"
                value={form.phone}
                onChange={set("phone")}
                placeholder="+994 50 000 00 00"
              />
            </Field>
            <Field label="Şifrə" required hint="Ən azı 8 simvol">
              <input
                type="password"
                value={form.password}
                onChange={set("password")}
                required
                minLength={8}
                autoComplete="new-password"
              />
            </Field>
            <Field label="Şifrənin təkrarı" required>
              <input
                type="password"
                value={form.passwordConfirm}
                onChange={set("passwordConfirm")}
                required
                autoComplete="new-password"
              />
            </Field>
            <button className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Qeydiyyat aparılır..." : "Qeydiyyatdan keç"}
            </button>
          </form>
          <div className="auth-switch">
            Artıq hesabınız var? <Link to="/login">Daxil olun</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
