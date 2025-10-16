import { Link, useNavigate, useLocation } from "react-router-dom";
import useAuth from "../hooks/useAuth";

export default function Navbar() {
  const { logout, roles } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const isActive = (path) => location.pathname === path;
  const has = (role) => roles?.includes(role);

  return (
    <nav className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
        {/* Logo */}
        <div className="flex items-center gap-3">
          <div className="bg-white text-blue-600 w-10 h-10 rounded-lg flex items-center justify-center text-2xl font-bold shadow-md">
            🧠
          </div>
          <div>
            <h1 className="font-bold text-xl tracking-tight">IntelliQuiz</h1>
            <p className="text-xs text-blue-200">Smart Learning Platform</p>
          </div>
        </div>

        {/* Links */}
        <div className="flex items-center gap-2">
          {has("ROLE_STUDENT") && (
            <>
              <Link
                to="/student/dashboard"
                className={`px-4 py-2 rounded-lg font-medium transition-all ${
                  isActive("/student/dashboard")
                    ? "bg-white text-blue-600 shadow-md"
                    : "hover:bg-blue-500 hover:shadow-md"
                }`}
              >
                📊 Dashboard
              </Link>
              <Link
                to="/quiz"
                className={`px-4 py-2 rounded-lg font-medium transition-all ${
                  isActive("/quiz")
                    ? "bg-white text-blue-600 shadow-md"
                    : "hover:bg-blue-500 hover:shadow-md"
                }`}
              >
                📝 Quiz
              </Link>
              <Link
                to="/leaderboard"
                className={`px-4 py-2 rounded-lg font-medium transition-all ${
                  isActive("/leaderboard")
                    ? "bg-white text-blue-600 shadow-md"
                    : "hover:bg-blue-500 hover:shadow-md"
                }`}
              >
                🏆 Leaderboard
              </Link>
            </>
          )}

          {has("ROLE_TEACHER") && (
            <Link
              to="/teacher/dashboard"
              className={`px-4 py-2 rounded-lg font-medium transition-all ${
                isActive("/teacher/dashboard")
                  ? "bg-white text-blue-600 shadow-md"
                  : "hover:bg-blue-500 hover:shadow-md"
              }`}
            >
              🧑‍🏫 Teacher
            </Link>
          )}

          {has("ROLE_ADMIN") && (
            <Link
              to="/admin/dashboard"
              className={`px-4 py-2 rounded-lg font-medium transition-all ${
                isActive("/admin/dashboard")
                  ? "bg-white text-blue-600 shadow-md"
                  : "hover:bg-blue-500 hover:shadow-md"
              }`}
            >
              ⚙️ Admin
            </Link>
          )}

          <button
            onClick={handleLogout}
            className="ml-4 bg-red-500 hover:bg-red-600 px-5 py-2 rounded-lg font-medium shadow-md hover:shadow-lg transition-all"
          >
            🚪 Logout
          </button>
        </div>
      </div>
    </nav>
  );
}
