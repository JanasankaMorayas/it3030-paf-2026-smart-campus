export default function PageShell({ eyebrow, title, description, actions, meta, children }) {
  return (
    <div className="page-stack">
      <section className="page-shell">
        <div className="page-shell__copy">
          {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
          <h2>{title}</h2>
          {description ? <p className="page-shell__description">{description}</p> : null}
          {meta ? <div className="page-shell__meta">{meta}</div> : null}
        </div>
        {actions ? <div className="page-shell__actions">{actions}</div> : null}
      </section>
      {children}
    </div>
  );
}
