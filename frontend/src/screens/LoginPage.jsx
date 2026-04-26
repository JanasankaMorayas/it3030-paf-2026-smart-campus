import { ArrowRight, KeyRound, RefreshCcw, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { api } from "../lib/api.js";

const demoPresets = [
  { label: "Demo User", username: "dev-user@smartcampus.local", password: "dev-user-pass" },
  { label: "Demo Technician", username: "dev-tech@smartcampus.local", password: "dev-tech-pass" },
  { label: "Demo Admin", username: "dev-admin@smartcampus.local", password: "dev-admin-pass" },
];

export default function LoginPage() {
  const navigate = useNavigate();
  const { loginWithBasic, refreshGoogleSession } = useAuth();
  const [form, setForm] = useState({
    username: "dev-user@smartcampus.local",
    password: "dev-user-pass",
  });
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [busy, setBusy] = useState(false);

  async function handleBasicLogin(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);

    try {
      await loginWithBasic(form.username, form.password);
      navigate("/", { replace: true });
    } catch (loginError) {
      setError(loginError);
    } finally {
      setBusy(false);
    }
  }

  async function handleGoogleRefresh() {
    setBusy(true);
    setError(null);
    setInfo(null);

    try {
      await refreshGoogleSession();
      navigate("/", { replace: true });
    } catch (sessionError) {
      setInfo({
        message: sessionError.status === 401
          ? "No active Google session was found yet. Finish the Google sign-in tab first, then come back and try again."
          : sessionError.message,
      });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-panel auth-panel--wide">
        <div className="auth-panel__header">
          <p className="eyebrow">Smart Campus Operations Hub</p>
          <h1>Sign in to the operations dashboard</h1>
          <p className="panel-description">
            Use Google OAuth in a browser tab or sign in with local Basic Auth demo users for assignment walkthroughs.
          </p>
        </div>

        <div className="auth-grid">
          <section className="auth-card">
            <div className="auth-card__title">
              <ShieldCheck size={18} />
              <h2>Google browser login</h2>
            </div>
            <p>
              This uses the backend OAuth flow at <strong>{api.auth.getApiBaseUrl()}</strong>. Open Google login,
              complete the browser flow, and you should land back on this dashboard automatically.
            </p>

            <div className="stacked-actions">
              <a
                className="button button--primary"
                href={api.auth.getGoogleLoginUrl()}
              >
                Continue with Google
                <ArrowRight size={16} />
              </a>
              <button type="button" className="button button--ghost" onClick={() => void handleGoogleRefresh()} disabled={busy}>
                <RefreshCcw size={16} />
                I finished Google sign-in
              </button>
            </div>

            <ul className="hint-list">
              <li>Backend must be running on `http://localhost:8080`.</li>
              <li>After Google sign-in, the backend redirects back to this frontend origin by default.</li>
              <li>If Google OAuth is not configured yet, use Basic Auth demo mode below.</li>
            </ul>
          </section>

          <section className="auth-card">
            <div className="auth-card__title">
              <KeyRound size={18} />
              <h2>Local demo login</h2>
            </div>

            <form className="form-grid" onSubmit={handleBasicLogin}>
              <label className="field">
                <span>Email</span>
                <input
                  type="email"
                  value={form.username}
                  onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))}
                  placeholder="dev-user@smartcampus.local"
                />
              </label>

              <label className="field">
                <span>Password</span>
                <input
                  type="password"
                  value={form.password}
                  onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                  placeholder="dev-user-pass"
                />
              </label>

              <div className="preset-row">
                {demoPresets.map((preset) => (
                  <button
                    key={preset.label}
                    type="button"
                    className="button button--chip"
                    onClick={() => setForm({ username: preset.username, password: preset.password })}
                  >
                    {preset.label}
                  </button>
                ))}
              </div>

              <FeedbackBanner error={error || info} kind={error ? "error" : "info"} />

              <button type="submit" className="button button--primary" disabled={busy}>
                Sign in with Basic Auth
              </button>
            </form>
          </section>
        </div>
      </div>
    </div>
  );
}
