import { ReactNode, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { roleLabels } from "../utils/format";

interface NavItem {
  to: string;
  label: string;
  icon: string;
  end?: boolean;
}

const memberNav: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", icon: "🏠", end: true },
  { to: "/plans", label: "Insurance Plans", icon: "📋" },
  { to: "/policy", label: "My Policy", icon: "🛡️" },
  { to: "/claims", label: "My Claims", icon: "🧾" },
  { to: "/chat", label: "AI Assistant", icon: "💬" },
  { to: "/notifications", label: "Notifications", icon: "🔔" },
  { to: "/profile", label: "Profile", icon: "👤" },
];

const staffNav: NavItem[] = [
  { to: "/staff", label: "Dashboard", icon: "📊", end: true },
  { to: "/staff/queue", label: "Review Queue", icon: "🗂️" },
  { to: "/staff/members", label: "Member Search", icon: "🔎" },
  { to: "/profile", label: "Profile", icon: "👤" },
];

const adminNav: NavItem[] = [
  { to: "/admin", label: "Dashboard", icon: "📊", end: true },
  { to: "/admin/plans", label: "Plan Management", icon: "📋" },
  { to: "/admin/policies", label: "Policies", icon: "🛡️" },
  { to: "/admin/users", label: "Users", icon: "👥" },
  { to: "/profile", label: "Profile", icon: "👤" },
];

export function Layout({ children }: { children: ReactNode }) {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const nav = role === "ADMIN" ? adminNav : role === "STAFF" ? staffNav : memberNav;

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <div className="layout">
      <aside className={`sidebar ${menuOpen ? "open" : ""}`}>
        <div className="sidebar-brand">
          <span className="brand-icon">🏥</span>
          <div>
            <strong>E-Health</strong>
            <span className="brand-sub">Insurance Platform</span>
          </div>
        </div>
        <nav className="sidebar-nav">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
              onClick={() => setMenuOpen(false)}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="user-avatar">
              {(user?.firstName?.[0] || "?").toUpperCase()}
              {(user?.lastName?.[0] || "").toUpperCase()}
            </div>
            <div className="user-meta">
              <strong>
                {user ? `${user.firstName} ${user.lastName}` : "User"}
              </strong>
              <span>{role ? roleLabels[role] : ""}</span>
            </div>
          </div>
          <button className="btn btn-ghost btn-block" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <button
            className="menu-toggle"
            onClick={() => setMenuOpen((v) => !v)}
            aria-label="Menu"
          >
            ☰
          </button>
          <span className="topbar-title">E-Health Insurance</span>
        </header>
        <main className="content">{children}</main>
      </div>
    </div>
  );
}
