import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './state/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import LoginRegister from './pages/LoginRegister.jsx';
import Dashboard from './pages/Dashboard.jsx';
import ReportEditor from './pages/ReportEditor.jsx';
import ReportHistory from './pages/ReportHistory.jsx';
import ReportDetail from './pages/ReportDetail.jsx';
import ManagerReview from './pages/ManagerReview.jsx';
import Projects from './pages/Projects.jsx';
import Users from './pages/Users.jsx';
import MemberProfile from './pages/MemberProfile.jsx';
import Reports from './pages/Reports.jsx';

function Protected({ children, managerOnly = false, adminOnly = false }) {
  const auth = useAuth();
  if (!auth.token) return <Navigate to="/login" replace />;
  if (adminOnly && !auth.isAdmin) return <Navigate to="/" replace />;
  if (managerOnly && !auth.isManager) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginRegister />} />
      <Route path="/" element={<Protected><Layout /></Protected>}>
        <Route index element={<ReportHistory />} />
        <Route path="reports/new" element={<ReportEditor />} />
        <Route path="reports/:id/edit" element={<ReportEditor />} />
        <Route path="reports/:id" element={<ReportDetail />} />
        <Route path="history" element={<ReportHistory />} />
        <Route path="dashboard" element={<Protected managerOnly><Dashboard /></Protected>} />
        <Route path="team-reports" element={<Protected managerOnly><Reports /></Protected>} />
        <Route path="review/:id" element={<Protected managerOnly><ManagerReview /></Protected>} />
        <Route path="projects" element={<Protected managerOnly><Projects /></Protected>} />
        <Route path="users" element={<Protected managerOnly><Users /></Protected>} />
        <Route path="members/:id" element={<Protected managerOnly><MemberProfile /></Protected>} />
      </Route>
    </Routes>
  );
}
