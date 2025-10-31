import React, { useState, useEffect } from "react";
import ResourceUpload from "../../components/ResourceUpload";
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip } from "recharts";
import axios from "axios";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";

export default function TeacherDashboard() {
  const [activeTab, setActiveTab] = useState("resources");
  const [resources, setResources] = useState([]);
  const [performance, setPerformance] = useState([]);
  const [loading, setLoading] = useState(false);
  const API_BASE = import.meta.env.VITE_API_URL;
  const token = localStorage.getItem("token");
  const navigate = useNavigate();

  // ---------- Fetch teacher resources ----------
  const fetchResources = async () => {
    setLoading(true);
    try {
      const res = await axios.get(`${API_BASE}/resources/list`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setResources(res.data);
    } catch {
      toast.error("Failed to load resources");
    } finally {
      setLoading(false);
    }
  };

  // ---------- Delete resource ----------
  const handleDeleteResource = async (id) => {
    if (!window.confirm("Delete this resource?")) return;
    try {
      await axios.delete(`${API_BASE}/resources/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      toast.success("Resource deleted");
      fetchResources();
    } catch {
      toast.error("Delete failed");
    }
  };

  // ---------- Generate quiz ----------
  const handleGenerateQuizFromResource = async (topic) => {
    try {
      const res = await axios.get(
        `${API_BASE}/quiz/generate?topic=${encodeURIComponent(topic)}&difficulty=medium`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (res.status === 200) {
        toast.success(`Quiz for "${topic}" ready!`);
        navigate(`/quiz?topic=${encodeURIComponent(topic)}`);
      }
    } catch {
      toast.error("Quiz generation failed");
    }
  };

  // ---------- Mock or real analytics ----------
  const fetchPerformance = async () => {
    try {
      const res = await axios.get(`${API_BASE}/analytics/classroom/1`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (Array.isArray(res.data.trend))
        setPerformance(res.data.trend.map((avg, i) => ({ quiz: i + 1, avg })));
      else setPerformance([]);
    } catch {
      toast.info("Using mock analytics (no classroom data yet)");
      setPerformance([
        { quiz: 1, avg: 72 },
        { quiz: 2, avg: 78 },
        { quiz: 3, avg: 83 },
        { quiz: 4, avg: 80 },
      ]);
    }
  };

  useEffect(() => {
    fetchResources();
    fetchPerformance();
  }, []);

  // ---------- UI Tabs ----------
  const tabs = [
    { id: "resources", label: "📚 My Resources" },
    { id: "quiz", label: "🧠 Create Quiz" },
    { id: "classroom", label: "🏫 Classroom" },
    { id: "analytics", label: "📈 Performance" },
  ];

  // ---------- Render Sections ----------
  const renderContent = () => {
    switch (activeTab) {
      case "resources":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">📚 My Resources</h2>
            <ResourceUpload onUploadSuccess={fetchResources} />
            <p className="text-sm text-gray-500 mt-2">* PDF limit 15 MB – Uploads appear below.</p>

            <h3 className="mt-6 font-medium text-gray-700 mb-2">Uploaded Files</h3>
            {loading ? (
              <p className="text-gray-500">Loading...</p>
            ) : resources.length === 0 ? (
              <p className="text-gray-500 text-sm">No resources uploaded yet.</p>
            ) : (
              <table className="w-full text-sm border border-gray-200 rounded-lg">
                <thead className="bg-gray-100 text-gray-700">
                  <tr>
                    <th className="px-3 py-2 text-left">File Name</th>
                    <th className="px-3 py-2 text-left">Topic</th>
                    <th className="px-3 py-2 text-left">Uploaded</th>
                    <th className="px-3 py-2 text-center">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {resources.map((r) => (
                    <tr key={r.id} className="border-t">
                      <td className="px-3 py-2">{r.fileName}</td>
                      <td className="px-3 py-2">{r.topic || "—"}</td>
                      <td className="px-3 py-2">
                        {new Date(r.uploadedAt).toLocaleDateString()}
                      </td>
                      <td className="px-3 py-2 text-center space-x-2">
                        <button
                          onClick={() => handleGenerateQuizFromResource(r.topic || "General")}
                          className="bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
                        >
                          Create Quiz
                        </button>
                        <button
                          onClick={() => handleDeleteResource(r.id)}
                          className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600"
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        );

      case "quiz":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">🧠 Create Quiz (Manual)</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleGenerateQuizFromResource(e.target.topic.value);
              }}
              className="space-y-4"
            >
              <input
                type="text"
                name="topic"
                placeholder="Enter topic"
                className="w-full border rounded-lg px-4 py-2"
                required
              />
              <button
                type="submit"
                className="bg-blue-600 text-white px-5 py-2 rounded-lg hover:bg-blue-700"
              >
                Generate Quiz
              </button>
            </form>
          </div>
        );

      case "classroom":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">🏫 Classroom</h2>
            <p className="text-gray-600 text-sm mb-4">
              Coming soon — class management and invites.
            </p>
          </div>
        );

      case "analytics":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">📈 Student Performance</h2>
            <LineChart width={800} height={300} data={performance}>
              <Line type="monotone" dataKey="avg" stroke="#2563eb" strokeWidth={3} />
              <CartesianGrid stroke="#e5e7eb" strokeDasharray="3 3" />
              <XAxis dataKey="quiz" />
              <YAxis domain={[0, 100]} />
              <Tooltip />
            </LineChart>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-6">
      <div className="max-w-6xl mx-auto bg-white rounded-2xl shadow-lg p-6 border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">🧑‍🏫 Teacher Dashboard</h1>

        <div className="flex gap-3 mb-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                activeTab === tab.id
                  ? "bg-blue-600 text-white shadow"
                  : "bg-gray-100 hover:bg-gray-200 text-gray-700"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {renderContent()}
      </div>
    </div>
  );
}
