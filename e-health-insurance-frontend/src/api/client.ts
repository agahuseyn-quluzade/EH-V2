import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from "axios";

const TOKEN_KEY = "ehi.az.accessToken";
const REFRESH_KEY = "ehi.az.refreshToken";
const USER_ID_KEY = "ehi.az.userId";
const USER_ROLE_KEY = "ehi.az.userRole";

export const tokenStore = {
  getAccess: () => localStorage.getItem(TOKEN_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  set(access: string, refresh: string) {
    localStorage.setItem(TOKEN_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export const userInfoStore = {
  getId: () => localStorage.getItem(USER_ID_KEY),
  getRole: () => localStorage.getItem(USER_ROLE_KEY),
  set(id: string, role: string) {
    localStorage.setItem(USER_ID_KEY, id);
    localStorage.setItem(USER_ROLE_KEY, role);
  },
  clear() {
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(USER_ROLE_KEY);
  },
};

export function decodeJwt(token: string): { userId?: string; role?: string; sub?: string } {
  try {
    const payload = JSON.parse(
      decodeURIComponent(
        atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))
          .split("")
          .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
          .join("")
      )
    );
    return { userId: payload.userId, role: payload.role, sub: payload.sub };
  } catch {
    return {};
  }
}

let onAuthFailure: (() => void) | null = null;
export function setOnAuthFailure(handler: () => void) {
  onAuthFailure = handler;
}

export const api: AxiosInstance = axios.create({
  baseURL: "",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let waitQueue: Array<(token: string | null) => void> = [];

function resolveQueue(token: string | null) {
  waitQueue.forEach((cb) => cb(token));
  waitQueue = [];
}

async function refreshToken(): Promise<string | null> {
  const refresh = tokenStore.getRefresh();
  if (!refresh) return null;
  try {
    const res = await axios.post(
      "/api/v1/auth/refresh",
      { refreshToken: refresh },
      { headers: { "Content-Type": "application/json" } }
    );
    // Backend wraps in ApiResponse<AuthResponse>; unwrap manually here
    const body = res.data;
    const authRes = body?.data ?? body;
    const { accessToken, refreshToken: newRefresh } = authRes as {
      accessToken: string;
      refreshToken: string;
    };
    tokenStore.set(accessToken, newRefresh);
    return accessToken;
  } catch {
    return null;
  }
}

api.interceptors.response.use(
  (res) => {
    // Unwrap ApiResponse<T>: { success: boolean, data: T } → T
    if (
      res.data !== null &&
      typeof res.data === "object" &&
      "success" in res.data &&
      "data" in res.data
    ) {
      res.data = res.data.data;
    }
    return res;
  },
  async (err: AxiosError) => {
    const original = err.config as AxiosRequestConfig & { _retried?: boolean };
    const status = err.response?.status;

    const isAuthEndpoint =
      typeof original?.url === "string" && original.url.includes("/api/v1/auth/");

    if (status === 401 && !original?._retried && !isAuthEndpoint) {
      original._retried = true;
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          waitQueue.push((token) => {
            if (!token) return reject(err);
            original.headers = original.headers || {};
            (original.headers as Record<string, string>).Authorization = `Bearer ${token}`;
            resolve(api(original));
          });
        });
      }
      isRefreshing = true;
      const newToken = await refreshToken();
      isRefreshing = false;
      resolveQueue(newToken);
      if (!newToken) {
        tokenStore.clear();
        userInfoStore.clear();
        if (onAuthFailure) onAuthFailure();
        return Promise.reject(err);
      }
      original.headers = original.headers || {};
      (original.headers as Record<string, string>).Authorization = `Bearer ${newToken}`;
      return api(original);
    }
    return Promise.reject(err);
  }
);

/** Extracts a user-facing message from a Spring error response. */
export function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    // Error body is ApiResponse<ErrorResponse>: { success: false, data: { message, ... } }
    // or may be plain ErrorResponse directly depending on the source
    const body = err.response?.data as Record<string, unknown> | undefined;
    const inner = (body?.data ?? body) as Record<string, unknown> | undefined;
    const message =
      (inner?.message as string) ||
      (body?.message as string) ||
      (inner?.error as string) ||
      (body?.error as string);
    if (message) return message;
    if (err.response?.status === 403) return "You do not have permission for this action";
    return err.message || "Request failed";
  }
  return (err as Error)?.message || "An unexpected error occurred";
}
