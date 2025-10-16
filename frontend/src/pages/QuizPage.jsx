import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const QuizPage = () => {
  const navigate = useNavigate();
  
  // State management
  const [questions, setQuestions] = useState([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState([]);
  const [selectedOption, setSelectedOption] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [quizCompleted, setQuizCompleted] = useState(false);
  const [score, setScore] = useState(null);
  const [recommendedDifficulty, setRecommendedDifficulty] = useState(null);

  // Fetch quiz on component mount
  useEffect(() => {
    generateQuiz();
  }, []);

  const generateQuiz = async () => {
    setLoading(true);
    setError(null);

    try {
      console.log('🎯 Generating quiz...');
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      // ✅ Correct API call to /quiz/generate
      const response = await fetch('http://localhost:8080/quiz/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          topic: 'General Knowledge',   // matches backend
          difficulty: 'medium'
        })
      });

      console.log('📡 Response Status:', response.status);

      if (!response.ok) {
        throw new Error(`Failed to generate quiz (${response.status})`);
      }

      const data = await response.json();
      console.log('✅ Quiz generated:', data);

      setQuestions(data.questions || []);
      setSelectedAnswers(new Array(data.questions?.length || 0).fill(null));
      setLoading(false);
    } catch (err) {
      console.error('❌ Error generating quiz:', err);
      setError(err.message);
      setLoading(false);
    }
  };


  const handleOptionSelect = (optionIndex) => {
    setSelectedOption(optionIndex);
  };

  const handleNext = () => {
    if (selectedOption === null) {
      alert('Please select an answer before proceeding');
      return;
    }

    // Save the selected answer
    const newAnswers = [...selectedAnswers];
    newAnswers[currentQuestionIndex] = selectedOption;
    setSelectedAnswers(newAnswers);

    // Move to next question or finish
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
      setSelectedOption(newAnswers[currentQuestionIndex + 1]);
    } else {
      submitQuiz(newAnswers);
    }
  };

  const handlePrevious = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
      setSelectedOption(selectedAnswers[currentQuestionIndex - 1]);
    }
  };

  const submitQuiz = async (answers) => {
    try {
      console.log('📤 Submitting quiz answers...');
      const token = localStorage.getItem('token');
      const userId = localStorage.getItem('userId') || 1;

      // 🔧 Build backend-compatible objects
      const formattedAnswers = questions.map((q, index) => {
        const selected = answers[index];
        if (selected === null) return { questionId: q.questionId, isCorrect: false };
        const selectedOption = q.options[selected];
        const correct = selectedOption === q.answer;
        return { questionId: q.questionId, isCorrect: correct };
      });

      const response = await fetch('http://localhost:8080/quiz/submit', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          userId: parseInt(userId),
          answers: formattedAnswers
        })
      });

      console.log('📡 Submit Response Status:', response.status);
      if (!response.ok) throw new Error(`Failed to submit quiz (${response.status})`);

      const result = await response.json();
      console.log('✅ Quiz submitted successfully:', result);

      setScore(result.score);
      setRecommendedDifficulty(result.nextLevel || 'medium');
      setQuizCompleted(true);
    } catch (err) {
      console.error('❌ Error submitting quiz:', err);
      setError(err.message);
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
    navigate('/dashboard');
  };

  // Loading state
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="bg-white p-8 rounded-lg shadow-md">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 text-center">Generating your quiz...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
        <div className="bg-white p-8 rounded-lg shadow-md max-w-md w-full">
          <div className="text-red-600 text-center mb-4">
            <svg className="w-16 h-16 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h2 className="text-xl font-bold mb-2">Error</h2>
            <p className="text-gray-600">{error}</p>
          </div>
          <button
            onClick={generateQuiz}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors mb-2"
          >
            Try Again
          </button>
          <button
            onClick={handleBackToDashboard}
            className="w-full bg-gray-200 text-gray-700 py-2 px-4 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  // Completion screen
  if (quizCompleted) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
        
        <div className="bg-white p-8 rounded-lg shadow-md max-w-md w-full">
          <div className="text-center">
            <div className="text-green-600 mb-4">
              <svg className="w-20 h-20 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h2 className="text-3xl font-bold text-gray-800 mb-4">Quiz Completed! 🎉</h2>
            
            <div className="bg-blue-50 rounded-lg p-6 mb-6">
              <p className="text-gray-600 mb-2">Your Score</p>
              <p className="text-5xl font-bold text-blue-600 mb-4">{score}%</p>
              <div className="w-full bg-gray-200 rounded-full h-3 mb-4">
                <div 
                  className="bg-blue-600 h-3 rounded-full transition-all duration-500"
                  style={{ width: `${score}%` }}
                ></div>
              </div>
              <p className="text-sm text-gray-600">
                {score >= 80 ? '🌟 Excellent work!' : score >= 60 ? '👍 Good job!' : '💪 Keep practicing!'}
              </p>
            </div>

            <div className="bg-purple-50 rounded-lg p-4 mb-6">
              <p className="text-sm text-gray-600 mb-1">Recommended Difficulty</p>
              <p className="text-xl font-semibold text-purple-600 capitalize">{recommendedDifficulty}</p>
            </div>

            <button
              onClick={handleRetakeQuiz}
              className="w-full bg-blue-600 text-white py-3 px-6 rounded-lg hover:bg-blue-700 transition-colors mb-3 font-medium"
            >
              Take Another Quiz
            </button>
            <button
              onClick={handleBackToDashboard}
              className="w-full bg-gray-200 text-gray-700 py-3 px-6 rounded-lg hover:bg-gray-300 transition-colors font-medium"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Quiz interface
  const currentQuestion = questions[currentQuestionIndex];
  const progress = ((currentQuestionIndex + 1) / questions.length) * 100;

  return (
    <div className="min-h-screen bg-gray-100 py-8 px-4">
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex justify-between items-center mb-4">
            <h1 className="text-2xl font-bold text-gray-800">IntelliQuiz 🎯</h1>
            <span className="text-sm text-gray-600 bg-blue-100 px-3 py-1 rounded-full">
              Question {currentQuestionIndex + 1} of {questions.length}
            </span>
          </div>
          
          {/* Progress Bar */}
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div 
              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
              style={{ width: `${progress}%` }}
            ></div>
          </div>
        </div>

        {/* Question Card */}
        <div className="bg-white rounded-lg shadow-md p-8">
          <h2 className="text-xl font-semibold text-gray-800 mb-6">
            {currentQuestion?.question}
          </h2>

          {/* Options */}
          <div className="space-y-3 mb-8">
            {currentQuestion?.options?.map((option, index) => (
              <label
                key={index}
                className={`flex items-center p-4 rounded-lg border-2 cursor-pointer transition-all ${
                  selectedOption === index
                    ? 'border-blue-600 bg-blue-50'
                    : 'border-gray-200 hover:border-blue-300 hover:bg-gray-50'
                }`}
              >
                <input
                  type="radio"
                  name="answer"
                  value={index}
                  checked={selectedOption === index}
                  onChange={() => handleOptionSelect(index)}
                  className="w-5 h-5 text-blue-600 focus:ring-blue-500"
                />
                <span className="ml-3 text-gray-700">{option}</span>
              </label>
            ))}
          </div>

          {/* Navigation Buttons */}
          <div className="flex justify-between gap-4">
            <button
              onClick={handlePrevious}
              disabled={currentQuestionIndex === 0}
              className={`px-6 py-3 rounded-lg font-medium transition-colors ${
                currentQuestionIndex === 0
                  ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              ← Previous
            </button>
            
            <button
              onClick={handleNext}
              className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
            >
              {currentQuestionIndex === questions.length - 1 ? 'Submit Quiz' : 'Next →'}
            </button>
          </div>
        </div>

        {/* Debug Info (remove in production) */}
        <div className="mt-6 bg-gray-800 rounded-lg p-4 text-white text-sm">
          <p className="font-semibold mb-2">🔍 Debug Info:</p>
          <p>Current Question: {currentQuestionIndex + 1}/{questions.length}</p>
          <p>Selected Answer: {selectedOption !== null ? selectedOption : 'None'}</p>
          <p>Answers: [{selectedAnswers.map(a => a !== null ? a : '_').join(', ')}]</p>
        </div>
      </div>
    </div>
  );
};

export default QuizPage;