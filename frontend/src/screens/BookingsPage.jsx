import { CheckCircle2, Pencil, Plus, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import Modal from "../components/Modal.jsx";
import PaginationBar from "../components/PaginationBar.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { formatDateRange, toDateTimeLocalValue } from "../lib/format.js";
import { BOOKING_SORT_OPTIONS, BOOKING_STATUSES, PAGE_SIZE_OPTIONS } from "../lib/options.js";
import { api } from "../lib/api.js";

const filterTemplate = {
  resourceId: "",
  requesterId: "",
  status: "",
  size: 10,
  sort: "startTime,asc",
};

const bookingFormTemplate = {
  resourceId: "",
  requesterId: "",
  purpose: "",
  expectedAttendees: "1",
  startTime: "",
  endTime: "",
};

export default function BookingsPage() {
  const { currentUser, isAdmin } = useAuth();
  const [resources, setResources] = useState([]);
  const [filters, setFilters] = useState(filterTemplate);
  const [appliedFilters, setAppliedFilters] = useState(filterTemplate);
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingBooking, setEditingBooking] = useState(null);
  const [formState, setFormState] = useState(bookingFormTemplate);
  const [formError, setFormError] = useState(null);
  const [statusModal, setStatusModal] = useState({ open: false, bookingId: null, status: "APPROVED", adminDecisionReason: "" });

  async function loadResources() {
    const response = await api.resources.list();
    setResources(response);
  }

  async function loadBookings(activePage = page, activeFilters = appliedFilters) {
    setLoading(true);
    setError(null);

    try {
      const response = await api.bookings.list({
        page: activePage,
        size: activeFilters.size,
        sort: activeFilters.sort,
        resourceId: activeFilters.resourceId,
        requesterId: activeFilters.requesterId,
        status: activeFilters.status,
      });
      setPageData(response);
    } catch (loadError) {
      setError(loadError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadResources();
  }, []);

  useEffect(() => {
    void loadBookings(page, appliedFilters);
  }, [page, appliedFilters]);

  function openCreate() {
    setEditingBooking(null);
    setFormState({
      ...bookingFormTemplate,
      requesterId: isAdmin ? "" : currentUser?.email || "",
    });
    setFormError(null);
    setModalOpen(true);
  }

  function openEdit(booking) {
    setEditingBooking(booking);
    setFormState({
      resourceId: String(booking.resourceId),
      requesterId: booking.requesterId || "",
      purpose: booking.purpose,
      expectedAttendees: String(booking.expectedAttendees),
      startTime: toDateTimeLocalValue(booking.startTime),
      endTime: toDateTimeLocalValue(booking.endTime),
    });
    setFormError(null);
    setModalOpen(true);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setFormError(null);

    const payload = {
      ...formState,
      resourceId: Number(formState.resourceId),
      expectedAttendees: Number(formState.expectedAttendees),
      requesterId: formState.requesterId || null,
    };

    try {
      if (editingBooking) {
        await api.bookings.update(editingBooking.id, payload);
      } else {
        await api.bookings.create(payload);
      }

      setModalOpen(false);
      await loadBookings(0, appliedFilters);
      setPage(0);
    } catch (submitError) {
      setFormError(submitError);
    }
  }

  async function handleStatusUpdate(event) {
    event.preventDefault();

    try {
      await api.bookings.updateStatus(statusModal.bookingId, {
        status: statusModal.status,
        adminDecisionReason: statusModal.adminDecisionReason || null,
      });

      setStatusModal({ open: false, bookingId: null, status: "APPROVED", adminDecisionReason: "" });
      await loadBookings(page, appliedFilters);
    } catch (statusError) {
      setFormError(statusError);
    }
  }

  async function handleCancel(bookingId) {
    if (!window.confirm("Cancel this booking?")) {
      return;
    }

    try {
      await api.bookings.cancel(bookingId);
      await loadBookings(page, appliedFilters);
    } catch (cancelError) {
      setError(cancelError);
    }
  }

  const bookings = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">Module B</p>
          <h2>Bookings</h2>
          <p>Create, review, approve, reject, and track resource usage requests with ownership-aware access.</p>
        </div>
        <button type="button" className="button button--primary" onClick={openCreate}>
          <Plus size={16} />
          Create booking
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
            <span>Resource</span>
            <select value={filters.resourceId} onChange={(event) => setFilters((current) => ({ ...current, resourceId: event.target.value }))}>
              <option value="">All resources</option>
              {resources.map((resource) => (
                <option key={resource.id} value={resource.id}>
                  {resource.resourceCode} · {resource.name}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Requester</span>
            <input
              value={filters.requesterId}
              onChange={(event) => setFilters((current) => ({ ...current, requesterId: event.target.value }))}
              placeholder={isAdmin ? "Filter by email" : "Your own bookings are already scoped"}
              disabled={!isAdmin}
            />
          </label>

          <label className="field">
            <span>Status</span>
            <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}>
              <option value="">All statuses</option>
              {BOOKING_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Sort</span>
            <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))}>
              {BOOKING_SORT_OPTIONS.map((option) => (
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
            <h3>{loading ? "Loading bookings..." : `${pageData?.totalElements ?? 0} matching bookings`}</h3>
          </div>
        </div>

        {bookings.length ? (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Resource</th>
                  <th>Owner</th>
                  <th>Purpose</th>
                  <th>Time slot</th>
                  <th>Attendees</th>
                  <th>Status</th>
                  <th>Decision</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {bookings.map((booking) => {
                  const canEdit = booking.status === "PENDING" && (isAdmin || booking.ownerEmail === currentUser.email);
                  const canCancel = (booking.status === "PENDING" || booking.status === "APPROVED") && (isAdmin || booking.ownerEmail === currentUser.email);

                  return (
                    <tr key={booking.id}>
                      <td>
                        <strong>{booking.resourceName}</strong>
                        <p className="table-subtext">{booking.resourceCode}</p>
                      </td>
                      <td>
                        <strong>{booking.ownerDisplayName || booking.ownerEmail}</strong>
                        <p className="table-subtext">{booking.requesterId}</p>
                      </td>
                      <td>{booking.purpose}</td>
                      <td>{formatDateRange(booking.startTime, booking.endTime)}</td>
                      <td>{booking.expectedAttendees}</td>
                      <td>
                        <StatusBadge value={booking.status} />
                      </td>
                      <td>{booking.adminDecisionReason || "—"}</td>
                      <td>
                        <div className="table-actions">
                          {canEdit ? (
                            <button type="button" className="icon-button" onClick={() => openEdit(booking)} aria-label="Edit booking">
                              <Pencil size={16} />
                            </button>
                          ) : null}

                          {isAdmin && booking.status === "PENDING" ? (
                            <>
                              <button
                                type="button"
                                className="icon-button icon-button--success"
                                onClick={() => {
                                  setFormError(null);
                                  setStatusModal({ open: true, bookingId: booking.id, status: "APPROVED", adminDecisionReason: "" });
                                }}
                                aria-label="Approve booking"
                              >
                                <CheckCircle2 size={16} />
                              </button>
                              <button
                                type="button"
                                className="icon-button icon-button--danger"
                                onClick={() => {
                                  setFormError(null);
                                  setStatusModal({ open: true, bookingId: booking.id, status: "REJECTED", adminDecisionReason: "" });
                                }}
                                aria-label="Reject booking"
                              >
                                <XCircle size={16} />
                              </button>
                            </>
                          ) : null}

                          {canCancel ? (
                            <button type="button" className="button button--subtle" onClick={() => void handleCancel(booking.id)}>
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
          <div className="empty-state">No bookings matched the current scope.</div>
        )}

        <PaginationBar pageData={pageData} onPageChange={setPage} />
      </section>

      <Modal
        isOpen={modalOpen}
        title={editingBooking ? "Update booking" : "Create booking"}
        subtitle="Booking ownership is enforced by the backend for normal users."
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="field">
            <span>Resource</span>
            <select value={formState.resourceId} onChange={(event) => setFormState((current) => ({ ...current, resourceId: event.target.value }))}>
              <option value="">Select a resource</option>
              {resources.map((resource) => (
                <option key={resource.id} value={resource.id}>
                  {resource.resourceCode} · {resource.name}
                </option>
              ))}
            </select>
          </label>

          {isAdmin ? (
            <label className="field">
              <span>Requester email</span>
              <input value={formState.requesterId} onChange={(event) => setFormState((current) => ({ ...current, requesterId: event.target.value }))} />
            </label>
          ) : null}

          <label className="field">
            <span>Purpose</span>
            <input value={formState.purpose} onChange={(event) => setFormState((current) => ({ ...current, purpose: event.target.value }))} />
          </label>

          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Expected attendees</span>
              <input type="number" min="1" value={formState.expectedAttendees} onChange={(event) => setFormState((current) => ({ ...current, expectedAttendees: event.target.value }))} />
            </label>
            <div />
          </div>

          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Start time</span>
              <input type="datetime-local" value={formState.startTime} onChange={(event) => setFormState((current) => ({ ...current, startTime: event.target.value }))} />
            </label>
            <label className="field">
              <span>End time</span>
              <input type="datetime-local" value={formState.endTime} onChange={(event) => setFormState((current) => ({ ...current, endTime: event.target.value }))} />
            </label>
          </div>

          <FeedbackBanner error={formError} />

          <div className="modal-actions">
            <button type="button" className="button button--ghost" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="button button--primary">
              {editingBooking ? "Save booking" : "Create booking"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={statusModal.open}
        title={`Set booking to ${statusModal.status}`}
        subtitle="Admin decision reason is optional for approvals and useful for rejections."
        onClose={() => setStatusModal({ open: false, bookingId: null, status: "APPROVED", adminDecisionReason: "" })}
      >
        <form className="form-grid" onSubmit={handleStatusUpdate}>
          <label className="field">
            <span>Status</span>
            <select value={statusModal.status} onChange={(event) => setStatusModal((current) => ({ ...current, status: event.target.value }))}>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </label>

          <label className="field">
            <span>Decision reason</span>
            <textarea
              rows={4}
              value={statusModal.adminDecisionReason}
              onChange={(event) => setStatusModal((current) => ({ ...current, adminDecisionReason: event.target.value }))}
            />
          </label>

          <FeedbackBanner error={formError} />

          <div className="modal-actions">
            <button
              type="button"
              className="button button--ghost"
              onClick={() => setStatusModal({ open: false, bookingId: null, status: "APPROVED", adminDecisionReason: "" })}
            >
              Close
            </button>
            <button type="submit" className="button button--primary">
              Update booking status
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
