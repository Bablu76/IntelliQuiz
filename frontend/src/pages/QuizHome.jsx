import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { toast } from "react-toastify";
import { Brain, Sparkles, TrendingUp } from "lucide-react";
import { getSavedDifficulty } from "../utils/adaptive";
import useAuth from "../hooks/useAuth";
import { useNavigate } from "react-router-dom";
import axios from "axios";

export default function QuizHome() {
  const { userId, fetchWithAuth } = useAuth();
  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const navigate = useNavigate();

  const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

  // 🔹 Compute next adaptive difficulty
  const computeNextLevel = (score, current) => {
    if (score >= 85 && current !== "hard") return "hard";
    if (score <= 50 && current !== "easy") return "easy";
    return current;
  };

  useEffect(() => {
    if (userId) fetchAvailableQuizzes();
  }, [userId]);

  // 🔹 Load user resources and difficulty info
  const fetchAvailableQuizzes = async () => {
    try {
      const token = localStorage.getItem("token");
      const res = await axios.get(`${API_BASE}/resources/list`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = res.data || [];

      let pastAttempts = [];
      try {
        const attemptRes = await fetchWithAuth(`${API_BASE}/quiz/attempts/${userId}`);
        pastAttempts = await attemptRes.json();
      } catch {
        console.warn("⚠️ Could not fetch past attempts");
      }

      const topicMap = {};
      if (Array.isArray(pastAttempts)) {
        pastAttempts.forEach((a) => {
          topicMap[a.topic.toLowerCase()] = {
            current: a.difficulty,
            score: a.score,
            next: computeNextLevel(a.score, a.difficulty),
          };
        });
      }

      const formatted = data.map((r) => {
        const key = r.topic.toLowerCase();
        return {
          id: r.id,
          topic: r.topic,
          resourceId: r.id,
          currentDifficulty: topicMap[key]?.current || getSavedDifficulty(r.topic) || "medium",
          nextDifficulty: topicMap[key]?.next || "medium",
          score: topicMap[key]?.score ?? null,
        };
      });

      setQuizzes(formatted);
    } catch (err) {
      console.error("❌ Failed to fetch quizzes:", err);
      toast.error("Failed to load quizzes");
    } finally {
      setLoading(false);
    }
  };

  const getDifficultyColor = (d) => {
    switch (d?.toLowerCase()) {
      case "easy":
        return "bg-green-100 text-green-700 border-green-300";
      case "hard":
        return "bg-red-100 text-red-700 border-red-300";
      default:
        return "bg-yellow-100 text-yellow-700 border-yellow-300";
    }
  };

  // 🔹 Core: Load existing or generate new quiz
  const fetchOrGenerateQuiz = async (quiz, difficulty) => {
    try {
      setGenerating(true);
      const token = localStorage.getItem("token");

      // ✅ Step 1: Check if quiz already exists in DB
      const existing = await axios.get(`${API_BASE}/quiz/list`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const match = existing.data?.find(
        (q) =>
          q.topic?.toLowerCase() === quiz.topic.toLowerCase() &&
          q.difficulty?.toLowerCase() === difficulty.toLowerCase()
      );

      if (match) {
        // ✅ Found saved quiz
        const { data } = await axios.get(`${API_BASE}/quiz/get/${match.id}`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        const normalized = {
          quizId: data.quizId || data.id,
          topic: data.topic,
          difficulty: data.difficulty,
          questions: data.questions || [],
        };

        localStorage.setItem("aiQuizData", JSON.stringify(normalized));
        toast.success(`✅ Loaded saved quiz: "${quiz.topic}" (${difficulty})`);

        navigate(`/quiz?topic=${encodeURIComponent(quiz.topic)}&difficulty=${difficulty}`);
        return;
      }

      // ❌ Not found → Generate new
      toast.info(`🤖 Generating ${difficulty} quiz for "${quiz.topic}"...`);

      const formData = new FormData();
      formData.append("resourceId", quiz.resourceId);
      formData.append("topic", quiz.topic);
      formData.append("difficulty", difficulty);
      formData.append("questionCount", 10);

      const res = await axios.post(`${API_BASE}/quiz/generate/ai`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      });

      if (res.data?.questions?.length > 0) {
        const normalized = {
          quizId: res.data.quizId || Date.now(),
          topic: res.data.topic,
          difficulty: res.data.difficulty,
          questions: res.data.questions,
        };

        localStorage.setItem("aiQuizData", JSON.stringify(normalized));
        toast.success(`🎯 AI Quiz ready for "${quiz.topic}" (${difficulty})`);

        navigate(`/quiz?topic=${encodeURIComponent(quiz.topic)}&difficulty=${difficulty}`);
      } else {
        toast.warn("⚠️ No questions generated by AI.");
      }
    } catch (err) {
      console.error("❌ Quiz generation error:", err);
      toast.error("Failed to generate or load quiz.");
    } finally {
      setGenerating(false);
    }
  };

  // 🧠 Button actions
  const handleStartCurrent = (quiz) =>
    fetchOrGenerateQuiz(quiz, quiz.currentDifficulty);

  const handleStartRecommended = (quiz) =>
    fetchOrGenerateQuiz(quiz, quiz.nextDifficulty);

  // 🌀 Loading state
  if (loading || generating) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="mt-4 text-gray-600">
          {generating ? "Generating quiz..." : "Loading available quizzes..."}
        </p>
      </div>
    );
  }

  return (
    <motion.div
      className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex flex-col items-center py-12 px-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4 }}
    >
      <motion.div
        className="bg-white p-8 rounded-2xl shadow-lg w-full max-w-3xl"
        initial={{ y: 40, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ delay: 0.2 }}
      >
        <h1 className="text-3xl font-bold text-center text-blue-700 mb-6 flex items-center justify-center gap-2">
          🧩 Available Quizzes
        </h1>

        {quizzes.length > 0 ? (
          <div className="space-y-4">
            {quizzes.map((quiz, idx) => (
              <motion.div
                key={quiz.id}
                className="border border-gray-200 p-5 rounded-xl hover:shadow-md hover:border-blue-300 transition"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.05 * idx }}
              >
                <div className="flex justify-between items-start flex-wrap">
                  <div className="flex-1">
                    <h3 className="font-semibold text-gray-800 capitalize text-lg">
                      {quiz.topic}
                    </h3>

                    <div className="flex flex-wrap gap-3 mt-2 text-sm text-gray-600 items-center">
                      <span>
                        Current:{" "}
                        <span
                          className={`font-semibold px-2 py-0.5 rounded-md border ${getDifficultyColor(
                            quiz.currentDifficulty
                          )}`}
                        >
                          {quiz.currentDifficulty.charAt(0).toUpperCase() +
                            quiz.currentDifficulty.slice(1)}
                        </span>
                      </span>

                      <span>
                        Recommended:{" "}
                        <span
                          className={`font-semibold px-2 py-0.5 rounded-md border ${getDifficultyColor(
                            quiz.nextDifficulty
                          )}`}
                        >
                          {quiz.nextDifficulty.charAt(0).toUpperCase() +
                            quiz.nextDifficulty.slice(1)}
                        </span>
                      </span>

                      {quiz.score !== null && (
                        <span className="text-gray-500 flex items-center gap-1">
                          <TrendingUp className="w-4 h-4 text-blue-500" />
                          Last Score: {quiz.score}%
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-col gap-2">
                    <button
                      onClick={() => handleStartCurrent(quiz)}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition shadow-sm"
                    >
                      <Brain size={16} /> Start Current
                    </button>

                    <button
                      onClick={() => handleStartRecommended(quiz)}
                      className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition shadow-sm"
                    >
                      <Sparkles size={16} /> Use Recommended
                    </button>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        ) : (
          <div className="text-center text-gray-600 py-8">
            <p className="mb-4 text-lg">No quizzes available yet.</p>
            <button
              onClick={() => navigate("/student/dashboard")}
              className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg transition shadow-md"
            >
              📚 Go to Dashboard
            </button>
          </div>
        )}
      </motion.div>
    </motion.div>
  );
}
