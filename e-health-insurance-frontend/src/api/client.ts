import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from "axios";

// All traffic goes through the API gateway. In dev, vite proxies /api to the
// gateway URL configured via VITE_GATEWAY_URL.
const BASE_URL = "";

const TOKEN_KEY = "ehi.accessToken";
const REFRESH_KEY = "ehi.refreshToken";

export const tokenStore = {
  getAccess(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  getRefresh(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  },
  set(access: string, refresh: string) {
    localStorage.setItem(TOKEN_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

let onAuthFailure: (() => void) | null = null;
export function setOnAuthFailure(handler: () => void) {
  onAuthFailure = handler;
}

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Identity (X-User-Id / X-User-Role) is derived from the JWT and injected by
  // the gateway's IdentityHeaderFilter, which strips any client-sent values and
  // re-signs them. Sending them from the browser is pointless and spoofable, so
  // we deliberately do not. The signed JWT is the only identity the client owns.
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
      "/api/iam/auth/refresh",
      { refreshToken: refresh },
      { headers: { "Content-Type": "application/json" } }
    );
    const { accessToken, refreshToken: newRefresh } = res.data as {
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
  (res) => res,
  async (err: AxiosError) => {
    const original = err.config as AxiosRequestConfig & { _retried?: boolean };
    const status = err.response?.status;

    // Refresh-once on 401 (skip the refresh endpoint itself)
    const isAuthEndpoint =
      typeof original?.url === "string" &&
      original.url.includes("/api/iam/auth/");
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

// Friendly error message extraction. Spring services typically return
// { message: "...", error: "...", status: 400 } or a problem-details body.
export function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as
      | { message?: string; error?: string; detail?: string }
      | undefined;
    return (
      data?.message ||
      data?.detail ||
      data?.error ||
      err.message ||
      "Request failed"
    );
  }
  return (err as Error)?.message || "Unexpected error";
}
