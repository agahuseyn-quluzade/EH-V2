import { api } from "./client";
import {
  ChangePasswordRequest,
  ChangeRoleRequest,
  ChangeStatusRequest,
  LoginRequest,
  RegisterRequest,
  SpringPage,
  TokenResponse,
  UpdateUserRequest,
  UserProfile,
} from "../types";

const ROOT = "/api/v1";

export const iamApi = {
  register: (body: RegisterRequest) =>
    api.post<TokenResponse>(`${ROOT}/auth/register`, body).then((r) => r.data),

  login: (body: LoginRequest) =>
    api.post<TokenResponse>(`${ROOT}/auth/login`, body).then((r) => r.data),

  me: () => api.get<UserProfile>(`${ROOT}/users/me`).then((r) => r.data),

  updateMe: (body: UpdateUserRequest) =>
    api.put<UserProfile>(`${ROOT}/users/me`, body).then((r) => r.data),

  getUser: (id: string) =>
    api.get<UserProfile>(`${ROOT}/users/${id}`).then((r) => r.data),

  changePassword: (body: ChangePasswordRequest) =>
    api.post<UserProfile>(`${ROOT}/users/me/password`, body).then((r) => r.data),

  changeRole: (id: string, body: ChangeRoleRequest) =>
    api.patch<UserProfile>(`${ROOT}/users/${id}/role`, body).then((r) => r.data),

  changeStatus: (id: string, body: ChangeStatusRequest) =>
    api.patch<UserProfile>(`${ROOT}/users/${id}/status`, body).then((r) => r.data),

  searchUsers: (query: string, page = 0, size = 20) =>
    api
      .get<SpringPage<UserProfile>>(
        `${ROOT}/users/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`
      )
      .then((r) => r.data),
};
