import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { mockQuizzes } from "../data/mockQuizData"; // ✅ import our local quiz data
import { toast } from "react-toastify";

const QuizPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);

  const topic = queryParams.get("topic") || "General Knowledge";
  const difficulty = queryParams.get("difficulty") || "medium";

  const [questions, setQuestions] = useState([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState([]);
  const [selectedOption, setSelectedOption] = useState(null);
  const [quizCompleted, setQuizCompleted] = useState(false);
  const [score, setScore] = useState(null);
  const [recommendedDifficulty, setRecommendedDifficulty] = useState(null);
  const [loading, setLoading] = useState(true);

  const API_BASE = import.meta.env.VITE_API_URL;
  const token = localStorage.getItem("token");

  // 🎯 Generate quiz on load
  useEffect(() => {
    generateQuiz();
  }, [topic, difficulty]);

  const generateQuiz = async () => {
    setLoading(true);
    try {
      console.log(`🎯 Generating quiz for ${topic} (${difficulty})`);
      const response = await fetch(
        `${API_BASE}/quiz/generate?topic=${encodeURIComponent(topic)}&difficulty=${difficulty}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (!response.ok) throw new Error(`Backend quiz failed (${response.status})`);
      const data = await response.json();

      const validQuestions = Array.isArray(data.questions)
        ? data.questions.slice(0, 5)
        : [];

      if (validQuestions.length === 0) throw new Error("No questions received");

      setQuestions(validQuestions);
      setSelectedAnswers(new Array(validQuestions.length).fill(null));
      setLoading(false);
    } catch (err) {
      console.warn("⚠️ Using mock quiz fallback:", err.message);
      const fallback = mockQuizzes[topic] || mockQuizzes["General Knowledge"];
      const mockSet = fallback.questions.slice(0, 5);
      setQuestions(mockSet);
      setSelectedAnswers(new Array(mockSet.length).fill(null));
      setLoading(false);
      toast.info("Loaded mock quiz questions.");
    }
  };

  // 🧠 Handle answer selection
  const handleOptionSelect = (optionIndex) => {
    setSelectedOption(optionIndex);
  };

  // ⏭️ Next Question or Submit
  const handleNext = () => {
    if (selectedOption === null) {
      toast.warn("Please select an answer before proceeding.");
      return;
    }

    const newAnswers = [...selectedAnswers];
    newAnswers[currentQuestionIndex] = selectedOption;
    setSelectedAnswers(newAnswers);

    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
      setSelectedOption(newAnswers[currentQuestionIndex + 1]);
    } else {
      submitQuiz(newAnswers);
    }
  };

  // ⏮️ Previous Question
  const handlePrevious = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
      setSelectedOption(selectedAnswers[currentQuestionIndex - 1]);
    }
  };

  // 📤 Submit Quiz
  const submitQuiz = async (answers) => {
    try {
      const formattedAnswers = questions.map((q, index) => {
        const selected = answers[index];
        const selectedOptionText = q.options[selected];
        const correct = selectedOptionText === q.answer;
        return { questionId: q.questionId || index + 1, isCorrect: correct };
      });

      const response = await fetch(`${API_BASE}/quiz/submit`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          topic,
          difficulty,
          userId: parseInt(localStorage.getItem("userId")),
          answers: formattedAnswers,
        }),
      });

      if (!response.ok) throw new Error(`Quiz submission failed (${response.status})`);
      const result = await response.json();

      setScore(result.scorePercentage || 0);
      setRecommendedDifficulty(result.nextLevel || "medium");
      setQuizCompleted(true);
    } catch (err) {
      console.error("❌ Quiz submission error:", err);
      toast.error("Quiz submission failed. Showing mock result.");
      const correctCount = selectedAnswers.reduce(
        (acc, sel, i) => (questions[i].options[sel] === questions[i].answer ? acc + 1 : acc),
        0
      );
      const scorePercent = Math.round((correctCount / questions.length) * 100);
      setScore(scorePercent);
      setRecommendedDifficulty(scorePercent > 80 ? "hard" : scorePercent < 50 ? "easy" : "medium");
      setQuizCompleted(true);
    }
  };

  const handleRetakeQuiz = () => {
    setQuizCompleted(false);
    setCurrentQuestionIndex(0);
    setSelectedAnswers([]);
    setSelectedOption(null);
    setScore(null);
    setRecommendedDifficulty(null);
    generateQuiz();
  };

  const handleBackToDashboard = () => {
    navigate("/student/dashboard");
  };

  // 🌀 Loading
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="mt-4 text-gray-600">Generating quiz for {topic}...</p>
      </div>
    );
  }

  // ✅ Quiz Completed
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
            onClick={handleRetakeQuiz}
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

  // 🎯 Quiz in Progress
  const currentQuestion = questions[currentQuestionIndex];
  const progress = ((currentQuestionIndex + 1) / questions.length) * 100;

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-lg p-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold text-gray-800">🧠 {topic} Quiz</h1>
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

        <h2 className="text-lg font-semibold text-gray-800 mb-4">{currentQuestion?.question}</h2>

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
