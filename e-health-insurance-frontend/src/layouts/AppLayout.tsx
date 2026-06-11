import { ReactNode } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { initials } from "../utils/format";

interface NavItem {
  to: string;
  label: string;
  icon: ReactNode;
  end?: boolean;
}

const ICON = {
  dashboard: "▤",
  plans: "▥",
  policy: "❑",
  claims: "✦",
  chat: "✉",
  bell: "◔",
  shield: "✦",
  user: "○",
  flag: "⚑",
  cog: "✺",
  users: "⌬",
  card: "▦",
  heart: "♥",
};

const MEMBER_NAV: NavItem[] = [
  { to: "/", label: "Dashboard", icon: ICON.dashboard, end: true },
  { to: "/plans", label: "Plans", icon: ICON.plans },
  { to: "/policy", label: "My Policy", icon: ICON.policy },
  { to: "/payments", label: "Payments", icon: ICON.card },
  { to: "/claims", label: "My Claims", icon: ICON.claims },
  { to: "/health-record", label: "Health Record", icon: ICON.heart },
  { to: "/chat", label: "AI Assistant", icon: ICON.chat },
  { to: "/notifications", label: "Notifications", icon: ICON.bell },
];

const STAFF_NAV: NavItem[] = [
  { to: "/staff", label: "Review Queue", icon: ICON.flag, end: true },
  { to: "/staff/claims", label: "All Claims", icon: ICON.claims },
  { to: "/staff/members", label: "Members", icon: ICON.users },
];

const ADMIN_NAV: NavItem[] = [
  { to: "/admin", label: "Overview", icon: ICON.dashboard, end: true },
  { to: "/admin/plans", label: "Plan Catalog", icon: ICON.plans },
  { to: "/admin/users", label: "Users", icon: ICON.users },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;
  const role = user.role;
  const nav =
    role === "ADMIN" ? ADMIN_NAV : role === "STAFF" ? STAFF_NAV : MEMBER_NAV;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-logo">H+</div>
          <div>
            <div className="brand-name">HealthAssure</div>
            <div className="tiny muted">e-health insurance</div>
          </div>
        </div>

        <div className="nav-section">
          {role === "ADMIN"
            ? "Administration"
            : role === "STAFF"
            ? "Claim Review"
            : "Member Portal"}
        </div>
        {nav.map((n) => (
          <NavLink
            key={n.to}
            to={n.to}
            end={n.end}
            className={({ isActive }) =>
              "nav-link" + (isActive ? " active" : "")
            }
          >
            <span style={{ width: 16, textAlign: "center", color: "var(--text-tertiary)" }}>
              {n.icon}
            </span>
            <span>{n.label}</span>
          </NavLink>
        ))}

        <div style={{ flex: 1 }} />

        <div className="nav-section">Account</div>
        <NavLink
          to="/profile"
          className={({ isActive }) => "nav-link" + (isActive ? " active" : "")}
        >
          <span style={{ width: 16, textAlign: "center", color: "var(--text-tertiary)" }}>
            {ICON.user}
          </span>
          <span>Profile</span>
        </NavLink>
        <button className="nav-link" onClick={logout} style={{ background: "none", border: 0, textAlign: "left", width: "100%" }}>
          <span style={{ width: 16, textAlign: "center", color: "var(--text-tertiary)" }}>↩</span>
          <span>Sign out</span>
        </button>
      </aside>

      <main className="main">
        <header className="topbar">
          <div />
          <div className="topbar-user" onClick={() => navigate("/profile")} style={{ cursor: "pointer" }}>
            <span className={`role-pill ${role.toLowerCase()}`}>{role}</span>
            <div className="avatar">{initials(user.firstName, user.lastName)}</div>
            <div>
              <div style={{ fontWeight: 500, lineHeight: 1.1 }}>
                {user.firstName} {user.lastName}
              </div>
              <div className="tiny muted">{user.email}</div>
            </div>
          </div>
        </header>
        <div className="content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
