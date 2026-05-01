import { Bell, Boxes, CalendarRange, LayoutDashboard, LogOut, Menu, ScrollText, Shield, UserCog, Wrench, Search, CheckCheck } from "lucide-react";
import { useEffect, useState, useRef } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { api } from "../lib/api.js";

function navigationForRole(role) {
  const common = [
    { to: "/", label: "Dashboard", icon: LayoutDashboard },
    { to: "/bookings", label: "Bookings", icon: CalendarRange },
    { to: "/resources", label: "Resources", icon: Boxes },
    { to: "/tickets", label: "Tickets", icon: Wrench },
    { to: "/notifications", label: "Notifications", icon: Bell },
  ];

  if (role === "ADMIN") {
    return [
      ...common,
      { to: "/users", label: "Users", icon: UserCog },
      { to: "/audit", label: "Audit", icon: ScrollText },
    ];
  }
  return common;
}

export default function AppLayout({ children }) {
  const { currentUser, logout } = useAuth();
  const [navOpen, setNavOpen] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [recentNotifications, setRecentNotifications] = useState([]);
  const dropdownRef = useRef(null);
  const searchContainerRef = useRef(null);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (!currentUser) return;
    async function fetchNotifications() {
      try {
        const [unreadRes, listRes] = await Promise.all([
          api.notifications.unread({ size: 1 }),
          api.notifications.list({ size: 5, sort: "createdAt,desc" })
        ]);
        setUnreadCount(unreadRes.totalElements || 0);
        setRecentNotifications(listRes.content || []);
      } catch (err) {
        console.error("Failed to load notifications", err);
      }
    }
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 60000); // Check every minute
    return () => clearInterval(interval);
  }, [currentUser]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
      if (searchContainerRef.current && !searchContainerRef.current.contains(event.target)) {
        setIsSearchFocused(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    setNavOpen(false);
    setShowDropdown(false);
  }, [location.pathname]);

  const links = navigationForRole(currentUser?.role);

  const pageSuggestions = searchQuery.trim() 
    ? links.filter(link => link.label.toLowerCase().includes(searchQuery.trim().toLowerCase())) 
    : [];

  async function handleMarkAllRead(e) {
    e.stopPropagation();
    try {
      await api.notifications.markAllRead();
      setUnreadCount(0);
      setRecentNotifications(prev => prev.map(n => ({ ...n, read: true })));
    } catch (err) {
      console.error("Failed to mark all read", err);
    }
  }

  async function handleNotificationClick(notification) {
    if (!notification.read) {
      try {
        await api.notifications.markRead(notification.id);
        setRecentNotifications(prev => prev.map(n => n.id === notification.id ? { ...n, read: true } : n));
        setUnreadCount(prev => Math.max(0, prev - 1));
      } catch (err) {
        console.error("Failed to mark read", err);
      }
    }
    setShowDropdown(false);
    
    if (notification.relatedEntityType === "TICKET") {
      navigate("/tickets");
    } else if (notification.relatedEntityType === "BOOKING") {
      navigate("/bookings");
    } else {
      navigate("/notifications");
    }
  }

  function handleSearch(event) {
    if (event.key === "Enter" && searchQuery.trim()) {
      const query = searchQuery.trim().toLowerCase();
      
      if (query.includes("ticket")) navigate("/tickets");
      else if (query.includes("book")) navigate("/bookings");
      else if (query.includes("notif")) navigate("/notifications");
      else if (query.includes("user") && currentUser?.role === "ADMIN") navigate("/users");
      else if (query.includes("audit") && currentUser?.role === "ADMIN") navigate("/audit");
      else if (query.includes("resource")) navigate("/resources");
      else {
        // Fallback: Search resources by location
        navigate(`/resources?location=${encodeURIComponent(searchQuery.trim())}`);
      }
      
      setSearchQuery("");
      setShowDropdown(false);
    }
  }

  return (
    <div className="new-dashboard-layout">
      <aside className={`new-sidebar ${navOpen ? "open" : ""}`}>
        <div className="new-sidebar-header">
           <Shield size={24} className="logo-icon" />
           <span className="logo-text">Smart Campus</span>
        </div>
        <nav className="new-sidebar-nav">
           {links.map((link) => {
             const Icon = link.icon;
             return (
               <NavLink key={link.to} to={link.to} end={link.to === "/"} className={({isActive}) => isActive ? "new-nav-item active" : "new-nav-item"}>
                 <Icon size={20} />
                 <span>{link.label}</span>
               </NavLink>
             );
           })}
        </nav>
        <div className="new-sidebar-footer">
           <button onClick={() => { logout(); navigate("/login"); }} className="new-nav-item logout-btn">
             <LogOut size={20} />
             <span>Logout</span>
           </button>
        </div>
      </aside>

      <div className="new-main-area">
        <header className="new-top-header">
           <div className="header-left">
             <button className="mobile-menu-btn" onClick={() => setNavOpen(!navOpen)}>
               <Menu size={24} />
             </button>
             <h2 className="header-title">Dashboard</h2>
           </div>
           <div className="header-right">
             <div className="search-container" ref={searchContainerRef} style={{ position: "relative" }}>
               <div className="search-box">
                  <Search size={18} />
                  <input 
                    type="text" 
                    placeholder="Search pages or locations..." 
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onKeyDown={handleSearch}
                    onFocus={() => setIsSearchFocused(true)}
                  />
               </div>
               {isSearchFocused && searchQuery.trim() && (
                 <div style={{
                   position: 'absolute', top: '100%', left: '0', right: '0', marginTop: '8px',
                   backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 10px 25px rgba(0,0,0,0.1)',
                   border: '1px solid #e2e8f0', zIndex: 50, overflow: 'hidden'
                 }}>
                   {pageSuggestions.length > 0 && (
                     <div style={{ padding: '8px 12px', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: '700', color: '#64748b', backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>Pages</div>
                   )}
                   {pageSuggestions.map(link => {
                     const Icon = link.icon;
                     return (
                       <div
                         key={link.to}
                         onClick={() => { navigate(link.to); setSearchQuery(""); setIsSearchFocused(false); }}
                         style={{ padding: '10px 12px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', color: '#334155', fontSize: '13px', borderBottom: '1px solid #f1f5f9' }}
                         onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#f0f9ff'}
                         onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                       >
                         <Icon size={16} color="#64748b" />
                         {link.label}
                       </div>
                     );
                   })}
                   <div style={{ padding: '8px 12px', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: '700', color: '#64748b', backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', borderTop: pageSuggestions.length > 0 ? '1px solid #e2e8f0' : 'none' }}>Locations</div>
                   <div
                     onClick={() => { navigate(`/resources?location=${encodeURIComponent(searchQuery.trim())}`); setSearchQuery(""); setIsSearchFocused(false); }}
                     style={{ padding: '10px 12px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', color: '#3b82f6', fontSize: '13px', fontWeight: '500' }}
                     onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#f0f9ff'}
                     onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                   >
                     <Search size={16} />
                     Search resources in "{searchQuery.trim()}"
                   </div>
                 </div>
               )}
             </div>
          <div className="icon-action-btn relative" ref={dropdownRef} onClick={() => setShowDropdown(!showDropdown)} style={{ cursor: "pointer", position: "relative" }}>
             <Bell size={20} />
             {unreadCount > 0 && (
               <span className="notification-dot" style={{
                 position: 'absolute', top: '0', right: '0', backgroundColor: '#ef4444', color: 'white', 
                 fontSize: '10px', fontWeight: 'bold', width: '16px', height: '16px', borderRadius: '50%', 
                 display: 'flex', alignItems: 'center', justifyContent: 'center', transform: 'translate(25%, -25%)'
               }}>
                 {unreadCount > 99 ? '99+' : unreadCount}
               </span>
             )}
             
             {showDropdown && (
               <div style={{
                 position: 'absolute', top: '100%', right: '0', marginTop: '12px', width: '320px', 
                 backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 10px 25px rgba(0,0,0,0.1)', 
                 border: '1px solid #e2e8f0', zIndex: 50, overflow: 'hidden', textAlign: 'left', cursor: 'default'
               }} onClick={(e) => e.stopPropagation()}>
                 <div style={{ padding: '12px 16px', borderBottom: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                   <span style={{ fontWeight: '600', color: '#1e293b' }}>Notifications</span>
                   {unreadCount > 0 && (
                     <button
                       onClick={handleMarkAllRead}
                       style={{ background: 'none', border: 'none', color: '#3b82f6', fontSize: '12px', fontWeight: '500', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', padding: 0 }}
                       onMouseOver={(e) => e.currentTarget.style.textDecoration = 'underline'}
                       onMouseOut={(e) => e.currentTarget.style.textDecoration = 'none'}
                     >
                       <CheckCheck size={14} /> Mark all read
                     </button>
                   )}
                 </div>
                 <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
                   {recentNotifications.length > 0 ? (
                     recentNotifications.map(n => (
                       <div 
                         key={n.id} 
                         onClick={() => handleNotificationClick(n)}
                         style={{ padding: '12px 16px', borderBottom: '1px solid #f1f5f9', backgroundColor: n.read ? 'white' : '#f0f9ff', cursor: 'pointer' }}
                       >
                         <div style={{ fontSize: '13px', fontWeight: n.read ? '500' : '600', color: '#334155', marginBottom: '4px' }}>{n.title}</div>
                         <div style={{ fontSize: '12px', color: '#64748b', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{n.message}</div>
                         <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '6px' }}>{new Date(n.createdAt).toLocaleString()}</div>
                       </div>
                     ))
                   ) : (
                     <div style={{ padding: '16px', textAlign: 'center', color: '#64748b', fontSize: '13px' }}>No notifications found</div>
                   )}
                 </div>
                 <div style={{ padding: '10px', borderTop: '1px solid #e2e8f0', backgroundColor: '#f8fafc', textAlign: 'center' }}>
                   <NavLink to="/notifications" style={{ fontSize: '13px', color: '#3b82f6', fontWeight: '600', textDecoration: 'none' }} onClick={() => setShowDropdown(false)}>
                     See all notifications
                   </NavLink>
                 </div>
               </div>
             )}
          </div>
             <NavLink to="/profile" className="user-profile-header" style={{ textDecoration: 'none', color: 'inherit', display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div className="avatar">{currentUser?.displayName?.charAt(0) || 'U'}</div>
                <div className="user-info">
                   <div className="user-name">{currentUser?.displayName || currentUser?.email || 'User'}</div>
                   <div className="user-role">{currentUser?.role || 'USER'}</div>
                </div>
             </NavLink>
           </div>
        </header>
        
        <main className="new-page-content">
          {children}
        </main>
      </div>
      
      {navOpen && <div className="new-sidebar-backdrop" onClick={() => setNavOpen(false)} />}
    </div>
  );
}
