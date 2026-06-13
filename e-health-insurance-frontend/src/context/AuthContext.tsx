import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { iamApi } from "../api/iam";
import {
  decodeJwt,
  setOnAuthFailure,
  tokenStore,
  userInfoStore,
} from "../api/client";
import { LoginRequest, RegisterRequest, Role, UserProfile } from "../types";

interface AuthContextValue {
  user: UserProfile | null;
  role: Role | null;
  isAuthenticated: boolean;
  initializing: boolean;
  login: (body: LoginRequest) => Promise<Role>;
  register: (body: RegisterRequest) => Promise<Role>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [role, setRole] = useState<Role | null>(
    (userInfoStore.getRole() as Role) || null
  );
  const [initializing, setInitializing] = useState(true);

  const clearSession = useCallback(() => {
    tokenStore.clear();
    userInfoStore.clear();
    setUser(null);
    setRole(null);
  }, []);

  useEffect(() => {
    setOnAuthFailure(() => {
      clearSession();
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    });
  }, [clearSession]);

  useEffect(() => {
    const token = tokenStore.getAccess();
    if (!token) {
      setInitializing(false);
      return;
    }
    iamApi
      .me()
      .then((profile) => {
        setUser(profile);
        setRole(profile.role);
        userInfoStore.set(profile.id, profile.role);
      })
      .catch(() => {
      })
      .finally(() => setInitializing(false));
  }, []);

  const applyTokens = useCallback(
    async (accessToken: string, refreshToken: string): Promise<Role> => {
      tokenStore.set(accessToken, refreshToken);
      const claims = decodeJwt(accessToken);
      const claimRole = (claims.role as Role) || "CUSTOMER";
      if (claims.userId) userInfoStore.set(claims.userId, claimRole);
      setRole(claimRole);
      try {
        const profile = await iamApi.me();
        setUser(profile);
        setRole(profile.role);
        userInfoStore.set(profile.id, profile.role);
        return profile.role;
      } catch {
        return claimRole;
      }
    },
    []
  );

  const login = useCallback(
    async (body: LoginRequest) => {
      const tokens = await iamApi.login(body);
      return applyTokens(tokens.accessToken, tokens.refreshToken);
    },
    [applyTokens]
  );

  const register = useCallback(
    async (body: RegisterRequest) => {
      const tokens = await iamApi.register(body);
      return applyTokens(tokens.accessToken, tokens.refreshToken);
    },
    [applyTokens]
  );

  const logout = useCallback(() => {
    clearSession();
  }, [clearSession]);

  const refreshProfile = useCallback(async () => {
    const profile = await iamApi.me();
    setUser(profile);
    setRole(profile.role);
    userInfoStore.set(profile.id, profile.role);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      role,
      isAuthenticated: !!tokenStore.getAccess(),
      initializing,
      login,
      register,
      logout,
      refreshProfile,
    }),
    [user, role, initializing, login, register, logout, refreshProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}

export function homePathForRole(role: Role | null): string {
  switch (role) {
    case "ADMIN":
      return "/admin";
    case "AGENT":
      return "/staff";
    case "CUSTOMER":
      return "/dashboard";
    default:
      return "/";
  }
}
