import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout.jsx";
import { useAuth } from "./context/AuthContext.jsx";
import AuditPage from "./screens/AuditPage.jsx";
import BookingsPage from "./screens/BookingsPage.jsx";
import DashboardPage from "./screens/DashboardPage.jsx";
import LoginPage from "./screens/LoginPage.jsx";
import NotificationsPage from "./screens/NotificationsPage.jsx";
import ResourcesPage from "./screens/ResourcesPage.jsx";
import TicketsPage from "./screens/TicketsPage.jsx";
import UsersPage from "./screens/UsersPage.jsx";

function FullScreenMessage({ title, message }) {
  return (
    <div className="auth-screen auth-screen--centered">
      <div className="auth-panel">
        <p className="eyebrow">Smart Campus Operations Hub</p>
        <h1>{title}</h1>
        <p className="panel-description">{message}</p>
      </div>
    </div>
  );
}

function ProtectedLayout() {
  const { currentUser, loading } = useAuth();

  if (loading) {
    return <FullScreenMessage title="Loading workspace" message="Syncing your backend session and dashboard access." />;
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  );
}

function LoginRoute() {
  const { currentUser, loading } = useAuth();

  if (loading) {
    return <FullScreenMessage title="Checking sign-in" message="Looking for your current Google or Basic Auth session." />;
  }

  if (currentUser) {
    return <Navigate to="/" replace />;
  }

  return <LoginPage />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginRoute />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/resources" element={<ResourcesPage />} />
        <Route path="/bookings" element={<BookingsPage />} />
        <Route path="/tickets" element={<TicketsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/audit" element={<AuditPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
