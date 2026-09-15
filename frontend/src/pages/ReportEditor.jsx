import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Plus, Save, Send } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import StatusBadge from '../components/StatusBadge.jsx';
import { useAuth } from '../state/AuthContext.jsx';

const emptyTask = { name: '', priority: 'MEDIUM', plannedPercent: 100, actualPercent: 0, status: 'IN_PROGRESS', timePlannedHours: 0, timeSpentHours: 0, deliverable: '' };
const emptyIssue = { description: '', keyIssue: false };
const emptyAchievement = { description: '', keyAchievement: false };
const emptyHours = { taskType: 'Development', hours: 0 };

function mondayToday() {
  const date = new Date();
  const day = date.getDay() || 7;
  date.setDate(date.getDate() - day + 1);
  return date.toISOString().slice(0, 10);
}

function plusSix(dateValue) {
  const date = new Date(`${dateValue}T00:00:00`);
  date.setDate(date.getDate() + 6);
  return date.toISOString().slice(0, 10);
}

export default function ReportEditor() {
  const { id } = useParams();
  const navigate = useNavigate();
  const auth = useAuth();
  const [projects, setProjects] = useState([]);
  const [error, setError] = useState('');
  const [blocked, setBlocked] = useState(false);
  const [reportStatus, setReportStatus] = useState(null);
  const [form, setForm] = useState({
    weekStart: mondayToday(),
    weekEnd: plusSix(mondayToday()),
    projectId: '',
    tasksCompleted: [{ ...emptyTask }],
    tasksPlannedNextWeek: [''],
    blockers: [{ ...emptyIssue }],
    achievements: [{ ...emptyAchievement }],
    hours: [{ ...emptyHours }],
    notes: ''
  });

  const title = useMemo(() => id ? 'Edit weekly report' : 'Create weekly report', [id]);

  useEffect(() => {
    api.projects().then((items) => {
      setProjects(items);
      setForm((current) => ({ ...current, projectId: current.projectId || items[0]?.id || '' }));
    });
    if (id) {
      api.report(id)
        .then((report) => {
          // Managers can review metadata elsewhere, but report content edits stay with the owner.
          if (report.userId !== auth.user?.id) {
            setBlocked(true);
            setError('Only the report owner can edit report content.');
            return;
          }
          setReportStatus(report.status);
          setForm({
            weekStart: report.weekStart,
            weekEnd: report.weekEnd,
            projectId: report.projectId,
            tasksCompleted: report.tasksCompleted.length ? report.tasksCompleted : [{ ...emptyTask }],
            tasksPlannedNextWeek: report.tasksPlannedNextWeek.length ? report.tasksPlannedNextWeek : [''],
            blockers: report.blockers.length ? report.blockers : [{ ...emptyIssue }],
            achievements: report.achievements.length ? report.achievements : [{ ...emptyAchievement }],
            hours: report.hours.length ? report.hours : [{ ...emptyHours }],
            notes: report.notes || ''
          });
        })
        .catch((err) => {
          setBlocked(true);
          setError(err.message);
        });
    }
  }, [auth.user?.id, id]);

  function updateList(key, index, patch) {
    setForm((current) => ({
      ...current,
      [key]: current[key].map((item, i) => i === index ? { ...item, ...patch } : item)
    }));
  }

  function addItem(key, item) {
    setForm((current) => ({ ...current, [key]: [...current[key], item] }));
  }

  async function save(submitAfter = false) {
    setError('');
    try {
      const saved = id ? await api.updateReport(id, form) : await api.createReport(form);
      // Submit is a second backend step so drafts and resubmissions share the same editor path.
      if (submitAfter) {
        await api.submitReport(saved.id);
      }
      navigate('/history');
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="report-editor-page">
      <div className="page-header report-editor-header">
        <div>
          <p className="eyebrow">Fixed weekly template</p>
          <h1>{title}</h1>
        </div>
        <div className="report-editor-header-actions">
          {reportStatus && <StatusBadge status={reportStatus} />}
          <Link className="secondary-button" to={auth.isManager ? '/team-reports' : '/history'}><ArrowLeft size={17} /> Back</Link>
        </div>
      </div>
      {blocked && (
        <section className="panel report-editor-blocked">
          <div className="notice">{error}</div>
          <Link className="secondary-button" to={auth.isManager ? '/team-reports' : '/history'}><ArrowLeft size={17} /> Back to reports</Link>
        </section>
      )}
      {!blocked && (
        <>
          {reportStatus === 'NEEDS_CORRECTION' && <div className="notice">This report was returned for correction. Update the same report and resubmit it.</div>}
          {error && <p className="error">{error}</p>}

          <section className="panel report-details-panel">
            <h2>Report Details</h2>
            <div className="editor-grid">
              <label>Week start
                <input type="date" value={form.weekStart} onChange={(e) => setForm({ ...form, weekStart: e.target.value, weekEnd: plusSix(e.target.value) })} />
              </label>
              <label>Week end
                <input type="date" value={form.weekEnd} onChange={(e) => setForm({ ...form, weekEnd: e.target.value })} />
              </label>
              <label>Project/category
                <select value={form.projectId} onChange={(e) => setForm({ ...form, projectId: Number(e.target.value) })}>
                  {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
                </select>
              </label>
            </div>
          </section>

          <Panel title="Tasks Completed">
            <div className="task-table editor-task-table">
              <div className="task-head"><span>Task</span><span>Priority</span><span>Plan %</span><span>Actual %</span><span>Status</span><span>Hours</span><span>Deliverable</span></div>
              {form.tasksCompleted.map((task, index) => (
                <div className="task-row editor-task-row" key={index}>
                  <input aria-label="Task" value={task.name} onChange={(e) => updateList('tasksCompleted', index, { name: e.target.value })} />
                  <select aria-label="Priority" value={task.priority} onChange={(e) => updateList('tasksCompleted', index, { priority: e.target.value })}>
                    <option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option>
                  </select>
                  <input aria-label="Planned percent" type="number" min="0" max="100" value={task.plannedPercent ?? 0} onChange={(e) => updateList('tasksCompleted', index, { plannedPercent: Number(e.target.value) })} />
                  <input aria-label="Actual percent" type="number" min="0" max="100" value={task.actualPercent ?? 0} onChange={(e) => updateList('tasksCompleted', index, { actualPercent: Number(e.target.value) })} />
                  <select aria-label="Status" value={task.status} onChange={(e) => updateList('tasksCompleted', index, { status: e.target.value })}>
                    <option>NOT_STARTED</option><option>IN_PROGRESS</option><option>COMPLETED</option><option>BLOCKED</option>
                  </select>
                  <input aria-label="Hours" type="number" min="0" value={task.timeSpentHours ?? 0} onChange={(e) => updateList('tasksCompleted', index, { timeSpentHours: Number(e.target.value) })} />
                  <input aria-label="Deliverable" value={task.deliverable || ''} onChange={(e) => updateList('tasksCompleted', index, { deliverable: e.target.value })} />
                </div>
              ))}
            </div>
            <button className="secondary-button editor-add-button" type="button" onClick={() => addItem('tasksCompleted', { ...emptyTask })}><Plus size={17} /> Add task</button>
          </Panel>

          <Panel title="Next Week Plan">
            <div className="editor-list">
              {form.tasksPlannedNextWeek.map((task, index) => (
                <input key={index} value={task} onChange={(e) => setForm({ ...form, tasksPlannedNextWeek: form.tasksPlannedNextWeek.map((item, i) => i === index ? e.target.value : item) })} />
              ))}
            </div>
            <button className="secondary-button editor-add-button" type="button" onClick={() => addItem('tasksPlannedNextWeek', '')}><Plus size={17} /> Add planned task</button>
          </Panel>

          <Panel title="Blockers And Achievements">
            <div className="two-column editor-two-column">
              <div className="editor-subsection">
                <h3>Blockers</h3>
                {form.blockers.map((item, index) => (
                  <label className="inline-check editor-check-row" key={index}>
                    <input value={item.description} onChange={(e) => updateList('blockers', index, { description: e.target.value })} />
                    <span><input type="checkbox" checked={item.keyIssue} onChange={(e) => updateList('blockers', index, { keyIssue: e.target.checked })} /> Key</span>
                  </label>
                ))}
                <button className="secondary-button editor-add-button" type="button" onClick={() => addItem('blockers', { ...emptyIssue })}><Plus size={17} /> Add blocker</button>
              </div>
              <div className="editor-subsection">
                <h3>Achievements</h3>
                {form.achievements.map((item, index) => (
                  <label className="inline-check editor-check-row" key={index}>
                    <input value={item.description} onChange={(e) => updateList('achievements', index, { description: e.target.value })} />
                    <span><input type="checkbox" checked={item.keyAchievement} onChange={(e) => updateList('achievements', index, { keyAchievement: e.target.checked })} /> Key</span>
                  </label>
                ))}
                <button className="secondary-button editor-add-button" type="button" onClick={() => addItem('achievements', { ...emptyAchievement })}><Plus size={17} /> Add achievement</button>
              </div>
            </div>
          </Panel>

          <Panel title="Hours By Task Type">
            <div className="hours-grid editor-hours-grid">
              {form.hours.map((item, index) => (
                <div key={index}>
                  <input value={item.taskType} onChange={(e) => updateList('hours', index, { taskType: e.target.value })} />
                  <input type="number" min="0" value={item.hours ?? 0} onChange={(e) => updateList('hours', index, { hours: Number(e.target.value) })} />
                </div>
              ))}
            </div>
            <button className="secondary-button editor-add-button" type="button" onClick={() => addItem('hours', { ...emptyHours })}><Plus size={17} /> Add hours row</button>
          </Panel>

          <Panel title="Notes And Links">
            <textarea className="editor-notes" rows="5" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </Panel>

          <div className="action-bar report-editor-action-bar">
            <button className="secondary-button" type="button" onClick={() => save(false)}><Save size={17} /> Save draft</button>
            <button className="primary-button" type="button" onClick={() => save(true)}><Send size={17} /> Submit for review</button>
          </div>
        </>
      )}
    </section>
  );
}

function Panel({ title, children }) {
  return <section className="panel report-editor-panel"><h2>{title}</h2>{children}</section>;
}
