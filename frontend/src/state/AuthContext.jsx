import { createContext, useContext, useMemo, useState } from 'react';
import { api } from '../api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('wrg_token'));
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('wrg_user');
    return raw ? JSON.parse(raw) : null;
  });

  async function login(payload) {
    const data = await api.login(payload);
    localStorage.setItem('wrg_token', data.token);
    localStorage.setItem('wrg_user', JSON.stringify(data.user));
    setToken(data.token);
    setUser(data.user);
  }

  async function register(payload) {
    const data = await api.register(payload);
    localStorage.setItem('wrg_token', data.token);
    localStorage.setItem('wrg_user', JSON.stringify(data.user));
    setToken(data.token);
    setUser(data.user);
  }

  async function logout() {
    try {
      if (token) await api.logout();
    } finally {
      localStorage.removeItem('wrg_token');
      localStorage.removeItem('wrg_user');
      setToken(null);
      setUser(null);
    }
  }

  const value = useMemo(() => ({
    token,
    user,
    login,
    register,
    logout,
    isManager: user?.role === 'MANAGER' || user?.role === 'ADMIN',
    isAdmin: user?.role === 'ADMIN'
  }), [token, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
