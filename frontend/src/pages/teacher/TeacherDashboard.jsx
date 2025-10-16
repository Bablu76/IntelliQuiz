import React, { useState } from "react";
import ResourceUpload from "../../components/ResourceUpload";
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip } from "recharts";

export default function TeacherDashboard() {
  const [activeTab, setActiveTab] = useState("resources");

  // ---- Mock Data ----
  const mockResources = [
    { id: 1, name: "AI_Fundamentals.pdf", uploaded: "2025-10-10" },
    { id: 2, name: "ML_Techniques.pdf", uploaded: "2025-10-09" },
  ];

  const mockStudents = [
    { id: 1, name: "Alice Johnson", email: "alice@student.com", status: "Active" },
    { id: 2, name: "Bob Smith", email: "bob@student.com", status: "Active" },
    { id: 3, name: "Charlie Brown", email: "charlie@student.com", status: "Pending" },
  ];

  const mockPerformance = [
    { quiz: 1, avg: 72 },
    { quiz: 2, avg: 78 },
    { quiz: 3, avg: 83 },
    { quiz: 4, avg: 80 },
  ];

  // ---- Render Sections ----
  const renderContent = () => {
    switch (activeTab) {
      case "resources":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">📚 My Resources</h2>
            <ResourceUpload />
            <h3 className="mt-6 font-medium text-gray-700 mb-2">Uploaded Files</h3>
            <ul className="divide-y">
              {mockResources.map((r) => (
                <li key={r.id} className="flex justify-between py-2">
                  <span>{r.name}</span>
                  <span className="text-sm text-gray-500">{r.uploaded}</span>
                </li>
              ))}
            </ul>
          </div>
        );

      case "quiz":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">🧠 Create Quiz</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                alert("✅ Mock: Quiz created successfully!");
              }}
              className="space-y-4"
            >
              <input
                type="text"
                placeholder="Enter topic"
                className="w-full border rounded-lg px-4 py-2"
                required
              />
              <select className="w-full border rounded-lg px-4 py-2" required>
                <option value="">Select difficulty</option>
                <option value="easy">Easy</option>
                <option value="medium">Medium</option>
                <option value="hard">Hard</option>
              </select>
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
            <table className="w-full border text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left px-4 py-2">Name</th>
                  <th className="text-left px-4 py-2">Email</th>
                  <th className="text-left px-4 py-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {mockStudents.map((s) => (
                  <tr key={s.id} className="border-t hover:bg-gray-50">
                    <td className="px-4 py-2">{s.name}</td>
                    <td className="px-4 py-2">{s.email}</td>
                    <td className="px-4 py-2">{s.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <button className="mt-4 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700">
              ➕ Invite Student
            </button>
          </div>
        );

      case "analytics":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">📈 Student Performance</h2>
            <LineChart width={800} height={300} data={mockPerformance}>
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

        {/* Tabs */}
        <div className="flex gap-3 mb-6">
          {[
            { id: "resources", label: "📚 My Resources" },
            { id: "quiz", label: "🧠 Create Quiz" },
            { id: "classroom", label: "🏫 Classroom" },
            { id: "analytics", label: "📈 Performance" },
          ].map((tab) => (
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
