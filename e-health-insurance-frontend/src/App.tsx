import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { homePathForRole, useAuth } from "./context/AuthContext";
import { LandingPage } from "./pages/public/LandingPage";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { DashboardPage } from "./pages/member/DashboardPage";
import { PlansPage } from "./pages/member/PlansPage";
import { PolicyPage } from "./pages/member/PolicyPage";
import { ClaimsPage } from "./pages/member/ClaimsPage";
import { NewClaimPage } from "./pages/member/NewClaimPage";
import { ClaimDetailPage } from "./pages/member/ClaimDetailPage";
import { ChatPage } from "./pages/member/ChatPage";
import { NotificationsPage } from "./pages/member/NotificationsPage";
import { ProfilePage } from "./pages/member/ProfilePage";
import { StaffDashboardPage } from "./pages/staff/StaffDashboardPage";
import { StaffQueuePage } from "./pages/staff/StaffQueuePage";
import { StaffClaimReviewPage } from "./pages/staff/StaffClaimReviewPage";
import { StaffMembersPage } from "./pages/staff/StaffMembersPage";
import { AdminDashboardPage } from "./pages/admin/AdminDashboardPage";
import { AdminPlansPage } from "./pages/admin/AdminPlansPage";
import { AdminPoliciesPage } from "./pages/admin/AdminPoliciesPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";

function Protected({
  roles,
  children,
}: {
  roles?: Array<"CUSTOMER" | "AGENT" | "ADMIN">;
  children: JSX.Element;
}) {
  return (
    <ProtectedRoute roles={roles}>
      <Layout>{children}</Layout>
    </ProtectedRoute>
  );
}

export default function App() {
  const { role } = useAuth();

  return (
    <Routes>
      {}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {}
      <Route path="/dashboard" element={<Protected roles={["CUSTOMER"]}><DashboardPage /></Protected>} />
      <Route path="/plans" element={<Protected roles={["CUSTOMER"]}><PlansPage /></Protected>} />
      <Route path="/policy" element={<Protected roles={["CUSTOMER"]}><PolicyPage /></Protected>} />
      <Route path="/claims" element={<Protected roles={["CUSTOMER"]}><ClaimsPage /></Protected>} />
      <Route path="/claims/new" element={<Protected roles={["CUSTOMER"]}><NewClaimPage /></Protected>} />
      <Route path="/claims/:id" element={<Protected><ClaimDetailPage /></Protected>} />
      <Route path="/chat" element={<Protected roles={["CUSTOMER"]}><ChatPage /></Protected>} />
      <Route path="/notifications" element={<Protected><NotificationsPage /></Protected>} />
      <Route path="/profile" element={<Protected><ProfilePage /></Protected>} />

      {}
      <Route path="/staff" element={<Protected roles={["AGENT", "ADMIN"]}><StaffDashboardPage /></Protected>} />
      <Route path="/staff/queue" element={<Protected roles={["AGENT"]}><StaffQueuePage /></Protected>} />
      <Route path="/staff/claims/:id" element={<Protected roles={["AGENT"]}><StaffClaimReviewPage /></Protected>} />
      <Route path="/staff/members" element={<Protected roles={["AGENT", "ADMIN"]}><StaffMembersPage /></Protected>} />

      {}
      <Route path="/admin" element={<Protected roles={["ADMIN"]}><AdminDashboardPage /></Protected>} />
      <Route path="/admin/plans" element={<Protected roles={["ADMIN"]}><AdminPlansPage /></Protected>} />
      <Route path="/admin/policies" element={<Protected roles={["ADMIN"]}><AdminPoliciesPage /></Protected>} />
      <Route path="/admin/users" element={<Protected roles={["ADMIN"]}><AdminUsersPage /></Protected>} />

      <Route path="*" element={<Navigate to={homePathForRole(role)} replace />} />
    </Routes>
  );
}
