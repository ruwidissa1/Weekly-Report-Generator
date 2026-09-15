import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import StatusBadge from '../components/StatusBadge.jsx';

export default function MemberProfile() {
  const { id } = useParams();
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    api.profile(id).then(setProfile);
  }, [id]);

  if (!profile) return <p>Loading...</p>;

  return (
    <section>
      <div className="page-header">
        <div>
          <p className="eyebrow">Team member profile</p>
          <h1>{profile.user.name}</h1>
        </div>
      </div>
      <div className="metrics">
        <Metric label="Submitted/reviewed" value={profile.reportsSubmitted} />
        <Metric label="Approved" value={profile.reportsApproved} />
        <Metric label="Needs correction" value={profile.reportsNeedingCorrection} />
      </div>
      <section className="panel">
        <h2>Report history</h2>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Week</th><th>Project</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {profile.recentReports.map((report) => (
                <tr key={report.id}>
                  <td>{report.weekStart} to {report.weekEnd}</td>
                  <td>{report.projectName}</td>
                  <td><StatusBadge status={report.status} /></td>
                  <td>
                    {report.status === 'DRAFT'
                      ? <span className="muted">Private draft</span>
                      : <Link to={`/reports/${report.id}`}>Open</Link>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function Metric({ label, value }) {
  return <div className="metric"><span>{label}</span><strong>{value}</strong></div>;
}
