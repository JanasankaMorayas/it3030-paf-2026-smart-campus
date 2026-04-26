import { Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import Modal from "../components/Modal.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { formatDateRange, toDateTimeLocalValue, titleizeEnum } from "../lib/format.js";
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
  const [filters, setFilters] = useState(initialFilters);
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingResource, setEditingResource] = useState(null);
  const [formState, setFormState] = useState(initialForm);
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

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
    void loadResources();
  }, []);

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

  async function handleDelete(resourceId) {
    if (!window.confirm("Delete this resource?")) {
      return;
    }

    try {
      await api.resources.remove(resourceId);
      await loadResources();
    } catch (deleteError) {
      setError(deleteError);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">Module A</p>
          <h2>Resources</h2>
          <p>Manage the campus resource catalogue, availability windows, and operations status.</p>
        </div>
        {isAdmin ? (
          <button type="button" className="button button--primary" onClick={openCreate}>
            <Plus size={16} />
            Create resource
          </button>
        ) : null}
      </section>

      <section className="panel">
        <form
          className="filters-grid"
          onSubmit={(event) => {
            event.preventDefault();
            void loadResources();
          }}
        >
          <label className="field">
            <span>Type</span>
            <select value={filters.type} onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value }))}>
              <option value="">All types</option>
              {RESOURCE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {titleizeEnum(type)}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Location</span>
            <input
              value={filters.location}
              onChange={(event) => setFilters((current) => ({ ...current, location: event.target.value }))}
              placeholder="Block A"
            />
          </label>

          <label className="field">
            <span>Min capacity</span>
            <input
              type="number"
              min="1"
              value={filters.minCapacity}
              onChange={(event) => setFilters((current) => ({ ...current, minCapacity: event.target.value }))}
            />
          </label>

          <label className="field">
            <span>Status</span>
            <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}>
              <option value="">All statuses</option>
              {RESOURCE_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {titleizeEnum(status)}
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
                setFilters(initialFilters);
                void loadResources(initialFilters);
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
            <p className="eyebrow">Catalogue</p>
            <h3>{loading ? "Loading resources..." : `${resources.length} resources found`}</h3>
          </div>
        </div>

        {resources.length ? (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Capacity</th>
                  <th>Location</th>
                  <th>Availability</th>
                  <th>Status</th>
                  {isAdmin ? <th>Actions</th> : null}
                </tr>
              </thead>
              <tbody>
                {resources.map((resource) => (
                  <tr key={resource.id}>
                    <td>{resource.resourceCode}</td>
                    <td>
                      <strong>{resource.name}</strong>
                      <p className="table-subtext">{resource.description || "No description"}</p>
                    </td>
                    <td>{titleizeEnum(resource.type)}</td>
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
                            onClick={() => void handleDelete(resource.id)}
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
          <div className="empty-state">No resources matched the current filters.</div>
        )}
      </section>

      <Modal
        isOpen={modalOpen}
        title={editingResource ? "Edit resource" : "Create resource"}
        subtitle="Resource writes require admin access."
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Resource code</span>
              <input value={formState.resourceCode} onChange={(event) => setFormState((current) => ({ ...current, resourceCode: event.target.value }))} />
            </label>
            <label className="field">
              <span>Name</span>
              <input value={formState.name} onChange={(event) => setFormState((current) => ({ ...current, name: event.target.value }))} />
            </label>
          </div>

          <label className="field">
            <span>Description</span>
            <textarea value={formState.description} onChange={(event) => setFormState((current) => ({ ...current, description: event.target.value }))} rows={3} />
          </label>

          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Type</span>
              <select value={formState.type} onChange={(event) => setFormState((current) => ({ ...current, type: event.target.value }))}>
                {RESOURCE_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {titleizeEnum(type)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Status</span>
              <select value={formState.status} onChange={(event) => setFormState((current) => ({ ...current, status: event.target.value }))}>
                {RESOURCE_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {titleizeEnum(status)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Capacity</span>
              <input type="number" min="1" value={formState.capacity} onChange={(event) => setFormState((current) => ({ ...current, capacity: event.target.value }))} />
            </label>
            <label className="field">
              <span>Location</span>
              <input value={formState.location} onChange={(event) => setFormState((current) => ({ ...current, location: event.target.value }))} />
            </label>
          </div>

          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Availability start</span>
              <input type="datetime-local" value={formState.availabilityStart} onChange={(event) => setFormState((current) => ({ ...current, availabilityStart: event.target.value }))} />
            </label>
            <label className="field">
              <span>Availability end</span>
              <input type="datetime-local" value={formState.availabilityEnd} onChange={(event) => setFormState((current) => ({ ...current, availabilityEnd: event.target.value }))} />
            </label>
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
    </div>
  );
}
