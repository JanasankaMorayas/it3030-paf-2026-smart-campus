import { ShieldCheck } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import { api } from "../lib/api.js";
import { useAuth } from "../context/AuthContext.jsx";

export default function RegisterPage() {
  const navigate = useNavigate();
  const { loginWithBasic } = useAuth();
  const [form, setForm] = useState({ displayName: "", email: "", password: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [success, setSuccess] = useState(false);

  async function handleRegister(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.auth.register(form);
      setSuccess(true);
      // සාර්ථකව Register වූ පසු ස්වයංක්‍රීයවම Login වේ
      try {
        await loginWithBasic(form.email, form.password);
        navigate("/", { replace: true });
      } catch (loginErr) {
        setTimeout(() => navigate("/login"), 2000);
      }
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  }

  return (
    <div 
      className="login-wrapper"
      style={{
        backgroundImage: "linear-gradient(rgba(15, 23, 42, 0.7), rgba(15, 23, 42, 0.9)), url('https://images.unsplash.com/photo-1541339907198-e08756dedf3f?q=80&w=2070&auto=format&fit=crop')",
        backgroundSize: "cover",
        backgroundPosition: "center",
        backgroundAttachment: "fixed",
        minHeight: "100vh"
      }}
    >
      <div className="login-card">
        <div className="login-header">
          <ShieldCheck size={48} className="login-logo" />
          <h2>Smart Campus</h2>
          <p>Create a new account</p>
        </div>

        {success ? (
          <div style={{ textAlign: "center", padding: "20px" }}>
            <div style={{ padding: "16px", backgroundColor: "#ecfdf5", color: "#0f766e", borderRadius: "6px", marginBottom: "24px", fontWeight: "500" }}>
              Registration successful! Logging you in...
            </div>
          </div>
        ) : (
          <form className="login-form" onSubmit={handleRegister}>
            <div className="login-field">
              <label>Full Name</label>
              <input
                type="text"
                value={form.displayName}
                onChange={(e) => setForm({ ...form, displayName: e.target.value })}
                placeholder="John Doe"
                required
              />
            </div>
            <div className="login-field">
              <label>Email Address</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="user@smartcampus.local"
                required
              />
            </div>
            <div className="login-field">
              <label>Password</label>
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Create a password"
                required
                minLength={6}
              />
            </div>
            <FeedbackBanner error={error} kind="error" />
            <button type="submit" className="btn-primary full-width" disabled={busy}>
              {busy ? "Creating account..." : "Sign up"}
            </button>
            <div style={{ textAlign: "center", marginTop: "16px" }}>
              <span style={{ color: "#64748b", fontSize: "14px" }}>Already have an account? </span>
              <Link to="/login" style={{ color: "#3b82f6", textDecoration: "none", fontSize: "14px", fontWeight: "600" }}>Sign in</Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}