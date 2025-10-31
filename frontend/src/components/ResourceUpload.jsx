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
    if (selected && selected.type === "application/pdf") setFile(selected);
    else toast.error("Please upload a valid PDF file.");
  };

  const handleUpload = async () => {
    if (!file) return toast.error("Select a PDF before uploading.");
    setIsUploading(true);
    setProgress(0);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("topic", topic);

    try {
      await axios.post(`${API_BASE}/resources/upload`, formData, {
        headers: { Authorization: `Bearer ${token}` },
        onUploadProgress: (evt) =>
          setProgress(Math.round((evt.loaded * 100) / evt.total)),
      });

      toast.success("File uploaded successfully!");
      setFile(null);
      setTopic("");
      if (onUploadSuccess) onUploadSuccess();
    } catch (err) {
      if (err.response?.status === 413)
        toast.error("File too large (max 15 MB).");
      else if (err.response?.data?.errorType)
        toast.error(err.response.data.errorType);
      else toast.error("Upload failed. Try again.");
    } finally {
      setIsUploading(false);
      setProgress(0);
    }
  };

  return (
    <div className="bg-white p-4 rounded-2xl shadow mb-4">
      <h2 className="text-lg font-semibold mb-2">Upload Learning Resource</h2>
      <input
        type="text"
        value={topic}
        onChange={(e) => setTopic(e.target.value)}
        placeholder="Enter topic name"
        className="border p-2 rounded w-full mb-2"
      />
      <input
        type="file"
        accept=".pdf"
        onChange={handleFileChange}
        className="mb-2"
      />

      {isUploading && (
        <div className="w-full bg-gray-200 rounded mb-2">
          <div
            className="bg-blue-600 text-xs text-white p-0.5 rounded text-center"
            style={{ width: `${progress}%` }}
          >
            {progress}%
          </div>
        </div>
      )}

      <button
        onClick={handleUpload}
        disabled={isUploading}
        className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
      >
        {isUploading ? "Uploading..." : "Upload"}
      </button>
    </div>
  );
};

export default ResourceUpload;
