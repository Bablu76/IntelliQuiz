import { useState, useEffect } from "react";

const Leaderboard = () => {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const API_BASE = import.meta.env.VITE_API_URL;

  useEffect(() => {
    const fetchLeaderboard = async () => {
      try {
        setLoading(true);
        const token = localStorage.getItem("token");
        const res = await fetch(`${API_BASE}/analytics/leaderboard?limit=10`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (!res.ok) throw new Error(`Failed (${res.status})`);
        const data = await res.json();

        // Filter only STUDENTS
        const filtered = data.filter(
          (u) =>
            Array.isArray(u.roles) &&
            u.roles.some((r) => String(r).toUpperCase().includes("STUDENT"))
        );

        const sorted = filtered
          .sort((a, b) => (b.points || 0) - (a.points || 0))
          .map((s, i) => ({
            ...s,
            rank: i + 1,
            name: s.username || "Anonymous",
            score: s.points || 0,
          }));

        setStudents(sorted);
        setError(null);
      } catch (err) {
        console.error("❌ Leaderboard fetch error:", err);
        setError(err.message);
        setStudents([]);
      } finally {
        setLoading(false);
      }
    };

    fetchLeaderboard();
  }, [API_BASE]);

  if (loading)
    return (
      <div className="flex justify-center items-center h-screen bg-gradient-to-br from-indigo-50 to-blue-100">
        <div className="animate-spin h-10 w-10 border-b-2 border-blue-600 rounded-full"></div>
        <p className="ml-4 text-gray-600 font-medium">Loading leaderboard...</p>
      </div>
    );

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-100 to-purple-100 py-10 px-6">
      <div className="max-w-4xl mx-auto bg-white/70 backdrop-blur-lg rounded-3xl shadow-xl overflow-hidden">
        <div className="bg-gradient-to-r from-blue-600 to-indigo-700 px-8 py-6 text-white">
          <h2 className="text-3xl font-bold tracking-tight flex items-center gap-3">
            🏆 IntelliQuiz Leaderboard
          </h2>
          <p className="text-blue-200 text-sm mt-1">
            Top {students.length} Students — Adaptive Learning Champions
          </p>
        </div>

        {error && (
          <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mx-6 my-4 text-yellow-800 rounded-md">
            ⚠️ Could not fetch data. Showing fallback results.
          </div>
        )}

        <div className="p-6 space-y-4">
          {students.length === 0 ? (
            <p className="text-center text-gray-500 py-6">No leaderboard data yet.</p>
          ) : (
            students.map((s) => (
              <div
                key={s.rank}
                className={`flex justify-between items-center rounded-xl p-4 transition-transform transform hover:scale-[1.02] ${
                  s.rank === 1
                    ? "bg-gradient-to-r from-yellow-100 to-yellow-50 border-l-4 border-yellow-400"
                    : s.rank === 2
                    ? "bg-gradient-to-r from-gray-100 to-gray-50 border-l-4 border-gray-300"
                    : s.rank === 3
                    ? "bg-gradient-to-r from-orange-100 to-orange-50 border-l-4 border-orange-400"
                    : "bg-white border border-gray-100"
                }`}
              >
                <div className="flex items-center gap-4">
                  <div className="text-2xl">
                    {s.rank === 1
                      ? "🥇"
                      : s.rank === 2
                      ? "🥈"
                      : s.rank === 3
                      ? "🥉"
                      : `#${s.rank}`}
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-800">{s.name}</h3>
                    <p className="text-xs text-gray-500">{s.badges || "No badges yet"}</p>
                  </div>
                </div>

                <div className="flex flex-col items-end">
                  <span className="text-lg font-bold text-blue-700">{s.score} pts</span>
                  <div className="w-32 bg-gray-200 h-2 rounded-full overflow-hidden mt-1">
                    <div
                      className="bg-blue-600 h-2 transition-all duration-700"
                      style={{ width: `${Math.min(s.score / 10, 100)}%` }}
                    />
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="bg-gray-50 text-center text-xs text-gray-500 py-3 border-t">
          Powered by IntelliQuiz • Adaptive Learning Gamified
        </div>
      </div>
    </div>
  );
};

export default Leaderboard;
