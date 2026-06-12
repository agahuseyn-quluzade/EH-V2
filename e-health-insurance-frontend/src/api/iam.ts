import { api } from "./client";
import {
  LoginRequest,
  RegisterRequest,
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
};
