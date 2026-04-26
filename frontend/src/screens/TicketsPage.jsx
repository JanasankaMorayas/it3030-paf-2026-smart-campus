import { ArrowRightLeft, Pencil, Plus, UserRoundCog } from "lucide-react";
import { useEffect, useState } from "react";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import Modal from "../components/Modal.jsx";
import PaginationBar from "../components/PaginationBar.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { formatCompactDateTime, joinMultilineList, parseMultilineList, titleizeEnum } from "../lib/format.js";
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
  const [statusModal, setStatusModal] = useState({ open: false, ticketId: null, status: "IN_PROGRESS", resolutionNotes: "" });
  const [assignModal, setAssignModal] = useState({ open: false, ticketId: null, assignedTechnician: "" });

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
    }
  }

  async function handleStatusSubmit(event) {
    event.preventDefault();

    try {
      await api.tickets.updateStatus(statusModal.ticketId, {
        status: statusModal.status,
        resolutionNotes: statusModal.resolutionNotes || null,
      });

      setStatusModal({ open: false, ticketId: null, status: "IN_PROGRESS", resolutionNotes: "" });
      await loadTickets(page, appliedFilters);
    } catch (statusError) {
      setFormError(statusError);
    }
  }

  async function handleAssignSubmit(event) {
    event.preventDefault();

    try {
      await api.tickets.assign(assignModal.ticketId, {
        assignedTechnician: assignModal.assignedTechnician,
      });

      setAssignModal({ open: false, ticketId: null, assignedTechnician: "" });
      await loadTickets(page, appliedFilters);
    } catch (assignError) {
      setFormError(assignError);
    }
  }

  async function handleCancel(ticketId) {
    if (!window.confirm("Cancel this ticket?")) {
      return;
    }

    try {
      await api.tickets.cancel(ticketId);
      await loadTickets(page, appliedFilters);
    } catch (cancelError) {
      setError(cancelError);
    }
  }

  const tickets = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">Module C</p>
          <h2>Tickets</h2>
          <p>Track maintenance incidents, technician assignment, and role-aware status transitions.</p>
        </div>
        <button type="button" className="button button--primary" onClick={openCreate}>
          <Plus size={16} />
          Create ticket
        </button>
      </section>

      <section className="panel">
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
              placeholder={isAdmin ? "Filter by reporter email" : "Scoped by backend ownership"}
              disabled={!isAdmin}
            />
          </label>

          <label className="field">
            <span>Assigned technician</span>
            <input
              value={filters.assignedTechnician}
              onChange={(event) => setFilters((current) => ({ ...current, assignedTechnician: event.target.value }))}
              placeholder={isAdmin ? "Filter by technician email" : "Assigned tickets are already scoped"}
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
            <button type="submit" className="button button--ghost">
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
      </section>

      <FeedbackBanner error={error} />

      <section className="panel">
        <div className="panel-header">
          <div>
            <p className="eyebrow">Paged results</p>
            <h3>{loading ? "Loading tickets..." : `${pageData?.totalElements ?? 0} matching tickets`}</h3>
          </div>
        </div>

        {tickets.length ? (
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
                  const canEdit = (ticket.status === "OPEN" || ticket.status === "IN_PROGRESS") && (isAdmin || ticket.reporterEmail === currentUser.email);
                  const canCancel = (ticket.status === "OPEN" || ticket.status === "IN_PROGRESS") && (isAdmin || ticket.reporterEmail === currentUser.email);
                  const canChangeStatus = isAdmin || isTechnician;

                  return (
                    <tr key={ticket.id}>
                      <td>
                        <strong>{ticket.title}</strong>
                        <p className="table-subtext">{ticket.location}</p>
                      </td>
                      <td>{ticket.reporterDisplayName || ticket.reporterEmail}</td>
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
                            <button type="button" className="button button--subtle" onClick={() => void handleCancel(ticket.id)}>
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
          <div className="empty-state">No tickets matched the current filters or ownership scope.</div>
        )}

        <PaginationBar pageData={pageData} onPageChange={setPage} />
      </section>

      <Modal
        isOpen={modalOpen}
        title={editingTicket ? "Update ticket" : "Create ticket"}
        subtitle="Image URLs are stored as newline-separated values for quick demos."
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
            <button type="submit" className="button button--primary">
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
            <button type="submit" className="button button--primary">
              Update status
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={assignModal.open}
        title="Assign technician"
        subtitle="Only active technician users can be assigned."
        onClose={() => setAssignModal({ open: false, ticketId: null, assignedTechnician: "" })}
      >
        <form className="form-grid" onSubmit={handleAssignSubmit}>
          <label className="field">
            <span>Technician</span>
            <select value={assignModal.assignedTechnician} onChange={(event) => setAssignModal((current) => ({ ...current, assignedTechnician: event.target.value }))}>
              <option value="">Select a technician</option>
              {technicians.map((technician) => (
                <option key={technician.id} value={technician.email}>
                  {technician.displayName} · {technician.email}
                </option>
              ))}
            </select>
          </label>

          <FeedbackBanner error={formError} />

          <div className="modal-actions">
            <button type="button" className="button button--ghost" onClick={() => setAssignModal({ open: false, ticketId: null, assignedTechnician: "" })}>
              Cancel
            </button>
            <button type="submit" className="button button--primary">
              Assign technician
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
