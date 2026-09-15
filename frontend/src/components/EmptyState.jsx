export default function EmptyState({ title, action }) {
  return (
    <div className="empty-state">
      <h3>{title}</h3>
      {action}
    </div>
  );
}
