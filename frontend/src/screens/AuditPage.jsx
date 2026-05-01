import { DatabaseBackup, RefreshCcw, ScrollText, Shield, User, Wrench, CalendarRange, Boxes, Bell, Activity } from "lucide-react";
import { useEffect, useState } from "react";
import ConfirmDialog from "../components/ConfirmDialog.jsx";
import DataToolbar from "../components/DataToolbar.jsx";
import EmptyState from "../components/EmptyState.jsx";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import LoadingState from "../components/LoadingState.jsx";
import PageShell from "../components/PageShell.jsx";
import PaginationBar from "../components/PaginationBar.jsx";
import RoleGate from "../components/RoleGate.jsx";
import SectionCard from "../components/SectionCard.jsx";
import StatCard from "../components/StatCard.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { AUDIT_ENTITY_TYPES, AUDIT_SORT_OPTIONS, PAGE_SIZE_OPTIONS } from "../lib/options.js";
import { compactCount, formatCompactDateTime, toDateTimeLocalValue, titleizeEnum } from "../lib/format.js";
import { api } from "../lib/api.js";

function getEntityIcon(type) {
  switch (type) {
    case "USER": return <User size={20} />;
    case "TICKET": return <Wrench size={20} />;
    case "BOOKING": return <CalendarRange size={20} />;
    case "RESOURCE": return <Boxes size={20} />;
    case "NOTIFICATION": return <Bell size={20} />;
    case "BACKFILL": return <DatabaseBackup size={20} />;
    default: return <Activity size={20} />;
  }
}

const filterTemplate = {
  entityType: "",
  action: "",
  performedBy: "",
  from: "",
  to: "",
  size: 10,
  sort: "createdAt,desc",
};

export default function AuditPage() {
  const { isAdmin, currentUser } = useAuth();
  const [filters, setFilters] = useState(filterTemplate);
  const [appliedFilters, setAppliedFilters] = useState(filterTemplate);
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [backfillSummary, setBackfillSummary] = useState(null);
  const [backfillError, setBackfillError] = useState(null);
  const [backfillBusy, setBackfillBusy] = useState(false);
  const [confirmBackfill, setConfirmBackfill] = useState(false);

  async function loadAuditLogs(activePage = page, activeFilters = appliedFilters) {
    setLoading(true);
    setError(null);

    try {
      const response = await api.audit.list({
        page: activePage,
        size: activeFilters.size,
        sort: activeFilters.sort,
        entityType: activeFilters.entityType,
        action: activeFilters.action,
        performedBy: activeFilters.performedBy,
        from: activeFilters.from || undefined,
        to: activeFilters.to || undefined,
      });
      setPageData(response);
    } catch (loadError) {
      setError(loadError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (isAdmin) {
      void loadAuditLogs(page, appliedFilters);
    }
  }, [page, appliedFilters, isAdmin]);

  async function handleBackfill() {
    setBackfillBusy(true);
    setBackfillSummary(null);
    setBackfillError(null);

    try {
      const response = await api.admin.backfillUserLinks();
      setBackfillSummary(response);
      setConfirmBackfill(false);
      await loadAuditLogs(0, appliedFilters);
      setPage(0);
    } catch (runError) {
      setBackfillError(runError);
    } finally {
      setBackfillBusy(false);
    }
  }

  const auditRows = pageData?.content || [];

  return (
    <RoleGate
      allow={isAdmin}
      fallback={(
        <PageShell
          eyebrow="Admin tools"
          title="Audit logs and backfill"
          description="This operational lane is reserved for administrators only."
          meta={<StatusBadge value={currentUser?.role} />}
        >
          <SectionCard>
            <EmptyState
              icon={Shield}
              title="Admin access required"
              message="Use an admin account to inspect audit history or run legacy user-link backfill."
            />
          </SectionCard>
        </PageShell>
      )}
    >
      <PageShell
        eyebrow="Admin tools"
        title="Audit history and legacy backfill"
        description="Trace operational actions, query admin history, and safely link older string-based records to real users."
        actions={(
          <button type="button" className="button button--primary" onClick={() => setConfirmBackfill(true)}>
            <DatabaseBackup size={16} />
            Run backfill
          </button>
        )}
        meta={(
          <>
            <StatusBadge value="ADMIN" />
            <span className="page-shell__meta-text">Audit and backfill actions are visible only to administrators and stay backend-tracked.</span>
          </>
        )}
      >
        <section className="dashboard-stat-grid dashboard-stat-grid--compact">
          <StatCard icon={ScrollText} label="Visible audit rows" value={loading ? "..." : compactCount(pageData?.totalElements ?? 0)} hint="Matching the current audit query" tone="teal" />
          <StatCard icon={DatabaseBackup} label="Backfill scanned" value={backfillSummary ? compactCount(backfillSummary.recordsScanned) : "-"} hint="Latest admin-triggered backfill run" tone="sand" />
          <StatCard icon={DatabaseBackup} label="Backfill linked" value={backfillSummary ? compactCount(backfillSummary.recordsLinked) : "-"} hint="Legacy links attached to real users" tone="cream" />
          <StatCard icon={DatabaseBackup} label="Backfill skipped" value={backfillSummary ? compactCount(backfillSummary.recordsSkipped) : "-"} hint="Records left untouched safely" tone="teal" />
        </section>

        <section className="dashboard-grid">
          <SectionCard
            eyebrow="Legacy support"
            title="User-link backfill"
            description="This scans old booking owner, ticket reporter, and technician strings and links them to local users when emails match."
            actions={(
              <button type="button" className="button button--primary" onClick={() => setConfirmBackfill(true)} disabled={backfillBusy}>
                <DatabaseBackup size={16} />
                Trigger backfill
              </button>
            )}
          >
            <FeedbackBanner error={backfillError || (backfillSummary ? { message: "Backfill completed successfully." } : null)} kind={backfillError ? "error" : "success"} />

            {backfillSummary ? (
              <div className="summary-grid">
                <div className="summary-item">
                  <span>Scanned</span>
                  <strong>{backfillSummary.recordsScanned}</strong>
                </div>
                <div className="summary-item">
                  <span>Linked</span>
                  <strong>{backfillSummary.recordsLinked}</strong>
                </div>
                <div className="summary-item">
                  <span>Skipped</span>
                  <strong>{backfillSummary.recordsSkipped}</strong>
                </div>
                <div className="summary-item">
                  <span>Bookings</span>
                  <strong>{backfillSummary.bookingsLinked}</strong>
                </div>
                <div className="summary-item">
                  <span>Ticket reporters</span>
                  <strong>{backfillSummary.ticketReportersLinked}</strong>
                </div>
                <div className="summary-item">
                  <span>Ticket technicians</span>
                  <strong>{backfillSummary.ticketTechniciansLinked}</strong>
                </div>
              </div>
            ) : (
              <EmptyState
                compact
                icon={DatabaseBackup}
                title="No backfill run yet"
                message="Run the admin utility when you need to attach legacy string-based records to local users."
              />
            )}
          </SectionCard>

          <DataToolbar
            eyebrow="Audit filters"
            title="Query admin history"
            description="Filter by entity type, action, operator, date range, sort order, and page size."
          >
            <form
              onSubmit={(event) => {
                event.preventDefault();
                setPage(0);
                setAppliedFilters({ ...filters });
              }}
              style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "flex-end", paddingTop: "12px" }}
            >
                <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 160px", minWidth: "140px" }}>
                  <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Entity type</span>
                  <select value={filters.entityType} onChange={(event) => setFilters((current) => ({ ...current, entityType: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
                    <option value="">All entities</option>
                    {AUDIT_ENTITY_TYPES.map((entityType) => (
                      <option key={entityType} value={entityType}>
                        {entityType}
                      </option>
                    ))}
                  </select>
                </label>
                <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 160px", minWidth: "140px" }}>
                  <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Action</span>
                  <input value={filters.action} onChange={(event) => setFilters((current) => ({ ...current, action: event.target.value }))} placeholder="BOOKING_CREATED" style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none" }} />
                </label>

              <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 200px", minWidth: "180px" }}>
                <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Performed by</span>
                <input value={filters.performedBy} onChange={(event) => setFilters((current) => ({ ...current, performedBy: event.target.value }))} placeholder="admin@example.com" style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none" }} />
              </label>

                <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 180px", minWidth: "150px" }}>
                  <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>From</span>
                  <input type="datetime-local" value={toDateTimeLocalValue(filters.from)} onChange={(event) => setFilters((current) => ({ ...current, from: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }} />
                </label>
                <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 180px", minWidth: "150px" }}>
                  <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>To</span>
                  <input type="datetime-local" value={toDateTimeLocalValue(filters.to)} onChange={(event) => setFilters((current) => ({ ...current, to: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }} />
                </label>

                <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 160px", minWidth: "140px" }}>
                  <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Sort</span>
                  <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
                    {AUDIT_SORT_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>

                <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "0 0 120px" }}>
                  <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Page size</span>
                  <select value={filters.size} onChange={(event) => setFilters((current) => ({ ...current, size: Number(event.target.value) }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
                    {PAGE_SIZE_OPTIONS.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </label>

              <div style={{ display: "flex", gap: "12px", flex: "1 1 100%", justifyContent: "flex-end", marginTop: "8px", paddingTop: "16px", borderTop: "1px dashed #cbd5e1" }}>
                <button
                  type="button"
                  className="button button--subtle"
                  style={{ padding: "8px 16px", minHeight: "36px", fontSize: "13px" }}
                  onClick={() => {
                    setFilters(filterTemplate);
                    setAppliedFilters(filterTemplate);
                    setPage(0);
                  }}
                >
                  Reset
                </button>
                <button type="submit" className="button button--primary" style={{ padding: "8px 16px", minHeight: "36px", fontSize: "13px" }}>
                  Apply audit filters
                </button>
              </div>
            </form>
          </DataToolbar>
        </section>

        <FeedbackBanner error={error} />

        <SectionCard
          eyebrow="Audit stream"
          title={loading ? "Loading audit history" : `${pageData?.totalElements ?? 0} matching events`}
          description="Operational history remains queryable without leaving the admin cockpit."
          actions={
            <button type="button" className="button button--subtle" onClick={() => void loadAuditLogs(page, appliedFilters)}>
              <RefreshCcw size={16} />
              Refresh
            </button>
          }
          footer={<PaginationBar pageData={pageData} onPageChange={setPage} />}
        >
          {loading ? (
            <LoadingState title="Loading audit stream" message="Pulling the latest admin-tracked backend activity." lines={6} />
          ) : auditRows.length ? (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Event & Entity</th>
                    <th>Action</th>
                    <th>Performed by</th>
                    <th>Details</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {auditRows.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                          <div style={{ width: "40px", height: "40px", borderRadius: "8px", backgroundColor: "#f1f5f9", color: "#64748b", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                            {getEntityIcon(row.entityType)}
                          </div>
                          <div>
                            <div style={{ fontWeight: "600", color: "#1e293b", fontSize: "14px" }}>{row.entityType ? titleizeEnum(row.entityType) : "System"}</div>
                            <div style={{ fontSize: "12px", color: "#64748b", marginTop: "2px", fontFamily: "monospace", letterSpacing: "0.5px" }}>#{row.entityId || "SYS"}</div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span style={{ display: "inline-flex", padding: "6px 10px", backgroundColor: "#eff6ff", color: "#3b82f6", borderRadius: "6px", fontSize: "12px", fontWeight: "600", border: "1px solid #dbeafe" }}>
                          {row.action}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                          <div style={{ width: "32px", height: "32px", borderRadius: "50%", backgroundColor: "#e0e7ff", color: "#4f46e5", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "12px", fontWeight: "bold", flexShrink: 0 }}>
                            {(row.performedByEmail || row.performedByIdentifier || 'S').charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div style={{ fontWeight: "500", color: "#1e293b", fontSize: "13px" }}>{row.performedByEmail || row.performedByIdentifier || "System"}</div>
                            <div style={{ fontSize: "11px", color: "#64748b" }}>{row.performedByEmail ? "User Account" : "System Event"}</div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div style={{ fontSize: "13px", color: "#475569", maxWidth: "300px", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden", lineHeight: "1.5" }}>
                          {row.details || "-"}
                        </div>
                      </td>
                      <td>
                        <div style={{ fontSize: "13px", color: "#475569", whiteSpace: "nowrap" }}>
                          {formatCompactDateTime(row.createdAt)}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No audit logs matched" message="Try a broader date range or clear one of the audit filters." />
          )}
        </SectionCard>

        <ConfirmDialog
          open={confirmBackfill}
          title="Run legacy backfill"
          message="Scan older booking and ticket records now and link any matching user emails to real local user accounts."
          confirmLabel="Run backfill"
          tone="primary"
          busy={backfillBusy}
          error={backfillError}
          onConfirm={handleBackfill}
          onClose={() => setConfirmBackfill(false)}
        />
      </PageShell>
    </RoleGate>
  );
}
