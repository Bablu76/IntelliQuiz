// 🔹 Compute next difficulty (same logic as backend)
export const computeNextDifficulty = (score, current = "medium") => {
  if (score >= 85 && current !== "hard") return "hard";
  if (score <= 50 && current !== "easy") return "easy";
  return "medium";
};

// 🔹 Save or update difficulty for a topic
export const saveDifficulty = (topic, difficulty) => {
  if (!topic) return;
  try {
    localStorage.setItem(`difficulty_${topic.toLowerCase()}`, difficulty);
    console.log(`📘 Saved difficulty for '${topic}': ${difficulty}`);
  } catch (err) {
    console.warn("⚠️ Failed to save difficulty:", err);
  }
};

// 🔹 Retrieve stored difficulty (fallback: medium)
export const getSavedDifficulty = (topic) => {
  if (!topic) return "medium";
  try {
    const saved = localStorage.getItem(`difficulty_${topic.toLowerCase()}`);
    return saved || "medium";
  } catch (err) {
    console.warn("⚠️ Failed to load saved difficulty:", err);
    return "medium";
  }
};
