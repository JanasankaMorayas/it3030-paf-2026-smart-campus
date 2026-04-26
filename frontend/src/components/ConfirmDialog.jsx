import FeedbackBanner from "./FeedbackBanner.jsx";
import Modal from "./Modal.jsx";

export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  tone = "danger",
  busy = false,
  error = null,
  onConfirm,
  onClose,
}) {
  return (
    <Modal isOpen={open} title={title} subtitle="Review the action before continuing." onClose={onClose}>
      <div className="confirm-dialog">
        <p className="confirm-dialog__message">{message}</p>
        <FeedbackBanner error={error} />
        <div className="modal-actions">
          <button type="button" className="button button--ghost" onClick={onClose} disabled={busy}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`button ${tone === "danger" ? "button--danger" : "button--primary"}`}
            onClick={() => void onConfirm()}
            disabled={busy}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  );
}
