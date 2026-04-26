import {
  Bell,
  Boxes,
  CalendarRange,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  Menu,
  RefreshCcw,
  ScrollText,
  Shield,
  UserCog,
  Wrench,
  X,
} from "lucide-react";
import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import StatusBadge from "./StatusBadge.jsx";

function navigationForUser(isAdmin) {
  return [
    { to: "/", label: "Dashboard", icon: LayoutDashboard },
    { to: "/resources", label: "Resources", icon: Boxes },
    { to: "/bookings", label: "Bookings", icon: CalendarRange },
    { to: "/tickets", label: "Tickets", icon: Wrench },
    { to: "/notifications", label: "Notifications", icon: Bell },
    ...(isAdmin
      ? [
          { to: "/users", label: "Users", icon: UserCog },
          { to: "/audit", label: "Audit & Backfill", icon: ScrollText },
        ]
      : []),
  ];
}

export default function AppLayout({ children }) {
  const { currentUser, authMode, isAdmin, refreshCurrentUser, logout } = useAuth();
  const [navigationOpen, setNavigationOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    setNavigationOpen(false);
  }, [location.pathname]);

  const navigation = navigationForUser(isAdmin);

  return (
    <div className="app-shell">
      <aside className={`sidebar ${navigationOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar-brand">
          <div className="sidebar-brand__mark">
            <Shield size={18} />
          </div>
          <div>
            <p>Smart Campus</p>
            <strong>Operations Hub</strong>
          </div>
        </div>

        <nav className="sidebar-nav">
          {navigation.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/"}
                className={({ isActive }) => `sidebar-link ${isActive ? "sidebar-link--active" : ""}`}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <p>Backend base URL</p>
          <strong>http://localhost:8080</strong>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <button type="button" className="icon-button topbar-toggle" onClick={() => setNavigationOpen((open) => !open)}>
            {navigationOpen ? <X size={18} /> : <Menu size={18} />}
          </button>

          <div className="topbar-title">
            <p>Operational dashboard</p>
            <h1>Smart Campus Control Desk</h1>
          </div>

          <div className="topbar-actions">
            <button type="button" className="button button--ghost" onClick={() => void refreshCurrentUser()}>
              <RefreshCcw size={16} />
              Refresh user
            </button>

            <div className="user-chip">
              <ClipboardList size={16} />
              <div>
                <strong>{currentUser?.displayName || currentUser?.email}</strong>
                <div className="user-chip__meta">
                  <span>{currentUser?.email}</span>
                  <StatusBadge value={currentUser?.role} />
                  <span className="user-chip__mode">{authMode === "basic" ? "Basic Auth" : "Google session"}</span>
                </div>
              </div>
            </div>

            <button type="button" className="button button--danger" onClick={() => void logout()}>
              <LogOut size={16} />
              Sign out
            </button>
          </div>
        </header>

        <main className="content-area">{children}</main>
      </div>
    </div>
  );
}
