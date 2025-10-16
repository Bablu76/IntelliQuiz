import { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';

const Leaderboard = () => {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchLeaderboard = async () => {
      try {
        setLoading(true);
        console.log('📊 Fetching leaderboard data...');
        
        const token = localStorage.getItem('token'); // ✅ get JWT

        const response = await fetch('http://localhost:8080/analytics/classroom/1', {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        });

        console.log('📡 Response Status:', response.status);

        if (!response.ok) {
          throw new Error(`Failed to fetch leaderboard (${response.status})`);
        }

        const data = await response.json();
        console.log('✅ Leaderboard data received:', data);
        
        // Sort by score descending and add rank
        const sortedData = data
          .sort((a, b) => b.score - a.score)
          .map((student, index) => ({
            ...student,
            rank: index + 1
          }));
        
        setStudents(sortedData);
        setError(null);
      } catch (err) {
        console.error('❌ Error fetching leaderboard:', err);
        setError(err.message);
        
        // Mock data for display if API fails
        const mockData = [
          { rank: 1, studentName: 'student1', score: 85 },
          { rank: 2, studentName: 'student2', score: 80 },
          { rank: 3, studentName: 'student3', score: 78 }
        ];
        setStudents(mockData);
      } finally {
        setLoading(false);
      }
    };

    fetchLeaderboard();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center p-8">
        <div className="text-gray-600">Loading leaderboard...</div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-4xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4">
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            🏆 Classroom Leaderboard
          </h2>
          <p className="text-blue-100 text-sm mt-1">Top performers in the class</p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mx-6 my-4">
            <div className="flex items-center">
              <span className="text-yellow-700 text-sm">
                ⚠️ Could not connect to server. Showing mock data.
              </span>
            </div>
          </div>
        )}

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b-2 border-gray-200">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Rank
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Student Name
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Score
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {students.length === 0 ? (
                <tr>
                  <td colSpan="3" className="px-6 py-8 text-center text-gray-500">
                    No students found
                  </td>
                </tr>
              ) : (
                students.map((student) => (
                  <tr 
                    key={student.rank}
                    className="hover:bg-gray-50 transition-colors duration-150"
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        {student.rank === 1 && <span className="text-2xl">🥇</span>}
                        {student.rank === 2 && <span className="text-2xl">🥈</span>}
                        {student.rank === 3 && <span className="text-2xl">🥉</span>}
                        <span className="text-lg font-semibold text-gray-700">
                          {student.rank}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {student.studentName}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <span className="text-lg font-bold text-blue-600">
                          {student.score}%
                        </span>
                        <div className="w-24 bg-gray-200 rounded-full h-2">
                          <div 
                            className="bg-blue-600 h-2 rounded-full transition-all duration-500"
                            style={{ width: `${student.score}%` }}
                          />
                        </div>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="bg-gray-50 px-6 py-3 border-t border-gray-200">
          <p className="text-xs text-gray-500 text-center">
            Showing top {students.length} students
          </p>
        </div>
      </div>
    </div>
  );
};

export default Leaderboard;