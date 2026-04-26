import { CheckCircle2, Pencil, Plus, XCircle } from "lucide-react";
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
import { compactCount, formatDateRange, toDateTimeLocalValue, titleizeEnum } from "../lib/format.js";
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
  const [submitting, setSubmitting] = useState(false);
  const [statusModal, setStatusModal] = useState({ open: false, bookingId: null, status: "APPROVED", adminDecisionReason: "" });
  const [cancelTarget, setCancelTarget] = useState(null);

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
    setSubmitting(true);
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
      setPage(0);
      await loadBookings(0, appliedFilters);
    } catch (submitError) {
      setFormError(submitError);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusUpdate(event) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);

    try {
      await api.bookings.updateStatus(statusModal.bookingId, {
        status: statusModal.status,
        adminDecisionReason: statusModal.adminDecisionReason || null,
      });

      setStatusModal({ open: false, bookingId: null, status: "APPROVED", adminDecisionReason: "" });
      await loadBookings(page, appliedFilters);
    } catch (statusError) {
      setFormError(statusError);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancelConfirmed() {
    if (!cancelTarget) {
      return;
    }

    try {
      await api.bookings.cancel(cancelTarget.id);
      setCancelTarget(null);
      await loadBookings(page, appliedFilters);
    } catch (cancelError) {
      setError(cancelError);
    }
  }

  const bookings = pageData?.content || [];
  const pendingCount = bookings.filter((booking) => booking.status === "PENDING").length;
  const approvedCount = bookings.filter((booking) => booking.status === "APPROVED").length;
  const cancelledCount = bookings.filter((booking) => booking.status === "CANCELLED").length;

  return (
    <PageShell
      eyebrow="Module B"
      title="Booking workflow"
      description="Manage reservation intake, admin decisions, and ownership-aware changes across campus resources."
      actions={(
        <div className="stacked-actions">
          <button type="button" className="button button--ghost" onClick={() => void loadBookings(page, appliedFilters)}>
            Refresh queue
          </button>
          <button type="button" className="button button--primary" onClick={openCreate}>
            <Plus size={16} />
            Create booking
          </button>
        </div>
      )}
      meta={(
        <>
          <StatusBadge value={isAdmin ? "ADMIN" : "USER"} variant="general" />
          <span className="page-shell__meta-text">
            {isAdmin ? "Admin decisions can approve, reject, or cancel requests." : "Your view is scoped to bookings you own."}
          </span>
        </>
      )}
    >
      <section className="dashboard-stat-grid dashboard-stat-grid--compact">
        <StatCard icon={Plus} label="Visible bookings" value={loading ? "..." : compactCount(pageData?.totalElements ?? 0)} hint="Across your current filters and access" tone="teal" />
        <StatCard icon={CheckCircle2} label="Pending on page" value={loading ? "..." : compactCount(pendingCount)} hint="Requests waiting for action in this page slice" tone="sand" />
        <StatCard icon={CheckCircle2} label="Approved on page" value={loading ? "..." : compactCount(approvedCount)} hint="Confirmed reservations in the current result set" tone="cream" />
        <StatCard icon={XCircle} label="Cancelled on page" value={loading ? "..." : compactCount(cancelledCount)} hint="Stopped requests in the current result set" tone="teal" />
      </section>

      <DataToolbar
        eyebrow="Queue filters"
        title="Shape the booking board"
        description="Filter by resource, requester, status, sorting, and page size without leaving the workflow."
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
            <span>Resource</span>
            <select value={filters.resourceId} onChange={(event) => setFilters((current) => ({ ...current, resourceId: event.target.value }))}>
              <option value="">All resources</option>
              {resources.map((resource) => (
                <option key={resource.id} value={resource.id}>
                  {resource.resourceCode} - {resource.name}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Requester</span>
            <input
              value={filters.requesterId}
              onChange={(event) => setFilters((current) => ({ ...current, requesterId: event.target.value }))}
              placeholder={isAdmin ? "Filter by email" : "Already scoped to your ownership"}
              disabled={!isAdmin}
            />
          </label>

          <label className="field">
            <span>Status</span>
            <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}>
              <option value="">All statuses</option>
              {BOOKING_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {titleizeEnum(status)}
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
        eyebrow="Reservation board"
        title={loading ? "Loading bookings" : `${pageData?.totalElements ?? 0} bookings in scope`}
        description="See ownership, status, timing, and approval decisions at one glance."
        footer={<PaginationBar pageData={pageData} onPageChange={setPage} />}
      >
        {loading ? (
          <LoadingState title="Loading booking queue" message="Pulling the latest reservation workflow state." lines={6} />
        ) : bookings.length ? (
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
                  const canEdit = booking.status === "PENDING" && (isAdmin || booking.ownerEmail === currentUser?.email);
                  const canCancel = (booking.status === "PENDING" || booking.status === "APPROVED")
                    && (isAdmin || booking.ownerEmail === currentUser?.email);

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
                      <td>{booking.adminDecisionReason || "-"}</td>
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
                            <button type="button" className="button button--subtle" onClick={() => setCancelTarget(booking)}>
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
            title="No bookings found"
            message="Try another filter combination or create a fresh booking request."
            action={<button type="button" className="button button--primary" onClick={openCreate}>Create booking</button>}
          />
        )}
      </SectionCard>

      <Modal
        isOpen={modalOpen}
        title={editingBooking ? "Update booking" : "Create booking"}
        subtitle="Normal users stay ownership-scoped. Admins can create on behalf of another requester when needed."
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="field">
            <span>Resource</span>
            <select value={formState.resourceId} onChange={(event) => setFormState((current) => ({ ...current, resourceId: event.target.value }))}>
              <option value="">Select a resource</option>
              {resources.map((resource) => (
                <option key={resource.id} value={resource.id}>
                  {resource.resourceCode} - {resource.name}
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
            <button type="submit" className="button button--primary" disabled={submitting}>
              {editingBooking ? "Save booking" : "Create booking"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={statusModal.open}
        title={`Set booking to ${titleizeEnum(statusModal.status)}`}
        subtitle="Use a clear reason when rejecting or cancelling so the requester can act on it quickly."
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
            <button type="submit" className="button button--primary" disabled={submitting}>
              Update booking status
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        title="Cancel booking"
        message={cancelTarget ? `Cancel booking for ${cancelTarget.resourceName} scheduled ${formatDateRange(cancelTarget.startTime, cancelTarget.endTime)}?` : ""}
        confirmLabel="Cancel booking"
        onConfirm={handleCancelConfirmed}
        onClose={() => setCancelTarget(null)}
      />
    </PageShell>
  );
}
