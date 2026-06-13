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
  { to: "/dashboard", label: "İdarə paneli", icon: "🏠", end: true },
  { to: "/plans", label: "Sığorta planları", icon: "📋" },
  { to: "/policy", label: "Mənim sığortam", icon: "🛡️" },
  { to: "/claims", label: "İddialarım", icon: "🧾" },
  { to: "/chat", label: "AI Köməkçi", icon: "💬" },
  { to: "/notifications", label: "Bildirişlər", icon: "🔔" },
  { to: "/profile", label: "Profil", icon: "👤" },
];

const staffNav: NavItem[] = [
  { to: "/staff", label: "İdarə paneli", icon: "📊", end: true },
  { to: "/staff/queue", label: "Baxış növbəsi", icon: "🗂️" },
  { to: "/staff/members", label: "Üzv axtarışı", icon: "🔎" },
  { to: "/profile", label: "Profil", icon: "👤" },
];

const adminNav: NavItem[] = [
  { to: "/admin", label: "İdarə paneli", icon: "📊", end: true },
  { to: "/admin/plans", label: "Plan idarəetməsi", icon: "📋" },
  { to: "/admin/policies", label: "Sığorta müqavilələri", icon: "🛡️" },
  { to: "/admin/users", label: "İstifadəçilər", icon: "👥" },
  { to: "/staff/queue", label: "Baxış növbəsi", icon: "🗂️" },
  { to: "/profile", label: "Profil", icon: "👤" },
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
            <strong>E-Sağlamlıq</strong>
            <span className="brand-sub">Sığorta Platforması</span>
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
                {user ? `${user.firstName} ${user.lastName}` : "İstifadəçi"}
              </strong>
              <span>{role ? roleLabels[role] : ""}</span>
            </div>
          </div>
          <button className="btn btn-ghost btn-block" onClick={handleLogout}>
            Çıxış
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <button
            className="menu-toggle"
            onClick={() => setMenuOpen((v) => !v)}
            aria-label="Menyu"
          >
            ☰
          </button>
          <span className="topbar-title">E-Sağlamlıq Sığortası</span>
        </header>
        <main className="content">{children}</main>
      </div>
    </div>
  );
}
