import { ShieldCheck } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import { api } from "../lib/api.js";

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1); // 1 = request code, 2 = enter code & new pass
  const [form, setForm] = useState({ email: "", token: "", newPassword: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  async function handleSendCode(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSuccessMsg(null);
    try {
      const res = await api.auth.forgotPassword(form.email);
      setSuccessMsg(res?.message || "Verification code sent to your email.");
      setStep(2);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api.auth.resetPassword(form.token, form.newPassword);
      setSuccessMsg(res?.message || "Password successfully updated.");
      setStep(3); // success stage
    } catch (err) {
      setError(err);
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
          <p>Reset your password</p>
        </div>

        {step === 1 && (
          <form className="login-form" onSubmit={handleSendCode}>
            <div className="login-field">
              <label>Email Address</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="user@smartcampus.local"
                required
              />
              <small style={{ color: "#64748b", marginTop: "4px", display: "block" }}>
                We'll email you a 6-digit verification code.
              </small>
            </div>
            <FeedbackBanner error={error} kind="error" />
            <button type="submit" className="btn-primary full-width" disabled={busy || !form.email}>
              {busy ? "Sending..." : "Send Verification Code"}
            </button>
          </form>
        )}

        {step === 2 && (
          <form className="login-form" onSubmit={handleResetPassword}>
            {successMsg && <div style={{ padding: "10px", backgroundColor: "#ecfdf5", color: "#0f766e", borderRadius: "6px", marginBottom: "16px", fontSize: "14px" }}>{successMsg}</div>}
            
            <div className="login-field">
              <label>Verification Code</label>
              <input
                type="text"
                value={form.token}
                onChange={(e) => setForm({ ...form, token: e.target.value })}
                placeholder="Enter the 6-digit code"
                required
              />
            </div>
            
            <div className="login-field">
              <label>New Password</label>
              <input
                type="password"
                value={form.newPassword}
                onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
                placeholder="Enter your new password"
                required
              />
            </div>

            <FeedbackBanner error={error} kind="error" />

            <button type="submit" className="btn-primary full-width" disabled={busy || !form.token || !form.newPassword}>
              {busy ? "Updating..." : "Reset Password"}
            </button>
            
            <button 
              type="button" 
              className="full-width" 
              style={{
                marginTop: "12px",
                backgroundColor: "#ffffff",
                color: "#3c4043",
                border: "1px solid #cbd5e1",
                padding: "12px",
                borderRadius: "8px",
                fontWeight: "600",
                fontSize: "15px",
                boxShadow: "0 1px 2px rgba(0, 0, 0, 0.05)",
                transition: "all 0.2s ease-in-out",
                cursor: "pointer"
              }}
              onMouseOver={(e) => { e.currentTarget.style.backgroundColor = "#f8fafc"; e.currentTarget.style.boxShadow = "0 4px 6px -1px rgba(0, 0, 0, 0.1)"; }}
              onMouseOut={(e) => { e.currentTarget.style.backgroundColor = "#ffffff"; e.currentTarget.style.boxShadow = "0 1px 2px rgba(0, 0, 0, 0.05)"; }}
              onClick={() => setStep(1)}
            >
              Back
            </button>
          </form>
        )}

        {step === 3 && (
          <div className="login-form" style={{ textAlign: "center", paddingTop: "10px" }}>
            <div style={{ padding: "16px", backgroundColor: "#ecfdf5", color: "#0f766e", borderRadius: "6px", marginBottom: "24px", fontWeight: "500" }}>
              Your password has been successfully updated!
            </div>
            <Link to="/login" className="btn-primary full-width" style={{ display: "block", textDecoration: "none" }}>
              Return to Sign in
            </Link>
          </div>
        )}

        {step !== 3 && (
          <div style={{ textAlign: "center", marginTop: "24px" }}>
            <Link to="/login" style={{ color: "#3b82f6", textDecoration: "none", fontSize: "14px", fontWeight: "500" }}>
              Return to Sign in
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}