import React, { useMemo } from "react";
import { Navigate, useLocation } from "react-router-dom";
import useAuth from "../hooks/useAuth";

export default function ProtectedRoute({ children, requiredRoles = [] }) {
  const auth = useAuth();
  const location = useLocation();

  // 0. Wait until hook fully resolves (prevents flash of redirect)
  const ready = useMemo(() => auth.isAuthenticated !== undefined, [auth.isAuthenticated]);
  if (!ready) return null; // Avoid premature render

  // 1. Not authenticated
  if (!auth.isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 2. Token expired or invalid
if (!auth.tokenValid()) {
  console.warn("Token invalid or expired → logging out...");
  auth.logout();
  return <Navigate to="/login" state={{ from: location, expired: true }} replace />;
}


  // 3. Role check
  if (Array.isArray(requiredRoles) && requiredRoles.length > 0) {
    const hasRole = requiredRoles.some((r) => auth.roles.includes(r));
    if (!hasRole) {
      if (auth.roles.includes("ROLE_ADMIN")) return <Navigate to="/admin/dashboard" replace />;
      if (auth.roles.includes("ROLE_TEACHER")) return <Navigate to="/teacher/dashboard" replace />;
      if (auth.roles.includes("ROLE_STUDENT")) return <Navigate to="/student/dashboard" replace />;
      return <Navigate to="/login" replace />;
    }
  }

  return children;
}
