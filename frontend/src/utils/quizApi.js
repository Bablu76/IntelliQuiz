import api from "./api";

// Generate quiz using topic + difficulty
export const generateQuiz = async (topic, difficulty) => {
  const response = await api.post("/quiz/generate", null, {
    params: { topic, difficulty },
  });
  return response.data;
};

// Submit quiz answers → returns score + nextLevel
export const submitQuiz = async (payload) => {
  const response = await api.post("/quiz/submit", payload);
  return response.data;
};
