import { User, Shield, Mail, Activity, Edit2 } from "lucide-react";
import { useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { api } from "../lib/api.js";
import FeedbackBanner from "../components/FeedbackBanner.jsx";

export default function ProfilePage() {
  const { currentUser, logout, refreshCurrentUser } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [displayName, setDisplayName] = useState(currentUser?.displayName || "");
  const [newPassword, setNewPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  if (!currentUser) return null;

  async function handleSaveProfile() {
    setBusy(true);
    setError(null);
    setSuccessMsg(null);
    try {
      const payload = { displayName };
      if (newPassword.trim()) {
        payload.password = newPassword.trim();
      }
      await api.users.updateProfile(payload);
      await refreshCurrentUser();
      setSuccessMsg("Profile updated successfully.");
      setIsEditing(false);
      setNewPassword("");
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">User Profile</h1>
        <p className="page-subtitle">Manage your account information and preferences</p>
      </div>

      <div className="dashboard-content" style={{ display: "flex", gap: "24px", alignItems: "flex-start", flexWrap: "wrap" }}>
        {/* Profile Card */}
        <div className="new-dashboard-card" style={{ flex: "1 1 350px" }}>
          <div className="card-header" style={{ borderBottom: "1px solid #e2e8f0", paddingBottom: "16px", marginBottom: "16px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h3 style={{ margin: 0, color: "#19315D", display: "flex", alignItems: "center", gap: "8px" }}>
              <User size={20} />
              Profile Details
            </h3>
            {!isEditing && (
              <button onClick={() => { setIsEditing(true); setSuccessMsg(null); }} style={{ background: "none", border: "none", color: "#3b82f6", cursor: "pointer", display: "flex", alignItems: "center", gap: "4px", fontSize: "14px", fontWeight: "500", padding: 0 }}>
                <Edit2 size={14} /> Edit
              </button>
            )}
          </div>
          
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
              <div 
                style={{ 
                  width: "64px", 
                  height: "64px", 
                  borderRadius: "50%", 
                  backgroundColor: "#19315D", 
                  color: "white", 
                  display: "flex", 
                  alignItems: "center", 
                  justifyContent: "center", 
                  fontSize: "24px", 
                  fontWeight: "bold" 
                }}
              >
              {(isEditing ? displayName : currentUser?.displayName)?.charAt(0)?.toUpperCase() || 'U'}
              </div>
            <div style={{ flex: 1 }}>
              {isEditing ? (
                <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                  <input 
                    type="text" 
                    value={displayName} 
                    onChange={(e) => setDisplayName(e.target.value)} 
                    style={{ padding: "8px 12px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "14px", width: "100%" }}
                    placeholder="Enter your name"
                  />
                  <input 
                    type="password" 
                    value={newPassword} 
                    onChange={(e) => setNewPassword(e.target.value)} 
                    style={{ padding: "8px 12px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "14px", width: "100%" }}
                    placeholder="New password (leave blank to keep current)"
                  />
                  <div style={{ display: "flex", gap: "8px" }}>
                    <button onClick={handleSaveProfile} disabled={busy || !displayName.trim()} style={{ padding: "6px 12px", backgroundColor: "#3b82f6", color: "white", border: "none", borderRadius: "6px", fontSize: "13px", fontWeight: "600", cursor: "pointer" }}>
                      {busy ? "Saving..." : "Save"}
                    </button>
                    <button onClick={() => { setIsEditing(false); setDisplayName(currentUser.displayName || ""); setNewPassword(""); setError(null); }} disabled={busy} style={{ padding: "6px 12px", backgroundColor: "white", color: "#64748b", border: "1px solid #cbd5e1", borderRadius: "6px", fontSize: "13px", fontWeight: "600", cursor: "pointer" }}>
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <h2 style={{ margin: "0 0 4px 0", color: "#334155" }}>{currentUser.displayName || "User"}</h2>
                  <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "#64748b", fontSize: "14px" }}>
                    <Mail size={14} />
                    {currentUser.email || "No email provided"}
                  </div>
                </>
              )}
            </div>
            </div>

          <FeedbackBanner error={error} />
          {successMsg && <div style={{ padding: "10px", backgroundColor: "#ecfdf5", color: "#0f766e", borderRadius: "6px", fontSize: "14px", fontWeight: "500", marginTop: "16px" }}>{successMsg}</div>}

            <div style={{ padding: "16px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px", color: "#334155", fontWeight: "600" }}>
                <Shield size={16} color="#19315D" />
                Account Role
              </div>
              <div style={{ 
                display: "inline-block", 
                padding: "6px 12px", 
                backgroundColor: currentUser.role === 'ADMIN' ? "#fee2e2" : "#e0e7ff", 
                color: currentUser.role === 'ADMIN' ? "#991b1b" : "#3730a3", 
                borderRadius: "20px", 
                fontSize: "13px", 
                fontWeight: "600" 
              }}>
                {currentUser.role || 'USER'}
              </div>
            </div>

            <div style={{ padding: "16px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px", color: "#334155", fontWeight: "600" }}>
                <Activity size={16} color="#19315D" />
                Account Status
              </div>
              <div style={{ color: "#64748b", fontSize: "14px" }}>
                Active and successfully synced.
              </div>
            </div>
            
            <button 
              onClick={() => logout()}
              style={{
                width: "100%",
                padding: "10px",
                backgroundColor: "transparent",
                border: "1px solid #ef4444",
                color: "#ef4444",
                borderRadius: "8px",
                fontWeight: "600",
                cursor: "pointer",
                marginTop: "16px",
                transition: "all 0.2s"
              }}
              onMouseOver={(e) => { e.currentTarget.style.backgroundColor = "#fef2f2"; }}
              onMouseOut={(e) => { e.currentTarget.style.backgroundColor = "transparent"; }}
            >
              Sign Out
            </button>
          </div>
        </div>

        {/* Preferences / Info Card (Placeholder) */}
        <div className="new-dashboard-card" style={{ flex: "2 1 450px" }}>
          <div className="card-header" style={{ borderBottom: "1px solid #e2e8f0", paddingBottom: "16px", marginBottom: "16px" }}>
            <h3 style={{ margin: 0, color: "#19315D" }}>Quick Info</h3>
          </div>
          <div style={{ color: "#64748b", lineHeight: "1.6" }}>
            <p>Your Smart Campus account allows you to securely access resources, book facilities, and manage your operational tickets.</p>
            <p>If you need to change your email or connected SSO accounts, please contact a system administrator.</p>
          </div>
        </div>
      </div>
    </div>
  );
}