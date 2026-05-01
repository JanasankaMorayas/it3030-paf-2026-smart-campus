import { ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { api } from "../lib/api.js";

export default function LoginPage() {
  const navigate = useNavigate();
  const { loginWithBasic } = useAuth();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [googleStatus, setGoogleStatus] = useState({ loading: true, configured: false });

  useEffect(() => {
    void (async () => {
      try {
        const status = await api.auth.getStatus();
        setGoogleStatus({ loading: false, configured: status.googleOauthConfigured });
      } catch (statusError) {
        setGoogleStatus({ loading: false, configured: false });
      }
    })();
  }, []);


  async function handleBasicLogin(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await loginWithBasic(form.username, form.password);
      navigate("/", { replace: true });
    } catch (loginError) {
      setError(loginError);
    } finally {
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
          <p>Sign in to your account</p>
        </div>

        <form className="login-form" onSubmit={handleBasicLogin}>
          <div className="login-field">
            <label>Email Address</label>
            <input
              type="email"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              placeholder="user@domain.com"
              required
            />
          </div>
          <div className="login-field" style={{ position: "relative" }}>
            <label style={{ display: "flex", justifyContent: "space-between" }}>
              Password
              <Link to="/forgot-password" style={{ color: "#3b82f6", textDecoration: "none", fontSize: "14px", fontWeight: "500" }}>Forgot password?</Link>
            </label>
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="Enter your password"
              required
            />
          </div>

          <FeedbackBanner error={error} kind="error" />

          <button type="submit" className="btn-primary full-width" disabled={busy}>
            Sign in
          </button>
          
          <div style={{ textAlign: "center", marginTop: "16px" }}>
            <span style={{ color: "#64748b", fontSize: "14px" }}>Don't have an account? </span>
            <Link to="/register" style={{ color: "#3b82f6", textDecoration: "none", fontSize: "14px", fontWeight: "600" }}>Sign up</Link>
          </div>
        </form>

        <div className="login-divider">
          <span>OR</span>
        </div>


        <div className="login-sso">
          <a
            href={googleStatus.configured ? api.auth.getGoogleLoginUrl() : "#"}
            className={`full-width ${!googleStatus.configured ? "disabled" : ""}`}
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: "12px",
              backgroundColor: "#ffffff",
              color: "#3c4043",
              border: "1px solid #cbd5e1",
              padding: "12px",
              borderRadius: "8px",
              fontWeight: "600",
              fontSize: "15px",
              textDecoration: "none",
              boxShadow: "0 1px 2px rgba(0, 0, 0, 0.05)",
              transition: "all 0.2s ease-in-out",
              opacity: !googleStatus.configured ? 0.6 : 1,
              cursor: !googleStatus.configured ? "not-allowed" : "pointer"
            }}
            onMouseOver={(e) => { if (googleStatus.configured) { e.currentTarget.style.backgroundColor = "#f8fafc"; e.currentTarget.style.boxShadow = "0 4px 6px -1px rgba(0, 0, 0, 0.1)"; } }}
            onMouseOut={(e) => { if (googleStatus.configured) { e.currentTarget.style.backgroundColor = "#ffffff"; e.currentTarget.style.boxShadow = "0 1px 2px rgba(0, 0, 0, 0.05)"; } }}
            onClick={(e) => { if (!googleStatus.configured) e.preventDefault(); }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.16v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.16C1.43 8.55 1 10.22 1 12s.43 3.45 1.16 4.93l3.68-2.84z" fill="#FBBC05"/>
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.16 7.07l3.68 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            </svg>
            Sign in with Google
          </a>
        </div>
      </div>
    </div>
  );
}
