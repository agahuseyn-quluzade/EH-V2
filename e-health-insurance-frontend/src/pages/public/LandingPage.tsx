import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { policyApi } from "../../api/policy";
import { homePathForRole, useAuth } from "../../context/AuthContext";
import { Plan } from "../../types";
import { money } from "../../utils/format";

const FEATURES = [
  {
    icon: "📋",
    title: "Plan seçimi və müqayisə",
    text: "Müxtəlif sığorta planlarını yan-yana müqayisə edin, büdcənizə və ehtiyaclarınıza uyğun olanı seçin.",
  },
  {
    icon: "⚡",
    title: "Ani iddia qiymətləndirməsi",
    text: "İddialarınız avtomatik qiymətləndirilir — əhatə yoxlaması və qərar bir neçə saniyəyə hazır olur.",
  },
  {
    icon: "🤖",
    title: "Süni intellekt köməkçisi",
    text: "AI çat-bot sığortanızla bağlı suallarınızı cavablandırır, sizə ən uyğun planı tövsiyə edir.",
  },
  {
    icon: "🔒",
    title: "Təhlükəsiz sənəd saxlanması",
    text: "Tibbi sənədlərinizi təhlükəsiz yükləyin — yalnız siz və səlahiyyətli əməkdaşlar onlara baxa bilər.",
  },
  {
    icon: "📧",
    title: "Anında bildirişlər",
    text: "İddia qərarları və sığorta dəyişiklikləri barədə e-poçt bildirişləri alın.",
  },
  {
    icon: "📊",
    title: "Şəffaf izləmə",
    text: "Limitlərinizi, ödənişlərinizi və iddia tarixçənizi istənilən vaxt onlayn izləyin.",
  },
];

const STEPS = [
  { n: "1", title: "Qeydiyyatdan keçin", text: "Bir neçə dəqiqəyə pulsuz hesab yaradın." },
  { n: "2", title: "Plan seçin", text: "Planları müqayisə edin və ya AI tövsiyəsi alın." },
  { n: "3", title: "Sığorta alın", text: "Başlama tarixini seçib müqaviləni onlayn rəsmiləşdirin." },
  { n: "4", title: "İddia göndərin", text: "Tibbi xərclərinizi sənədlərlə birgə təqdim edin, ödənişi izləyin." },
];

export function LandingPage() {
  const { isAuthenticated, role } = useAuth();
  const [plans, setPlans] = useState<Plan[]>([]);

  useEffect(() => {
    // Plan kataloqu açıq endpoint-dir — giriş tələb olunmur
    policyApi
      .listPlans()
      .then((all) => setPlans(all.filter((p) => p.active === true).slice(0, 3)))
      .catch(() => setPlans([]));
  }, []);

  const panelPath = homePathForRole(role);

  return (
    <div className="landing">
      <header className="landing-nav">
        <div className="landing-container landing-nav-inner">
          <div className="landing-logo">
            <span className="brand-icon">🏥</span>
            <strong>E-Sağlamlıq Sığortası</strong>
          </div>
          <nav className="landing-nav-links">
            <a href="#features">Üstünlüklər</a>
            <a href="#plans">Planlar</a>
            <a href="#how">Necə işləyir?</a>
          </nav>
          <div className="landing-nav-actions">
            {isAuthenticated ? (
              <Link to={panelPath} className="btn btn-primary">
                Panelə keç →
              </Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-secondary">
                  Daxil ol
                </Link>
                <Link to="/register" className="btn btn-primary">
                  Qeydiyyat
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <section className="landing-hero">
        <div className="landing-container landing-hero-inner">
          <div className="landing-hero-text">
            <span className="landing-eyebrow">Rəqəmsal tibbi sığorta platforması</span>
            <h1>
              Sağlamlığınız üçün <span className="landing-accent">etibarlı sığorta</span>,
              tam onlayn
            </h1>
            <p>
              Plan seçimindən iddia ödənişinə qədər hər şey bir platformada.
              Kağız işi yoxdur, gözləmə yoxdur — sığortanızı dəqiqələr içində
              idarə edin.
            </p>
            <div className="landing-hero-actions">
              {isAuthenticated ? (
                <Link to={panelPath} className="btn btn-primary btn-lg">
                  Panelə keç
                </Link>
              ) : (
                <>
                  <Link to="/register" className="btn btn-primary btn-lg">
                    İndi başla — pulsuz
                  </Link>
                  <a href="#plans" className="btn btn-secondary btn-lg">
                    Planlara bax
                  </a>
                </>
              )}
            </div>
            <div className="landing-trust">
              <div>
                <strong>5 dəq</strong>
                <span>qeydiyyat müddəti</span>
              </div>
              <div>
                <strong>~30 san</strong>
                <span>avtomatik iddia qərarı</span>
              </div>
              <div>
                <strong>24/7</strong>
                <span>AI köməkçi dəstəyi</span>
              </div>
            </div>
          </div>
          <div className="landing-hero-card">
            <div className="landing-mock-card">
              <div className="landing-mock-head">
                <span>🛡️ Aktiv sığorta</span>
                <span className="badge badge-success">Aktiv</span>
              </div>
              <div className="landing-mock-row">
                <span>Plan</span>
                <strong>Premium Sağlamlıq</strong>
              </div>
              <div className="landing-mock-row">
                <span>Əhatə faizi</span>
                <strong>90%</strong>
              </div>
              <div className="landing-mock-row">
                <span>İllik limit</span>
                <strong>20 000 ₼</strong>
              </div>
              <div className="landing-mock-bar">
                <div style={{ width: "32%" }} />
              </div>
              <small>Limitdən istifadə: 32%</small>
            </div>
          </div>
        </div>
      </section>

      <section className="landing-section" id="features">
        <div className="landing-container">
          <h2 className="landing-section-title">Niyə E-Sağlamlıq Sığortası?</h2>
          <p className="landing-section-sub">
            Sığortanızı idarə etmək heç vaxt bu qədər asan olmayıb
          </p>
          <div className="grid grid-3">
            {FEATURES.map((f) => (
              <div key={f.title} className="card landing-feature">
                <div className="landing-feature-icon">{f.icon}</div>
                <h3>{f.title}</h3>
                <p className="muted mb-0">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="landing-section landing-section-alt" id="plans">
        <div className="landing-container">
          <h2 className="landing-section-title">Sığorta planları</h2>
          <p className="landing-section-sub">
            Hər büdcəyə uyğun şəffaf qiymətlər
          </p>
          {plans.length === 0 ? (
            <p className="muted" style={{ textAlign: "center" }}>
              Planları görmək üçün qeydiyyatdan keçin — kataloq daim yenilənir.
            </p>
          ) : (
            <div className="grid grid-3">
              {plans.map((plan) => (
                <div key={plan.id} className="card plan-card">
                  <h3 className="mb-0">{plan.name}</h3>
                  {plan.description && (
                    <p className="muted" style={{ fontSize: "0.83rem" }}>
                      {plan.description}
                    </p>
                  )}
                  <div className="plan-price">
                    {money(plan.premiumAmount)} <small>/ ay</small>
                  </div>
                  <ul className="plan-features">
                    <li>
                      <span>Əhatə məbləği</span>
                      <span>{money(plan.coverageAmount)}</span>
                    </li>
                    <li>
                      <span>Müddət</span>
                      <span>{plan.durationMonths} ay</span>
                    </li>
                  </ul>
                  <Link
                    to={isAuthenticated ? "/plans" : "/register"}
                    className="btn btn-primary"
                  >
                    {isAuthenticated ? "Ətraflı bax" : "Bu planla başla"}
                  </Link>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="landing-section" id="how">
        <div className="landing-container">
          <h2 className="landing-section-title">Necə işləyir?</h2>
          <p className="landing-section-sub">4 sadə addımda sığortalanın</p>
          <div className="grid grid-4">
            {STEPS.map((s) => (
              <div key={s.n} className="landing-step">
                <div className="landing-step-num">{s.n}</div>
                <h3>{s.title}</h3>
                <p className="muted mb-0">{s.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="landing-cta">
        <div className="landing-container">
          <h2>Bu gün sığortalanın</h2>
          <p>Qeydiyyat pulsuzdur və 5 dəqiqədən az çəkir.</p>
          {isAuthenticated ? (
            <Link to={panelPath} className="btn btn-lg landing-cta-btn">
              Panelə keç
            </Link>
          ) : (
            <Link to="/register" className="btn btn-lg landing-cta-btn">
              Pulsuz hesab yarat
            </Link>
          )}
        </div>
      </section>

      <footer className="landing-footer">
        <div className="landing-container landing-footer-inner">
          <div className="landing-logo">
            <span className="brand-icon">🏥</span>
            <strong>E-Sağlamlıq Sığortası</strong>
          </div>
          <nav className="landing-footer-links">
            <a href="#features">Üstünlüklər</a>
            <a href="#plans">Planlar</a>
            <a href="#how">Necə işləyir?</a>
            <Link to="/login">Daxil ol</Link>
            <Link to="/register">Qeydiyyat</Link>
          </nav>
          <span className="muted">
            © {new Date().getFullYear()} E-Sağlamlıq Sığortası
          </span>
        </div>
      </footer>
    </div>
  );
}
