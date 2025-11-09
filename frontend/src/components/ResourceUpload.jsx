import React, { useState } from "react";
import axios from "axios";
import { toast } from "react-toastify";

const ResourceUpload = ({ onUploadSuccess }) => {
  const [file, setFile] = useState(null);
  const [topic, setTopic] = useState("");
  const [progress, setProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);

  const API_BASE = import.meta.env.VITE_API_URL;
  const token = localStorage.getItem("token");

  const handleFileChange = (e) => {
    const selected = e.target.files[0];
    if (!selected) return;

    if (selected.type !== "application/pdf") {
      toast.error("❌ Please upload a valid PDF file only.");
      e.target.value = "";
      return;
    }

    setFile(selected);
    toast.info(`📄 Selected: ${selected.name}`);
  };

  const handleUpload = async () => {
    if (!file || !topic.trim()) {
      toast.warning("⚠️ Please provide both a topic and a PDF file before uploading.");
      return;
    }

    setIsUploading(true);
    setProgress(0);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("topic", topic.trim());

    try {
      const response = await axios.post(`${API_BASE}/resources/upload`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        onUploadProgress: (evt) => {
          const percent = Math.round((evt.loaded * 100) / evt.total);
          setProgress(percent);
        },
      });

      if (response.status === 200) {
        toast.success(`✅ '${file.name}' uploaded successfully!`);
        setFile(null);
        setTopic("");
        if (onUploadSuccess) onUploadSuccess(); // 🔁 refresh list
      } else {
        toast.error("⚠️ Upload failed — unexpected server response.");
      }
    } catch (err) {
      console.error("Upload error:", err);
      if (err.response?.status === 413) {
        toast.error("🚫 File too large (max 15MB).");
      } else if (err.response?.data?.message) {
        toast.error(`❌ ${err.response.data.message}`);
      } else {
        toast.error("Upload failed. Please try again.");
      }
    } finally {
      setIsUploading(false);
      setProgress(0);
    }
  };

  return (
    <div className="bg-white p-6 rounded-2xl shadow-lg mb-6">
      <h2 className="text-xl font-bold text-gray-800 mb-4">📘 Upload Learning Resource</h2>

      <div className="flex flex-col gap-3 mb-4">
        <input
          type="text"
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
          placeholder="Enter topic name (e.g. Machine Learning)"
          className="border p-2 rounded-lg w-full focus:ring-2 focus:ring-blue-400 outline-none"
        />

        <input
          type="file"
          accept="application/pdf"
          onChange={handleFileChange}
          className="border p-2 rounded-lg"
        />

        {/* Progress bar */}
        {isUploading && (
          <div className="w-full bg-gray-200 rounded-full h-3 mt-2 overflow-hidden">
            <div
              className="bg-blue-600 h-3 transition-all duration-300 text-xs text-white text-center"
              style={{ width: `${progress}%` }}
            >
              {progress}%
            </div>
          </div>
        )}
      </div>

      <button
        onClick={handleUpload}
        disabled={isUploading}
        className={`w-full py-2 rounded-lg font-semibold text-white transition ${
          isUploading
            ? "bg-gray-400 cursor-not-allowed"
            : "bg-blue-600 hover:bg-blue-700"
        }`}
      >
        {isUploading ? "⏳ Uploading..." : "⬆️ Upload Resource"}
      </button>
    </div>
  );
};

export default ResourceUpload;
