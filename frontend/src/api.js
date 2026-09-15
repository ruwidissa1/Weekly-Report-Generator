const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

function authHeaders() {
  const token = localStorage.getItem('wrg_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message || 'Request failed');
  }

  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  login: (payload) => request('/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  register: (payload) => request('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  projects: (includeInactive = false) => request(`/projects?includeInactive=${includeInactive}`),
  createProject: (payload) => request('/projects', { method: 'POST', body: JSON.stringify(payload) }),
  updateProject: (id, payload) => request(`/projects/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteProject: (id) => request(`/projects/${id}`, { method: 'DELETE' }),
  users: () => request('/users'),
  updateUser: (id, payload) => request(`/users/${id}`, { method: 'PATCH', body: JSON.stringify(payload) }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE' }),
  profile: (id) => request(`/users/${id}/profile`),
  myReports: () => request('/reports/mine?size=50'),
  reports: (params = {}) => {
    const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value)).toString();
    return request(`/reports${query ? `?${query}` : ''}`);
  },
  report: (id) => request(`/reports/${id}`),
  teamWeek: (params = {}) => {
    const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value)).toString();
    return request(`/reports/team-week${query ? `?${query}` : ''}`);
  },
  createReport: (payload) => request('/reports', { method: 'POST', body: JSON.stringify(payload) }),
  updateReport: (id, payload) => request(`/reports/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  submitReport: (id) => request(`/reports/${id}/submit`, { method: 'POST' }),
  deleteReport: (id) => request(`/reports/${id}`, { method: 'DELETE' }),
  approveReport: (id) => request(`/reports/${id}/approve`, { method: 'POST' }),
  requestChanges: (id, comment) => request(`/reports/${id}/request-changes`, { method: 'POST', body: JSON.stringify({ comment }) }),
  dashboard: (weekStart) => request(`/dashboard${weekStart ? `?weekStart=${weekStart}` : ''}`),
  assistantChat: (payload) => request('/assistant/chat', { method: 'POST', body: JSON.stringify(payload) })
};
