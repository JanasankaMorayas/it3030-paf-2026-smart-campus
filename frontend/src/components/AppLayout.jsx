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

function navigationForRole(role) {
  const common = [
    { to: "/", label: "Dashboard", icon: LayoutDashboard },
    { to: "/resources", label: "Resources", icon: Boxes },
    { to: "/bookings", label: "Bookings", icon: CalendarRange },
    { to: "/tickets", label: "Tickets", icon: Wrench },
    { to: "/notifications", label: "Notifications", icon: Bell },
  ];

  if (role === "ADMIN") {
    return [
      ...common,
      { to: "/users", label: "Users", icon: UserCog },
      { to: "/audit", label: "Audit & Backfill", icon: ScrollText },
    ];
  }

  return common;
}

function pageCopy(pathname) {
  if (pathname === "/") {
    return {
      title: "Operations cockpit",
      subtitle: "Live posture across resources, bookings, tickets, notifications, and admin activity.",
    };
  }

  if (pathname.startsWith("/resources")) {
    return { title: "Resource control", subtitle: "Campus spaces and equipment readiness." };
  }

  if (pathname.startsWith("/bookings")) {
    return { title: "Booking workflow", subtitle: "Reservation intake, approvals, and capacity flow." };
  }

  if (pathname.startsWith("/tickets")) {
    return { title: "Incident desk", subtitle: "Maintenance reporting, assignment, and resolution." };
  }

  if (pathname.startsWith("/notifications")) {
    return { title: "Notification inbox", subtitle: "Unread signals and operational follow-up." };
  }

  if (pathname.startsWith("/users")) {
    return { title: "User directory", subtitle: "Roles, account posture, and admin control." };
  }

  if (pathname.startsWith("/audit")) {
    return { title: "Audit history", subtitle: "Trace changes and run legacy user-link backfill." };
  }

  return { title: "Smart Campus Control Desk", subtitle: "Operational dashboard" };
}

function roleMessage(role) {
  switch (role) {
    case "ADMIN":
      return "Campus-wide control across approvals, roles, audits, and legacy cleanup.";
    case "TECHNICIAN":
      return "Assigned maintenance workload, status progress, and inbox follow-up.";
    default:
      return "Self-service view into your bookings, tickets, and notifications.";
  }
}

export default function AppLayout({ children }) {
  const { currentUser, authMode, refreshCurrentUser, logout } = useAuth();
  const [navigationOpen, setNavigationOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    setNavigationOpen(false);
  }, [location.pathname]);

  const navigation = navigationForRole(currentUser?.role);
  const page = pageCopy(location.pathname);

  return (
    <div className="app-shell">
      <div
        className={`sidebar-backdrop ${navigationOpen ? "sidebar-backdrop--visible" : ""}`}
        role="presentation"
        onClick={() => setNavigationOpen(false)}
      />

      <aside className={`sidebar ${navigationOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar-brand">
          <div className="sidebar-brand__mark">
            <Shield size={20} />
          </div>
          <div>
            <p>Smart Campus</p>
            <strong>Operations Hub</strong>
          </div>
        </div>

        <div className="sidebar-role-card">
          <div className="sidebar-role-card__top">
            <span className="sidebar-role-card__label">Signed in as</span>
            <StatusBadge value={currentUser?.role} />
          </div>
          <strong>{currentUser?.displayName || currentUser?.email}</strong>
          <p>{roleMessage(currentUser?.role)}</p>
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
          <div className="sidebar-footer__meta">
            <ClipboardList size={14} />
            <span>{authMode === "basic" ? "Basic Auth demo mode" : "Google session mode"}</span>
          </div>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <div className="topbar-main">
            <button
              type="button"
              className="icon-button topbar-toggle"
              onClick={() => setNavigationOpen((open) => !open)}
            >
              {navigationOpen ? <X size={18} /> : <Menu size={18} />}
            </button>

            <div className="topbar-title">
              <p>Operational dashboard</p>
              <h1>{page.title}</h1>
              <span>{page.subtitle}</span>
            </div>
          </div>

          <div className="topbar-actions">
            <button type="button" className="button button--ghost" onClick={() => void refreshCurrentUser()}>
              <RefreshCcw size={16} />
              Refresh session
            </button>

            <div className="user-chip">
              <div className="user-chip__avatar">
                <Shield size={16} />
              </div>
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
