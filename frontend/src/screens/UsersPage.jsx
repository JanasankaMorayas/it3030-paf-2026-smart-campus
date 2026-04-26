import { Save, Shield, UserCog, Wrench } from "lucide-react";
import { useEffect, useState } from "react";
import EmptyState from "../components/EmptyState.jsx";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import LoadingState from "../components/LoadingState.jsx";
import PageShell from "../components/PageShell.jsx";
import RoleGate from "../components/RoleGate.jsx";
import SectionCard from "../components/SectionCard.jsx";
import StatCard from "../components/StatCard.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { compactCount } from "../lib/format.js";
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
      const targetUser = users.find((user) => user.id === userId);
      await api.users.updateRole(userId, { role: roles[userId] });
      setMessage({ message: "User role updated successfully." });
      await loadUsers();
      if (targetUser?.email === currentUser?.email) {
        await refreshCurrentUser();
      }
    } catch (updateError) {
      setError(updateError);
    }
  }

  const adminCount = users.filter((user) => user.role === "ADMIN").length;
  const technicianCount = users.filter((user) => user.role === "TECHNICIAN").length;
  const activeCount = users.filter((user) => user.active).length;

  return (
    <RoleGate
      allow={isAdmin}
      fallback={(
        <PageShell
          eyebrow="Module E"
          title="Users and roles"
          description="This area is reserved for administrators who manage roles and account posture."
          meta={<StatusBadge value={currentUser?.role} />}
        >
          <SectionCard>
            <EmptyState
              icon={Shield}
              title="Admin access required"
              message="Sign in with an admin account to review synced users and change platform roles."
            />
          </SectionCard>
        </PageShell>
      )}
    >
      <PageShell
        eyebrow="Module E"
        title="Users and role control"
        description="Review synced users, check auth providers, and promote or demote operational roles."
        actions={(
          <button type="button" className="button button--ghost" onClick={() => void loadUsers()}>
            Refresh users
          </button>
        )}
        meta={(
          <>
            <StatusBadge value="ADMIN" />
            <span className="page-shell__meta-text">Role changes take effect in the backend immediately and are audit-tracked.</span>
          </>
        )}
      >
        <section className="dashboard-stat-grid dashboard-stat-grid--compact">
          <StatCard icon={UserCog} label="Users" value={loading ? "..." : compactCount(users.length)} hint="Known local and Google-synced accounts" tone="teal" />
          <StatCard icon={Shield} label="Admins" value={loading ? "..." : compactCount(adminCount)} hint="Accounts with full platform control" tone="sand" />
          <StatCard icon={Wrench} label="Technicians" value={loading ? "..." : compactCount(technicianCount)} hint="Users eligible for ticket assignment" tone="cream" />
          <StatCard icon={UserCog} label="Active accounts" value={loading ? "..." : compactCount(activeCount)} hint="Currently active user records" tone="teal" />
        </section>

        <FeedbackBanner error={error || message} kind={error ? "error" : "success"} />

        <SectionCard
          eyebrow="Directory"
          title={loading ? "Loading users" : `${users.length} users available`}
          description="Use the role selector to align each account with user, technician, or admin responsibilities."
        >
          {loading ? (
            <LoadingState title="Loading user directory" message="Pulling the current backend user list." lines={5} />
          ) : users.length ? (
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
                        {user.email === currentUser?.email ? <p className="table-subtext">Current signed-in account</p> : null}
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
                          <button type="button" className="button button--primary" onClick={() => void handleRoleUpdate(user.id)}>
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
            <EmptyState
              title="No users yet"
              message="No accounts have been created or synced through login yet."
            />
          )}
        </SectionCard>
      </PageShell>
    </RoleGate>
  );
}
