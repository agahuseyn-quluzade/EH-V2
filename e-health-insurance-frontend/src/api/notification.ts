import { api } from "./client";
import { Notification } from "../types";

const ROOT = "/api/notification/notifications";

export const notificationApi = {
  mine: () => api.get<Notification[]>(`${ROOT}/me`).then((r) => r.data),

  sendEmail: (body: {
    recipientId: string;
    recipientEmail: string;
    template: string;
    subject: string;
    templateVariables?: Record<string, unknown>;
  }) => api.post<Notification>(`${ROOT}/email`, body).then((r) => r.data),
};
