import { useState } from "react";
import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { generateAIQuiz } from "../utils/quizApi";

export default function QuizGenerator() {
  const [file, setFile] = useState(null);
  const [topic, setTopic] = useState("");
  const [difficulty, setDifficulty] = useState("medium");
  const [questionCount, setQuestionCount] = useState(5);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState("");

  const navigate = useNavigate();

  const handleGenerate = async () => {
    if (!file || !topic) {
      alert("Please upload a PDF and enter a topic.");
      return;
    }

    setLoading(true);
    setStatus("Extracting content and generating quiz...");

    try {
      // 🧩 1️⃣ Request backend to generate quiz
      const result = await generateAIQuiz(file, topic, difficulty, questionCount);

      if (!result || !result.questions) {
        setStatus("❌ No questions returned from AI generator.");
        return;
      }

      // 🧩 2️⃣ Save locally for persistence
      const quizData = {
        questions: result.questions,
        topic,
        difficulty,
        questionCount,
        generatedAt: new Date().toISOString(),
      };
      localStorage.setItem("aiQuizData", JSON.stringify(quizData));

      // 🧩 3️⃣ Log success
      setStatus(
        `✅ Generated ${result.questionsGenerated || result.questions.length} questions in ${
          result.generationTimeMs || 0
        }ms`
      );

      // 🧩 4️⃣ Navigate to main Quiz Page with context
      navigate(`/quiz?topic=${encodeURIComponent(topic)}&difficulty=${difficulty}`, {
        state: { fromAI: true },
      });
    } catch (err) {
      console.error("❌ Quiz generation failed:", err);
      setStatus("❌ Quiz generation failed. " + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      className="min-h-screen bg-gray-100 flex flex-col items-center py-12 px-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <motion.div
        className="bg-white p-8 rounded-2xl shadow-lg w-full max-w-3xl"
        initial={{ y: 40, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ delay: 0.2 }}
      >
        <h1 className="text-3xl font-bold text-center text-blue-700 mb-6">
          🧠 AI Quiz Generator
        </h1>

        <div className="flex flex-col gap-3 mb-6">
          {/* 🗂️ File Input */}
          <input
            type="file"
            accept="application/pdf"
            onChange={(e) => setFile(e.target.files[0])}
            className="border p-2 rounded-lg"
          />

          {/* 🧩 Topic */}
          <input
            type="text"
            placeholder="Enter topic (e.g. Machine Learning)"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            className="border p-2 rounded-lg"
          />

          {/* 🎚️ Difficulty */}
          <select
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value)}
            className="border p-2 rounded-lg"
          >
            <option value="easy">Easy</option>
            <option value="medium">Medium</option>
            <option value="hard">Hard</option>
          </select>

          {/* 🔢 Question Count */}
          <div className="flex flex-col">
            <label className="text-sm text-gray-700 mb-1 font-medium">
              Number of Questions
            </label>
            <input
              type="number"
              min="1"
              max="30"
              value={questionCount}
              onChange={(e) => setQuestionCount(Number(e.target.value))}
              className="border p-2 rounded-lg w-full"
            />
          </div>

          {/* 🚀 Generate Button */}
          <button
            onClick={handleGenerate}
            disabled={loading}
            className={`w-full py-2 rounded-lg text-white transition ${
              loading ? "bg-gray-400" : "bg-blue-600 hover:bg-blue-700"
            }`}
          >
            {loading ? "Generating..." : "Generate AI Quiz"}
          </button>
        </div>

        {/* 💬 Status message */}
        {status && (
          <p className="text-sm text-center text-gray-700 mb-4">{status}</p>
        )}
      </motion.div>
    </motion.div>
  );
}
