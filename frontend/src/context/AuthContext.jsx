import { createContext, useContext, useEffect, useState } from "react";
import { api } from "../lib/api.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [authMode, setAuthMode] = useState(null);
  const [loading, setLoading] = useState(true);

  async function syncUser(preferredMode) {
    setLoading(true);

    try {
      if (preferredMode === "basic") {
        const stored = api.auth.getStoredBasicAuth();
        if (!stored) {
          throw { status: 401, message: "No stored demo credentials found." };
        }

        const user = await api.auth.getCurrentUser({ basicAuth: stored });
        setCurrentUser(user);
        setAuthMode("basic");
        return user;
      }

      const user = await api.auth.getCurrentUser();
      setCurrentUser(user);
      setAuthMode("google");
      return user;
    } catch (error) {
      if (preferredMode === "basic") {
        api.auth.clearStoredBasicAuth();
      }

      if (!preferredMode && api.auth.getStoredBasicAuth()) {
        try {
          const fallbackUser = await syncUser("basic");
          return fallbackUser;
        } catch {
          // ignore fallback failure and continue to clear state
        }
      }

      setCurrentUser(null);
      setAuthMode(null);
      throw error;
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void (async () => {
      try {
        await syncUser();
      } catch {
        setCurrentUser(null);
        setAuthMode(null);
      }
    })();
  }, []);

  async function loginWithBasic(username, password) {
    const credentials = { username: username.trim(), password };
    api.auth.storeBasicAuth(credentials);

    try {
      const user = await api.auth.getCurrentUser({ basicAuth: credentials });
      setCurrentUser(user);
      setAuthMode("basic");
      return user;
    } catch (error) {
      api.auth.clearStoredBasicAuth();
      setCurrentUser(null);
      setAuthMode(null);
      throw error;
    }
  }

  async function refreshGoogleSession() {
    return syncUser("google");
  }

  async function refreshCurrentUser() {
    return syncUser(authMode === "basic" ? "basic" : undefined);
  }

  async function logout() {
    try {
      await api.auth.logout({ basicAuth: authMode === "basic" ? api.auth.getStoredBasicAuth() : null });
    } catch {
      // best-effort logout only
    }

    api.auth.clearStoredBasicAuth();
    setCurrentUser(null);
    setAuthMode(null);
  }

  const value = {
    currentUser,
    authMode,
    loading,
    isAdmin: currentUser?.role === "ADMIN",
    isTechnician: currentUser?.role === "TECHNICIAN",
    loginWithBasic,
    refreshGoogleSession,
    refreshCurrentUser,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider.");
  }

  return context;
}
