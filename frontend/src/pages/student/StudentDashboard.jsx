import React, { useState, useEffect, useRef } from "react";
import {
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
} from "recharts";
import useAuth from "../../hooks/useAuth";
import ResourceUpload from "../../components/ResourceUpload";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { toast } from "react-toastify";
import { generateAIQuiz } from "../../utils/quizApi";
import { getSavedDifficulty, saveDifficulty } from "../../utils/adaptive";

export default function StudentDashboard() {
  const { fetchWithAuth, userId, logout } = useAuth();
  const [activeTab, setActiveTab] = useState("learning");
  const [analytics, setAnalytics] = useState(null);
  const [resources, setResources] = useState([]);
  const [quizAttempts, setQuizAttempts] = useState([]);
  const [quizPrompt, setQuizPrompt] = useState({
    visible: false,
    topic: "",
    difficulty: "medium",
    questionCount: 10,
  });

  const fetchInitiated = useRef(false);
  const navigate = useNavigate();
  const API_BASE = import.meta.env.VITE_API_URL;
  const token = localStorage.getItem("token");

  // 📊 Fetch Analytics
  const fetchAnalytics = async () => {
    try {
      const res = await fetchWithAuth(`${API_BASE}/analytics/student/${userId}`);
      if (res.status === 401) return logout();
      if (!res.ok) throw new Error(`Failed to fetch analytics: ${res.status}`);
      const data = await res.json();

      const chartData = Array.isArray(data.trend)
        ? data.trend.map((score, index) => ({
            quiz: index + 1,
            score: Number(score),
          }))
        : [];

      setAnalytics({
        accuracy:
          typeof data.accuracy === "string"
            ? data.accuracy
            : `${data.accuracy || 0}%`,
        totalQuizzes: data.totalQuizzes || 0,
        avgScore: Number(data.avgScore || 0),
        badges: Array.isArray(data.badges) ? data.badges : [],
        chartData,
      });
    } catch {
      toast.error("Failed to fetch analytics");
    }
  };

  // 📚 Fetch Resources
  const fetchResources = async () => {
    try {
      const res = await axios.get(`${API_BASE}/resources/list`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const fetched = Array.isArray(res.data) ? res.data : [];
      setResources(
        fetched.map((r) => ({
          ...r,
          adaptiveDifficulty: getSavedDifficulty(r.topic),
        }))
      );
    } catch {
      toast.error("Failed to load resources");
      setResources([]);
    }
  };

  // 🧩 Delete Resource
  const handleDeleteResource = async (id) => {
    if (!window.confirm("Delete this file?")) return;
    try {
      await axios.delete(`${API_BASE}/resources/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      toast.success("File deleted successfully");
      fetchResources();
    } catch {
      toast.error("Failed to delete file");
    }
  };

  // 📘 Fetch Quiz Attempts
  const fetchQuizAttempts = async () => {
    try {
      const res = await axios.get(`${API_BASE}/quiz/attempts/${userId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setQuizAttempts(Array.isArray(res.data) ? res.data : []);
    } catch {
      setQuizAttempts([]);
    }
  };

  // ====================== QUIZ HANDLERS ======================

  const handleGenerateClick = (topic) => {
    setQuizPrompt({
      visible: true,
      topic,
      difficulty: getSavedDifficulty(topic),
      questionCount: 10,
    });
  };

  const handleStartQuiz = async (topic, difficulty, questionCount) => {
    const trimmedTopic = topic.trim();
    if (!trimmedTopic) return toast.warn("Please provide a valid topic.");

    toast.info(`🤖 Generating ${questionCount} ${difficulty} questions for '${trimmedTopic}'...`);

    try {
      const resource = resources.find(
        (r) => r.topic?.toLowerCase() === trimmedTopic.toLowerCase()
      );

      const resourceId = resource?.id;
      const result = await generateAIQuiz(
        resourceId,
        trimmedTopic,
        difficulty,
        questionCount,
        token
      );

      if (result && result.questions?.length > 0) {
        localStorage.setItem("aiQuizData", JSON.stringify(result));
        saveDifficulty(trimmedTopic, difficulty);
        toast.success(`✅ AI quiz ready! (${result.questions.length} questions)`);
        navigate(`/quiz?topic=${encodeURIComponent(trimmedTopic)}&difficulty=${difficulty}`);
      } else {
        toast.warn("⚠️ No questions generated. Using fallback.");
        navigate(`/quiz?topic=${encodeURIComponent(trimmedTopic)}&difficulty=${difficulty}`);
      }
    } catch (err) {
      console.error("❌ AI quiz generation failed:", err);
      toast.error("Failed to generate AI quiz. Check backend logs for details.");
    }
  };

  // ====================== INITIAL LOAD ======================

  useEffect(() => {
    if (!token || !userId) return logout();
    if (fetchInitiated.current) return;
    fetchInitiated.current = true;
    fetchAnalytics();
    fetchResources();
    fetchQuizAttempts();
  }, []);

  // ====================== BADGE HELPER ======================

  const renderBadge = (difficulty) => {
    const colors = {
      easy: "bg-green-100 text-green-800 border-green-400",
      medium: "bg-yellow-100 text-yellow-800 border-yellow-400",
      hard: "bg-red-100 text-red-800 border-red-400",
    };
    return (
      <span
        className={`text-xs font-semibold px-2 py-1 border rounded-md ${
          colors[difficulty] || colors.medium
        }`}
      >
        {difficulty.charAt(0).toUpperCase() + difficulty.slice(1)}
      </span>
    );
  };

  // ====================== RENDER SECTIONS ======================

  const renderLearning = () => (
    <div className="bg-white p-6 rounded-xl shadow-md border border-gray-200 relative">
      <h2 className="text-lg font-semibold mb-4 text-gray-800">📚 My Learning</h2>
      <ResourceUpload onUploadSuccess={fetchResources} />
      <h3 className="mt-6 font-medium text-gray-700 mb-2">Uploaded Resources</h3>

      {resources.length === 0 ? (
        <p className="text-gray-500 text-sm">No uploads yet.</p>
      ) : (
        <table className="w-full text-sm border border-gray-200 rounded-lg">
          <thead className="bg-gray-100 text-gray-700">
            <tr>
              <th className="px-3 py-2 text-left">Topic</th>
              <th className="px-3 py-2 text-left">Adaptive Level</th>
              <th className="px-3 py-2 text-left">Uploaded</th>
              <th className="px-3 py-2 text-center">Actions</th>
            </tr>
          </thead>
          <tbody>
            {resources.map((r) => (
              <tr key={r.id} className="border-t">
                <td className="px-3 py-2 font-medium text-gray-800">{r.topic || r.fileName}</td>
                <td className="px-3 py-2">{renderBadge(r.adaptiveDifficulty)}</td>
                <td className="px-3 py-2 text-gray-500 text-xs">
                  {new Date(r.uploadedAt).toLocaleDateString()}
                </td>
                <td className="px-3 py-2 text-center space-x-2">
                  <button
                    onClick={() => handleGenerateClick(r.topic || "General")}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded-md text-sm"
                  >
                    Generate Quiz
                  </button>
                  <button
                    onClick={() => handleDeleteResource(r.id)}
                    className="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded-md text-sm"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {quizPrompt.visible && (
        <div className="fixed inset-0 bg-black bg-opacity-30 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-2xl shadow-2xl w-full max-w-sm">
            <h3 className="font-semibold text-gray-800 mb-4 text-center">
              Generate Quiz for: <span className="text-blue-700">{quizPrompt.topic}</span>
            </h3>

            <div className="flex justify-center gap-3 mb-5">
              {["easy", "medium", "hard"].map((level) => (
                <button
                  key={level}
                  onClick={() => setQuizPrompt((p) => ({ ...p, difficulty: level }))}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                    quizPrompt.difficulty === level
                      ? "bg-blue-600 text-white"
                      : "bg-gray-100 hover:bg-gray-200 text-gray-700"
                  }`}
                >
                  {level.charAt(0).toUpperCase() + level.slice(1)}
                </button>
              ))}
            </div>

            <div className="mb-4">
              <label className="block text-sm text-gray-700 mb-1">Number of Questions</label>
              <input
                type="number"
                min="1"
                max="30"
                value={quizPrompt.questionCount}
                onChange={(e) =>
                  setQuizPrompt((p) => ({
                    ...p,
                    questionCount: Number(e.target.value),
                  }))
                }
                className="w-full border border-gray-300 rounded-lg p-2 text-sm"
              />
            </div>

            <div className="flex justify-end gap-3 mt-4">
              <button
                onClick={() => setQuizPrompt({ ...quizPrompt, visible: false })}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg text-sm"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  setQuizPrompt({ ...quizPrompt, visible: false });
                  handleStartQuiz(
                    quizPrompt.topic,
                    quizPrompt.difficulty,
                    quizPrompt.questionCount
                  );
                }}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm"
              >
                Generate
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );

  const renderQuizzes = () => (
    <div className="bg-white p-6 rounded-xl shadow-md border border-gray-200">
      <h2 className="text-lg font-semibold mb-4 text-gray-800">🧩 My Quizzes</h2>
      {quizAttempts.length === 0 ? (
        <div className="text-center">
          <p className="text-gray-500 mb-4">No quiz attempts yet.</p>
          <h3 className="font-medium text-gray-700 mb-2">Try these topics:</h3>
          {["AI Basics", "Python Fundamentals", "Database Systems", "Mathematics", "General Knowledge"].map((topic) => (
            <button
              key={topic}
              onClick={() => handleGenerateClick(topic)}
              className="bg-indigo-500 hover:bg-indigo-600 text-white px-5 py-2 rounded-lg shadow-md m-2 transition"
            >
              Start {topic} Quiz
            </button>
          ))}
        </div>
      ) : (
        <table className="w-full text-sm border border-gray-200 rounded-lg">
          <thead className="bg-gray-100 text-gray-700">
            <tr>
              <th className="px-3 py-2 text-left">Topic</th>
              <th className="px-3 py-2 text-left">Score</th>
              <th className="px-3 py-2 text-left">Date</th>
            </tr>
          </thead>
          <tbody>
            {quizAttempts.map((q, index) => (
              <tr key={index} className="border-t">
                <td className="px-3 py-2">{q.topic}</td>
                <td className="px-3 py-2 text-blue-600 font-semibold">{q.score}%</td>
                <td className="px-3 py-2 text-gray-500">
                  {q.date ? new Date(q.date).toLocaleDateString() : "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );

  const renderPerformance = () => (
    <div className="bg-white p-6 rounded-xl shadow-md border border-gray-200">
      <h2 className="text-lg font-semibold mb-4 text-gray-800">📈 My Performance</h2>
      {analytics && analytics.chartData?.length > 0 ? (
        <>
          <LineChart width={800} height={300} data={analytics.chartData}>
            <Line type="monotone" dataKey="score" stroke="#2563eb" strokeWidth={3} />
            <CartesianGrid stroke="#e5e7eb" strokeDasharray="3 3" />
            <XAxis dataKey="quiz" />
            <YAxis domain={[0, 100]} />
            <Tooltip />
            <Legend />
          </LineChart>
          <div className="mt-6 flex justify-around">
            <div>
              <p className="text-gray-500 text-sm">Accuracy</p>
              <p className="text-xl font-bold text-blue-600">{analytics.accuracy}</p>
            </div>
            <div>
              <p className="text-gray-500 text-sm">Average Score</p>
              <p className="text-xl font-bold text-purple-600">
                {analytics.avgScore ? analytics.avgScore.toFixed(1) : 0}%
              </p>
            </div>
            <div>
              <p className="text-gray-500 text-sm">Badges</p>
              <p className="text-xl font-bold text-green-600">
                {analytics.badges.join(", ") || "—"}
              </p>
            </div>
          </div>
        </>
      ) : (
        <p className="text-gray-500 text-sm">No analytics available yet.</p>
      )}
    </div>
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-6 relative">
      <div className="max-w-6xl mx-auto bg-white rounded-2xl shadow-lg border border-gray-200 p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">🎓 Student Dashboard</h1>
        <div className="flex gap-3 mb-6">
          {[
            { id: "learning", label: "📚 My Learning" },
            { id: "quizzes", label: "🧩 My Quizzes" },
            { id: "performance", label: "📈 My Performance" },
          ].map((tab) => (
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

        {activeTab === "learning" && renderLearning()}
        {activeTab === "quizzes" && renderQuizzes()}
        {activeTab === "performance" && renderPerformance()}
      </div>
    </div>
  );
}
