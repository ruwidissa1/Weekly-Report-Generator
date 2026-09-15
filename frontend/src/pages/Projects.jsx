import { useEffect, useState } from 'react';
import { api } from '../api.js';
import DeleteIconButton from '../components/DeleteIconButton.jsx';

export default function Projects() {
  const [projects, setProjects] = useState([]);
  const [form, setForm] = useState({ name: '', description: '' });
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');

  function load() {
    api.projects().then(setProjects);
  }

  useEffect(load, []);

  async function save(event) {
    event.preventDefault();
    setError('');
    if (editingId) {
      await api.updateProject(editingId, form);
    } else {
      await api.createProject(form);
    }
    setForm({ name: '', description: '' });
    setEditingId(null);
    load();
  }

  async function deleteProject(project) {
    setError('');
    try {
      await api.deleteProject(project.id);
      if (editingId === project.id) {
        setEditingId(null);
        setForm({ name: '', description: '' });
      }
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section>
      <div className="page-header">
        <div><p className="eyebrow">Manager</p><h1>Projects and categories</h1></div>
      </div>
      {error && <p className="error">{error}</p>}
      <form className="panel form-grid" onSubmit={save}>
        <label>Name<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
        <label>Description<input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
        <button className="primary-button" type="submit">{editingId ? 'Update project' : 'Add project'}</button>
      </form>
      <div className="table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Description</th><th>Edit / Delete</th></tr></thead>
          <tbody>
            {projects.map((project) => (
              <tr key={project.id}>
                <td>{project.name}</td>
                <td>{project.description}</td>
                <td className="row-actions">
                  <button onClick={() => { setEditingId(project.id); setForm({ name: project.name, description: project.description }); }}>Edit</button>
                  <DeleteIconButton
                    label={`Delete ${project.name}`}
                    warning={`Delete ${project.name}? Existing reports will keep their saved project name.`}
                    onConfirm={() => deleteProject(project)}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
