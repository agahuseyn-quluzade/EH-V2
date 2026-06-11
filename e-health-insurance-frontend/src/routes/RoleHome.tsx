import { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Loader } from "../components/Loader";

// Routes a logged-in user to the right "home" page for their role. Mounted at "/".
export function RoleHome({
  member,
}: {
  member: ReactNode;
}) {
  const { user, loading } = useAuth();
  if (loading) return <Loader />;
  if (!user) return <Navigate to="/login" replace />;
  if (user.role === "STAFF") return <Navigate to="/staff" replace />;
  if (user.role === "ADMIN") return <Navigate to="/admin" replace />;
  return <>{member}</>;
}
