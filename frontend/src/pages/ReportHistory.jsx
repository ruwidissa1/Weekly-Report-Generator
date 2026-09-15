import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import StatusBadge from '../components/StatusBadge.jsx';
import EmptyState from '../components/EmptyState.jsx';

export default function ReportHistory() {
  const [reports, setReports] = useState([]);

  useEffect(() => {
    api.myReports().then((data) => setReports(data.content || []));
  }, []);

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="eyebrow">Personal workspace</p>
          <h1>Report history</h1>
        </div>
        <Link className="primary-button" to="/reports/new">New report</Link>
      </div>
      {!reports.length ? (
        <EmptyState title="No reports yet" action={<Link className="primary-button" to="/reports/new">Create report</Link>} />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Week</th><th>Project</th><th>Status</th><th>Version</th><th>Updated</th><th></th></tr>
            </thead>
            <tbody>
              {reports.map((report) => (
                <tr key={report.id}>
                  <td>{report.weekStart} to {report.weekEnd}</td>
                  <td>{report.projectName}</td>
                  <td><StatusBadge status={report.status} /></td>
                  <td>v{report.currentVersion}</td>
                  <td>{new Date(report.updatedAt).toLocaleString()}</td>
                  <td className="row-actions">
                    <Link to={`/reports/${report.id}`}>View</Link>
                    {['DRAFT', 'NEEDS_CORRECTION'].includes(report.status) && <Link to={`/reports/${report.id}/edit`}>Edit</Link>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
