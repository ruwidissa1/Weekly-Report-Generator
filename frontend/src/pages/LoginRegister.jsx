import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../state/AuthContext.jsx';

export default function LoginRegister() {
  const auth = useAuth();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');

  if (auth.token) return <Navigate to="/" replace />;

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      if (mode === 'login') {
        await auth.login({ email: form.email, password: form.password });
      } else {
        await auth.register({ name: form.name, email: form.email, password: form.password });
      }
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-panel">
        <div>
          <h1 className="eyebrow">Weekly Report Generator</h1>
          <h2>Team reporting with reviewing.</h2>
        </div>
        <div className="segmented">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Login</button>
          <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button>
        </div>
        <form onSubmit={submit} className="form-grid">
          {mode === 'register' && (
            <label>Name<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
          )}
          <label>Email<input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
          <label>Password<input required minLength={8} type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
          {error && <p className="error">{error}</p>}
          <button className="primary-button" type="submit">{mode === 'login' ? 'Login' : 'Create account'}</button>
        </form>
      </section>
    </main>
  );
}
