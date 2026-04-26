import { Bell, Boxes, CalendarRange, ScrollText, Shield, Wrench } from "lucide-react";
import { useEffect, useState } from "react";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { formatCompactDateTime, formatDateRange } from "../lib/format.js";
import { api } from "../lib/api.js";

function MetricCard({ label, value, hint, icon: Icon }) {
  return (
    <article className="metric-card">
      <div className="metric-card__icon">
        <Icon size={20} />
      </div>
      <div>
        <p>{label}</p>
        <strong>{value ?? "—"}</strong>
        <span>{hint}</span>
      </div>
    </article>
  );
}

export default function DashboardPage() {
  const { isAdmin } = useAuth();
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

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h2>Operational overview</h2>
          <p>Quick visibility into live resources, workflow queues, unread alerts, and admin activity.</p>
        </div>
        <button type="button" className="button button--ghost" onClick={() => void loadDashboard()}>
          Refresh dashboard
        </button>
      </section>

      <FeedbackBanner error={error} />

      <section className="metric-grid">
        <MetricCard
          label="Resources"
          value={loading ? "…" : snapshot?.resources.length ?? 0}
          hint="Public catalogue size"
          icon={Boxes}
        />
        <MetricCard
          label="Bookings"
          value={loading ? "…" : snapshot?.bookings?.totalElements ?? 0}
          hint="Filtered to your current access"
          icon={CalendarRange}
        />
        <MetricCard
          label="Tickets"
          value={loading ? "…" : snapshot?.tickets?.totalElements ?? 0}
          hint="Visible incident workload"
          icon={Wrench}
        />
        <MetricCard
          label="Unread notifications"
          value={loading ? "…" : snapshot?.notifications?.totalElements ?? 0}
          hint="Current inbox pressure"
          icon={Bell}
        />
        <MetricCard
          label="Audit logs"
          value={loading ? "…" : snapshot?.auditLogs?.totalElements ?? (isAdmin ? 0 : "Admin")}
          hint={isAdmin ? "Recent admin-tracked actions" : "Admin only"}
          icon={ScrollText}
        />
        <MetricCard label="Access mode" value={isAdmin ? "Admin" : "User"} hint="Current dashboard privilege level" icon={Shield} />
      </section>

      <section className="three-up-grid">
        <article className="panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Latest bookings</p>
              <h3>Resource requests</h3>
            </div>
          </div>
          {snapshot?.bookings?.content?.length ? (
            <div className="list-stack">
              {snapshot.bookings.content.map((booking) => (
                <div key={booking.id} className="list-item">
                  <div>
                    <strong>{booking.resourceName}</strong>
                    <p>{booking.ownerEmail}</p>
                  </div>
                  <div className="list-item__meta">
                    <StatusBadge value={booking.status} />
                    <span>{formatDateRange(booking.startTime, booking.endTime)}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">No bookings available for this account yet.</div>
          )}
        </article>

        <article className="panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Latest tickets</p>
              <h3>Operations issues</h3>
            </div>
          </div>
          {snapshot?.tickets?.content?.length ? (
            <div className="list-stack">
              {snapshot.tickets.content.map((ticket) => (
                <div key={ticket.id} className="list-item">
                  <div>
                    <strong>{ticket.title}</strong>
                    <p>{ticket.location}</p>
                  </div>
                  <div className="list-item__meta">
                    <StatusBadge value={ticket.status} />
                    <span>{ticket.priority}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">No tickets visible right now.</div>
          )}
        </article>

        <article className="panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Unread notifications</p>
              <h3>Inbox focus</h3>
            </div>
          </div>
          {snapshot?.notifications?.content?.length ? (
            <div className="list-stack">
              {snapshot.notifications.content.map((notification) => (
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
            <div className="empty-state">No unread notifications at the moment.</div>
          )}
        </article>
      </section>
    </div>
  );
}
