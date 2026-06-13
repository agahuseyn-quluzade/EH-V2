import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { policyApi } from "../../api/policy";
import { homePathForRole, useAuth } from "../../context/AuthContext";
import { Plan } from "../../types";
import { money } from "../../utils/format";

const FEATURES = [
  {
    icon: "📋",
    title: "Plan selection and comparison",
    text: "Compare different insurance plans side by side and choose the one that fits your budget and needs.",
  },
  {
    icon: "⚡",
    title: "Instant claim evaluation",
    text: "Your claims are evaluated automatically — coverage checks and decisions are ready within seconds.",
  },
  {
    icon: "🤖",
    title: "AI assistant",
    text: "Our AI chatbot answers your insurance-related questions and recommends the plan that suits you best.",
  },
  {
    icon: "🔒",
    title: "Secure document storage",
    text: "Upload your medical documents securely — only you and authorized staff can view them.",
  },
  {
    icon: "📧",
    title: "Instant notifications",
    text: "Receive email notifications about claim decisions and insurance changes.",
  },
  {
    icon: "📊",
    title: "Transparent tracking",
    text: "Track your limits, payments, and claim history online at any time.",
  },
];

const STEPS = [
  { n: "1", title: "Register", text: "Create a free account in a few minutes." },
  { n: "2", title: "Choose a plan", text: "Compare plans or get an AI recommendation." },
  { n: "3", title: "Buy insurance", text: "Pick a start date and finalize your contract online." },
  { n: "4", title: "Submit a claim", text: "Submit your medical expenses with documents and track payment." },
];

export function LandingPage() {
  const { isAuthenticated, role } = useAuth();
  const [plans, setPlans] = useState<Plan[]>([]);

  useEffect(() => {
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
            <strong>E-Health Insurance</strong>
          </div>
          <nav className="landing-nav-links">
            <a href="#features">Features</a>
            <a href="#plans">Plans</a>
            <a href="#how">How it works</a>
          </nav>
          <div className="landing-nav-actions">
            {isAuthenticated ? (
              <Link to={panelPath} className="btn btn-primary">
                Go to dashboard →
              </Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-secondary">
                  Log in
                </Link>
                <Link to="/register" className="btn btn-primary">
                  Sign up
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <section className="landing-hero">
        <div className="landing-container landing-hero-inner">
          <div className="landing-hero-text">
            <span className="landing-eyebrow">Digital health insurance platform</span>
            <h1>
              <span className="landing-accent">Reliable insurance</span> for your health,
              fully online
            </h1>
            <p>
              From choosing a plan to claim payouts — everything in one platform.
              No paperwork, no waiting — manage your insurance in minutes.
            </p>
            <div className="landing-hero-actions">
              {isAuthenticated ? (
                <Link to={panelPath} className="btn btn-primary btn-lg">
                  Go to dashboard
                </Link>
              ) : (
                <>
                  <Link to="/register" className="btn btn-primary btn-lg">
                    Get started — free
                  </Link>
                  <a href="#plans" className="btn btn-secondary btn-lg">
                    View plans
                  </a>
                </>
              )}
            </div>
            <div className="landing-trust">
              <div>
                <strong>5 min</strong>
                <span>registration time</span>
              </div>
              <div>
                <strong>~30 sec</strong>
                <span>automatic claim decision</span>
              </div>
              <div>
                <strong>24/7</strong>
                <span>AI assistant support</span>
              </div>
            </div>
          </div>
          <div className="landing-hero-card">
            <div className="landing-mock-card">
              <div className="landing-mock-head">
                <span>🛡️ Active insurance</span>
                <span className="badge badge-success">Active</span>
              </div>
              <div className="landing-mock-row">
                <span>Plan</span>
                <strong>Premium Health</strong>
              </div>
              <div className="landing-mock-row">
                <span>Coverage rate</span>
                <strong>90%</strong>
              </div>
              <div className="landing-mock-row">
                <span>Annual limit</span>
                <strong>$20,000</strong>
              </div>
              <div className="landing-mock-bar">
                <div style={{ width: "32%" }} />
              </div>
              <small>Limit used: 32%</small>
            </div>
          </div>
        </div>
      </section>

      <section className="landing-section" id="features">
        <div className="landing-container">
          <h2 className="landing-section-title">Why E-Health Insurance?</h2>
          <p className="landing-section-sub">
            Managing your insurance has never been this easy
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
          <h2 className="landing-section-title">Insurance plans</h2>
          <p className="landing-section-sub">
            Transparent pricing for every budget
          </p>
          {plans.length === 0 ? (
            <p className="muted" style={{ textAlign: "center" }}>
              Sign up to see the plans — the catalog is updated regularly.
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
                    {money(plan.premiumAmount)} <small>/ year</small>
                  </div>
                  <ul className="plan-features">
                    <li>
                      <span>Coverage amount</span>
                      <span>{money(plan.coverageAmount)}</span>
                    </li>
                    <li>
                      <span>Duration</span>
                      <span>{plan.durationMonths} months</span>
                    </li>
                  </ul>
                  <Link
                    to={isAuthenticated ? "/plans" : "/register"}
                    className="btn btn-primary"
                  >
                    {isAuthenticated ? "View details" : "Get started with this plan"}
                  </Link>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="landing-section" id="how">
        <div className="landing-container">
          <h2 className="landing-section-title">How it works</h2>
          <p className="landing-section-sub">Get insured in 4 simple steps</p>
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
          <h2>Get insured today</h2>
          <p>Registration is free and takes less than 5 minutes.</p>
          {isAuthenticated ? (
            <Link to={panelPath} className="btn btn-lg landing-cta-btn">
              Go to dashboard
            </Link>
          ) : (
            <Link to="/register" className="btn btn-lg landing-cta-btn">
              Create a free account
            </Link>
          )}
        </div>
      </section>

      <footer className="landing-footer">
        <div className="landing-container landing-footer-inner">
          <div className="landing-logo">
            <span className="brand-icon">🏥</span>
            <strong>E-Health Insurance</strong>
          </div>
          <nav className="landing-footer-links">
            <a href="#features">Features</a>
            <a href="#plans">Plans</a>
            <a href="#how">How it works</a>
            <Link to="/login">Log in</Link>
            <Link to="/register">Sign up</Link>
          </nav>
          <span className="muted">
            © {new Date().getFullYear()} E-Health Insurance
          </span>
        </div>
      </footer>
    </div>
  );
}
