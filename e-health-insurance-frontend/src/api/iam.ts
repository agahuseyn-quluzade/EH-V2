import { api } from "./client";
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  UpdateUserRequest,
  UserProfile,
  Role,
} from "../types";

// IAM is mounted under `/api/v1/auth` and `/api/v1/users`. The gateway
// rewrites `/api/iam/(.*)` → `/api/v1/$1`, so we omit `/api/v1` here.
const ROOT = "/api/iam";

export const iamApi = {
  register: (body: RegisterRequest) =>
    api.post<AuthResponse>(`${ROOT}/auth/register`, body).then((r) => r.data),

  login: (body: LoginRequest) =>
    api.post<AuthResponse>(`${ROOT}/auth/login`, body).then((r) => r.data),

  refresh: (refreshToken: string) =>
    api
      .post<TokenResponse>(`${ROOT}/auth/refresh`, { refreshToken })
      .then((r) => r.data),

  me: () => api.get<UserProfile>(`${ROOT}/users/me`).then((r) => r.data),

  updateMe: (body: UpdateUserRequest) =>
    api.put<UserProfile>(`${ROOT}/users/me`, body).then((r) => r.data),

  getUser: (id: string) =>
    api.get<UserProfile>(`${ROOT}/users/${id}`).then((r) => r.data),

  changeRole: (id: string, role: Role) =>
    api
      .patch<UserProfile>(`${ROOT}/users/${id}/role`, { role })
      .then((r) => r.data),
};
