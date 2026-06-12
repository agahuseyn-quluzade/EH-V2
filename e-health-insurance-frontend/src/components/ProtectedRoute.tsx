import { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { homePathForRole, useAuth } from "../context/AuthContext";
import { Role } from "../types";
import { Spinner } from "./ui";

export function ProtectedRoute({
  roles,
  children,
}: {
  roles?: Role[];
  children: ReactNode;
}) {
  const { isAuthenticated, role, initializing } = useAuth();
  const location = useLocation();

  if (initializing) {
    return (
      <div className="page-center">
        <Spinner />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (roles && role && !roles.includes(role)) {
    return <Navigate to={homePathForRole(role)} replace />;
  }

  return <>{children}</>;
}
