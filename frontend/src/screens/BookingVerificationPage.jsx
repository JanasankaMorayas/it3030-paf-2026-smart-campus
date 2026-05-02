import { CheckCircle2, XCircle, CalendarRange, User, Clock, AlertTriangle } from "lucide-react";
import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../lib/api.js";
import PageShell from "../components/PageShell.jsx";
import SectionCard from "../components/SectionCard.jsx";
import LoadingState from "../components/LoadingState.jsx";
import StatusBadge from "../components/StatusBadge.jsx";

export default function BookingVerificationPage() {
  const { id } = useParams();
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function verify() {
      try {
        const res = await api.bookings.get(id);
        setBooking(res);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    }
    verify();
  }, [id]);

  return (
    <PageShell
      eyebrow="Verification"
      title="Booking Check-In"
      description="Scan results for the requested resource booking."
    >
      <SectionCard>
        {loading ? (
          <LoadingState title="Verifying booking..." message="Checking securely with the system." />
        ) : error ? (
          <div style={{ textAlign: "center", padding: "40px 20px" }}>
            <XCircle size={64} color="#ef4444" style={{ margin: "0 auto 16px" }} />
            <h2 style={{ color: "#1e293b", marginBottom: "8px" }}>Invalid Booking</h2>
            <p style={{ color: "#64748b" }}>This QR code is invalid, or you do not have permission to view it.</p>
            <div style={{ marginTop: "24px" }}>
              <Link to="/bookings" className="button button--primary">Return to Bookings</Link>
            </div>
          </div>
        ) : booking ? (
          <div style={{ maxWidth: "600px", margin: "0 auto", padding: "20px" }}>
            <div style={{ 
              textAlign: "center", padding: "32px", 
              backgroundColor: booking.status === "APPROVED" ? "#ecfdf5" : "#fef2f2",
              border: `2px solid ${booking.status === "APPROVED" ? "#10b981" : "#ef4444"}`,
              borderRadius: "16px", marginBottom: "24px"
            }}>
              {booking.status === "APPROVED" ? (
                <CheckCircle2 size={64} color="#10b981" style={{ margin: "0 auto 16px" }} />
              ) : (
                <AlertTriangle size={64} color="#ef4444" style={{ margin: "0 auto 16px" }} />
              )}
              <h2 style={{ color: "#1e293b", margin: "0 0 12px 0", fontSize: "28px" }}>
                {booking.status === "APPROVED" ? "Verified & Approved" : "Not Approved"}
              </h2>
              <StatusBadge value={booking.status} />
            </div>

            <div style={{ backgroundColor: "#f8fafc", padding: "24px", borderRadius: "16px", border: "1px solid #e2e8f0", display: "flex", flexDirection: "column", gap: "16px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                <CalendarRange size={24} color="#3b82f6" />
                <div><div style={{ fontSize: "13px", color: "#64748b", textTransform: "uppercase", fontWeight: "600" }}>Resource</div><div style={{ fontSize: "16px", fontWeight: "600", color: "#1e293b" }}>{booking.resourceName} ({booking.resourceCode})</div></div>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                <Clock size={24} color="#f59e0b" />
                <div><div style={{ fontSize: "13px", color: "#64748b", textTransform: "uppercase", fontWeight: "600" }}>Time Slot</div><div style={{ fontSize: "15px", color: "#334155" }}>{new Date(booking.startTime).toLocaleString()} - {new Date(booking.endTime).toLocaleTimeString()}</div></div>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                <User size={24} color="#8b5cf6" />
                <div><div style={{ fontSize: "13px", color: "#64748b", textTransform: "uppercase", fontWeight: "600" }}>Owner</div><div style={{ fontSize: "15px", color: "#334155" }}>{booking.ownerDisplayName || booking.ownerEmail}</div></div>
              </div>
            </div>
          </div>
        ) : null}
      </SectionCard>
    </PageShell>
  );
}