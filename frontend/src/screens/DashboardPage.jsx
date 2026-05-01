import { CalendarRange, Boxes, Wrench, Ticket as TicketIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { api } from "../lib/api.js";
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from "recharts";
import StatusBadge from "../components/StatusBadge.jsx";

export default function DashboardPage() {
  const { currentUser } = useAuth();
  const [snapshot, setSnapshot] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      setLoading(true);
      try {
        const [resources, bookings, tickets] = await Promise.allSettled([
          api.resources.list(),
          api.bookings.list({ page: 0, size: 50, sort: "createdAt,desc" }),
          api.tickets.list({ page: 0, size: 50, sort: "createdAt,desc" })
        ]);

        setSnapshot({
          resources: resources.status === "fulfilled" ? resources.value : [],
          bookings: bookings.status === "fulfilled" ? bookings.value?.content || [] : [],
          tickets: tickets.status === "fulfilled" ? tickets.value?.content || [] : [],
        });
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const resources = snapshot?.resources || [];
  const bookings = snapshot?.bookings || [];
  const tickets = snapshot?.tickets || [];

  const activeResources = resources.filter(r => r.status === "ACTIVE").length;
  const pendingBookings = bookings.filter(b => b.status === "PENDING").length;
  const openTickets = tickets.filter(t => t.status === "OPEN" || t.status === "IN_PROGRESS").length;

  const firstName = currentUser?.displayName?.split(" ")[0] || "User";

  // Prepare data for Pie Chart (Tickets)
  const ticketStatusCount = tickets.reduce((acc, t) => {
    acc[t.status] = (acc[t.status] || 0) + 1;
    return acc;
  }, {});
  
  const pieData = [
    { name: 'Open', value: ticketStatusCount['OPEN'] || 0, color: '#ef4444' }, // Red
    { name: 'In Progress', value: ticketStatusCount['IN_PROGRESS'] || 0, color: '#f59e0b' }, // Orange
    { name: 'Resolved', value: ticketStatusCount['RESOLVED'] || 0, color: '#10b981' }, // Green
    { name: 'Closed', value: ticketStatusCount['CLOSED'] || 0, color: '#64748b' }, // Slate
  ].filter(d => d.value > 0);

  // Prepare data for Bar Chart (Bookings)
  const bookingStatusCount = bookings.reduce((acc, b) => {
    acc[b.status] = (acc[b.status] || 0) + 1;
    return acc;
  }, {});
  
  const barData = [
    { name: 'Pending', count: bookingStatusCount['PENDING'] || 0, color: '#f59e0b' },
    { name: 'Approved', count: bookingStatusCount['APPROVED'] || 0, color: '#10b981' },
    { name: 'Rejected', count: bookingStatusCount['REJECTED'] || 0, color: '#ef4444' },
    { name: 'Cancelled', count: bookingStatusCount['CANCELLED'] || 0, color: '#94a3b8' },
  ];

  return (
    <div className="dashboard-wrapper">
      <div className="welcome-banner">
        <h1>Welcome back, {firstName}!</h1>
        <p>Here is what's happening on your campus operations today.</p>
      </div>

      <div className="quick-stats-grid">
        <div className="quick-stat-card">
          <div className="stat-icon-wrap"><Boxes size={24} /></div>
          <div className="stat-info">
            <h3>Active Resources</h3>
            <p className="stat-value">{loading ? "-" : activeResources}</p>
          </div>
        </div>
        <div className="quick-stat-card">
          <div className="stat-icon-wrap"><CalendarRange size={24} /></div>
          <div className="stat-info">
            <h3>Pending Bookings</h3>
            <p className="stat-value">{loading ? "-" : pendingBookings}</p>
          </div>
        </div>
        <div className="quick-stat-card">
          <div className="stat-icon-wrap"><Wrench size={24} /></div>
          <div className="stat-info">
            <h3>Open Tickets</h3>
            <p className="stat-value">{loading ? "-" : openTickets}</p>
          </div>
        </div>
      </div>

      <div className="dashboard-charts" style={{ display: "flex", gap: "24px", marginTop: "24px", flexWrap: "wrap" }}>
        {/* Donut Chart for Tickets */}
        <div className="dashboard-panel" style={{ flex: "1 1 300px", padding: "24px", backgroundColor: "white", borderRadius: "16px", border: "1px solid #e2e8f0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.05)" }}>
          <h2 style={{ fontSize: "16px", marginBottom: "16px", color: "#1e293b", fontWeight: "600" }}>Tickets by Status</h2>
          <div style={{ height: "250px", width: "100%" }}>
            {pieData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={70} outerRadius={90} paddingAngle={4} stroke="none">
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 10px 25px rgba(0,0,0,0.1)' }} itemStyle={{ fontWeight: 600 }} />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '13px', paddingTop: '10px' }} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ display: "flex", height: "100%", alignItems: "center", justifyContent: "center", color: "#94a3b8", fontSize: "13px" }}>No ticket data available</div>
            )}
          </div>
        </div>

        {/* Bar Chart for Bookings */}
        <div className="dashboard-panel" style={{ flex: "1 1 400px", padding: "24px", backgroundColor: "white", borderRadius: "16px", border: "1px solid #e2e8f0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.05)" }}>
          <h2 style={{ fontSize: "16px", marginBottom: "16px", color: "#1e293b", fontWeight: "600" }}>Bookings Overview</h2>
          <div style={{ height: "250px", width: "100%" }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={barData} margin={{ top: 20, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} tickMargin={12} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} tickMargin={12} />
                <Tooltip cursor={{ fill: '#f1f5f9' }} contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 10px 25px rgba(0,0,0,0.1)' }} itemStyle={{ fontWeight: 600 }} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]} barSize={45}>
                  {barData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="dashboard-sections" style={{ display: "flex", gap: "24px", marginTop: "24px", paddingBottom: "32px", flexWrap: "wrap" }}>
        <div className="dashboard-panel" style={{ flex: "1 1 400px", padding: "24px", backgroundColor: "white", borderRadius: "16px", border: "1px solid #e2e8f0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.05)" }}>
          <h2 style={{ fontSize: "16px", marginBottom: "16px", color: "#1e293b", fontWeight: "600", display: "flex", alignItems: "center", gap: "8px" }}>
            <CalendarRange size={18} color="#3b82f6" />
            Recent Bookings
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {bookings.length === 0 ? <p style={{ color: "#94a3b8", fontSize: "13px" }}>No recent bookings found.</p> : null}
            {bookings.slice(0, 5).map(booking => (
              <div 
                key={booking.id} 
                style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "14px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #f1f5f9", transition: "all 0.2s" }}
                onMouseOver={(e) => { e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.backgroundColor = '#f1f5f9'; }} 
                onMouseOut={(e) => { e.currentTarget.style.borderColor = '#f1f5f9'; e.currentTarget.style.backgroundColor = '#f8fafc'; }}
              >
                <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                  <span style={{ fontSize: "14px", fontWeight: "600", color: "#334155" }}>{booking.resourceName || booking.purpose}</span>
                  <span style={{ fontSize: "12px", color: "#64748b" }}>{booking.ownerDisplayName || booking.ownerEmail} &bull; {new Date(booking.startTime).toLocaleDateString()}</span>
                </div>
                <div>
                  <StatusBadge value={booking.status} />
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="dashboard-panel" style={{ flex: "1 1 400px", padding: "24px", backgroundColor: "white", borderRadius: "16px", border: "1px solid #e2e8f0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.05)" }}>
          <h2 style={{ fontSize: "16px", marginBottom: "16px", color: "#1e293b", fontWeight: "600", display: "flex", alignItems: "center", gap: "8px" }}>
            <TicketIcon size={18} color="#f59e0b" />
            Latest Tickets
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {tickets.length === 0 ? <p style={{ color: "#94a3b8", fontSize: "13px" }}>No active tickets found.</p> : null}
            {tickets.slice(0, 5).map(ticket => (
              <div 
                key={ticket.id} 
                style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "14px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #f1f5f9", transition: "all 0.2s" }}
                onMouseOver={(e) => { e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.backgroundColor = '#f1f5f9'; }} 
                onMouseOut={(e) => { e.currentTarget.style.borderColor = '#f1f5f9'; e.currentTarget.style.backgroundColor = '#f8fafc'; }}
              >
                <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                  <span style={{ fontSize: "14px", fontWeight: "600", color: "#334155" }}>{ticket.title}</span>
                  <span style={{ fontSize: "12px", color: "#64748b" }}>{ticket.location} &bull; {new Date(ticket.createdAt).toLocaleDateString()}</span>
                </div>
                <div>
                  <StatusBadge value={ticket.status} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
