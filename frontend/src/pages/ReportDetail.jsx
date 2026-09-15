import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import StatusBadge from '../components/StatusBadge.jsx';
import { useAuth } from '../state/AuthContext.jsx';

export default function ReportDetail() {
  const { id } = useParams();
  const auth = useAuth();
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setError('');
    api.report(id)
      .then(setReport)
      .catch((err) => setError(err.message));
  }, [id]);

  if (error) {
    return (
      <section>
        <div className="page-header">
          <div>
            <p className="eyebrow">Report unavailable</p>
            <h1>Cannot open this report</h1>
          </div>
        </div>
        <div className="notice">{error}</div>
        <p>You can only open reports allowed for your role.</p>
        <Link className="secondary-button" to={auth.isManager ? '/team-reports' : '/history'}>Back to reports</Link>
      </section>
    );
  }

  if (!report) return <p>Loading...</p>;

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="eyebrow">{report.userName} - {report.projectName}</p>
          <h1>{report.weekStart} to {report.weekEnd}</h1>
        </div>
        <StatusBadge status={report.status} />
      </div>
      {report.latestReviewComment && <div className="notice">{report.latestReviewComment}</div>}
      <div className="action-bar">
        {['DRAFT', 'NEEDS_CORRECTION'].includes(report.status) && report.userId === auth.user?.id && <Link className="secondary-button" to={`/reports/${report.id}/edit`}>Edit</Link>}
        {auth.isManager && report.status === 'SUBMITTED' && <Link className="primary-button" to={`/review/${report.id}`}>Review</Link>}
      </div>
      <ReportContent report={report} title={`Current content - v${report.currentVersion || 0}`} />
      <section className="panel">
        <h2>Review history</h2>
        {report.reviewComments?.length ? report.reviewComments.map((comment) => (
          <div className="timeline-item" key={comment.id}>
            <strong>{comment.resultingStatus}</strong> by {comment.reviewerName} against v{comment.versionNumber}
            <span> - {new Date(comment.createdAt).toLocaleString()}</span>
            <p>{comment.comment}</p>
          </div>
        )) : <p>No manager review comments yet.</p>}
      </section>
      <section className="panel">
        <h2>Submitted versions</h2>
        {report.versions?.length ? report.versions.map((version) => (
          <VersionSnapshot key={version.id} version={version} comments={report.reviewComments || []} />
        )) : <p>No submitted versions yet.</p>}
      </section>
    </section>
  );
}

function VersionSnapshot({ version, comments }) {
  const snapshot = parseSnapshot(version.snapshotJson);
  const versionComments = comments.filter((comment) => comment.versionNumber === version.versionNumber);

  return (
    <details>
      <summary>Version {version.versionNumber} - submitted {new Date(version.submittedAt).toLocaleString()}</summary>
      {snapshot ? <ReportContent report={snapshot} title={`Snapshot v${version.versionNumber}`} compact /> : <p>Snapshot could not be loaded.</p>}
      <div className="version-comments">
        <h3>Comments on v{version.versionNumber}</h3>
        {versionComments.length ? versionComments.map((comment) => (
          <p key={comment.id}><strong>{comment.resultingStatus}</strong>: {comment.comment}</p>
        )) : <p>No comments recorded against this version.</p>}
      </div>
    </details>
  );
}

function ReportContent({ report, title, compact = false }) {
  const tasks = report.tasksCompleted || [];
  const planned = report.tasksPlannedNextWeek || [];
  const blockers = report.blockers || [];
  const achievements = report.achievements || [];
  const hours = report.hours || [];

  if (compact) {
    return (
      <div className="version-body">
        <h3>{title}</h3>
        {report.notes && <p>{report.notes}</p>}
        <div className="table-wrap">
          <table>
            <thead><tr><th>Task</th><th>Priority</th><th>Plan</th><th>Actual</th><th>Status</th><th>Hours</th><th>Deliverable</th></tr></thead>
            <tbody>{tasks.map((task, index) => (
              <tr key={task.id || `${task.name}-${index}`}>
                <td>{task.name}</td>
                <td>{task.priority}</td>
                <td>{task.plannedPercent}%</td>
                <td>{task.actualPercent}%</td>
                <td>{task.status}</td>
                <td>{task.timeSpentHours}</td>
                <td>{task.deliverable}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <div className="version-grid">
          <MiniSection title="Next week" items={planned} />
          <MiniSection title="Blockers" items={blockers.map((b) => `${b.description}${b.keyIssue ? ' (key)' : ''}`)} />
          <MiniSection title="Achievements" items={achievements.map((a) => `${a.description}${a.keyAchievement ? ' (key)' : ''}`)} />
          <MiniSection title="Hours" items={hours.map((h) => `${h.taskType}: ${h.hours}h`)} />
        </div>
      </div>
    );
  }

  return (
    <>
      <section className="panel">
        <h2>{title}</h2>
        {report.notes && <p>{report.notes}</p>}
        <div className="table-wrap">
          <table>
            <thead><tr><th>Task</th><th>Priority</th><th>Plan</th><th>Actual</th><th>Status</th><th>Hours</th><th>Deliverable</th></tr></thead>
            <tbody>{tasks.map((task, index) => (
              <tr key={task.id || `${task.name}-${index}`}>
                <td>{task.name}</td>
                <td>{task.priority}</td>
                <td>{task.plannedPercent}%</td>
                <td>{task.actualPercent}%</td>
                <td>{task.status}</td>
                <td>{task.timeSpentHours}</td>
                <td>{task.deliverable}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </section>
      {!compact && (
        <div className="two-column">
          <Section title="Next week" items={planned} />
          <Section title="Blockers" items={blockers.map((b) => `${b.description}${b.keyIssue ? ' (key)' : ''}`)} />
          <Section title="Achievements" items={achievements.map((a) => `${a.description}${a.keyAchievement ? ' (key)' : ''}`)} />
          <Section title="Hours" items={hours.map((h) => `${h.taskType}: ${h.hours}h`)} />
        </div>
      )}
    </>
  );
}

function Section({ title, items }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      {items.length ? <ul>{items.map((item, index) => <li key={index}>{item}</li>)}</ul> : <p>None recorded.</p>}
    </section>
  );
}

function MiniSection({ title, items }) {
  return (
    <div className="mini-section">
      <h3>{title}</h3>
      {items.length ? <ul>{items.map((item, index) => <li key={index}>{item}</li>)}</ul> : <p>None recorded.</p>}
    </div>
  );
}

function parseSnapshot(value) {
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}
