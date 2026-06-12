import { api } from "./client";
import { Notification } from "../types";

const ROOT = "/api/v1/notifications";

export const notificationApi = {
  mine: () => api.get<Notification[]>(`${ROOT}/me`).then((r) => r.data),
};
