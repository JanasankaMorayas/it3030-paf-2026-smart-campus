export default function PaginationBar({ pageData, onPageChange }) {
  if (!pageData) {
    return null;
  }

  const currentPage = pageData.page + 1;
  const from = pageData.totalElements === 0 ? 0 : pageData.page * pageData.size + 1;
  const to = Math.min((pageData.page + 1) * pageData.size, pageData.totalElements);
  const pageButtons = [];

  for (let index = 0; index < pageData.totalPages; index += 1) {
    if (index === 0 || index === pageData.totalPages - 1 || Math.abs(index - pageData.page) <= 1) {
      pageButtons.push(index);
    }
  }

  const uniqueButtons = [...new Set(pageButtons)];

  return (
    <div className="pagination-bar">
      <div className="pagination-meta">
        <span>
          Page {currentPage} of {Math.max(pageData.totalPages, 1)}
        </span>
        <span>
          Showing {from}-{to} of {pageData.totalElements}
        </span>
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

        {uniqueButtons.map((buttonPage, index) => {
          const previous = uniqueButtons[index - 1];
          const showGap = previous !== undefined && buttonPage - previous > 1;

          return (
            <span key={buttonPage} className="pagination-actions__group">
              {showGap ? <span className="pagination-ellipsis">...</span> : null}
              <button
                type="button"
                className={`button ${buttonPage === pageData.page ? "button--primary" : "button--ghost"}`}
                onClick={() => onPageChange(buttonPage)}
              >
                {buttonPage + 1}
              </button>
            </span>
          );
        })}

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
