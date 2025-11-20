import api from "./api";

/** 🔹 Generate AI-based quiz */
export const generateAIQuiz = async (
  fileOrResourceId,
  topic,
  difficulty = "medium",
  questionCount = 10,
  token
) => {
  const formData = new FormData();

  if (fileOrResourceId instanceof File) {
    formData.append("file", fileOrResourceId);
  } else if (fileOrResourceId) {
    formData.append("resourceId", fileOrResourceId);
  }

  formData.append("topic", topic);
  formData.append("difficulty", difficulty);
  formData.append("questionCount", questionCount);

  const res = await api.post("/quiz/generate/ai", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: `Bearer ${token}`,
    },
  });

  return res.data;
};

/** 🔹 Submit quiz answers */
export const submitQuiz = async (payload) => {
  const response = await api.post("/quiz/submit", payload);
  return response.data;
};
