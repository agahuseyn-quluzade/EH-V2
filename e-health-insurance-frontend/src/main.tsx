import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import "./styles/globals.css";

// Mock mode: when VITE_USE_MOCKS=true, swap the axios adapter so every API
// call is served from in-memory data — no backend required.
if (import.meta.env.VITE_USE_MOCKS === "true") {
  const { installMocks } = await import("./api/mocks");
  installMocks();
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  </React.StrictMode>
);
