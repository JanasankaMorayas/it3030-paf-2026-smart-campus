function joinClasses(...classes) {
  return classes.filter(Boolean).join(" ");
}

export default function StatCard({ icon: Icon, label, value, hint, tone = "default", trend }) {
  return (
    <article className={joinClasses("stat-card", `stat-card--${tone}`)}>
      <div className="stat-card__top">
        <div className="stat-card__label-group">
          <span className="stat-card__label">{label}</span>
          <strong className="stat-card__value">{value}</strong>
        </div>
        {Icon ? (
          <div className="stat-card__icon">
            <Icon size={18} />
          </div>
        ) : null}
      </div>
      {hint ? <p className="stat-card__hint">{hint}</p> : null}
      {trend ? <p className="stat-card__trend">{trend}</p> : null}
    </article>
  );
}
