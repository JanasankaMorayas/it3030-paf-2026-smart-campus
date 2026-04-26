import { DatabaseBackup, RefreshCcw, ScrollText, Shield } from "lucide-react";
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
import { compactCount, formatCompactDateTime, toDateTimeLocalValue } from "../lib/format.js";
import { api } from "../lib/api.js";

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
          <div className="stacked-actions">
            <button type="button" className="button button--ghost" onClick={() => void loadAuditLogs(page, appliedFilters)}>
              <RefreshCcw size={16} />
              Refresh logs
            </button>
            <button type="button" className="button button--primary" onClick={() => setConfirmBackfill(true)}>
              <DatabaseBackup size={16} />
              Run backfill
            </button>
          </div>
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
              className="form-grid"
              onSubmit={(event) => {
                event.preventDefault();
                setPage(0);
                setAppliedFilters({ ...filters });
              }}
            >
              <div className="form-grid form-grid--two">
                <label className="field">
                  <span>Entity type</span>
                  <select value={filters.entityType} onChange={(event) => setFilters((current) => ({ ...current, entityType: event.target.value }))}>
                    <option value="">All entities</option>
                    {AUDIT_ENTITY_TYPES.map((entityType) => (
                      <option key={entityType} value={entityType}>
                        {entityType}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Action</span>
                  <input value={filters.action} onChange={(event) => setFilters((current) => ({ ...current, action: event.target.value }))} placeholder="BOOKING_CREATED" />
                </label>
              </div>

              <label className="field">
                <span>Performed by</span>
                <input value={filters.performedBy} onChange={(event) => setFilters((current) => ({ ...current, performedBy: event.target.value }))} placeholder="admin@example.com" />
              </label>

              <div className="form-grid form-grid--two">
                <label className="field">
                  <span>From</span>
                  <input type="datetime-local" value={toDateTimeLocalValue(filters.from)} onChange={(event) => setFilters((current) => ({ ...current, from: event.target.value }))} />
                </label>
                <label className="field">
                  <span>To</span>
                  <input type="datetime-local" value={toDateTimeLocalValue(filters.to)} onChange={(event) => setFilters((current) => ({ ...current, to: event.target.value }))} />
                </label>
              </div>

              <div className="form-grid form-grid--two">
                <label className="field">
                  <span>Sort</span>
                  <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))}>
                    {AUDIT_SORT_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  <span>Page size</span>
                  <select value={filters.size} onChange={(event) => setFilters((current) => ({ ...current, size: Number(event.target.value) }))}>
                    {PAGE_SIZE_OPTIONS.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="filter-actions">
                <button type="submit" className="button button--primary">
                  Apply audit filters
                </button>
                <button
                  type="button"
                  className="button button--subtle"
                  onClick={() => {
                    setFilters(filterTemplate);
                    setAppliedFilters(filterTemplate);
                    setPage(0);
                  }}
                >
                  Reset
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
          footer={<PaginationBar pageData={pageData} onPageChange={setPage} />}
        >
          {loading ? (
            <LoadingState title="Loading audit stream" message="Pulling the latest admin-tracked backend activity." lines={6} />
          ) : auditRows.length ? (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>When</th>
                    <th>Entity</th>
                    <th>Action</th>
                    <th>Performed by</th>
                    <th>Details</th>
                  </tr>
                </thead>
                <tbody>
                  {auditRows.map((row) => (
                    <tr key={row.id}>
                      <td>{formatCompactDateTime(row.createdAt)}</td>
                      <td>
                        <StatusBadge value={row.entityType} variant="general" />
                        <p className="table-subtext">#{row.entityId || "-"}</p>
                      </td>
                      <td>{row.action}</td>
                      <td>{row.performedByEmail || row.performedByIdentifier || "System"}</td>
                      <td>{row.details || "-"}</td>
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
