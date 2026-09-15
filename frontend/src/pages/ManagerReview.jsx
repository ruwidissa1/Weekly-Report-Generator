import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import ReportDetail from './ReportDetail.jsx';

export default function ManagerReview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [comment, setComment] = useState('');
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.report(id).then(setReport);
  }, [id]);

  async function approve() {
    await api.approveReport(id);
    navigate('/team-reports');
  }

  async function requestChanges() {
    setError('');
    try {
      await api.requestChanges(id, comment);
      navigate('/team-reports');
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section>
      <div className="review-dock">
        <div>
          <strong>{report ? `Reviewing ${report.userName}'s report` : 'Review report'}</strong>
          <p>Managers can approve or return the report with one general correction comment.</p>
        </div>
        <textarea rows="2" placeholder="Correction comment" value={comment} onChange={(e) => setComment(e.target.value)} />
        {error && <p className="error">{error}</p>}
        <button className="secondary-button" onClick={requestChanges}>Request changes</button>
        <button className="primary-button" onClick={approve}>Approve</button>
      </div>
      <ReportDetail />
    </section>
  );
}
