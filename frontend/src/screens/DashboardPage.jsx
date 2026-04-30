import {
  Bell,
  Boxes,
  CalendarRange,
  Clock3,
  ExternalLink,
  Shield,
  Sparkles,
  Wrench,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import EmptyState from "../components/EmptyState.jsx";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import LoadingState from "../components/LoadingState.jsx";
import PageShell from "../components/PageShell.jsx";
import SectionCard from "../components/SectionCard.jsx";
import StatCard from "../components/StatCard.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { compactCount, formatCompactDateTime, formatDateRange } from "../lib/format.js";
import { api } from "../lib/api.js";

function deriveRoleFocus({ isAdmin, isTechnician }) {
  if (isAdmin) {
    return {
      title: "Administrative command lane",
      message: "Oversee approvals, assignments, role control, audit history, and legacy support from one portal home.",
      actionLabel: "Open audit workbench",
      actionHref: "/audit",
      quickLinks: [
        { label: "Review users", href: "/users" },
        { label: "Audit logs", href: "/audit" },
        { label: "Ticket desk", href: "/tickets" },
      ],
    };
  }

  if (isTechnician) {
    return {
      title: "Technician service lane",
      message: "Focus on assigned tickets, active maintenance status changes, and unread operational follow-up.",
      actionLabel: "Open ticket desk",
      actionHref: "/tickets",
      quickLinks: [
        { label: "Assigned tickets", href: "/tickets" },
        { label: "Notification inbox", href: "/notifications" },
        { label: "Resource catalogue", href: "/resources" },
      ],
    };
  }

  return {
    title: "Self-service campus lane",
    message: "Keep your bookings, issue reports, and notification follow-up visible from one clean home screen.",
    actionLabel: "Open bookings",
    actionHref: "/bookings",
    quickLinks: [
      { label: "Create booking", href: "/bookings" },
      { label: "Report issue", href: "/tickets" },
      { label: "Browse resources", href: "/resources" },
    ],
  };
}

function describeNotificationPressure(total) {
  if (!total) {
    return "Inbox is calm right now.";
  }

  if (total >= 5) {
    return "Unread notifications need active follow-up.";
  }

  return "A few operational reminders are waiting.";
}

export default function DashboardPage() {
  const { currentUser, isAdmin, isTechnician, authMode } = useAuth();
  const [snapshot, setSnapshot] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  async function loadDashboard() {
    setLoading(true);
    setError(null);

    try {
      const [resources, bookings, tickets, notifications, auditLogs] = await Promise.allSettled([
        api.resources.list(),
        api.bookings.list({ page: 0, size: 6, sort: "createdAt,desc" }),
        api.tickets.list({ page: 0, size: 6, sort: "createdAt,desc" }),
        api.notifications.list({ page: 0, size: 6, sort: "createdAt,desc" }),
        isAdmin ? api.audit.list({ page: 0, size: 6, sort: "createdAt,desc" }) : Promise.resolve(null),
      ]);

      setSnapshot({
        resources: resources.status === "fulfilled" ? resources.value : [],
        bookings: bookings.status === "fulfilled" ? bookings.value : null,
        tickets: tickets.status === "fulfilled" ? tickets.value : null,
        notifications: notifications.status === "fulfilled" ? notifications.value : null,
        auditLogs: auditLogs.status === "fulfilled" ? auditLogs.value : null,
      });
    } catch (dashboardError) {
      setError(dashboardError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadDashboard();
  }, [isAdmin]);

  const resources = snapshot?.resources || [];
  const bookings = snapshot?.bookings?.content || [];
  const tickets = snapshot?.tickets?.content || [];
  const notifications = snapshot?.notifications?.content || [];
  const auditRows = snapshot?.auditLogs?.content || [];

  const activeResources = resources.filter((resource) => resource.status === "ACTIVE").length;
  const pendingBookings = bookings.filter((booking) => booking.status === "PENDING").length;
  const ticketPressure = tickets.filter((ticket) => ticket.status !== "CLOSED" && ticket.status !== "RESOLVED").length;
  const unreadNotifications = notifications.filter((notification) => !notification.isRead).length;
  const roleFocus = deriveRoleFocus({ isAdmin, isTechnician });

  const operationalAlerts = useMemo(() => {
    const alerts = [];

    if (pendingBookings > 0) {
      alerts.push({
        tone: "warning",
        title: `${pendingBookings} booking request${pendingBookings === 1 ? "" : "s"} waiting`,
        message: isAdmin
          ? "Approvals are sitting in the queue and may need attention soon."
          : "Your visible booking queue still has pending decisions.",
      });
    }

    if (ticketPressure > 0) {
      alerts.push({
        tone: "danger",
        title: `${ticketPressure} active ticket${ticketPressure === 1 ? "" : "s"} in motion`,
        message: isTechnician
          ? "Assigned maintenance work is still active in your current view."
          : "Incident resolution work remains active across the service desk.",
      });
    }

    if (unreadNotifications > 0) {
      alerts.push({
        tone: "info",
        title: `${unreadNotifications} unread notification${unreadNotifications === 1 ? "" : "s"}`,
        message: describeNotificationPressure(unreadNotifications),
      });
    }

    if (!alerts.length) {
      alerts.push({
        tone: "success",
        title: "Operations look stable",
        message: "No urgent queue spikes are visible in this portal snapshot.",
      });
    }

    return alerts.slice(0, 3);
  }, [isAdmin, isTechnician, pendingBookings, ticketPressure, unreadNotifications]);

  const activityTimeline = useMemo(() => {
    const items = [
      ...bookings.map((booking) => ({
        id: `booking-${booking.id}`,
        type: "Booking",
        tone: "booking",
        title: booking.resourceName || booking.purpose || "Booking activity",
        summary: `${booking.status} · ${booking.ownerDisplayName || booking.ownerEmail || "Campus user"}`,
        detail: formatDateRange(booking.startTime, booking.endTime),
        createdAt: booking.updatedAt || booking.createdAt || booking.startTime,
      })),
      ...tickets.map((ticket) => ({
        id: `ticket-${ticket.id}`,
        type: "Ticket",
        tone: "ticket",
        title: ticket.title,
        summary: `${ticket.status} · ${ticket.priority}`,
        detail: ticket.location,
        createdAt: ticket.updatedAt || ticket.createdAt,
      })),
      ...notifications.map((notification) => ({
        id: `notification-${notification.id}`,
        type: "Notification",
        tone: "notification",
        title: notification.title,
        summary: notification.type,
        detail: notification.message,
        createdAt: notification.createdAt,
      })),
      ...auditRows.map((row) => ({
        id: `audit-${row.id}`,
        type: "Audit",
        tone: "audit",
        title: row.action,
        summary: row.entityType,
        detail: row.details || row.performedByEmail || row.performedByIdentifier || "System action",
        createdAt: row.createdAt,
      })),
    ];

    return items
      .filter((item) => item.createdAt)
      .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
      .slice(0, 8);
  }, [auditRows, bookings, notifications, tickets]);

  const firstName = currentUser?.displayName?.split(" ")?.[0] || "there";

  return (
    <PageShell
      eyebrow="Campus portal home"
      title={`Welcome back, ${firstName}`}
      description="Use this portal home to move between operational modules quickly, understand current queue pressure, and pick the next action that matters."
      actions={(
        <div className="stacked-actions">
          <button type="button" className="button button--ghost" onClick={() => void loadDashboard()}>
            Refresh portal
          </button>
          <Link className="button button--primary" to={roleFocus.actionHref}>
            {roleFocus.actionLabel}
          </Link>
        </div>
      )}
      meta={(
        <>
          <StatusBadge value={currentUser?.role} />
          <span className="page-shell__meta-text">{authMode === "basic" ? "Basic Auth demo session" : "Google-authenticated portal session"}</span>
        </>
      )}
    >
      <FeedbackBanner error={error} />

      <section className="portal-home-grid">
        <SectionCard
          tone="hero"
          eyebrow="Today at a glance"
          title="Your operations workspace"
          description={roleFocus.message}
          className="portal-home-grid__hero"
          actions={(
            <div className="stacked-actions">
              {roleFocus.quickLinks.map((link) => (
                <Link key={link.href + link.label} className="button button--subtle" to={link.href}>
                  {link.label}
                </Link>
              ))}
            </div>
          )}
        >
          <div className="portal-hero">
            <div className="portal-hero__metrics">
              <div className="portal-hero__metric">
                <span>Resources ready</span>
                <strong>{loading ? "..." : compactCount(activeResources)}</strong>
              </div>
              <div className="portal-hero__metric">
                <span>Pending bookings</span>
                <strong>{loading ? "..." : compactCount(pendingBookings)}</strong>
              </div>
              <div className="portal-hero__metric">
                <span>Active tickets</span>
                <strong>{loading ? "..." : compactCount(ticketPressure)}</strong>
              </div>
            </div>

            <div className="portal-hero__panel">
              <span className="portal-hero__eyebrow">Session posture</span>
              <strong>{currentUser?.displayName || currentUser?.email}</strong>
              <p>{isAdmin ? "Administrative control is active." : isTechnician ? "Technician workflow access is active." : "Self-service workspace access is active."}</p>
              <div className="portal-hero__chips">
                <StatusBadge value={currentUser?.role} />
                <span className="portal-chip">{authMode === "basic" ? "Basic Auth" : "Google OAuth"}</span>
                <span className="portal-chip">Backend live</span>
              </div>
            </div>
          </div>
        </SectionCard>

        <SectionCard
          tone="soft"
          eyebrow="Announcements and alerts"
          title="Operational focus"
          description="Short prompts that help the current role decide what to do next."
          className="portal-home-grid__announcements"
        >
          <div className="announcement-stack">
            {operationalAlerts.map((alert) => (
              <article key={alert.title} className={`announcement-card announcement-card--${alert.tone}`}>
                <strong>{alert.title}</strong>
                <p>{alert.message}</p>
              </article>
            ))}
          </div>
        </SectionCard>
      </section>

      <section className="dashboard-stat-grid dashboard-stat-grid--portal">
        <StatCard
          icon={Boxes}
          label="Resources"
          value={loading ? "..." : compactCount(resources.length)}
          hint={`${activeResources} ready for campus use`}
          trend="Catalogue module"
          tone="teal"
        />
        <StatCard
          icon={CalendarRange}
          label="Bookings"
          value={loading ? "..." : compactCount(snapshot?.bookings?.totalElements ?? 0)}
          hint={`${pendingBookings} pending in the visible queue`}
          trend="Reservation workflow"
          tone="cream"
        />
        <StatCard
          icon={Wrench}
          label="Tickets"
          value={loading ? "..." : compactCount(snapshot?.tickets?.totalElements ?? 0)}
          hint={`${ticketPressure} currently active`}
          trend="Maintenance desk"
          tone="sand"
        />
        <StatCard
          icon={Bell}
          label="Notifications"
          value={loading ? "..." : compactCount(snapshot?.notifications?.totalElements ?? 0)}
          hint={`${unreadNotifications} unread messages in inbox`}
          trend="Inbox and alerts"
          tone="teal"
        />
      </section>

      <section className="portal-dashboard-columns">
        <SectionCard
          eyebrow="Quick actions"
          title="Portal modules"
          description="Jump into the module that matches the next operational step."
          className="portal-dashboard-columns__wide"
        >
          {loading && !snapshot ? (
            <LoadingState title="Loading module snapshot" message="Preparing the portal modules." />
          ) : (
            <div className="module-grid">
              <Link to="/resources" className="module-card">
                <div className="module-card__icon">
                  <Boxes size={18} />
                </div>
                <div>
                  <strong>Resource catalogue</strong>
                  <p>Browse campus spaces, status, and availability windows.</p>
                </div>
                <span>{resources.length} visible</span>
              </Link>

              <Link to="/bookings" className="module-card">
                <div className="module-card__icon">
                  <CalendarRange size={18} />
                </div>
                <div>
                  <strong>Booking workspace</strong>
                  <p>Move through pending approvals, requests, and ownership updates.</p>
                </div>
                <span>{snapshot?.bookings?.totalElements ?? 0} in scope</span>
              </Link>

              <Link to="/tickets" className="module-card">
                <div className="module-card__icon">
                  <Wrench size={18} />
                </div>
                <div>
                  <strong>Maintenance desk</strong>
                  <p>Track incident pressure, technician assignments, and service progress.</p>
                </div>
                <span>{snapshot?.tickets?.totalElements ?? 0} active records</span>
              </Link>

              <Link to="/notifications" className="module-card">
                <div className="module-card__icon">
                  <Bell size={18} />
                </div>
                <div>
                  <strong>Notification inbox</strong>
                  <p>Clear unread alerts and keep operational follow-up moving.</p>
                </div>
                <span>{snapshot?.notifications?.totalElements ?? 0} messages</span>
              </Link>

              {isAdmin ? (
                <>
                  <Link to="/users" className="module-card">
                    <div className="module-card__icon">
                      <Shield size={18} />
                    </div>
                    <div>
                      <strong>User directory</strong>
                      <p>Review platform roles, technicians, and access posture.</p>
                    </div>
                    <span>Admin control</span>
                  </Link>

                  <Link to="/audit" className="module-card">
                    <div className="module-card__icon">
                      <Clock3 size={18} />
                    </div>
                    <div>
                      <strong>Audit workbench</strong>
                      <p>Read audit history and run legacy data support tools.</p>
                    </div>
                    <span>{snapshot?.auditLogs?.totalElements ?? 0} logged actions</span>
                  </Link>
                </>
              ) : null}
            </div>
          )}
        </SectionCard>

        <SectionCard
          tone="soft"
          eyebrow="Recent activity"
          title="Operational timeline"
          description="A blended stream of booking, ticket, notification, and audit activity from the current visible scope."
        >
          {loading && !snapshot ? (
            <LoadingState title="Loading timeline" message="Merging recent operational events." compact />
          ) : activityTimeline.length ? (
            <div className="timeline-list">
              {activityTimeline.map((item) => (
                <article key={item.id} className="timeline-item">
                  <div className={`timeline-item__dot timeline-item__dot--${item.tone}`} />
                  <div className="timeline-item__copy">
                    <div className="timeline-item__top">
                      <strong>{item.title}</strong>
                      <span>{formatCompactDateTime(item.createdAt)}</span>
                    </div>
                    <p>{item.detail}</p>
                    <div className="timeline-item__meta">
                      <StatusBadge value={item.type} variant="general" />
                      <span>{item.summary}</span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <EmptyState
              compact
              icon={Sparkles}
              title="No recent activity"
              message="As new bookings, tickets, notifications, or audit events arrive, they will surface here."
            />
          )}
        </SectionCard>
      </section>

      <section className="portal-dashboard-columns">
        <SectionCard
          eyebrow="Live queues"
          title="Current operational lanes"
          description="Read the top items in each queue without leaving portal home."
          className="portal-dashboard-columns__wide"
        >
          {loading && !snapshot ? (
            <LoadingState title="Loading queue cards" message="Checking bookings, tickets, and notifications." />
          ) : (
            <div className="lane-grid">
              <article className="lane-card">
                <div className="lane-card__header">
                  <div>
                    <span>Bookings</span>
                    <strong>{snapshot?.bookings?.totalElements ?? 0}</strong>
                  </div>
                  <Link to="/bookings" className="lane-card__link">
                    Open
                    <ExternalLink size={14} />
                  </Link>
                </div>
                {bookings.length ? (
                  <div className="list-stack">
                    {bookings.slice(0, 3).map((booking) => (
                      <div key={booking.id} className="list-item">
                        <div>
                          <strong>{booking.resourceName}</strong>
                          <p>{booking.ownerDisplayName || booking.ownerEmail || "Campus user"}</p>
                        </div>
                        <div className="list-item__meta">
                          <StatusBadge value={booking.status} />
                          <span>{formatDateRange(booking.startTime, booking.endTime)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState compact title="No booking items" message="No booking records are visible in this portal scope." />
                )}
              </article>

              <article className="lane-card">
                <div className="lane-card__header">
                  <div>
                    <span>Tickets</span>
                    <strong>{snapshot?.tickets?.totalElements ?? 0}</strong>
                  </div>
                  <Link to="/tickets" className="lane-card__link">
                    Open
                    <ExternalLink size={14} />
                  </Link>
                </div>
                {tickets.length ? (
                  <div className="list-stack">
                    {tickets.slice(0, 3).map((ticket) => (
                      <div key={ticket.id} className="list-item">
                        <div>
                          <strong>{ticket.title}</strong>
                          <p>{ticket.location}</p>
                        </div>
                        <div className="list-item__meta">
                          <StatusBadge value={ticket.priority} variant={ticket.priority} />
                          <StatusBadge value={ticket.status} />
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState compact title="No active tickets" message="Ticket pressure is currently low in this scope." />
                )}
              </article>

              <article className="lane-card">
                <div className="lane-card__header">
                  <div>
                    <span>Notifications</span>
                    <strong>{snapshot?.notifications?.totalElements ?? 0}</strong>
                  </div>
                  <Link to="/notifications" className="lane-card__link">
                    Open
                    <ExternalLink size={14} />
                  </Link>
                </div>
                {notifications.length ? (
                  <div className="list-stack">
                    {notifications.slice(0, 3).map((notification) => (
                      <div key={notification.id} className="list-item">
                        <div>
                          <strong>{notification.title}</strong>
                          <p>{notification.message}</p>
                        </div>
                        <div className="list-item__meta">
                          <StatusBadge value={notification.type} variant="general" />
                          <span>{formatCompactDateTime(notification.createdAt)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState compact title="Inbox is clear" message="No notification records are visible right now." />
                )}
              </article>
            </div>
          )}
        </SectionCard>

        <SectionCard
          tone="soft"
          eyebrow="Portal guidance"
          title={roleFocus.title}
          description="A simple reminder of what this role should prioritise next."
        >
          <div className="dashboard-sidecard">
            <div className="dashboard-sidecard__icon">
              <Sparkles size={18} />
            </div>
            <div>
              <strong>Suggested next move</strong>
              <p>
                {isAdmin
                  ? "Review queue pressure, then move into the user directory or audit workbench for any admin tasks that need escalation."
                  : isTechnician
                    ? "Open assigned tickets, move the next live issue forward, and clear inbox alerts after each update."
                    : "Review your latest booking and ticket activity, then clear unread notifications before starting a new request."}
              </p>
            </div>
          </div>

          <div className="dashboard-insight-list">
            <div className="dashboard-insight">
              <Clock3 size={16} />
              <span>This home view always reflects live backend data instead of placeholder widgets.</span>
            </div>
            <div className="dashboard-insight">
              <Shield size={16} />
              <span>Role-aware visibility keeps the portal clean and limits each lane to relevant operational work.</span>
            </div>
          </div>
        </SectionCard>
      </section>
    </PageShell>
  );
}
