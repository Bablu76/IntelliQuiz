import React, { useState } from "react";

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState("users");

  // ---- Mock Data ----
  const mockUsers = [
    { id: 1, username: "admin1", role: "ADMIN", status: "Active" },
    { id: 2, username: "teacher1", role: "TEACHER", status: "Active" },
    { id: 3, username: "student1", role: "STUDENT", status: "Pending" },
  ];

  const mockClasses = [
    { id: 1, className: "AI 101", teacher: "teacher1", students: 25 },
    { id: 2, className: "ML Advanced", teacher: "teacher2", students: 18 },
  ];

  const renderContent = () => {
    switch (activeTab) {
      case "users":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">👥 Users</h2>
            <table className="w-full border text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left px-4 py-2">Username</th>
                  <th className="text-left px-4 py-2">Role</th>
                  <th className="text-left px-4 py-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {mockUsers.map((u) => (
                  <tr key={u.id} className="border-t hover:bg-gray-50">
                    <td className="px-4 py-2">{u.username}</td>
                    <td className="px-4 py-2">
                      <select
                        defaultValue={u.role}
                        className="border rounded-lg px-2 py-1 text-sm"
                      >
                        <option value="ADMIN">Admin</option>
                        <option value="TEACHER">Teacher</option>
                        <option value="STUDENT">Student</option>
                      </select>
                    </td>
                    <td className="px-4 py-2">{u.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );

      case "classes":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">🏫 Classes</h2>
            <table className="w-full border text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left px-4 py-2">Class Name</th>
                  <th className="text-left px-4 py-2">Teacher</th>
                  <th className="text-left px-4 py-2">Students</th>
                </tr>
              </thead>
              <tbody>
                {mockClasses.map((c) => (
                  <tr key={c.id} className="border-t hover:bg-gray-50">
                    <td className="px-4 py-2">{c.className}</td>
                    <td className="px-4 py-2">{c.teacher}</td>
                    <td className="px-4 py-2">{c.students}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );

      case "settings":
        return (
          <div className="bg-white p-6 rounded-xl shadow-md text-center">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">⚙️ Settings</h2>
            <p className="text-gray-600">
              System configurations and advanced controls will appear here soon.
            </p>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-200 p-6">
      <div className="max-w-6xl mx-auto bg-white rounded-2xl shadow-lg p-6 border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">⚙️ Admin Dashboard</h1>

        {/* Tabs */}
        <div className="flex gap-3 mb-6">
          {[
            { id: "users", label: "👥 Users" },
            { id: "classes", label: "🏫 Classes" },
            { id: "settings", label: "⚙️ Settings" },
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
