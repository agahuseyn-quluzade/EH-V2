import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useNavigate } from "react-router-dom";
import { iamApi, setOnAuthFailure, tokenStore } from "../api";
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  Role,
  UserProfile,
} from "../types";

interface AuthState {
  user: UserProfile | null;
  role: Role | null;
  loading: boolean;
  login: (body: LoginRequest) => Promise<AuthResponse>;
  register: (body: RegisterRequest) => Promise<AuthResponse>;
  logout: () => void;
  refreshMe: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const refreshMe = useCallback(async () => {
    if (!tokenStore.getAccess()) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const me = await iamApi.me();
      setUser(me);
    } catch {
      setUser(null);
      tokenStore.clear();
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshMe();
  }, [refreshMe]);

  useEffect(() => {
    setOnAuthFailure(() => {
      setUser(null);
      navigate("/login", { replace: true });
    });
  }, [navigate]);

  const login = useCallback(async (body: LoginRequest) => {
    const res = await iamApi.login(body);
    tokenStore.set(res.accessToken, res.refreshToken);
    const me = await iamApi.me();
    setUser(me);
    return res;
  }, []);

  const register = useCallback(async (body: RegisterRequest) => {
    const res = await iamApi.register(body);
    tokenStore.set(res.accessToken, res.refreshToken);
    const me = await iamApi.me();
    setUser(me);
    return res;
  }, []);

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    navigate("/login", { replace: true });
  }, [navigate]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      role: user?.role ?? null,
      loading,
      login,
      register,
      logout,
      refreshMe,
    }),
    [user, loading, login, register, logout, refreshMe]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
