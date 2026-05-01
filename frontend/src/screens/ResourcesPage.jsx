import { Boxes, Pencil, Plus, RefreshCcw, Trash2, Wrench, Info, CalendarRange, MapPin, Users } from "lucide-react";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import ConfirmDialog from "../components/ConfirmDialog.jsx";
import DataToolbar from "../components/DataToolbar.jsx";
import EmptyState from "../components/EmptyState.jsx";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import LoadingState from "../components/LoadingState.jsx";
import Modal from "../components/Modal.jsx";
import PageShell from "../components/PageShell.jsx";
import SectionCard from "../components/SectionCard.jsx";
import StatCard from "../components/StatCard.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { compactCount, formatDateRange, toDateTimeLocalValue, titleizeEnum } from "../lib/format.js";
import { RESOURCE_STATUSES, RESOURCE_TYPES } from "../lib/options.js";
import { api } from "../lib/api.js";

const initialFilters = {
  type: "",
  location: "",
  minCapacity: "",
  status: "",
};

const initialForm = {
  resourceCode: "",
  name: "",
  description: "",
  type: "LAB",
  capacity: "1",
  location: "",
  availabilityStart: "",
  availabilityEnd: "",
  status: "ACTIVE",
};

export default function ResourcesPage() {
  const { isAdmin } = useAuth();
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState(initialFilters);
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingResource, setEditingResource] = useState(null);
  const [formState, setFormState] = useState(initialForm);
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [resourceToDelete, setResourceToDelete] = useState(null);

  async function loadResources(activeFilters = filters) {
    setLoading(true);
    setError(null);

    try {
      const response = await api.resources.list(activeFilters);
      setResources(response);
    } catch (loadError) {
      setError(loadError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const locParam = searchParams.get("location");
    if (locParam) {
      setFilters((current) => ({ ...current, location: locParam }));
      void loadResources({ ...initialFilters, location: locParam });
    } else {
      void loadResources();
    }
  }, [searchParams]);

  function openCreate() {
    setEditingResource(null);
    setFormState(initialForm);
    setFormError(null);
    setModalOpen(true);
  }

  function openEdit(resource) {
    setEditingResource(resource);
    setFormState({
      resourceCode: resource.resourceCode,
      name: resource.name,
      description: resource.description || "",
      type: resource.type,
      capacity: String(resource.capacity),
      location: resource.location,
      availabilityStart: toDateTimeLocalValue(resource.availabilityStart),
      availabilityEnd: toDateTimeLocalValue(resource.availabilityEnd),
      status: resource.status,
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
      capacity: Number(formState.capacity),
      availabilityStart: formState.availabilityStart || null,
      availabilityEnd: formState.availabilityEnd || null,
    };

    try {
      if (editingResource) {
        await api.resources.update(editingResource.id, payload);
      } else {
        await api.resources.create(payload);
      }

      setModalOpen(false);
      await loadResources();
    } catch (submitError) {
      setFormError(submitError);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteConfirmed() {
    if (!resourceToDelete) {
      return;
    }

    try {
      await api.resources.remove(resourceToDelete.id);
      setResourceToDelete(null);
      await loadResources();
    } catch (deleteError) {
      setError(deleteError);
    }
  }

  const activeCount = resources.filter((resource) => resource.status === "ACTIVE").length;
  const offlineCount = resources.filter((resource) => resource.status === "OUT_OF_SERVICE").length;
  const labCount = resources.filter((resource) => resource.type === "LAB").length;

  return (
    <PageShell
      eyebrow="Module A"
      title="Resource operations"
      description="Keep the campus catalogue clean, searchable, and ready for booking and maintenance workflows."
      actions={isAdmin ? (
        <button type="button" className="button button--primary" onClick={openCreate}>
          <Plus size={16} />
          Create resource
        </button>
      ) : (
        null
      )}
      meta={(
        <>
          <StatusBadge value={isAdmin ? "ADMIN" : "USER"} variant="general" />
          <span className="page-shell__meta-text">
            {isAdmin ? "Write access is live for resource operations." : "You have read-only access to the public resource catalogue."}
          </span>
        </>
      )}
    >
      <section className="dashboard-stat-grid dashboard-stat-grid--compact">
        <StatCard icon={Boxes} label="Visible resources" value={loading ? "..." : compactCount(resources.length)} hint="Filtered catalogue count" tone="teal" />
        <StatCard icon={Boxes} label="Active" value={loading ? "..." : compactCount(activeCount)} hint="Ready for usage and bookings" tone="cream" />
        <StatCard icon={Wrench} label="Out of service" value={loading ? "..." : compactCount(offlineCount)} hint="Temporarily unavailable assets" tone="sand" />
        <StatCard icon={Boxes} label="Labs" value={loading ? "..." : compactCount(labCount)} hint="Spaces configured as labs" tone="teal" />
      </section>

      <DataToolbar
        eyebrow="Catalogue filters"
        title="Search the resource inventory"
        description="Narrow the public catalogue by type, location, capacity, or operating status."
      >
        <form
          onSubmit={(event) => {
            event.preventDefault();
            void loadResources();
          }}
          style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "flex-end", paddingTop: "12px" }}
        >
          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 180px", minWidth: "150px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Type</span>
            <select value={filters.type} onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              <option value="">All types</option>
              {RESOURCE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {titleizeEnum(type)}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 180px", minWidth: "150px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Location</span>
            <input
              value={filters.location}
              onChange={(event) => setFilters((current) => ({ ...current, location: event.target.value }))}
              placeholder="Block A"
              style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none" }}
            />
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 120px", minWidth: "100px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Min capacity</span>
            <input
              type="number"
              min="1"
              value={filters.minCapacity}
              onChange={(event) => setFilters((current) => ({ ...current, minCapacity: event.target.value }))}
              placeholder="40"
              style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none" }}
            />
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 180px", minWidth: "150px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Status</span>
            <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              <option value="">All statuses</option>
              {RESOURCE_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {titleizeEnum(status)}
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
                setFilters(initialFilters);
                void loadResources(initialFilters);
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
        eyebrow="Catalogue board"
        title={loading ? "Loading resources" : `${resources.length} resources in view`}
        description="Operational overview of spaces and assets available to the campus workflow."
        actions={
          <button type="button" className="button button--subtle" onClick={() => void loadResources()}>
            <RefreshCcw size={16} />
            Refresh
          </button>
        }
      >
        {loading ? (
          <LoadingState title="Loading resource catalogue" message="Fetching the latest campus inventory." lines={5} />
        ) : resources.length ? (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Resource</th>
                  <th>Description</th>
                  <th>Type</th>
                  <th>Details</th>
                  <th>Availability</th>
                  <th>Status</th>
                  {isAdmin ? <th>Actions</th> : null}
                </tr>
              </thead>
              <tbody>
                {resources.map((resource) => (
                  <tr key={resource.id}>
                    <td>
                      <strong>{resource.resourceCode}</strong>
                    </td>
                    <td>
                      <strong>{resource.name}</strong>
                      <p className="table-subtext">{resource.description || "No description recorded yet."}</p>
                    </td>
                    <td>
                      <StatusBadge value={resource.type} />
                    </td>
                    <td>{resource.capacity}</td>
                    <td>{resource.location}</td>
                    <td>{formatDateRange(resource.availabilityStart, resource.availabilityEnd)}</td>
                    <td>
                      <StatusBadge value={resource.status} />
                    </td>
                    {isAdmin ? (
                      <td>
                        <div className="table-actions">
                          <button type="button" className="icon-button" onClick={() => openEdit(resource)} aria-label="Edit resource">
                            <Pencil size={16} />
                          </button>
                          <button
                            type="button"
                            className="icon-button icon-button--danger"
                            onClick={() => setResourceToDelete(resource)}
                            aria-label="Delete resource"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="No resources matched"
            message="Try widening the filters or reset the search to view the full catalogue."
            action={(
              <button
                type="button"
                className="button button--ghost"
                onClick={() => {
                  setFilters(initialFilters);
                  void loadResources(initialFilters);
                }}
              >
                Reset filters
              </button>
            )}
          />
        )}
      </SectionCard>

      <Modal
        isOpen={modalOpen}
        title={editingResource ? "Edit resource" : "Create resource"}
        subtitle="Resource writes are admin-controlled and feed the shared operations catalogue."
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={handleSubmit}>
          <div style={{ padding: "20px", backgroundColor: "#f8fafc", borderRadius: "12px", border: "1px solid #e2e8f0", marginBottom: "16px" }}>
            <h4 style={{ margin: "0 0 16px 0", fontSize: "15px", color: "#1e293b", display: "flex", alignItems: "center", gap: "6px" }}><Info size={16} color="#3b82f6"/> General Information</h4>
            <div className="form-grid form-grid--two">
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Resource code</span>
                <input value={formState.resourceCode} onChange={(event) => setFormState((current) => ({ ...current, resourceCode: event.target.value }))} placeholder="E.g. LAB-001" required style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Name</span>
                <input value={formState.name} onChange={(event) => setFormState((current) => ({ ...current, name: event.target.value }))} placeholder="Resource name" required style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
            </div>
            <label className="field" style={{ marginTop: "16px" }}>
              <span style={{ fontWeight: 500, color: "#475569" }}>Description</span>
              <textarea value={formState.description} onChange={(event) => setFormState((current) => ({ ...current, description: event.target.value }))} rows={3} placeholder="Brief details about the resource..." style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
            </label>
          </div>

          <div style={{ padding: "20px", backgroundColor: "#f8fafc", borderRadius: "12px", border: "1px solid #e2e8f0", marginBottom: "16px" }}>
            <h4 style={{ margin: "0 0 16px 0", fontSize: "15px", color: "#1e293b", display: "flex", alignItems: "center", gap: "6px" }}><Boxes size={16} color="#8b5cf6"/> Details & Classification</h4>
            <div className="form-grid form-grid--two">
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Type</span>
                <select value={formState.type} onChange={(event) => setFormState((current) => ({ ...current, type: event.target.value }))} style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }}>
                  {RESOURCE_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {titleizeEnum(type)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Status</span>
                <select value={formState.status} onChange={(event) => setFormState((current) => ({ ...current, status: event.target.value }))} style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }}>
                  {RESOURCE_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {titleizeEnum(status)}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="form-grid form-grid--two" style={{ marginTop: "16px" }}>
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Capacity</span>
                <input type="number" min="1" value={formState.capacity} onChange={(event) => setFormState((current) => ({ ...current, capacity: event.target.value }))} required style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Location</span>
                <input value={formState.location} onChange={(event) => setFormState((current) => ({ ...current, location: event.target.value }))} placeholder="Building, floor, or room" required style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
            </div>
          </div>

          <div style={{ padding: "20px", backgroundColor: "#f8fafc", borderRadius: "12px", border: "1px solid #e2e8f0", marginBottom: "24px" }}>
            <h4 style={{ margin: "0 0 16px 0", fontSize: "15px", color: "#1e293b", display: "flex", alignItems: "center", gap: "6px" }}><CalendarRange size={16} color="#10b981"/> Availability Window</h4>
            <div className="form-grid form-grid--two">
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Availability start</span>
                <input type="datetime-local" value={formState.availabilityStart} onChange={(event) => setFormState((current) => ({ ...current, availabilityStart: event.target.value }))} style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
              <label className="field">
                <span style={{ fontWeight: 500, color: "#475569" }}>Availability end</span>
                <input type="datetime-local" value={formState.availabilityEnd} onChange={(event) => setFormState((current) => ({ ...current, availabilityEnd: event.target.value }))} style={{ padding: "10px", borderRadius: "6px", border: "1px solid #cbd5e1" }} />
              </label>
            </div>
          </div>

          <FeedbackBanner error={formError} />

          <div className="modal-actions">
            <button type="button" className="button button--ghost" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="button button--primary" disabled={submitting}>
              {editingResource ? "Save resource" : "Create resource"}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(resourceToDelete)}
        title="Delete resource"
        message={resourceToDelete ? `Delete ${resourceToDelete.resourceCode} - ${resourceToDelete.name}? This removes it from the catalogue.` : ""}
        confirmLabel="Delete resource"
        onConfirm={handleDeleteConfirmed}
        onClose={() => setResourceToDelete(null)}
      />
    </PageShell>
  );
}
