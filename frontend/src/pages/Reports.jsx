import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import DeleteIconButton from '../components/DeleteIconButton.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import { useAuth } from '../state/AuthContext.jsx';

const reportStatuses = [
  ['SUBMITTED', 'Submitted'],
  ['NEEDS_CORRECTION', 'Needs correction'],
  ['APPROVED', 'Approved']
];

const trackerStatuses = [
  ['DRAFT', 'Draft'],
  ['SUBMITTED', 'Submitted'],
  ['NEEDS_CORRECTION', 'Needs correction'],
  ['APPROVED', 'Approved'],
  ['NOT_STARTED', 'Not yet started']
];

function mondayToday() {
  const date = new Date();
  const day = date.getDay() || 7;
  date.setDate(date.getDate() - day + 1);
  return date.toISOString().slice(0, 10);
}

export default function Reports() {
  const auth = useAuth();
  const [reports, setReports] = useState([]);
  const [weekData, setWeekData] = useState(null);
  const [users, setUsers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [filters, setFilters] = useState({ weekStart: mondayToday() });
  const [sectionView, setSectionView] = useState('blockers');
  const [error, setError] = useState('');

  useEffect(() => {
    api.users().then(setUsers);
    api.projects().then(setProjects);
  }, []);

  function updateFilters(patch) {
    setFilters((current) => ({ ...current, ...patch }));
  }

  function loadReports() {
    const { weekStart, trackerStatus, ...reportFilters } = filters;
    api.reports({ ...reportFilters, size: 100 }).then((data) => setReports(data.content || []));
  }

  function loadWeek() {
    api.teamWeek({
      weekStart: filters.weekStart,
      userId: filters.userId,
      projectId: filters.projectId
    }).then(setWeekData);
  }

  useEffect(() => {
    loadReports();
    loadWeek();
  }, [filters]);

  async function deleteReport(report) {
    setError('');
    try {
      await api.deleteReport(report.id);
      loadReports();
      loadWeek();
    } catch (err) {
      setError(err.message);
    }
  }

  const trackedStatuses = (weekData?.memberStatuses || []).filter((item) =>
    !filters.trackerStatus || item.status === filters.trackerStatus);
  // Draft and not started values are tracker only, the backend withholds draft ids and contents
  const sectionItems = sectionView === 'blockers'
    ? weekData?.blockersByMember || []
    : weekData?.achievementsByMember || [];
  const submittedCount = trackedStatuses.filter((item) => item.status === 'SUBMITTED').length;
  const needsCorrectionCount = trackedStatuses.filter((item) => item.status === 'NEEDS_CORRECTION').length;
  const approvedCount = trackedStatuses.filter((item) => item.status === 'APPROVED').length;
  const notStartedCount = trackedStatuses.filter((item) => item.status === 'NOT_STARTED').length;

  return (
    <section className="team-reports-page">
      <div className="page-header team-reports-header">
        <div>
          <p className="eyebrow">Manager</p>
          <h1>Team reports</h1>
        </div>
        <label className="team-week-picker">Selected week
          <input type="date" value={filters.weekStart} onChange={(event) => updateFilters({ weekStart: event.target.value })} />
        </label>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="team-report-summary">
        <SummaryTile label="Tracked members" value={trackedStatuses.length} />
        <SummaryTile label="Submitted" value={submittedCount} />
        <SummaryTile label="Needs correction" value={needsCorrectionCount} />
        <SummaryTile label="Approved" value={approvedCount} />
        <SummaryTile label="Not started" value={notStartedCount} />
      </div>

      <section className="panel team-filter-panel">
        <div className="section-toolbar">
          <h2>Filters</h2>
          <button className="secondary-button" onClick={() => setFilters({ weekStart: mondayToday() })}>Reset</button>
        </div>
        <div className="team-filters">
          <label>Team member
            <select value={filters.userId || ''} onChange={(event) => updateFilters({ userId: event.target.value })}>
              <option value="">All members</option>
              {users.filter((user) => user.role === 'TEAM_MEMBER').map((user) => <option key={user.id} value={user.id}>{user.name}</option>)}
            </select>
          </label>
          <label>Project
            <select value={filters.projectId || ''} onChange={(event) => updateFilters({ projectId: event.target.value })}>
              <option value="">All projects</option>
              {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
            </select>
          </label>
          <label>Report status
            <select value={filters.status || ''} onChange={(event) => updateFilters({ status: event.target.value })}>
              <option value="">All statuses</option>
              {reportStatuses.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <label>Tracker status
            <select value={filters.trackerStatus || ''} onChange={(event) => updateFilters({ trackerStatus: event.target.value })}>
              <option value="">All tracked statuses</option>
              {trackerStatuses.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <label>From
            <input type="date" value={filters.from || ''} onChange={(event) => updateFilters({ from: event.target.value })} />
          </label>
          <label>To
            <input type="date" value={filters.to || ''} onChange={(event) => updateFilters({ to: event.target.value })} />
          </label>
        </div>
      </section>

      <section className="panel team-panel">
        <div className="section-toolbar">
          <h2>Selected Week Tracker</h2>
          <span className="muted">{trackedStatuses.length} members</span>
        </div>
        <div className="tracker-grid team-tracker-grid">
          {trackedStatuses.length ? trackedStatuses.map((item) => (
            <div className="tracker-item team-tracker-item" key={item.userId}>
              <div>
                <strong>{item.userName}</strong>
                {item.projectName && <small>{item.projectName}</small>}
                {item.latestReviewComment && <p>{item.latestReviewComment}</p>}
              </div>
              <StatusBadge status={item.status} />
              {item.canOpen && <Link to={`/reports/${item.reportId}`}>Open report</Link>}
            </div>
          )) : <p className="muted">No team members match these filters.</p>}
        </div>
      </section>

      <section className="panel team-panel">
        <div className="section-toolbar">
          <h2>{sectionView === 'blockers' ? 'Blockers By Member' : 'Achievements By Member'}</h2>
          <div className="segmented small">
            <button className={sectionView === 'blockers' ? 'active' : ''} onClick={() => setSectionView('blockers')}>Blockers</button>
            <button className={sectionView === 'achievements' ? 'active' : ''} onClick={() => setSectionView('achievements')}>Achievements</button>
          </div>
        </div>
        <div className="team-section-grid">
          {sectionItems.length ? sectionItems.map((entry, index) => (
            <div className="team-section-card" key={index}>
              <div>
                <strong>{entry.member}</strong>
                <small>{entry.project}</small>
              </div>
              {entry.items.length ? <ul>{entry.items.map((item, itemIndex) => <li key={itemIndex}>{item}</li>)}</ul> : <p>None recorded.</p>}
              <Link to={`/reports/${entry.reportId}`}>Open report</Link>
            </div>
          )) : <p className="muted">No visible submitted report sections for this week.</p>}
        </div>
      </section>

      <section className="team-table-section">
        <div className="section-toolbar">
          <h2>Reports</h2>
          <span className="muted">{reports.length} visible reports</span>
        </div>
        <div className="table-wrap">
          <table className="team-reports-table">
            <thead><tr><th>Member</th><th>Week</th><th>Project</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {reports.length ? reports.map((report) => (
                <tr key={report.id}>
                  <td><Link to={`/members/${report.userId}`}>{report.userName}</Link></td>
                  <td>{report.weekStart} to {report.weekEnd}</td>
                  <td>{report.projectName}</td>
                  <td><StatusBadge status={report.status} /></td>
                  <td className="row-actions">
                    <Link to={`/reports/${report.id}`}>View</Link>
                    {report.status === 'SUBMITTED' && <Link to={`/review/${report.id}`}>Review</Link>}
                    {auth.user?.role === 'MANAGER' && report.status === 'SUBMITTED' && (
                      <DeleteIconButton
                        label={`Delete ${report.userName}'s report`}
                        warning={`Delete ${report.userName}'s submitted report for ${report.weekStart}?`}
                        onConfirm={() => deleteReport(report)}
                      />
                    )}
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan="5" className="empty-table-cell">No reports match these filters.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function SummaryTile({ label, value }) {
  return (
    <div className="team-summary-tile">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
