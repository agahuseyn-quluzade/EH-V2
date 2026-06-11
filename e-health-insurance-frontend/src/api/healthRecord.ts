import { api } from "./client";
import {
  AddLabResultRequest,
  AddMedicalEntryRequest,
  AddPrescriptionRequest,
  HealthSummary,
  LabResult,
  MedicalEntry,
  Prescription,
  RecordStatus,
} from "../types";

// Gateway rewrites /api/health-record/(.*) → /api/v1/$1.
// Upstream HealthRecordController is mounted at /api/v1/records.
const ROOT = "/api/health-record/records";

export const healthRecordApi = {
  // Returns 404 until the member creates a record; callers handle that.
  myRecord: () =>
    api.get<HealthSummary>(`${ROOT}/me`).then((r) => r.data),

  createMyRecord: () =>
    api.post<HealthSummary>(`${ROOT}/me`).then((r) => r.data),

  updateMyStatus: (status: RecordStatus) =>
    api
      .patch<HealthSummary>(`${ROOT}/me/status`, { status })
      .then((r) => r.data),

  // ── Medical entries ────────────────────────────────────────────────────────
  addEntry: (body: AddMedicalEntryRequest) =>
    api.post<MedicalEntry>(`${ROOT}/me/entries`, body).then((r) => r.data),

  listEntries: () =>
    api.get<MedicalEntry[]>(`${ROOT}/me/entries`).then((r) => r.data),

  // ── Prescriptions ──────────────────────────────────────────────────────────
  addPrescription: (body: AddPrescriptionRequest) =>
    api.post<Prescription>(`${ROOT}/me/prescriptions`, body).then((r) => r.data),

  listPrescriptions: () =>
    api.get<Prescription[]>(`${ROOT}/me/prescriptions`).then((r) => r.data),

  // ── Lab results ────────────────────────────────────────────────────────────
  addLabResult: (body: AddLabResultRequest) =>
    api.post<LabResult>(`${ROOT}/me/lab-results`, body).then((r) => r.data),

  listLabResults: () =>
    api.get<LabResult[]>(`${ROOT}/me/lab-results`).then((r) => r.data),
};
