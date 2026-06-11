// Install a custom axios adapter that intercepts every request and routes
// it through the mock handler table. Imported only when
// VITE_USE_MOCKS === "true"; in normal mode this code never runs.

import type { AxiosAdapter, AxiosResponse } from "axios";
import { api } from "../client";
import { handle, MockRequest } from "./handlers";

const MOCK_LATENCY_MS = 250;

function parseBody(config: { data?: unknown; headers?: Record<string, unknown> }): unknown {
  const data = config.data;
  if (data == null) return undefined;

  // FormData (multipart uploads). Pull the first file out so handlers can
  // record file metadata without needing a real upload pipeline.
  if (data instanceof FormData) {
    const out: Record<string, unknown> = {};
    data.forEach((value, key) => {
      if (value instanceof File) {
        out[key] = {
          name: value.name,
          size: value.size,
          type: value.type,
        };
      } else {
        out[key] = value;
      }
    });
    return out;
  }
  if (typeof data === "string") {
    try {
      return JSON.parse(data);
    } catch {
      return data;
    }
  }
  return data;
}

function buildHeaders(raw: unknown): Record<string, string> {
  const out: Record<string, string> = {};
  if (!raw || typeof raw !== "object") return out;
  for (const [k, v] of Object.entries(raw as Record<string, unknown>)) {
    if (v == null) continue;
    out[k] = String(v);
  }
  return out;
}

const mockAdapter: AxiosAdapter = async (config) => {
  const method = (config.method || "get").toUpperCase();
  const url = config.url || "";
  // Strip baseURL if present.
  let pathname = url;
  let search = "";
  const qIdx = pathname.indexOf("?");
  if (qIdx >= 0) {
    search = pathname.slice(qIdx + 1);
    pathname = pathname.slice(0, qIdx);
  }

  const req: MockRequest = {
    method,
    url,
    pathname,
    search: new URLSearchParams(search),
    params: {},
    headers: buildHeaders(config.headers),
    body: parseBody(config),
    auth: null,
  };

  // Tiny artificial latency so loading states are visible.
  await new Promise((r) => setTimeout(r, MOCK_LATENCY_MS));

  const result = await handle(req);

  const isError = "error" in result;
  const errorMsg = isError ? (result as { error: string }).error : "";
  const data = isError ? { message: errorMsg } : (result as { data?: unknown }).data;

  const response: AxiosResponse = {
    data,
    status: result.status,
    statusText: result.status >= 400 ? "MOCK_ERROR" : "OK",
    headers: {},
    config,
    request: {},
  };

  if (result.status >= 400) {
    // Axios treats non-2xx as rejection. Surface a real AxiosError-ish object.
    const err = new Error(errorMsg || "Mock error") as Error & {
      response?: AxiosResponse;
      isAxiosError?: boolean;
      config?: unknown;
    };
    err.isAxiosError = true;
    err.response = response;
    err.config = config;
    throw err;
  }
  return response;
};

export function installMocks() {
  api.defaults.adapter = mockAdapter;
  if (typeof window !== "undefined") {
    // Tiny banner so it's obvious we're in mock mode.
    queueMicrotask(() => {
      const tag = document.createElement("div");
      tag.textContent = "MOCK MODE";
      tag.style.cssText =
        "position:fixed;bottom:8px;left:8px;background:#0c447c;color:#fff;font:600 10px/1 system-ui;padding:4px 8px;border-radius:6px;letter-spacing:0.08em;z-index:9999;opacity:0.85;pointer-events:none;";
      document.body.appendChild(tag);
    });
    // eslint-disable-next-line no-console
    console.info(
      "%c[mocks] All API calls are being served from in-memory data. Set VITE_USE_MOCKS=false to use the real backend.",
      "color:#185fa5"
    );
  }
}
