import SectionCard from "./SectionCard.jsx";

export default function DataToolbar({ eyebrow, title, description, actions, children }) {
  return (
    <SectionCard eyebrow={eyebrow} title={title} description={description} actions={actions} className="data-toolbar">
      {children}
    </SectionCard>
  );
}
