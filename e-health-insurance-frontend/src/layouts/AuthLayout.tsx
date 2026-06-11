import { Outlet } from "react-router-dom";

export function AuthLayout() {
  return (
    <div className="auth-shell">
      <div className="auth-marketing">
        <div>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 12,
              fontSize: 16,
              fontWeight: 500,
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 10,
                background: "rgba(255,255,255,0.18)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 600,
              }}
            >
              H+
            </div>
            HealthAssure
          </div>
        </div>
        <div>
          <h1>Health insurance, simplified end-to-end.</h1>
          <p style={{ marginTop: 18 }}>
            Compare plans, submit claims with a photo of your bill, and get an
            answer fast. Our AI assistant explains your coverage in plain
            language — and our staff are here when you need them.
          </p>
        </div>
        <div style={{ fontSize: 12, color: "rgba(255,255,255,0.7)" }}>
          © {new Date().getFullYear()} HealthAssure Insurance
        </div>
      </div>
      <div className="auth-form-side">
        <Outlet />
      </div>
    </div>
  );
}
