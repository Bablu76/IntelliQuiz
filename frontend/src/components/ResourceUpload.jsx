import React, { useState } from "react";

export default function ResourceUpload() {
  const [file, setFile] = useState(null);

  const handleUpload = () => {
    if (!file) return alert("Please select a file first!");
    alert(`📤 Mock upload success: ${file.name}`);
  };

  return (
    <div className="flex flex-col items-center justify-center border-2 border-dashed border-gray-300 p-8 rounded-xl bg-gray-50 hover:bg-gray-100 transition">
      <input
        type="file"
        accept=".pdf"
        onChange={(e) => setFile(e.target.files[0])}
        className="mb-4"
      />
      <button
        onClick={handleUpload}
        className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-md text-sm shadow"
      >
        Upload PDF
      </button>
      {file && <p className="text-sm text-gray-600 mt-2">Selected: {file.name}</p>}
    </div>
  );
}
