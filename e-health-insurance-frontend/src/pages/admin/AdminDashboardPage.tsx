import { Link } from "react-router-dom";

export function AdminDashboardPage() {
  return (
    <>
      <div className="page-header">
        <div>
          <h1>Admin Dashboard</h1>
          <p>Platform overview</p>
        </div>
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <h2>Quick links</h2>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <Link to="/admin/plans" className="btn btn-primary">
            📋 Plan Management
          </Link>
          <Link to="/admin/policies" className="btn btn-secondary">
            🛡️ Policies
          </Link>
          <Link to="/admin/users" className="btn btn-secondary">
            👥 Users
          </Link>
        </div>
      </div>
    </>
  );
}
