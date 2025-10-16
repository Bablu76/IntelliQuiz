import React, { useState, useEffect, useRef } from "react";
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip } from "recharts";
import useAuth from "../../hooks/useAuth";
import ResourceUpload from "../../components/ResourceUpload";
import { useNavigate } from "react-router-dom";

export default function StudentDashboard() {
  const { fetchWithAuth, userId, logout } = useAuth();
  const [activeTab, setActiveTab] = useState("performance");
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const fetchInitiated = useRef(false);
  const navigate = useNavigate();

  const fetchAnalytics = async () => {
    try {
      const res = await fetchWithAuth(`http://localhost:8080/analytics/student/${userId}`);
      if (res.status === 401) {
        logout();
        return;
      }
      if (!res.ok) throw new Error(`Failed to fetch analytics: ${res.status}`);
      const data = await res.json();
      const chartData = Array.isArray(data.trend)
        ? data.trend.map((score, index) => ({ quiz: index + 1, score: Number(score) }))
        : [];
      const avgScore =
        chartData.length > 0
          ? chartData.reduce((sum, item) => sum + item.score, 0) / chartData.length
          : 0;
      setAnalytics({
        studentId: data.studentId || userId,
        accuracy: typeof data.accuracy === "string" ? data.accuracy : `${data.accuracy || 0}%`,
        totalQuizzes: Number(data.totalQuizzes || 0),
        badges: Array.isArray(data.badges) ? data.badges : [],
        chartData,
        averageScore: avgScore,
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const token = localStorage.getItem("token");
    const uid = localStorage.getItem("userId");
    if (!token || !uid) {
      logout();
      return;
    }
    if (fetchInitiated.current) return;
    fetchInitiated.current = true;
    fetchAnalytics();
  }, []);

  const tabs = [
    { id: "learning", label: "📚 My Learning" },
    { id: "quizzes", label: "🧩 My Quizzes" },
    { id: "performance", label: "📈 My Performance" },
    { id: "leaderboard", label: "🏆 Leaderboard" },
  ];

  const renderContent = () => {
    if (activeTab === "learning") {
      return (
        <div className="bg-white p-6 rounded-xl shadow-md border border-gray-200">
          <h2 className="text-lg font-semibold mb-4 text-gray-800">📚 Upload Your Resources</h2>
          <ResourceUpload />
        </div>
      );
    }

    if (activeTab === "quizzes") {
      return (
        <div className="bg-white p-6 rounded-xl shadow-md border border-gray-200">
          <h2 className="text-lg font-semibold mb-4 text-gray-800">🧩 Available Quizzes</h2>
          <p className="text-gray-600 text-sm mb-4">
            Practice quizzes generated from your resources or topics of interest.
          </p>
          <div className="flex flex-col gap-3">
            <div className="border p-4 rounded-lg flex justify-between items-center bg-gray-50">
              <span>AI Basics – Medium Difficulty</span>
              <button
                onClick={() => navigate("/quiz")}
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm"
              >
                Start Quiz
              </button>
            </div>
            <div className="border p-4 rounded-lg flex justify-between items-center bg-gray-50">
              <span>Machine Learning – Hard Difficulty</span>
              <button
                onClick={() => navigate("/quiz")}
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm"
              >
                Start Quiz
              </button>
            </div>
          </div>
        </div>
      );
    }

    if (activeTab === "leaderboard") {
      return (
        <div className="bg-white p-6 rounded-xl shadow-md border border-gray-200 text-center">
          <h2 className="text-lg font-semibold mb-4 text-gray-800">🏆 Leaderboard</h2>
          <p className="text-gray-600 mb-3">See how you rank among others!</p>
          <button
            onClick={() => navigate("/leaderboard")}
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md"
          >
            Go to Leaderboard
          </button>
        </div>
      );
    }

    if (loading) {
      return <p className="text-gray-500 text-center mt-10">📊 Loading analytics...</p>;
    }
    if (error) {
      return <p className="text-red-600 text-center mt-10">❌ {error}</p>;
    }
    if (!analytics) {
      return <p className="text-gray-400 text-center mt-10">No analytics yet</p>;
    }

    // PERFORMANCE TAB
    return (
      <div className="bg-white p-6 rounded-xl shadow-md border border-gray-200">
        <h2 className="text-lg font-semibold mb-4 text-gray-800">📈 Performance Overview</h2>
        {analytics.chartData.length > 0 ? (
          <LineChart width={800} height={300} data={analytics.chartData}>
            <Line type="monotone" dataKey="score" stroke="#2563eb" strokeWidth={3} />
            <CartesianGrid stroke="#e5e7eb" strokeDasharray="3 3" />
            <XAxis dataKey="quiz" />
            <YAxis domain={[0, 100]} />
            <Tooltip />
          </LineChart>
        ) : (
          <p className="text-gray-500 text-sm">No trend data yet</p>
        )}
        <div className="mt-6 flex justify-around">
          <div>
            <p className="text-gray-500 text-sm">Accuracy</p>
            <p className="text-xl font-bold text-blue-600">{analytics.accuracy}</p>
          </div>
          <div>
            <p className="text-gray-500 text-sm">Total Quizzes</p>
            <p className="text-xl font-bold text-green-600">{analytics.totalQuizzes}</p>
          </div>
          <div>
            <p className="text-gray-500 text-sm">Average Score</p>
            <p className="text-xl font-bold text-purple-600">
              {analytics.averageScore.toFixed(1)}%
            </p>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-6">
      <div className="max-w-6xl mx-auto bg-white rounded-2xl shadow-lg border border-gray-200 p-6">
        {/* Header */}
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-800">🎓 Student Dashboard</h1>
          <button
            onClick={logout}
            className="bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-5 rounded-lg text-sm"
          >
            Logout
          </button>
        </div>

        {/* Tabs */}
        <div className="flex gap-3 mb-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                activeTab === tab.id
                  ? "bg-blue-600 text-white shadow"
                  : "bg-gray-100 hover:bg-gray-200 text-gray-700"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        {renderContent()}
      </div>
    </div>
  );
}
