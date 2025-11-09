import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import "./index.css";
import "react-toastify/dist/ReactToastify.css";

import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import Leaderboard from "./components/Leaderboard";
import { ToastContainer } from "react-toastify";

import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import StudentDashboard from "./pages/student/StudentDashboard";
import TeacherDashboard from "./pages/teacher/TeacherDashboard";
import AdminDashboard from "./pages/admin/AdminDashboard";
import QuizHome from "./pages/QuizHome";
import QuizPage from "./pages/QuizPage";
import QuizGenerator from "./pages/QuizGenerator";

function Layout() {
  const location = useLocation();
  const hideNavbarRoutes = ["/login", "/register"];

  return (
    <>
      {!hideNavbarRoutes.includes(location.pathname) && <Navbar />}
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/student/dashboard"
          element={
            <ProtectedRoute requiredRoles={["ROLE_STUDENT"]}>
              <StudentDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/quizzes"
          element={
            <ProtectedRoute requiredRoles={["ROLE_STUDENT"]}>
              <QuizHome />
            </ProtectedRoute>
          }
        />
        <Route
          path="/quiz"
          element={
            <ProtectedRoute requiredRoles={["ROLE_STUDENT"]}>
              <QuizPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/quiz/start"
          element={
            <ProtectedRoute requiredRoles={["ROLE_STUDENT"]}>
              <QuizPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/quiz/generate"
          element={
            <ProtectedRoute requiredRoles={["ROLE_STUDENT"]}>
              <QuizGenerator />
            </ProtectedRoute>
          }
        />
        <Route
          path="/leaderboard"
          element={
            <ProtectedRoute>
              <Leaderboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/teacher/dashboard"
          element={
            <ProtectedRoute requiredRoles={["ROLE_TEACHER"]}>
              <TeacherDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/dashboard"
          element={
            <ProtectedRoute requiredRoles={["ROLE_ADMIN"]}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/student/dashboard" replace />} />
      </Routes>
    </>
  );
}

// ✅ No export — just render the app
ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <Layout />
      <ToastContainer position="bottom-right" autoClose={3000} />
    </BrowserRouter>
  </React.StrictMode>
);
