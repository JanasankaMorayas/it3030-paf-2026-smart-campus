import {
  Bell,
  Boxes,
  CalendarRange,
  ChevronRight,
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
import { useEffect, useMemo, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import StatusBadge from "./StatusBadge.jsx";

function navigationForRole(role) {
  const common = [
    { to: "/", label: "Home", icon: LayoutDashboard, group: "workspace" },
    { to: "/resources", label: "Resources", icon: Boxes, group: "workspace" },
    { to: "/bookings", label: "Bookings", icon: CalendarRange, group: "workspace" },
    { to: "/tickets", label: "Tickets", icon: Wrench, group: "workspace" },
    { to: "/notifications", label: "Inbox", icon: Bell, group: "workspace" },
  ];

  if (role === "ADMIN") {
    return [
      ...common,
      { to: "/users", label: "Users", icon: UserCog, group: "admin" },
      { to: "/audit", label: "Audit", icon: ScrollText, group: "admin" },
    ];
  }

  return common;
}

function pageCopy(pathname) {
  if (pathname === "/") {
    return {
      key: "dashboard",
      section: "Home workspace",
      title: "Campus operations portal",
      subtitle: "A single desk for service requests, bookings, resources, inbox activity, and oversight.",
    };
  }

  if (pathname.startsWith("/resources")) {
    return {
      key: "resources",
      section: "Resource catalogue",
      title: "Spaces and asset readiness",
      subtitle: "Search, maintain, and update the shared campus inventory used by bookings and operations.",
    };
  }

  if (pathname.startsWith("/bookings")) {
    return {
      key: "bookings",
      section: "Booking workspace",
      title: "Reservation queue and approvals",
      subtitle: "Track ownership, status transitions, and next actions across the booking flow.",
    };
  }

  if (pathname.startsWith("/tickets")) {
    return {
      key: "tickets",
      section: "Maintenance desk",
      title: "Incident response and technician flow",
      subtitle: "Follow reports, assignments, and status updates without losing operational context.",
    };
  }

  if (pathname.startsWith("/notifications")) {
    return {
      key: "notifications",
      section: "Notification centre",
      title: "Alerts, reminders, and follow-up",
      subtitle: "Stay on top of unread operational signals tied to your role and current workload.",
    };
  }

  if (pathname.startsWith("/users")) {
    return {
      key: "users",
      section: "Admin directory",
      title: "User roles and platform access",
      subtitle: "Review synced accounts, technician eligibility, and role posture across the platform.",
    };
  }

  if (pathname.startsWith("/audit")) {
    return {
      key: "audit",
      section: "Admin audit workbench",
      title: "Audit trail and legacy support",
      subtitle: "Trace key actions and run careful backfill utilities for older local records.",
    };
  }

  return {
    key: "workspace",
    section: "Workspace",
    title: "Smart Campus Operations Hub",
    subtitle: "Operational workspace",
  };
}

function roleMessage(role) {
  switch (role) {
    case "ADMIN":
      return "Campus-wide approvals, user control, audit visibility, and legacy cleanup tools are available.";
    case "TECHNICIAN":
      return "Your portal highlights assigned maintenance work, ticket transitions, and inbox follow-up.";
    default:
      return "Your portal focuses on self-service bookings, issue reporting, and notification tracking.";
  }
}

function authModeLabel(authMode) {
  return authMode === "basic" ? "Basic Auth demo session" : "Google session";
}

export default function AppLayout({ children }) {
  const { currentUser, authMode, refreshCurrentUser, logout } = useAuth();
  const [navigationOpen, setNavigationOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    setNavigationOpen(false);
  }, [location.pathname]);

  const page = pageCopy(location.pathname);
  const navigation = navigationForRole(currentUser?.role);
  const workspaceLinks = navigation.filter((item) => item.group === "workspace");
  const adminLinks = navigation.filter((item) => item.group === "admin");
  const initials = useMemo(() => {
    const source = currentUser?.displayName || currentUser?.email || "SC";
    return source
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("");
  }, [currentUser?.displayName, currentUser?.email]);

  return (
    <div className={`app-shell page-${page.key}`}>
      <div
        className={`portal-drawer-backdrop ${navigationOpen ? "portal-drawer-backdrop--visible" : ""}`}
        role="presentation"
        onClick={() => setNavigationOpen(false)}
      />

      <aside className={`portal-drawer ${navigationOpen ? "portal-drawer--open" : ""}`}>
        <div className="portal-drawer__header">
          <div className="portal-brand">
            <div className="portal-brand__mark">
              <Shield size={18} />
            </div>
            <div>
              <p>Smart Campus</p>
              <strong>Operations Hub</strong>
            </div>
          </div>
          <button type="button" className="icon-button" onClick={() => setNavigationOpen(false)} aria-label="Close navigation">
            <X size={16} />
          </button>
        </div>

        <div className="portal-drawer__profile">
          <div className="portal-avatar portal-avatar--large">{initials || "SC"}</div>
          <div>
            <strong>{currentUser?.displayName || currentUser?.email}</strong>
            <p>{currentUser?.email}</p>
          </div>
          <StatusBadge value={currentUser?.role} />
        </div>

        <div className="portal-drawer__section">
          <span className="portal-nav__label">Workspace</span>
          <nav className="portal-mobile-nav">
            {workspaceLinks.map((item) => {
              const Icon = item.icon;

              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === "/"}
                  className={({ isActive }) => `portal-mobile-link ${isActive ? "portal-mobile-link--active" : ""}`}
                >
                  <Icon size={17} />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </div>

        {adminLinks.length ? (
          <div className="portal-drawer__section">
            <span className="portal-nav__label">Admin tools</span>
            <nav className="portal-mobile-nav">
              {adminLinks.map((item) => {
                const Icon = item.icon;

                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) => `portal-mobile-link ${isActive ? "portal-mobile-link--active" : ""}`}
                  >
                    <Icon size={17} />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </nav>
          </div>
        ) : null}

        <div className="portal-drawer__footer">
          <div className="portal-session-note">
            <ClipboardList size={15} />
            <span>{authModeLabel(authMode)}</span>
          </div>
          <button type="button" className="button button--danger" onClick={() => void logout()}>
            <LogOut size={16} />
            Sign out
          </button>
        </div>
      </aside>

      <div className="app-main">
        <header className="portal-header">
          <div className="portal-header__top">
            <div className="portal-header__left">
              <button
                type="button"
                className="icon-button portal-header__menu"
                onClick={() => setNavigationOpen((open) => !open)}
                aria-label="Open navigation"
              >
                <Menu size={18} />
              </button>

              <div className="portal-brand">
                <div className="portal-brand__mark">
                  <Shield size={18} />
                </div>
                <div>
                  <p>Smart Campus</p>
                  <strong>Operations Hub</strong>
                </div>
              </div>

              <div className="portal-breadcrumb">
                <span>{page.section}</span>
                <ChevronRight size={14} />
                <strong>{page.title}</strong>
              </div>
            </div>

            <div className="portal-header__actions">
              <NavLink to="/notifications" className="portal-utility-link">
                <Bell size={16} />
                <span>Inbox</span>
              </NavLink>

              <button type="button" className="button button--ghost" onClick={() => void refreshCurrentUser()}>
                <RefreshCcw size={16} />
                Refresh session
              </button>

              <div className="portal-profile-card">
                <div className="portal-avatar">{initials || "SC"}</div>
                <div className="portal-profile-card__copy">
                  <strong>{currentUser?.displayName || currentUser?.email}</strong>
                  <div className="portal-profile-card__meta">
                    <span>{authModeLabel(authMode)}</span>
                    <StatusBadge value={currentUser?.role} />
                  </div>
                </div>
              </div>

              <button type="button" className="button button--danger" onClick={() => void logout()}>
                <LogOut size={16} />
                Sign out
              </button>
            </div>
          </div>

          <div className="portal-header__nav">
            <nav className="portal-nav portal-nav--primary" aria-label="Primary workspace navigation">
              <span className="portal-nav__label">Campus workspace</span>
              <div className="portal-nav__links">
                {workspaceLinks.map((item) => {
                  const Icon = item.icon;

                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      end={item.to === "/"}
                      className={({ isActive }) => `portal-nav__link ${isActive ? "portal-nav__link--active" : ""}`}
                    >
                      <Icon size={16} />
                      <span>{item.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </nav>

            {adminLinks.length ? (
              <nav className="portal-nav portal-nav--secondary" aria-label="Administrative navigation">
                <span className="portal-nav__label">Administrative lanes</span>
                <div className="portal-nav__links">
                  {adminLinks.map((item) => {
                    const Icon = item.icon;

                    return (
                      <NavLink
                        key={item.to}
                        to={item.to}
                        className={({ isActive }) => `portal-nav__link ${isActive ? "portal-nav__link--active" : ""}`}
                      >
                        <Icon size={16} />
                        <span>{item.label}</span>
                      </NavLink>
                    );
                  })}
                </div>
              </nav>
            ) : (
              <div className="portal-context-card">
                <div className="portal-context-card__header">
                  <StatusBadge value={currentUser?.role} />
                  <span>Current workspace posture</span>
                </div>
                <p>{roleMessage(currentUser?.role)}</p>
              </div>
            )}
          </div>
        </header>

        <main className="content-area">{children}</main>
      </div>
    </div>
  );
}
