const labels = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  NEEDS_CORRECTION: 'Needs correction',
  APPROVED: 'Approved',
  PENDING: 'Pending',
  LATE: 'Late',
  NOT_STARTED: 'Not yet started'
};

export default function StatusBadge({ status }) {
  return <span className={`status ${status?.toLowerCase()}`}>{labels[status] || status}</span>;
}
