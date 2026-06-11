import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./layouts/AppLayout";
import { AuthLayout } from "./layouts/AuthLayout";
import { LoginPage } from "./pages/public/LoginPage";
import { RegisterPage } from "./pages/public/RegisterPage";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { RoleHome } from "./routes/RoleHome";

// Member
import { MemberDashboard } from "./pages/member/MemberDashboard";
import { PlansPage } from "./pages/member/PlansPage";
import { MyPolicyPage } from "./pages/member/MyPolicyPage";
import { PaymentsPage } from "./pages/member/PaymentsPage";
import { HealthRecordPage } from "./pages/member/HealthRecordPage";
import { MyClaimsPage } from "./pages/member/MyClaimsPage";
import { NewClaimPage } from "./pages/member/NewClaimPage";
import { ClaimDetailPage } from "./pages/member/ClaimDetailPage";
import { ChatPage } from "./pages/member/ChatPage";
import { NotificationsPage } from "./pages/member/NotificationsPage";
import { ProfilePage } from "./pages/member/ProfilePage";

// Staff
import { StaffReviewQueuePage } from "./pages/staff/StaffReviewQueuePage";
import { StaffClaimsPage } from "./pages/staff/StaffClaimsPage";
import { StaffMembersPage } from "./pages/staff/StaffMembersPage";

// Admin
import { AdminOverviewPage } from "./pages/admin/AdminOverviewPage";
import { AdminPlansPage } from "./pages/admin/AdminPlansPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";

export default function App() {
  return (
    <Routes>
      {/* Public auth routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Protected app */}
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        {/* Home dispatches by role */}
        <Route index element={<RoleHome member={<MemberDashboard />} />} />

        {/* Member routes */}
        <Route path="/plans" element={<PlansPage />} />
        <Route path="/policy" element={<MyPolicyPage />} />
        <Route path="/payments" element={<PaymentsPage />} />
        <Route path="/health-record" element={<HealthRecordPage />} />
        <Route path="/claims" element={<MyClaimsPage />} />
        <Route path="/claims/new" element={<NewClaimPage />} />
        <Route path="/claims/:id" element={<ClaimDetailPage />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/profile" element={<ProfilePage />} />

        {/* Staff routes */}
        <Route
          path="/staff"
          element={
            <ProtectedRoute roles={["STAFF", "ADMIN"]}>
              <StaffReviewQueuePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/staff/claims"
          element={
            <ProtectedRoute roles={["STAFF", "ADMIN"]}>
              <StaffClaimsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/staff/claims/:id"
          element={
            <ProtectedRoute roles={["STAFF", "ADMIN"]}>
              <ClaimDetailPage staffView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/staff/members"
          element={
            <ProtectedRoute roles={["STAFF", "ADMIN"]}>
              <StaffMembersPage />
            </ProtectedRoute>
          }
        />

        {/* Admin routes */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminOverviewPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/plans"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminPlansPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminUsersPage />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
