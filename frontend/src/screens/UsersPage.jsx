import { Save } from "lucide-react";
import { useEffect, useState } from "react";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { USER_ROLES } from "../lib/options.js";
import { api } from "../lib/api.js";

export default function UsersPage() {
  const { isAdmin, currentUser, refreshCurrentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  async function loadUsers() {
    setLoading(true);
    setError(null);

    try {
      const response = await api.users.list();
      setUsers(response);
      setRoles(Object.fromEntries(response.map((user) => [user.id, user.role])));
    } catch (loadError) {
      setError(loadError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (isAdmin) {
      void loadUsers();
    }
  }, [isAdmin]);

  async function handleRoleUpdate(userId) {
    setError(null);
    setMessage(null);

    try {
      await api.users.updateRole(userId, { role: roles[userId] });
      setMessage({ message: "User role updated successfully." });
      await loadUsers();
      await refreshCurrentUser();
    } catch (updateError) {
      setError(updateError);
    }
  }

  if (!isAdmin) {
    return (
      <div className="page-stack">
        <section className="panel">
          <h2>Users</h2>
          <p>This screen is only available to admin users.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">Module E</p>
          <h2>Users & roles</h2>
          <p>Review the synced backend user list and promote roles for technicians or admin operations access.</p>
        </div>
      </section>

      <FeedbackBanner error={error || message} kind={error ? "error" : "success"} />

      <section className="panel">
        <div className="panel-header">
          <div>
            <p className="eyebrow">Directory</p>
            <h3>{loading ? "Loading users..." : `${users.length} users available`}</h3>
          </div>
        </div>

        {users.length ? (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Provider</th>
                  <th>Current role</th>
                  <th>Active</th>
                  <th>Role update</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <strong>{user.displayName}</strong>
                      <p className="table-subtext">{user.email}</p>
                    </td>
                    <td>{user.provider}</td>
                    <td>
                      <StatusBadge value={user.role} />
                      {user.email === currentUser.email ? <p className="table-subtext">Current signed-in account</p> : null}
                    </td>
                    <td>{user.active ? "Yes" : "No"}</td>
                    <td>
                      <div className="inline-form">
                        <select value={roles[user.id] || user.role} onChange={(event) => setRoles((current) => ({ ...current, [user.id]: event.target.value }))}>
                          {USER_ROLES.map((role) => (
                            <option key={role} value={role}>
                              {role}
                            </option>
                          ))}
                        </select>
                        <button type="button" className="button button--ghost" onClick={() => void handleRoleUpdate(user.id)}>
                          <Save size={16} />
                          Save
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">No users have signed into the backend yet.</div>
        )}
      </section>
    </div>
  );
}
