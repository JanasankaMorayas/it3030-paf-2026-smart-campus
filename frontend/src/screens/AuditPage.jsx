import { DatabaseBackup, RefreshCcw } from "lucide-react";
import { useEffect, useState } from "react";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import PaginationBar from "../components/PaginationBar.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { AUDIT_ENTITY_TYPES, AUDIT_SORT_OPTIONS, PAGE_SIZE_OPTIONS } from "../lib/options.js";
import { formatCompactDateTime, toDateTimeLocalValue } from "../lib/format.js";
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
  const { isAdmin } = useAuth();
  const [filters, setFilters] = useState(filterTemplate);
  const [appliedFilters, setAppliedFilters] = useState(filterTemplate);
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [backfillSummary, setBackfillSummary] = useState(null);
  const [backfillError, setBackfillError] = useState(null);
  const [backfillBusy, setBackfillBusy] = useState(false);

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
      await loadAuditLogs(0, appliedFilters);
      setPage(0);
    } catch (runError) {
      setBackfillError(runError);
    } finally {
      setBackfillBusy(false);
    }
  }

  if (!isAdmin) {
    return (
      <div className="page-stack">
        <section className="panel">
          <h2>Audit & backfill</h2>
          <p>This screen is only available to admin users.</p>
        </section>
      </div>
    );
  }

  const auditRows = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">Admin tools</p>
          <h2>Audit logs & legacy backfill</h2>
          <p>Inspect operational history and safely link older string-based ownership records to real users.</p>
        </div>
        <button type="button" className="button button--primary" onClick={() => void loadAuditLogs(page, appliedFilters)}>
          <RefreshCcw size={16} />
          Refresh logs
        </button>
      </section>

      <section className="two-up-grid">
        <article className="panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Legacy support</p>
              <h3>Run user-link backfill</h3>
            </div>
          </div>

          <p className="panel-description">
            This scans old booking owner, ticket reporter, and technician string fields and links them to local users when emails match.
          </p>

          <div className="stacked-actions">
            <button type="button" className="button button--primary" onClick={() => void handleBackfill()} disabled={backfillBusy}>
              <DatabaseBackup size={16} />
              Trigger backfill
            </button>
          </div>

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
          ) : null}
        </article>

        <article className="panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Audit filters</p>
              <h3>Query history</h3>
            </div>
          </div>

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
              <button type="submit" className="button button--ghost">
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
        </article>
      </section>

      <FeedbackBanner error={error} />

      <section className="panel">
        <div className="panel-header">
          <div>
            <p className="eyebrow">Audit history</p>
            <h3>{loading ? "Loading audit logs..." : `${pageData?.totalElements ?? 0} matching events`}</h3>
          </div>
        </div>

        {auditRows.length ? (
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
                      <p className="table-subtext">#{row.entityId || "—"}</p>
                    </td>
                    <td>{row.action}</td>
                    <td>{row.performedByEmail || row.performedByIdentifier || "System"}</td>
                    <td>{row.details || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">No audit logs matched the selected filters.</div>
        )}

        <PaginationBar pageData={pageData} onPageChange={setPage} />
      </section>
    </div>
  );
}
