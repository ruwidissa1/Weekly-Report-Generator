import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api.js';
import StatusBadge from '../components/StatusBadge.jsx';

const colors = ['#2563eb', '#059669', '#d97706', '#dc2626', '#7c3aed', '#0f766e'];
const statusColors = {
  SUBMITTED: '#2563eb',
  NEEDS_CORRECTION: '#dc2626',
  APPROVED: '#059669',
  PENDING: '#d97706',
  LATE: '#c2410c'
};
const chartMargin = { top: 10, right: 16, bottom: 10, left: 4 };

function mondayToday() {
  const date = new Date();
  const day = date.getDay() || 7;
  date.setDate(date.getDate() - day + 1);
  return date.toISOString().slice(0, 10);
}

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [weekStart, setWeekStart] = useState(mondayToday());

  useEffect(() => {
    setData(null);
    api.dashboard(weekStart).then(setData);
  }, [weekStart]);

  if (!data) {
    return (
      <section className="dashboard-page">
        <DashboardHeader weekStart={weekStart} setWeekStart={setWeekStart} />
        <div className="dashboard-loading">Loading dashboard...</div>
      </section>
    );
  }

  const complianceData = [
    { name: 'Submitted', value: data.submittedThisWeek, color: statusColors.SUBMITTED },
    { name: 'Pending', value: data.pendingThisWeek, color: statusColors.PENDING },
    { name: 'Late', value: data.lateThisWeek, color: statusColors.LATE }
  ].filter((item) => item.value > 0);

  // Recharts stacks one numeric field per status, so each member is mapped to a single active bar segment.
  const statusByMemberChart = data.statusByMember.map((item) => ({
    member: item.member,
    Submitted: item.status === 'SUBMITTED' ? 1 : 0,
    'Needs correction': item.status === 'NEEDS_CORRECTION' ? 1 : 0,
    Approved: item.status === 'APPROVED' ? 1 : 0,
    Pending: item.status === 'PENDING' ? 1 : 0,
    Late: item.status === 'LATE' ? 1 : 0
  }));

  return (
    <section className="dashboard-page">
      <DashboardHeader weekStart={weekStart} setWeekStart={setWeekStart} />

      <div className="dashboard-metrics">
        <Metric label="Submitted this week" value={data.totalReportsThisWeek} tone="blue" />
        <Metric label="Compliance rate" value={`${data.complianceRate}%`} tone="green" />
        <Metric label="Pending" value={data.pendingThisWeek} tone="amber" />
        <Metric label="Late" value={data.lateThisWeek} tone="orange" />
        <Metric label="Needs correction" value={data.needsCorrection} tone="red" />
        <Metric label="Open blockers" value={data.openBlockers} tone="teal" />
      </div>

      <div className="dashboard-grid">
        <ChartPanel title="Submission Compliance">
          {complianceData.length ? (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={complianceData} dataKey="value" nameKey="name" innerRadius={48} outerRadius={86} paddingAngle={3}>
                  {complianceData.map((item) => <Cell key={item.name} fill={item.color} />)}
                </Pie>
                <Tooltip />
                <Legend verticalAlign="bottom" height={32} />
              </PieChart>
            </ResponsiveContainer>
          ) : <EmptyChart />}
        </ChartPanel>

        <ChartPanel title="Tasks Completed Trend">
          {data.tasksTrend.length ? (
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={data.tasksTrend} margin={chartMargin}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="week" tick={{ fontSize: 11 }} tickMargin={10} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} width={34} />
                <Tooltip />
                <Line type="monotone" dataKey="tasks" stroke="#2563eb" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
              </LineChart>
            </ResponsiveContainer>
          ) : <EmptyChart />}
        </ChartPanel>

        <ChartPanel title="Status By Team Member" className="dashboard-wide">
          {statusByMemberChart.length ? (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={statusByMemberChart} layout="vertical" margin={{ top: 10, right: 24, bottom: 12, left: 22 }}>
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" allowDecimals={false} hide />
                <YAxis type="category" dataKey="member" width={130} tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend verticalAlign="bottom" height={32} />
                <Bar dataKey="Submitted" stackId="status" fill={statusColors.SUBMITTED} />
                <Bar dataKey="Needs correction" stackId="status" fill={statusColors.NEEDS_CORRECTION} />
                <Bar dataKey="Approved" stackId="status" fill={statusColors.APPROVED} />
                <Bar dataKey="Pending" stackId="status" fill={statusColors.PENDING} />
                <Bar dataKey="Late" stackId="status" fill={statusColors.LATE} />
              </BarChart>
            </ResponsiveContainer>
          ) : <EmptyChart />}
          <div className="dashboard-status-list">
            {data.statusByMember.map((item, index) => (
              item.reportId
                ? <Link key={index} to={`/reports/${item.reportId}`}><span>{item.member}</span><StatusBadge status={item.status} /></Link>
                : <div className="status-row" key={index}><span>{item.member}</span><StatusBadge status={item.status} /></div>
            ))}
          </div>
        </ChartPanel>

        <ChartPanel title="Workload By Project">
          {data.workloadByProject.length ? (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data.workloadByProject} margin={{ top: 10, right: 14, bottom: 18, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="project" tick={{ fontSize: 11 }} tickMargin={10} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} width={34} />
                <Tooltip />
                <Bar dataKey="tasks" fill="#059669" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : <EmptyChart />}
        </ChartPanel>

        <ChartPanel title="Time By Task Type">
          {data.timeByTaskType.length ? (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={data.timeByTaskType} dataKey="hours" nameKey="type" innerRadius={48} outerRadius={86} paddingAngle={3}>
                  {data.timeByTaskType.map((_, index) => <Cell key={index} fill={colors[index % colors.length]} />)}
                </Pie>
                <Tooltip />
                <Legend verticalAlign="bottom" height={32} />
              </PieChart>
            </ResponsiveContainer>
          ) : <EmptyChart />}
        </ChartPanel>
      </div>

      <div className="dashboard-two-column">
        <SectionCompare title="Blockers Across Team" data={data.blockersByMember} emptyText="No blockers recorded this week." />
        <SectionCompare title="Achievements Across Team" data={data.achievementsByMember} emptyText="No achievements recorded this week." />
      </div>

      <section className="dashboard-activity">
        <h2>Recent Activity</h2>
        <div className="dashboard-activity-grid">
          <div>
            <h3>Recent Reports</h3>
            <div className="table-wrap dashboard-table-wrap">
              <table className="dashboard-table">
                <thead><tr><th>Member</th><th>Week</th><th>Project</th><th>Status</th><th></th></tr></thead>
                <tbody>
                  {data.recentActivity.map((report) => (
                    <tr key={report.id}>
                      <td>{report.userName}</td>
                      <td>{report.weekStart}</td>
                      <td>{report.projectName}</td>
                      <td><StatusBadge status={report.status} /></td>
                      <td><Link to={`/reports/${report.id}`}>Open</Link></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div>
            <h3>Recent Review Actions</h3>
            <div className="review-action-list">
              {data.recentReviewActions.length ? data.recentReviewActions.map((action, index) => (
                <Link key={index} to={`/reports/${action.reportId}`}>
                  <span><strong>{action.member}</strong><small>{action.project}</small></span>
                  <StatusBadge status={action.action} />
                  <small>v{action.versionNumber} by {action.reviewer} on {new Date(action.createdAt).toLocaleString()}</small>
                  <p>{action.comment}</p>
                </Link>
              )) : <p className="muted">No recent review actions.</p>}
            </div>
          </div>
        </div>
      </section>
    </section>
  );
}

function DashboardHeader({ weekStart, setWeekStart }) {
  return (
    <div className="page-header dashboard-header">
      <div>
        <p className="eyebrow">Manager dashboard</p>
        <h1>Team reporting overview</h1>
      </div>
      <label className="dashboard-week-picker">Week starting
        <input type="date" value={weekStart} onChange={(event) => setWeekStart(event.target.value)} />
      </label>
    </div>
  );
}

function Metric({ label, value, tone }) {
  return <div className={`metric dashboard-metric ${tone}`}><span>{label}</span><strong>{value}</strong></div>;
}

function ChartPanel({ title, children, className = '' }) {
  return <section className={`panel chart-panel dashboard-chart-panel ${className}`}><h2>{title}</h2>{children}</section>;
}

function EmptyChart() {
  return <div className="empty-chart">No report data for this view.</div>;
}

function SectionCompare({ title, data, emptyText }) {
  return (
    <section className="panel dashboard-compare-panel">
      <h2>{title}</h2>
      <div className="compare-list">
        {data.length ? data.map((entry, index) => (
          <div key={index}>
            <strong>{entry.member}</strong>
            <small>{entry.project}</small>
            {entry.items.length ? <ul>{entry.items.map((item, itemIndex) => <li key={itemIndex}>{item}</li>)}</ul> : <p>None recorded.</p>}
          </div>
        )) : <p className="muted">{emptyText}</p>}
      </div>
    </section>
  );
}
