function joinClasses(...classes) {
  return classes.filter(Boolean).join(" ");
}

export default function SectionCard({
  eyebrow,
  title,
  description,
  actions,
  footer,
  className,
  children,
}) {
  return (
    <section className={joinClasses("section-card", className)}>
      {eyebrow || title || description || actions ? (
        <div className="section-card__header">
          <div className="section-card__heading">
            {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
            {title ? <h3>{title}</h3> : null}
            {description ? <p>{description}</p> : null}
          </div>
          {actions ? <div className="section-card__actions">{actions}</div> : null}
        </div>
      ) : null}

      <div className="section-card__body">{children}</div>

      {footer ? <div className="section-card__footer">{footer}</div> : null}
    </section>
  );
}
