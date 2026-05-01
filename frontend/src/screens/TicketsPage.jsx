import { ArrowRightLeft, Pencil, Plus, RefreshCcw, UserRoundCog, Wrench, MapPin, MessageSquare } from "lucide-react";
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
  const [statusModal, setStatusModal] = useState({ open: false, ticketId: null, currentStatus: "OPEN", status: "IN_PROGRESS", resolutionNotes: "" });
  const [assignModal, setAssignModal] = useState({ open: false, ticketId: null, assignedTechnician: "" });
  const [cancelTarget, setCancelTarget] = useState(null);
  const [imageFiles, setImageFiles] = useState([]);
  const [imagePreviews, setImagePreviews] = useState([]);
  const [enlargedImage, setEnlargedImage] = useState(null);
  
  const [commentsModal, setCommentsModal] = useState({ open: false, ticket: null });
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [loadingComments, setLoadingComments] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState(null);

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
    setImageFiles([]);
    setImagePreviews([]);
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
    });
    setImageFiles([]);
    setImagePreviews(ticket.imageUrls || []);
    setFormError(null);
    setModalOpen(true);
  }

  function handleFileChange(event) {
    const files = Array.from(event.target.files);
    if (files.length > 3) {
      setFormError({ message: "You can only select up to 3 images." });
      return;
    }
    setFormError(null);
    setImageFiles(files);
    setImagePreviews(files.map((file) => URL.createObjectURL(file)));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);

    try {
      let uploadedUrls = editingTicket ? editingTicket.imageUrls : [];
      if (imageFiles.length > 0) {
        uploadedUrls = await api.tickets.uploadImages(imageFiles);
      }

      const payload = {
        ...formState,
        reportedBy: formState.reportedBy || null,
        imageUrls: uploadedUrls,
      };

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

      setStatusModal({ open: false, ticketId: null, currentStatus: "OPEN", status: "IN_PROGRESS", resolutionNotes: "" });
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

  async function openComments(ticket) {
    setCommentsModal({ open: true, ticket });
    setLoadingComments(true);
    try {
      const res = await api.tickets.getComments(ticket.id);
      setComments(res);
    } catch (err) {
      setError(err);
    } finally {
      setLoadingComments(false);
    }
  }

  async function handleAddComment(e) {
    e.preventDefault();
    if (!newComment.trim()) return;
    setSubmitting(true);
    try {
      if (editingCommentId) {
        await api.tickets.updateComment(commentsModal.ticket.id, editingCommentId, { content: newComment });
      } else {
        await api.tickets.addComment(commentsModal.ticket.id, { content: newComment });
      }
      setNewComment("");
      setEditingCommentId(null);
      const res = await api.tickets.getComments(commentsModal.ticket.id);
      setComments(res);
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteComment(commentId) {
    if(!window.confirm("Are you sure you want to delete this comment?")) return;
    try {
      await api.tickets.removeComment(commentsModal.ticket.id, commentId);
      setComments(comments.filter(c => c.id !== commentId));
    } catch(err) {
      setError(err);
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
        <button type="button" className="button button--primary" onClick={openCreate}>
          <Plus size={16} />
          Create ticket
        </button>
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
          onSubmit={(event) => {
            event.preventDefault();
            setPage(0);
            setAppliedFilters({ ...filters });
          }}
          style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "flex-end", paddingTop: "12px" }}
        >
          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 140px", minWidth: "120px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Status</span>
            <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              <option value="">All statuses</option>
              {TICKET_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {titleizeEnum(status)}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 140px", minWidth: "120px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Priority</span>
            <select value={filters.priority} onChange={(event) => setFilters((current) => ({ ...current, priority: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              <option value="">All priorities</option>
              {TICKET_PRIORITIES.map((priority) => (
                <option key={priority} value={priority}>
                  {titleizeEnum(priority)}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 140px", minWidth: "120px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Category</span>
            <select value={filters.category} onChange={(event) => setFilters((current) => ({ ...current, category: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              <option value="">All categories</option>
              {TICKET_CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {titleizeEnum(category)}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 160px", minWidth: "140px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Reporter</span>
            <input
              value={filters.reportedBy}
              onChange={(event) => setFilters((current) => ({ ...current, reportedBy: event.target.value }))}
              placeholder={isAdmin ? "Filter by reporter email" : "Already ownership-scoped"}
              disabled={!isAdmin}
              style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: !isAdmin ? "#f1f5f9" : "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: !isAdmin ? "not-allowed" : "text" }}
            />
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 180px", minWidth: "150px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Assigned technician</span>
            <input
              value={filters.assignedTechnician}
              onChange={(event) => setFilters((current) => ({ ...current, assignedTechnician: event.target.value }))}
              placeholder={isAdmin ? "Filter by technician email" : "Technician scoping is already enforced"}
              disabled={!isAdmin && !isTechnician}
              style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: !isAdmin && !isTechnician ? "#f1f5f9" : "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: !isAdmin && !isTechnician ? "not-allowed" : "text" }}
            />
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 160px", minWidth: "140px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Sort</span>
            <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              {TICKET_SORT_OPTIONS.map((option) => (
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
              Apply filters
            </button>
          </div>
        </form>
      </DataToolbar>

      <FeedbackBanner error={error} />

      <SectionCard
        eyebrow="Incident queue"
        title={loading ? "Loading tickets" : `${pageData?.totalElements ?? 0} tickets in scope`}
        description="Priority, ownership, technician assignment, and lifecycle state remain visible on one board."
        actions={
          <button type="button" className="button button--subtle" onClick={() => void loadTickets(page, appliedFilters)}>
            <RefreshCcw size={16} />
            Refresh
          </button>
        }
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
                        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                          <div style={{ width: "40px", height: "40px", borderRadius: "8px", backgroundColor: "#fef3c7", color: "#d97706", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                            <Wrench size={20} />
                          </div>
                          <div>
                            <div style={{ fontWeight: "600", color: "#1e293b", fontSize: "14px" }}>{ticket.title}</div>
                            <div style={{ fontSize: "12px", color: "#64748b", marginTop: "4px", display: "flex", alignItems: "center", gap: "4px" }}>
                              <MapPin size={12} /> {ticket.location}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                          <div style={{ width: "32px", height: "32px", borderRadius: "50%", backgroundColor: "#e0e7ff", color: "#4f46e5", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "12px", fontWeight: "bold", flexShrink: 0 }}>
                            {(ticket.reporterDisplayName || ticket.reporterEmail || 'U').charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div style={{ fontWeight: "500", color: "#1e293b", fontSize: "13px" }}>{ticket.reporterDisplayName || ticket.reporterEmail}</div>
                            <div style={{ fontSize: "11px", color: "#64748b" }}>{ticket.reportedBy || ticket.reporterEmail}</div>
                          </div>
                        </div>
                      </td>
                      <td>
                        {ticket.technicianEmail ? (
                          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                            <div style={{ width: "32px", height: "32px", borderRadius: "50%", backgroundColor: "#dcfce7", color: "#059669", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "12px", fontWeight: "bold", flexShrink: 0 }}>
                              {(ticket.technicianDisplayName || ticket.technicianEmail).charAt(0).toUpperCase()}
                            </div>
                            <div>
                              <div style={{ fontWeight: "500", color: "#1e293b", fontSize: "13px" }}>{ticket.technicianDisplayName || ticket.technicianEmail}</div>
                              <div style={{ fontSize: "11px", color: "#64748b" }}>Technician</div>
                            </div>
                          </div>
                        ) : (
                          <span style={{ display: "inline-flex", padding: "6px 12px", backgroundColor: "#f1f5f9", color: "#64748b", border: "1px dashed #cbd5e1", borderRadius: "6px", fontSize: "12px", fontWeight: "500" }}>Unassigned</span>
                        )}
                      </td>
                      <td>
                        <div style={{ display: "flex", flexDirection: "column", gap: "6px", alignItems: "flex-start" }}>
                          <StatusBadge value={ticket.category} variant="general" />
                          <StatusBadge value={ticket.priority} variant={ticket.priority} />
                        </div>
                      </td>
                      <td>
                        <StatusBadge value={ticket.status} />
                      </td>
                      <td>
                        <div style={{ fontSize: "13px", color: "#475569", whiteSpace: "nowrap" }}>
                          {formatCompactDateTime(ticket.updatedAt || ticket.createdAt)}
                        </div>
                      </td>
                      <td>
                        <div className="table-actions">
                          {canEdit ? (
                            <button type="button" className="icon-button" onClick={() => openEdit(ticket)} aria-label="Edit ticket" title="Edit ticket">
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
                              title="Assign technician"
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
                                  currentStatus: ticket.status,
                                  status: ticket.status === "OPEN" ? "IN_PROGRESS" : (ticket.status === "IN_PROGRESS" ? "RESOLVED" : (ticket.status === "RESOLVED" && isAdmin ? "CLOSED" : ticket.status)),
                                  resolutionNotes: ticket.resolutionNotes || "",
                                });
                              }}
                              aria-label="Update ticket status"
                              title="Update ticket status"
                            >
                              <ArrowRightLeft size={16} />
                            </button>
                          ) : null}

                          <button type="button" className="icon-button" onClick={() => openComments(ticket)} aria-label="View comments" title="Comments">
                            <MessageSquare size={16} />
                          </button>

                          {canCancel ? (
                            <button type="button" className="button button--subtle" onClick={() => setCancelTarget(ticket)} title="Cancel ticket">
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
          <div style={{ padding: "20px", backgroundColor: "#f8fafc", borderRadius: "12px", border: "1px solid #e2e8f0", marginBottom: "16px" }}>
            <h4 style={{ margin: "0 0 16px 0", fontSize: "15px", color: "#1e293b", display: "flex", alignItems: "center", gap: "6px" }}><Wrench size={16} color="#3b82f6"/> General Information</h4>
            <div className="form-grid form-grid--two">
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Issue Title</span>
                <input value={formState.title} onChange={(event) => setFormState((current) => ({ ...current, title: event.target.value }))} placeholder="Brief summary of the issue" required style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Location</span>
                <input value={formState.location} onChange={(event) => setFormState((current) => ({ ...current, location: event.target.value }))} placeholder="E.g. Block A, Room 101" required style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
            </div>
            <label className="field" style={{ marginTop: "16px" }}>
              <span style={{ fontWeight: 500, color: "#475569" }}>Detailed Description</span>
              <textarea rows={3} value={formState.description} onChange={(event) => setFormState((current) => ({ ...current, description: event.target.value }))} placeholder="Provide specific details about what needs to be fixed..." required style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
            </label>
          </div>

          <div style={{ padding: "20px", backgroundColor: "#f8fafc", borderRadius: "12px", border: "1px solid #e2e8f0", marginBottom: "16px" }}>
            <h4 style={{ margin: "0 0 16px 0", fontSize: "15px", color: "#1e293b" }}>Classification</h4>
            <div className="form-grid form-grid--two">
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Category</span>
                <select value={formState.category} onChange={(event) => setFormState((current) => ({ ...current, category: event.target.value }))} style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }}>
                  {TICKET_CATEGORIES.map((category) => (
                    <option key={category} value={category}>
                      {titleizeEnum(category)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Priority</span>
                <select value={formState.priority} onChange={(event) => setFormState((current) => ({ ...current, priority: event.target.value }))} style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }}>
                  {TICKET_PRIORITIES.map((priority) => (
                    <option key={priority} value={priority}>
                      {titleizeEnum(priority)}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {isAdmin ? (
              <label className="field" style={{ marginTop: "16px" }}>
                <span style={{ fontWeight: 500, color: "#475569" }}>Reporter email (Admin override)</span>
                <input value={formState.reportedBy} onChange={(event) => setFormState((current) => ({ ...current, reportedBy: event.target.value }))} placeholder="Leave blank to use your own account" style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
            ) : null}
          </div>

          <div style={{ padding: "20px", backgroundColor: "#ffffff", borderRadius: "12px", border: "1px dashed #94a3b8", marginBottom: "24px" }}>
            <h4 style={{ margin: "0 0 12px 0", fontSize: "15px", color: "#1e293b" }}>Attachments</h4>
            <label className="field">
              <span style={{ fontWeight: 500, color: "#475569" }}>Upload Images (Max 3)</span>
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={handleFileChange}
              disabled={submitting}
            />
              <small style={{ color: "#64748b", marginTop: "6px", fontSize: "12px" }}>
              Please select up to 3 images from your device.
            </small>
          </label>

          {imagePreviews.length > 0 ? (
              <div className="image-previews" style={{ display: "flex", gap: "12px", marginTop: "16px" }}>
              {imagePreviews.map((src, index) => (
                <img
                  key={index}
                  src={src}
                  alt={`Preview ${index + 1}`}
                    style={{ width: "90px", height: "90px", objectFit: "cover", borderRadius: "8px", border: "2px solid #e2e8f0", cursor: "pointer", boxShadow: "0 2px 4px rgba(0,0,0,0.05)" }}
                  onClick={() => setEnlargedImage(src)}
                />
              ))}
            </div>
          ) : null}
          </div>

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
        onClose={() => setStatusModal({ open: false, ticketId: null, currentStatus: "OPEN", status: "IN_PROGRESS", resolutionNotes: "" })}
      >
        <form className="form-grid" onSubmit={handleStatusSubmit}>
          <label className="field">
            <span>Status</span>
            <select value={statusModal.status} onChange={(event) => setStatusModal((current) => ({ ...current, status: event.target.value }))}>
              {TICKET_STATUSES.filter((status) => {
                const current = statusModal.currentStatus;
                if (current === status) return true;

                if (isAdmin) {
                  if (current === "OPEN" && ["IN_PROGRESS", "RESOLVED", "CANCELLED"].includes(status)) return true;
                  if (current === "IN_PROGRESS" && ["RESOLVED", "CANCELLED"].includes(status)) return true;
                  if (current === "RESOLVED" && ["CLOSED", "IN_PROGRESS"].includes(status)) return true;
                  return false;
                }

                if (isTechnician) {
                  if (current === "OPEN" && status === "IN_PROGRESS") return true;
                  if (current === "IN_PROGRESS" && status === "RESOLVED") return true;
                  if (current === "RESOLVED" && status === "IN_PROGRESS") return true;
                  return false;
                }
                return false;
              }).map((status) => (
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
            <button type="button" className="button button--ghost" onClick={() => setStatusModal({ open: false, ticketId: null, currentStatus: "OPEN", status: "IN_PROGRESS", resolutionNotes: "" })}>
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

      <Modal
        isOpen={commentsModal.open}
        title={`Comments: ${commentsModal.ticket?.title}`}
        subtitle="Discuss this incident with technicians and admins."
        onClose={() => { setCommentsModal({ open: false, ticket: null }); setNewComment(""); setEditingCommentId(null); }}
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "16px", maxHeight: "50vh", overflowY: "auto", paddingBottom: "16px" }}>
          {loadingComments ? (
            <LoadingState title="Loading comments" message="Please wait..." compact />
          ) : comments.length > 0 ? (
            comments.map(c => (
              <div key={c.id} style={{ padding: "12px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <span style={{ fontWeight: 600, fontSize: "13px", color: "#1e293b" }}>{c.authorDisplayName || c.authorEmail}</span>
                  <span style={{ fontSize: "11px", color: "#64748b" }}>{formatCompactDateTime(c.createdAt)}</span>
                </div>
                <p style={{ margin: 0, fontSize: "13px", color: "#334155", whiteSpace: "pre-wrap" }}>{c.content}</p>
                {(isAdmin || c.authorEmail === currentUser?.email) && (
                  <div style={{ display: "flex", gap: "12px", marginTop: "12px", justifyContent: "flex-end" }}>
                    <button type="button" onClick={() => { setEditingCommentId(c.id); setNewComment(c.content); }} style={{ background: "none", border: "none", color: "#3b82f6", fontSize: "12px", fontWeight: "600", cursor: "pointer", padding: 0 }}>Edit</button>
                    <button type="button" onClick={() => handleDeleteComment(c.id)} style={{ background: "none", border: "none", color: "#ef4444", fontSize: "12px", fontWeight: "600", cursor: "pointer", padding: 0 }}>Delete</button>
                  </div>
                )}
              </div>
            ))
          ) : (
            <p style={{ color: "#64748b", fontSize: "13px", textAlign: "center" }}>No comments yet. Start the conversation!</p>
          )}
        </div>
        
        <form onSubmit={handleAddComment} style={{ display: "flex", flexDirection: "column", gap: "8px", borderTop: "1px solid #e2e8f0", paddingTop: "16px", marginTop: "8px" }}>
          <textarea rows={3} value={newComment} onChange={e => setNewComment(e.target.value)} placeholder="Type your comment here..." style={{ padding: "10px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", resize: "vertical", width: "100%" }} required />
          <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
            {editingCommentId && <button type="button" className="button button--ghost" onClick={() => { setEditingCommentId(null); setNewComment(""); }}>Cancel</button>}
            <button type="submit" className="button button--primary" disabled={submitting || !newComment.trim()}>{editingCommentId ? "Update Comment" : "Post Comment"}</button>
          </div>
        </form>
      </Modal>

      {enlargedImage && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            width: "100vw",
            height: "100vh",
            backgroundColor: "rgba(0, 0, 0, 0.8)",
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            cursor: "zoom-out",
          }}
          onClick={() => setEnlargedImage(null)}
        >
          <img
            src={enlargedImage}
            alt="Enlarged ticket attachment"
            style={{
              maxWidth: "90vw",
              maxHeight: "90vh",
              objectFit: "contain",
              borderRadius: "8px",
              boxShadow: "0 4px 12px rgba(0,0,0,0.5)",
            }}
          />
        </div>
      )}
    </PageShell>
  );
}
