export default function PaginationBar({ pageData, onPageChange }) {
  if (!pageData) {
    return null;
  }

  return (
    <div className="pagination-bar">
      <div className="pagination-meta">
        <span>
          Page {pageData.page + 1} of {Math.max(pageData.totalPages, 1)}
        </span>
        <span>{pageData.totalElements} total records</span>
      </div>
      <div className="pagination-actions">
        <button
          type="button"
          className="button button--ghost"
          onClick={() => onPageChange(pageData.page - 1)}
          disabled={pageData.first}
        >
          Previous
        </button>
        <button
          type="button"
          className="button button--ghost"
          onClick={() => onPageChange(pageData.page + 1)}
          disabled={pageData.last}
        >
          Next
        </button>
      </div>
    </div>
  );
}
