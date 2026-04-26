import { ArrowRightLeft, Pencil, Plus, UserRoundCog, Wrench } from "lucide-react";
import { useEffect, useState } from "react";
import ConfirmDialog from "../components/ConfirmDialog.jsx";
import DataToolbar from "../components/DataToolbar.jsx";
import EmptyState from "../components/EmptyState.jsx";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import LoadingState from "../components/LoadingState.jsx";
import Modal from "../components/Modal.jsx";
import PageShell from "../components/PageShell.jsx";
import PaginationBar from "../components/PaginationBar.jsx";
import SectionCard from "../components/SectionCard.jsx";
import StatCard from "../components/StatCard.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { compactCount, formatCompactDateTime, joinMultilineList, parseMultilineList, titleizeEnum } from "../lib/format.js";
import {
  PAGE_SIZE_OPTIONS,
  TICKET_CATEGORIES,
  TICKET_PRIORITIES,
  TICKET_SORT_OPTIONS,
  TICKET_STATUSES,
} from "../lib/options.js";
import { api } from "../lib/api.js";

const filterTemplate = {
  status: "",
  priority: "",
  category: "",
  reportedBy: "",
  assignedTechnician: "",
  size: 10,
  sort: "createdAt,desc",
};

const ticketFormTemplate = {
  title: "",
  description: "",
  category: "OTHER",
  priority: "MEDIUM",
  location: "",
  reportedBy: "",
  imageUrlsText: "",
};

export default function TicketsPage() {
  const { currentUser, isAdmin, isTechnician } = useAuth();
  const [technicians, setTechnicians] = useState([]);
  const [filters, setFilters] = useState(filterTemplate);
  const [appliedFilters, setAppliedFilters] = useState(filterTemplate);
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTicket, setEditingTicket] = useState(null);
  const [formState, setFormState] = useState(ticketFormTemplate);
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [statusModal, setStatusModal] = useState({ open: false, ticketId: null, status: "IN_PROGRESS", resolutionNotes: "" });
  const [assignModal, setAssignModal] = useState({ open: false, ticketId: null, assignedTechnician: "" });
  const [cancelTarget, setCancelTarget] = useState(null);

  async function loadTechnicians() {
    if (!isAdmin) {
      setTechnicians([]);
      return;
    }

    const response = await api.users.list();
    setTechnicians(response.filter((user) => user.role === "TECHNICIAN" && user.active));
  }

  async function loadTickets(activePage = page, activeFilters = appliedFilters) {
    setLoading(true);
    setError(null);

    try {
      const response = await api.tickets.list({
        page: activePage,
        size: activeFilters.size,
        sort: activeFilters.sort,
        status: activeFilters.status,
        priority: activeFilters.priority,
        category: activeFilters.category,
        reportedBy: activeFilters.reportedBy,
        assignedTechnician: activeFilters.assignedTechnician,
      });
      setPageData(response);
    } catch (loadError) {
      setError(loadError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadTechnicians();
  }, [isAdmin]);

  useEffect(() => {
    void loadTickets(page, appliedFilters);
  }, [page, appliedFilters]);

  function openCreate() {
    setEditingTicket(null);
    setFormState({
      ...ticketFormTemplate,
      reportedBy: isAdmin ? "" : currentUser?.email || "",
    });
    setFormError(null);
    setModalOpen(true);
  }

  function openEdit(ticket) {
    setEditingTicket(ticket);
    setFormState({
      title: ticket.title,
      description: ticket.description,
      category: ticket.category,
      priority: ticket.priority,
      location: ticket.location,
      reportedBy: ticket.reportedBy || "",
      imageUrlsText: joinMultilineList(ticket.imageUrls),
    });
    setFormError(null);
    setModalOpen(true);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);

    const payload = {
      ...formState,
      reportedBy: formState.reportedBy || null,
      imageUrls: parseMultilineList(formState.imageUrlsText),
    };

    try {
      if (editingTicket) {
        await api.tickets.update(editingTicket.id, payload);
      } else {
        await api.tickets.create(payload);
      }

      setModalOpen(false);
      setPage(0);
      await loadTickets(0, appliedFilters);
    } catch (submitError) {
      setFormError(submitError);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);

    try {
      await api.tickets.updateStatus(statusModal.ticketId, {
        status: statusModal.status,
        resolutionNotes: statusModal.resolutionNotes || null,
      });

      setStatusModal({ open: false, ticketId: null, status: "IN_PROGRESS", resolutionNotes: "" });
      await loadTickets(page, appliedFilters);
    } catch (statusError) {
      setFormError(statusError);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAssignSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);

    try {
      await api.tickets.assign(assignModal.ticketId, {
        assignedTechnician: assignModal.assignedTechnician,
      });

      setAssignModal({ open: false, ticketId: null, assignedTechnician: "" });
      await loadTickets(page, appliedFilters);
    } catch (assignError) {
      setFormError(assignError);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancelConfirmed() {
    if (!cancelTarget) {
      return;
    }

    try {
      await api.tickets.cancel(cancelTarget.id);
      setCancelTarget(null);
      await loadTickets(page, appliedFilters);
    } catch (cancelError) {
      setError(cancelError);
    }
  }

  const tickets = pageData?.content || [];
  const openCount = tickets.filter((ticket) => ticket.status === "OPEN").length;
  const progressCount = tickets.filter((ticket) => ticket.status === "IN_PROGRESS").length;
  const criticalCount = tickets.filter((ticket) => ticket.priority === "CRITICAL").length;
  const assignedCount = tickets.filter((ticket) => ticket.technicianEmail).length;

  return (
    <PageShell
      eyebrow="Module C"
      title="Maintenance incident desk"
      description="Track maintenance issues, technician assignment, urgency, and status movement from one operational surface."
      actions={(
        <div className="stacked-actions">
          <button type="button" className="button button--ghost" onClick={() => void loadTickets(page, appliedFilters)}>
            Refresh ticket lane
          </button>
          <button type="button" className="button button--primary" onClick={openCreate}>
            <Plus size={16} />
            Create ticket
          </button>
        </div>
      )}
      meta={(
        <>
          <StatusBadge value={currentUser?.role} />
          <span className="page-shell__meta-text">
            {isAdmin
              ? "Admins can assign technicians and control all incidents."
              : isTechnician
                ? "Technicians see tickets assigned to them and can move approved workflow states."
                : "Users stay scoped to incidents they reported."}
          </span>
        </>
      )}
    >
      <section className="dashboard-stat-grid dashboard-stat-grid--compact">
        <StatCard icon={Wrench} label="Visible tickets" value={loading ? "..." : compactCount(pageData?.totalElements ?? 0)} hint="Across your current scope and filters" tone="teal" />
        <StatCard icon={ArrowRightLeft} label="Open on page" value={loading ? "..." : compactCount(openCount)} hint="Fresh incidents waiting to move" tone="sand" />
        <StatCard icon={ArrowRightLeft} label="In progress" value={loading ? "..." : compactCount(progressCount)} hint="Active maintenance work in this page slice" tone="cream" />
        <StatCard icon={Wrench} label="Critical priority" value={loading ? "..." : compactCount(criticalCount)} hint="Critical incidents visible right now" tone="teal" />
        <StatCard icon={UserRoundCog} label="Assigned" value={loading ? "..." : compactCount(assignedCount)} hint="Tickets already attached to a technician" tone="cream" />
      </section>

      <DataToolbar
        eyebrow="Ticket filters"
        title="Shape the incident queue"
        description="Filter by status, priority, category, reporter, technician, sorting, and page size."
      >
        <form
          className="filters-grid"
          onSubmit={(event) => {
            event.preventDefault();
            setPage(0);
            setAppliedFilters({ ...filters });
          }}
        >
          <label className="field">
            <span>Status</span>
            <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}>
              <option value="">All statuses</option>
              {TICKET_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {titleizeEnum(status)}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Priority</span>
            <select value={filters.priority} onChange={(event) => setFilters((current) => ({ ...current, priority: event.target.value }))}>
              <option value="">All priorities</option>
              {TICKET_PRIORITIES.map((priority) => (
                <option key={priority} value={priority}>
                  {titleizeEnum(priority)}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Category</span>
            <select value={filters.category} onChange={(event) => setFilters((current) => ({ ...current, category: event.target.value }))}>
              <option value="">All categories</option>
              {TICKET_CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {titleizeEnum(category)}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Reporter</span>
            <input
              value={filters.reportedBy}
              onChange={(event) => setFilters((current) => ({ ...current, reportedBy: event.target.value }))}
              placeholder={isAdmin ? "Filter by reporter email" : "Already ownership-scoped"}
              disabled={!isAdmin}
            />
          </label>

          <label className="field">
            <span>Assigned technician</span>
            <input
              value={filters.assignedTechnician}
              onChange={(event) => setFilters((current) => ({ ...current, assignedTechnician: event.target.value }))}
              placeholder={isAdmin ? "Filter by technician email" : "Technician scoping is already enforced"}
              disabled={!isAdmin && !isTechnician}
            />
          </label>

          <label className="field">
            <span>Sort</span>
            <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))}>
              {TICKET_SORT_OPTIONS.map((option) => (
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

          <div className="filter-actions">
            <button type="submit" className="button button--primary">
              Apply filters
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

      <FeedbackBanner error={error} />

      <SectionCard
        eyebrow="Incident queue"
        title={loading ? "Loading tickets" : `${pageData?.totalElements ?? 0} tickets in scope`}
        description="Priority, ownership, technician assignment, and lifecycle state remain visible on one board."
        footer={<PaginationBar pageData={pageData} onPageChange={setPage} />}
      >
        {loading ? (
          <LoadingState title="Loading tickets" message="Pulling the latest maintenance and incident workflow." lines={6} />
        ) : tickets.length ? (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Ticket</th>
                  <th>Reporter</th>
                  <th>Technician</th>
                  <th>Category / Priority</th>
                  <th>Status</th>
                  <th>Updated</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((ticket) => {
                  const canEdit = (ticket.status === "OPEN" || ticket.status === "IN_PROGRESS")
                    && (isAdmin || ticket.reporterEmail === currentUser?.email);
                  const canCancel = (ticket.status === "OPEN" || ticket.status === "IN_PROGRESS")
                    && (isAdmin || ticket.reporterEmail === currentUser?.email);
                  const canChangeStatus = isAdmin || isTechnician;

                  return (
                    <tr key={ticket.id}>
                      <td>
                        <strong>{ticket.title}</strong>
                        <p className="table-subtext">{ticket.location}</p>
                      </td>
                      <td>
                        <strong>{ticket.reporterDisplayName || ticket.reporterEmail}</strong>
                        <p className="table-subtext">{ticket.reportedBy || ticket.reporterEmail}</p>
                      </td>
                      <td>{ticket.technicianDisplayName || ticket.technicianEmail || "Unassigned"}</td>
                      <td>
                        <div className="stack-inline">
                          <StatusBadge value={ticket.category} variant="general" />
                          <StatusBadge value={ticket.priority} variant={ticket.priority} />
                        </div>
                      </td>
                      <td>
                        <StatusBadge value={ticket.status} />
                      </td>
                      <td>{formatCompactDateTime(ticket.updatedAt || ticket.createdAt)}</td>
                      <td>
                        <div className="table-actions">
                          {canEdit ? (
                            <button type="button" className="icon-button" onClick={() => openEdit(ticket)} aria-label="Edit ticket">
                              <Pencil size={16} />
                            </button>
                          ) : null}

                          {isAdmin ? (
                            <button
                              type="button"
                              className="icon-button"
                              onClick={() => {
                                setFormError(null);
                                setAssignModal({ open: true, ticketId: ticket.id, assignedTechnician: ticket.assignedTechnician || "" });
                              }}
                              aria-label="Assign technician"
                            >
                              <UserRoundCog size={16} />
                            </button>
                          ) : null}

                          {canChangeStatus ? (
                            <button
                              type="button"
                              className="icon-button"
                              onClick={() => {
                                setFormError(null);
                                setStatusModal({
                                  open: true,
                                  ticketId: ticket.id,
                                  status: ticket.status === "OPEN" ? "IN_PROGRESS" : ticket.status,
                                  resolutionNotes: ticket.resolutionNotes || "",
                                });
                              }}
                              aria-label="Update ticket status"
                            >
                              <ArrowRightLeft size={16} />
                            </button>
                          ) : null}

                          {canCancel ? (
                            <button type="button" className="button button--subtle" onClick={() => setCancelTarget(ticket)}>
                              Cancel
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No tickets matched"
            message="Try another filter combination or create a fresh maintenance incident."
            action={<button type="button" className="button button--primary" onClick={openCreate}>Create ticket</button>}
          />
        )}
      </SectionCard>

      <Modal
        isOpen={modalOpen}
        title={editingTicket ? "Update ticket" : "Create ticket"}
        subtitle="Image URLs are still stored as quick demo inputs, one per line."
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Title</span>
              <input value={formState.title} onChange={(event) => setFormState((current) => ({ ...current, title: event.target.value }))} />
            </label>
            <label className="field">
              <span>Location</span>
              <input value={formState.location} onChange={(event) => setFormState((current) => ({ ...current, location: event.target.value }))} />
            </label>
          </div>

          <label className="field">
            <span>Description</span>
            <textarea rows={4} value={formState.description} onChange={(event) => setFormState((current) => ({ ...current, description: event.target.value }))} />
          </label>

          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Category</span>
              <select value={formState.category} onChange={(event) => setFormState((current) => ({ ...current, category: event.target.value }))}>
                {TICKET_CATEGORIES.map((category) => (
                  <option key={category} value={category}>
                    {titleizeEnum(category)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Priority</span>
              <select value={formState.priority} onChange={(event) => setFormState((current) => ({ ...current, priority: event.target.value }))}>
                {TICKET_PRIORITIES.map((priority) => (
                  <option key={priority} value={priority}>
                    {titleizeEnum(priority)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {isAdmin ? (
            <label className="field">
              <span>Reporter email</span>
              <input value={formState.reportedBy} onChange={(event) => setFormState((current) => ({ ...current, reportedBy: event.target.value }))} />
            </label>
          ) : null}

          <label className="field">
            <span>Image URLs</span>
            <textarea
              rows={4}
              value={formState.imageUrlsText}
              onChange={(event) => setFormState((current) => ({ ...current, imageUrlsText: event.target.value }))}
              placeholder="One URL per line"
            />
          </label>

          <FeedbackBanner error={formError} />

          <div className="modal-actions">
            <button type="button" className="button button--ghost" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="button button--primary" disabled={submitting}>
              {editingTicket ? "Save ticket" : "Create ticket"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={statusModal.open}
        title="Update ticket status"
        subtitle="Technician and admin workflow rules are still enforced by the backend."
        onClose={() => setStatusModal({ open: false, ticketId: null, status: "IN_PROGRESS", resolutionNotes: "" })}
      >
        <form className="form-grid" onSubmit={handleStatusSubmit}>
          <label className="field">
            <span>Status</span>
            <select value={statusModal.status} onChange={(event) => setStatusModal((current) => ({ ...current, status: event.target.value }))}>
              {TICKET_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {titleizeEnum(status)}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Resolution notes</span>
            <textarea
              rows={4}
              value={statusModal.resolutionNotes}
              onChange={(event) => setStatusModal((current) => ({ ...current, resolutionNotes: event.target.value }))}
            />
          </label>

          <FeedbackBanner error={formError} />

          <div className="modal-actions">
            <button type="button" className="button button--ghost" onClick={() => setStatusModal({ open: false, ticketId: null, status: "IN_PROGRESS", resolutionNotes: "" })}>
              Cancel
            </button>
            <button type="submit" className="button button--primary" disabled={submitting}>
              Update status
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={assignModal.open}
        title="Assign technician"
        subtitle="Only active technician users can be attached to a ticket."
        onClose={() => setAssignModal({ open: false, ticketId: null, assignedTechnician: "" })}
      >
        <form className="form-grid" onSubmit={handleAssignSubmit}>
          <label className="field">
            <span>Technician</span>
            <select value={assignModal.assignedTechnician} onChange={(event) => setAssignModal((current) => ({ ...current, assignedTechnician: event.target.value }))}>
              <option value="">Select a technician</option>
              {technicians.map((technician) => (
                <option key={technician.id} value={technician.email}>
                  {technician.displayName} - {technician.email}
                </option>
              ))}
            </select>
          </label>

          <FeedbackBanner error={formError} />

          <div className="modal-actions">
            <button type="button" className="button button--ghost" onClick={() => setAssignModal({ open: false, ticketId: null, assignedTechnician: "" })}>
              Cancel
            </button>
            <button type="submit" className="button button--primary" disabled={submitting}>
              Assign technician
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        title="Cancel ticket"
        message={cancelTarget ? `Cancel ticket "${cancelTarget.title}" at ${cancelTarget.location}? This keeps the history but ends the active workflow.` : ""}
        confirmLabel="Cancel ticket"
        onConfirm={handleCancelConfirmed}
        onClose={() => setCancelTarget(null)}
      />
    </PageShell>
  );
}
