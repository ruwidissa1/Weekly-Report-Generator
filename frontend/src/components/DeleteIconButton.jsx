import { Trash2 } from 'lucide-react';

export default function DeleteIconButton({ label, warning, onConfirm }) {
  async function handleClick() {
    // Warning to delete confirmation
    if (!window.confirm(`Warning: ${warning}`)) {
      return;
    }
    await onConfirm();
  }

  return (
    <button className="icon-button danger-button" title={label} aria-label={label} onClick={handleClick}>
      <Trash2 aria-hidden="true" />
    </button>
  );
}
