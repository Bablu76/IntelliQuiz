import { useState } from "react";
import { useNavigate } from "react-router-dom";
import useAuth from "../hooks/useAuth";

export default function LoginPage() {
  const navigate = useNavigate();
  const auth = useAuth();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    console.log("🔐 Login Attempt Started");
    try {
      const response = await fetch("http://localhost:8080/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      const contentType = response.headers.get("content-type");
      const data = contentType?.includes("application/json")
        ? await response.json()
        : { message: await response.text() };

      console.log("📡 Response Status:", response.status);
      console.log("📦 Response Data:", data);

      if (response.ok && data.token) {
        console.log("✅ Login successful!");

        // 🔸 Ensure these keys exist
        const token = data.token;
        const roles = Array.isArray(data.roles) ? data.roles : [data.roles];
        const userId = data.id;

        // 🔸 Save to auth context + localStorage
        auth.saveAuth({ token, roles, userId });
        localStorage.setItem("username", data.username || username);
        localStorage.setItem("email", data.email || "");

        // 🧭 Unified role redirect logic
        const role =
          roles.includes("ROLE_ADMIN")
            ? "admin"
            : roles.includes("ROLE_TEACHER")
            ? "teacher"
            : roles.includes("ROLE_STUDENT")
            ? "student"
            : "user";

        console.log(`🔀 Redirecting to /dashboard (${role})`);

        // 🔸 Delay navigate slightly to let auth context persist
        let targetPath = "/login"; // fallback
        if (role === "admin") targetPath = "/admin/dashboard";
        else if (role === "teacher") targetPath = "/teacher/dashboard";
        else if (role === "student") targetPath = "/student/dashboard";

        setTimeout(() => navigate(targetPath, { replace: true }), 150);

      } else {
        console.error("❌ Login failed:", response.status);
        if (response.status === 401) setError("Invalid username or password");
        else if (response.status === 403) setError("Account is disabled or locked");
        else if (response.status === 500) setError("Server error. Please try again later");
        else setError(data.message || `Login failed with status ${response.status}`);
      }
    } catch (err) {
      console.error("❌ Network or Fetch Error:", err);
      if (err.message.includes("Failed to fetch"))
        setError("Cannot connect to backend. Is it running?");
      else if (err.message.includes("NetworkError"))
        setError("Network error. Check backend CORS config.");
      else setError("Unexpected error: " + err.message);
    } finally {
      setLoading(false);
      console.log("🏁 Login attempt completed");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-100 to-purple-100 p-6">
      <div className="bg-white shadow-lg rounded-lg p-8 w-full max-w-md">
        <h2 className="text-3xl font-bold text-center text-gray-800 mb-6">
          🔐 IntelliQuiz Login
        </h2>

        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-300 rounded">
            <p className="text-red-700 text-sm font-medium">Error:</p>
            <p className="text-red-600 text-sm mt-1">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-700 font-medium mb-1">
              Username
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              placeholder="Enter username"
              required
              disabled={loading}
            />
          </div>

          <div>
            <label className="block text-gray-700 font-medium mb-1">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              placeholder="Enter password"
              required
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className={`w-full py-2 px-4 rounded-md text-white font-semibold transition-colors ${
              loading
                ? "bg-gray-400 cursor-not-allowed"
                : "bg-blue-600 hover:bg-blue-700"
            }`}
          >
            {loading ? "🔄 Logging in..." : "Login"}
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-gray-600 text-sm">
            Don't have an account?{" "}
            <button
              onClick={() => navigate("/register")}
              className="text-blue-600 hover:text-blue-700 font-semibold hover:underline focus:outline-none"
              type="button"
              disabled={loading}
            >
              Register here
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
