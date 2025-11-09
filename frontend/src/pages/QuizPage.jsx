import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { submitQuiz } from "../utils/quizApi";

const QuizPage = () => {
  const navigate = useNavigate();
  const [questions, setQuestions] = useState([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState([]);
  const [selectedOption, setSelectedOption] = useState(null);
  const [quizCompleted, setQuizCompleted] = useState(false);
  const [score, setScore] = useState(null);
  const [recommendedDifficulty, setRecommendedDifficulty] = useState(null);
  const [loading, setLoading] = useState(true);
  const [quizId, setQuizId] = useState(null);
  const [topic, setTopic] = useState("General Knowledge");
  const [difficulty, setDifficulty] = useState("medium");
  const [startTime, setStartTime] = useState(null);

  // ✅ Load quiz data from localStorage
  useEffect(() => {
    try {
      console.log("🔹 QuizPage loaded!");
      const stored = localStorage.getItem("aiQuizData");
      if (!stored) {
        toast.warn("⚠️ No quiz found. Please generate one first!");
        navigate("/student/dashboard");
        return;
      }

      const parsed = JSON.parse(stored);
      if (!parsed?.questions?.length) {
        toast.warn("⚠️ Stored quiz has no questions!");
        navigate("/student/dashboard");
        return;
      }

      console.log("✅ Loaded AI Quiz:", parsed);

      setQuestions(parsed.questions);
      setQuizId(parsed.quizId || null);
      setTopic(parsed.topic || "General Knowledge");
      setDifficulty(parsed.difficulty || "medium");
      setSelectedAnswers(new Array(parsed.questions.length).fill(null));
      setStartTime(Date.now());
      setLoading(false);
    } catch (err) {
      console.error("❌ Failed to parse stored quiz:", err);
      toast.error("Quiz loading failed.");
      navigate("/student/dashboard");
    }
  }, []);

  // 🧠 Handle answer select
  const handleOptionSelect = (index) => {
    setSelectedOption(index);
  };

  // ⏭️ Next or Submit
  const handleNext = () => {
    if (selectedOption === null) {
      toast.warn("Please select an answer before continuing.");
      return;
    }

    const newAnswers = [...selectedAnswers];
    newAnswers[currentQuestionIndex] = selectedOption;
    setSelectedAnswers(newAnswers);

    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
      setSelectedOption(newAnswers[currentQuestionIndex + 1]);
    } else {
      submitQuizAnswers(newAnswers);
    }
  };

  // ⏮️ Previous
  const handlePrevious = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
      setSelectedOption(selectedAnswers[currentQuestionIndex - 1]);
    }
  };

  // 📤 Submit quiz to backend
  const submitQuizAnswers = async (answers) => {
    try {
      const formattedAnswers = answers.map((idx) => ({
        selectedIndex: idx,
      }));

      const timeTakenSec = startTime ? Math.round((Date.now() - startTime) / 1000) : 0;

      const payload = {
        quizId,
        topic,
        difficulty,
        answers: formattedAnswers,
        timeTaken: timeTakenSec,
      };

      console.log("📦 Submitting payload:", payload);

      toast.info("📤 Submitting quiz...");

      const result = await submitQuiz(payload);
      console.log("✅ Submission response:", result);

      const scorePercent = result.scorePercent ?? result.scorePercentage ?? 0;
      setScore(scorePercent);
      setRecommendedDifficulty(result.nextDifficulty || "medium");
      setQuizCompleted(true);

      // 🧹 Cleanup stored quiz data
      localStorage.removeItem("aiQuizData");

      toast.success(`✅ Quiz submitted! Score: ${scorePercent}%`);
    } catch (err) {
      console.error("❌ Quiz submission failed:", err);
      toast.error("Submission failed. Calculating locally...");

      // fallback to local evaluation
      const correctCount = answers.reduce(
        (acc, idx, i) =>
          idx !== null && questions[i]?.options?.[idx] === questions[i]?.answer ? acc + 1 : acc,
        0
      );
      const scorePercent = Math.round((correctCount / questions.length) * 100);
      setScore(scorePercent);
      setRecommendedDifficulty(
        scorePercent > 80 ? "hard" : scorePercent < 50 ? "easy" : "medium"
      );
      setQuizCompleted(true);
    }
  };

  // ♻️ Restart or Retake
  const handleRetake = () => {
    setQuizCompleted(false);
    setCurrentQuestionIndex(0);
    setSelectedAnswers(new Array(questions.length).fill(null));
    setSelectedOption(null);
    setScore(null);
    setRecommendedDifficulty(null);
    setStartTime(Date.now());
  };

  // 🏠 Back to Dashboard
  const handleBackToDashboard = () => {
    navigate("/student/dashboard");
  };

  // 🌀 Loading UI
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="mt-4 text-gray-600">Loading your quiz...</p>
      </div>
    );
  }

  // ✅ Completed View
  if (quizCompleted) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
        <div className="bg-white rounded-lg shadow-md p-8 w-full max-w-md text-center">
          <h2 className="text-2xl font-bold text-gray-800 mb-4">🎉 Quiz Completed!</h2>
          <p className="text-gray-600 mb-2">Topic: {topic}</p>
          <p className="text-5xl font-bold text-blue-600 mb-2">{score}%</p>
          <p className="text-gray-500 mb-4">
            Recommended Next Level:{" "}
            <span className="text-purple-600 font-semibold">{recommendedDifficulty}</span>
          </p>

          <button
            onClick={handleRetake}
            className="w-full bg-blue-600 text-white py-3 rounded-lg mb-2 hover:bg-blue-700 transition"
          >
            Retake Quiz
          </button>
          <button
            onClick={handleBackToDashboard}
            className="w-full bg-gray-200 text-gray-700 py-3 rounded-lg hover:bg-gray-300 transition"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  // 🎯 Active Quiz View
  const currentQuestion = questions[currentQuestionIndex];
  const progress = ((currentQuestionIndex + 1) / questions.length) * 100;

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-lg p-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-800">
            🧠 {topic} Quiz ({difficulty})
          </h1>
          <span className="text-sm text-gray-600 bg-blue-100 px-3 py-1 rounded-full">
            Question {currentQuestionIndex + 1} of {questions.length}
          </span>
        </div>

        <div className="w-full bg-gray-200 rounded-full h-2 mb-4">
          <div
            className="bg-blue-600 h-2 rounded-full transition-all duration-300"
            style={{ width: `${progress}%` }}
          ></div>
        </div>

        <h2 className="text-lg font-semibold text-gray-800 mb-4">
          {currentQuestion?.question}
        </h2>

        <div className="space-y-3 mb-6">
          {currentQuestion?.options?.map((option, index) => (
            <label
              key={index}
              className={`flex items-center p-3 rounded-lg border-2 cursor-pointer transition-all ${
                selectedOption === index
                  ? "border-blue-600 bg-blue-50"
                  : "border-gray-200 hover:border-blue-300"
              }`}
            >
              <input
                type="radio"
                name="answer"
                value={index}
                checked={selectedOption === index}
                onChange={() => handleOptionSelect(index)}
                className="w-4 h-4 text-blue-600"
              />
              <span className="ml-3 text-gray-700">{option}</span>
            </label>
          ))}
        </div>

        <div className="flex justify-between">
          <button
            onClick={handlePrevious}
            disabled={currentQuestionIndex === 0}
            className={`px-5 py-2 rounded-lg ${
              currentQuestionIndex === 0
                ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                : "bg-gray-200 hover:bg-gray-300 text-gray-700"
            }`}
          >
            ← Previous
          </button>
          <button
            onClick={handleNext}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            {currentQuestionIndex === questions.length - 1 ? "Submit" : "Next →"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default QuizPage;
