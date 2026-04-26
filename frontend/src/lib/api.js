const BASIC_AUTH_STORAGE_KEY = "smart-campus-basic-auth";
const DEFAULT_BACKEND_HOST = "localhost:8080";

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL;
  if (configuredBaseUrl) {
    return configuredBaseUrl;
  }

  if (typeof window === "undefined") {
    return `http://${DEFAULT_BACKEND_HOST}`;
  }

  const frontendHost = window.location.hostname;
  const backendHost = frontendHost === "127.0.0.1" ? "127.0.0.1:8080" : DEFAULT_BACKEND_HOST;
  return `${window.location.protocol}//${backendHost}`;
}

const API_BASE_URL = resolveApiBaseUrl();

function encodeBasicAuth({ username, password }) {
  return `Basic ${btoa(`${username}:${password}`)}`;
}

function buildUrl(path, params) {
  const url = new URL(path.startsWith("http") ? path : `${API_BASE_URL}${path}`);

  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value === undefined || value === null || value === "") {
        continue;
      }

      url.searchParams.append(key, String(value));
    }
  }

  return url.toString();
}

async function parseResponse(response) {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }

  const text = await response.text();
  return text || null;
}

function normalizeError(response, payload) {
  if (payload && typeof payload === "object" && !Array.isArray(payload)) {
    return {
      status: response.status,
      error: payload.error || response.statusText,
      message: payload.message || response.statusText,
      validationErrors: payload.validationErrors || {},
      path: payload.path,
    };
  }

  return {
    status: response.status,
    error: response.statusText,
    message: typeof payload === "string" && payload ? payload : "Request failed.",
    validationErrors: {},
  };
}

function createNetworkError(error) {
  return {
    status: 0,
    error: "NetworkError",
    message: error instanceof TypeError
      ? `Could not reach the backend at ${API_BASE_URL}. Check that Spring Boot is running and the current frontend origin is allowed by CORS.`
      : "A network error interrupted the request.",
    validationErrors: {},
  };
}

async function request(path, options = {}) {
  const {
    method = "GET",
    body,
    params,
    headers = {},
    basicAuth,
  } = options;

  const resolvedBasicAuth = basicAuth || api.auth.getStoredBasicAuth();
  const requestHeaders = new Headers({
    Accept: "application/json",
    ...headers,
  });

  if (resolvedBasicAuth?.username && resolvedBasicAuth?.password) {
    requestHeaders.set("Authorization", encodeBasicAuth(resolvedBasicAuth));
  }

  const fetchOptions = {
    method,
    headers: requestHeaders,
    credentials: "include",
  };

  if (body !== undefined && body !== null) {
    requestHeaders.set("Content-Type", "application/json");
    fetchOptions.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(buildUrl(path, params), fetchOptions);
  } catch (error) {
    throw createNetworkError(error);
  }

  const payload = await parseResponse(response);

  if (!response.ok) {
    throw normalizeError(response, payload);
  }

  return payload;
}

const authApi = {
  getStoredBasicAuth() {
    const rawValue = window.localStorage.getItem(BASIC_AUTH_STORAGE_KEY);
    return rawValue ? JSON.parse(rawValue) : null;
  },

  storeBasicAuth(credentials) {
    window.localStorage.setItem(BASIC_AUTH_STORAGE_KEY, JSON.stringify(credentials));
  },

  clearStoredBasicAuth() {
    window.localStorage.removeItem(BASIC_AUTH_STORAGE_KEY);
  },

  async getCurrentUser({ basicAuth } = {}) {
    return request("/api/users/me", { basicAuth });
  },

  async getStatus() {
    const redirectTarget = typeof window !== "undefined" ? `${window.location.origin}/` : "/";
    return request("/api/auth/status", { params: { redirect_uri: redirectTarget } });
  },

  async logout({ basicAuth } = {}) {
    return request("/logout", { method: "POST", basicAuth });
  },

  getGoogleLoginUrl() {
    const redirectTarget = typeof window !== "undefined" ? `${window.location.origin}/` : "/";
    return buildUrl("/login", { redirect_uri: redirectTarget });
  },

  getApiBaseUrl() {
    return API_BASE_URL;
  },
};

export const api = {
  auth: authApi,

  resources: {
    list(params = {}) {
      return request("/api/resources", { params });
    },
    get(id) {
      return request(`/api/resources/${id}`);
    },
    create(payload) {
      return request("/api/resources", { method: "POST", body: payload });
    },
    update(id, payload) {
      return request(`/api/resources/${id}`, { method: "PUT", body: payload });
    },
    remove(id) {
      return request(`/api/resources/${id}`, { method: "DELETE" });
    },
  },

  bookings: {
    list(params = {}) {
      return request("/api/bookings", { params });
    },
    get(id) {
      return request(`/api/bookings/${id}`);
    },
    create(payload) {
      return request("/api/bookings", { method: "POST", body: payload });
    },
    update(id, payload) {
      return request(`/api/bookings/${id}`, { method: "PUT", body: payload });
    },
    updateStatus(id, payload) {
      return request(`/api/bookings/${id}/status`, { method: "PATCH", body: payload });
    },
    cancel(id) {
      return request(`/api/bookings/${id}`, { method: "DELETE" });
    },
  },

  tickets: {
    list(params = {}) {
      return request("/api/tickets", { params });
    },
    get(id) {
      return request(`/api/tickets/${id}`);
    },
    create(payload) {
      return request("/api/tickets", { method: "POST", body: payload });
    },
    update(id, payload) {
      return request(`/api/tickets/${id}`, { method: "PUT", body: payload });
    },
    updateStatus(id, payload) {
      return request(`/api/tickets/${id}/status`, { method: "PATCH", body: payload });
    },
    assign(id, payload) {
      return request(`/api/tickets/${id}/assign`, { method: "PATCH", body: payload });
    },
    cancel(id) {
      return request(`/api/tickets/${id}`, { method: "DELETE" });
    },
  },

  notifications: {
    list(params = {}) {
      return request("/api/notifications", { params });
    },
    unread(params = {}) {
      return request("/api/notifications/unread", { params });
    },
    markRead(id) {
      return request(`/api/notifications/${id}/read`, { method: "PATCH" });
    },
    markAllRead(recipient) {
      return request("/api/notifications/read-all", {
        method: "PATCH",
        params: recipient ? { recipient } : {},
      });
    },
    remove(id) {
      return request(`/api/notifications/${id}`, { method: "DELETE" });
    },
  },

  users: {
    list() {
      return request("/api/users");
    },
    updateRole(id, payload) {
      return request(`/api/users/${id}/role`, { method: "PATCH", body: payload });
    },
  },

  audit: {
    list(params = {}) {
      return request("/api/audit-logs", { params });
    },
    entityHistory(entityType, entityId, params = {}) {
      return request(`/api/audit-logs/${entityType}/${entityId}`, { params });
    },
  },

  admin: {
    backfillUserLinks() {
      return request("/api/admin/backfill/user-links", { method: "POST" });
    },
  },
};
