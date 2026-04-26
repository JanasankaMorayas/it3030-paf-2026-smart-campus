import {
  Bell,
  Boxes,
  CalendarRange,
  Clock3,
  ScrollText,
  Shield,
  Sparkles,
  Wrench,
} from "lucide-react";
import { useEffect, useState } from "react";
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
      title: "Admin command posture",
      message: "You can review approvals, assignments, role changes, audits, and legacy cleanup from one desk.",
      actionLabel: "Open audit lane",
      actionHref: "/audit",
    };
  }

  if (isTechnician) {
    return {
      title: "Technician workload",
      message: "Stay focused on assigned incidents, ticket transitions, and unread alerts tied to active work.",
      actionLabel: "Open ticket queue",
      actionHref: "/tickets",
    };
  }

  return {
    title: "User self-service lane",
    message: "Track your requests, report issues quickly, and follow notifications without leaving the dashboard.",
    actionLabel: "Open bookings",
    actionHref: "/bookings",
  };
}

export default function DashboardPage() {
  const { currentUser, isAdmin, isTechnician } = useAuth();
  const [snapshot, setSnapshot] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  async function loadDashboard() {
    setLoading(true);
    setError(null);

    try {
      const [resources, bookings, tickets, notifications, auditLogs] = await Promise.allSettled([
        api.resources.list(),
        api.bookings.list({ page: 0, size: 5, sort: "createdAt,desc" }),
        api.tickets.list({ page: 0, size: 5, sort: "createdAt,desc" }),
        api.notifications.unread({ page: 0, size: 5, sort: "createdAt,desc" }),
        isAdmin ? api.audit.list({ page: 0, size: 5, sort: "createdAt,desc" }) : Promise.resolve(null),
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
  const outOfServiceResources = resources.filter((resource) => resource.status === "OUT_OF_SERVICE").length;
  const pendingBookings = bookings.filter((booking) => booking.status === "PENDING").length;
  const resolvedTickets = tickets.filter((ticket) => ticket.status === "RESOLVED").length;
  const roleFocus = deriveRoleFocus({ isAdmin, isTechnician });

  return (
    <PageShell
      eyebrow="Operations cockpit"
      title="Smart Campus control desk"
      description="Live visibility into resource readiness, booking workflow, maintenance workload, notifications, and admin oversight."
      actions={(
        <div className="stacked-actions">
          <button type="button" className="button button--ghost" onClick={() => void loadDashboard()}>
            Refresh cockpit
          </button>
          <Link className="button button--primary" to={roleFocus.actionHref}>
            {roleFocus.actionLabel}
          </Link>
        </div>
      )}
      meta={(
        <>
          <StatusBadge value={currentUser?.role} />
          <span className="page-shell__meta-text">{roleFocus.message}</span>
        </>
      )}
    >
      <FeedbackBanner error={error} />

      <section className="dashboard-stat-grid">
        <StatCard
          icon={Boxes}
          label="Resources"
          value={loading ? "..." : compactCount(resources.length)}
          hint={loading ? "Syncing public catalogue" : `${activeResources} active, ${outOfServiceResources} unavailable`}
          tone="teal"
        />
        <StatCard
          icon={CalendarRange}
          label="Bookings in scope"
          value={loading ? "..." : compactCount(snapshot?.bookings?.totalElements ?? 0)}
          hint={loading ? "Checking reservation flow" : `${pendingBookings} pending on this board`}
          tone="sand"
        />
        <StatCard
          icon={Wrench}
          label="Tickets in scope"
          value={loading ? "..." : compactCount(snapshot?.tickets?.totalElements ?? 0)}
          hint={loading ? "Reviewing maintenance queue" : `${resolvedTickets} resolved in the current snapshot`}
          tone="teal"
        />
        <StatCard
          icon={Bell}
          label="Unread alerts"
          value={loading ? "..." : compactCount(snapshot?.notifications?.totalElements ?? 0)}
          hint={loading ? "Checking inbox pressure" : "Unread notification inbox for this session"}
          tone="cream"
        />
        <StatCard
          icon={ScrollText}
          label="Audit visibility"
          value={loading ? "..." : isAdmin ? compactCount(snapshot?.auditLogs?.totalElements ?? 0) : "Admin"}
          hint={isAdmin ? "Recent admin-tracked activity" : "Audit lane unlocks for admins only"}
          tone="cream"
        />
        <StatCard
          icon={Shield}
          label="Current access"
          value={currentUser?.role || "-"}
          hint={currentUser?.displayName || currentUser?.email}
          tone="teal"
        />
      </section>

      <section className="dashboard-grid">
        <SectionCard
          eyebrow="Workflow board"
          title="Live queues"
          description="Keep the immediate next decisions visible without opening every module."
          className="dashboard-grid__wide"
        >
          {loading && !snapshot ? (
            <LoadingState title="Loading live queues" message="Pulling bookings, tickets, and notification activity." />
          ) : (
            <div className="cockpit-lanes">
              <article className="cockpit-lane">
                <div className="cockpit-lane__header">
                  <div>
                    <span className="cockpit-lane__label">Bookings</span>
                    <strong>{snapshot?.bookings?.totalElements ?? 0}</strong>
                  </div>
                  <StatusBadge value={isAdmin ? "ADMIN" : "USER"} variant="general" />
                </div>
                {bookings.length ? (
                  <div className="list-stack">
                    {bookings.map((booking) => (
                      <div key={booking.id} className="list-item">
                        <div>
                          <strong>{booking.resourceName}</strong>
                          <p>{booking.ownerDisplayName || booking.ownerEmail}</p>
                        </div>
                        <div className="list-item__meta">
                          <StatusBadge value={booking.status} />
                          <span>{formatDateRange(booking.startTime, booking.endTime)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState title="No booking traffic" message="No booking records are visible in your current backend scope." compact />
                )}
              </article>

              <article className="cockpit-lane">
                <div className="cockpit-lane__header">
                  <div>
                    <span className="cockpit-lane__label">Tickets</span>
                    <strong>{snapshot?.tickets?.totalElements ?? 0}</strong>
                  </div>
                  <StatusBadge value={isTechnician ? "TECHNICIAN" : currentUser?.role} variant="general" />
                </div>
                {tickets.length ? (
                  <div className="list-stack">
                    {tickets.map((ticket) => (
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
                  <EmptyState title="No ticket pressure" message="The current ticket queue is empty for this role and scope." compact />
                )}
              </article>

              <article className="cockpit-lane">
                <div className="cockpit-lane__header">
                  <div>
                    <span className="cockpit-lane__label">Inbox</span>
                    <strong>{snapshot?.notifications?.totalElements ?? 0}</strong>
                  </div>
                  <StatusBadge value="UNREAD" variant="pending" />
                </div>
                {notifications.length ? (
                  <div className="list-stack">
                    {notifications.map((notification) => (
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
                  <EmptyState title="Inbox calm" message="No unread notifications are waiting right now." compact />
                )}
              </article>
            </div>
          )}
        </SectionCard>

        <SectionCard
          eyebrow="Role guidance"
          title={roleFocus.title}
          description={roleFocus.message}
          actions={(
            <Link className="button button--ghost" to={roleFocus.actionHref}>
              Open lane
            </Link>
          )}
        >
          <div className="dashboard-sidecard">
            <div className="dashboard-sidecard__icon">
              <Sparkles size={18} />
            </div>
            <div>
              <strong>Recommended next move</strong>
              <p>
                {isAdmin
                  ? "Review pending bookings, assign any unowned incidents, then scan audit history for important admin changes."
                  : isTechnician
                    ? "Open the ticket queue, move active issues into progress, and clear assignment notifications as you go."
                    : "Check your pending bookings, watch unread alerts, and update open tickets with fresh notes when needed."}
              </p>
            </div>
          </div>
          <div className="dashboard-insight-list">
            <div className="dashboard-insight">
              <Clock3 size={16} />
              <span>Last dashboard sync refreshes backend-backed data only, no mock widgets.</span>
            </div>
            <div className="dashboard-insight">
              <Shield size={16} />
              <span>Views remain role-aware, so totals reflect your current access lane.</span>
            </div>
          </div>
        </SectionCard>
      </section>

      {isAdmin ? (
        <SectionCard
          eyebrow="Admin pulse"
          title="Recent audit activity"
          description="Quick read on important backend-tracked changes without leaving the cockpit."
          actions={<Link className="button button--ghost" to="/audit">Open full audit</Link>}
        >
          {loading && !snapshot ? (
            <LoadingState title="Loading audit pulse" message="Reading the latest admin activity." compact />
          ) : auditRows.length ? (
            <div className="list-stack">
              {auditRows.map((row) => (
                <div key={row.id} className="list-item">
                  <div>
                    <strong>{row.action}</strong>
                    <p>{row.details || `${row.entityType} #${row.entityId || "-"}`}</p>
                  </div>
                  <div className="list-item__meta">
                    <StatusBadge value={row.entityType} variant="general" />
                    <span>{row.performedByEmail || row.performedByIdentifier || "System"}</span>
                    <span>{formatCompactDateTime(row.createdAt)}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="No audit activity" message="No admin-tracked events are available in the latest snapshot." compact />
          )}
        </SectionCard>
      ) : null}
    </PageShell>
  );
}
