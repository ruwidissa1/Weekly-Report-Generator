import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import DeleteIconButton from '../components/DeleteIconButton.jsx';
import { useAuth } from '../state/AuthContext.jsx';

export default function Users() {
  const auth = useAuth();
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const showDeleteColumn = auth.isAdmin;

  function load() {
    api.users().then(setUsers);
  }

  useEffect(load, []);

  async function update(user, patch) {
    await api.updateUser(user.id, patch);
    load();
  }

  async function deleteUser(user) {
    setError('');
    try {
      await api.deleteUser(user.id);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section>
      <div className="page-header">
        <div><p className="eyebrow">{auth.isAdmin ? 'Admin' : 'Manager'}</p><h1>User management</h1></div>
      </div>
      {error && <p className="error">{error}</p>}
      <div className="table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Role</th>{showDeleteColumn && <th>Delete</th>}</tr></thead>
          <tbody>
            {users.map((user) => {
              const isSelf = auth.user?.id === user.id;
              const canEdit = auth.isAdmin && !isSelf;
              return (
                <tr key={user.id}>
                  <td><Link to={`/members/${user.id}`}>{user.name}</Link></td>
                  <td>{user.email}</td>
                  <td className="row-actions">
                    {canEdit ? (
                      <select value={user.role} onChange={(e) => update(user, { role: e.target.value })}>
                        <option value="TEAM_MEMBER">Team member</option>
                        <option value="MANAGER">Manager</option>
                        <option value="ADMIN">Admin</option>
                      </select>
                    ) : user.role}
                  </td>
                  {showDeleteColumn && (
                    <td>
                      {isSelf ? (
                        <span className="muted">Locked</span>
                      ) : (
                        <DeleteIconButton
                          label={`Delete ${user.name}`}
                          warning={`Delete ${user.name}? This will also remove reports owned by this user.`}
                          onConfirm={() => deleteUser(user)}
                        />
                      )}
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
