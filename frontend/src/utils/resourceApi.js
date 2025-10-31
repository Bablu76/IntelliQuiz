import api from "./api";

// Upload resource (PDF only)
export const uploadResource = async (file, topic, onProgress) => {
  if (!file || file.type !== "application/pdf") {
    throw new Error("Only PDF files are allowed");
  }

  const formData = new FormData();
  formData.append("file", file);
  if (topic) formData.append("topic", topic);

  const response = await api.post("/resources/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
    onUploadProgress: (event) => {
      if (onProgress) {
        const percent = Math.round((event.loaded * 100) / event.total);
        onProgress(percent);
      }
    },
  });

  return response.data;
};

// List current user's resources
export const listResources = async () => {
  const response = await api.get("/resources/list");
  return response.data;
};

// Delete a resource by ID
export const deleteResource = async (id) => {
  const response = await api.delete(`/resources/${id}`);
  return response.data;
};
