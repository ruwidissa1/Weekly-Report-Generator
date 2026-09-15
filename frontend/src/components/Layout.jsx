import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { BarChart3, ClipboardList, FolderKanban, LogOut, PenLine, Users } from 'lucide-react';
import { useAuth } from '../state/AuthContext.jsx';
import ChatAssistant from './ChatAssistant.jsx';

export default function Layout() {
  const auth = useAuth();
  const navigate = useNavigate();

  async function logout() {
    await auth.logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">WR</span>
          <div>
            <strong>Weekly Reports</strong>
            <small>{auth.user?.name}</small>
          </div>
        </div>
        <nav>
          <NavLink to="/history"><ClipboardList size={18} /> My history</NavLink>
          <NavLink to="/reports/new"><PenLine size={18} /> New report</NavLink>
          {auth.isManager && <NavLink to="/dashboard"><BarChart3 size={18} /> Dashboard</NavLink>}
          {auth.isManager && <NavLink to="/team-reports"><ClipboardList size={18} /> Team reports</NavLink>}
          {auth.isManager && <NavLink to="/projects"><FolderKanban size={18} /> Projects</NavLink>}
          {auth.isManager && <NavLink to="/users"><Users size={18} /> Users</NavLink>}
        </nav>
        <button className="ghost-button" onClick={logout}><LogOut size={17} /> Logout</button>
      </aside>
      <main className="content">
        <Outlet />
      </main>
      {auth.isManager && <ChatAssistant />}
    </div>
  );
}
