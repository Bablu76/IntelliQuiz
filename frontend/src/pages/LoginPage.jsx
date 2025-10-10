// File: src/pages/LoginPage.jsx

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    // Enhanced debug logs
    console.log('🔐 Login Attempt Started');
    console.log('📍 Backend URL:', 'http://localhost:8080/auth/login');
    console.log('👤 Username:', username);
    console.log('⏰ Timestamp:', new Date().toISOString());

    try {
      const response = await fetch('http://localhost:8080/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      console.log('📡 Response Status:', response.status);
      console.log('📡 Response Status Text:', response.statusText);
      console.log('📡 Response OK:', response.ok);
      console.log('📡 Response Headers:', Object.fromEntries(response.headers.entries()));

      // Try to parse response
      const contentType = response.headers.get('content-type');
      let data;
      
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
        console.log('📦 Response Data (JSON):', data);
      } else {
        const text = await response.text();
        console.log('📦 Response Data (Text):', text);
        data = { message: text };
      }

      if (response.ok) {
        // Success
        if (data.token) {
          localStorage.setItem('token', data.token);
          console.log('✅ Login successful! Token stored.');
          console.log('🎫 Token preview:', data.token.substring(0, 20) + '...');
          navigate('/dashboard');
        } else {
          console.error('⚠️ No token in response:', data);
          setError('Login successful but no token received. Check backend response format.');
        }
      } else {
        // Error responses
        console.error('❌ Login failed with status:', response.status);
        
        if (response.status === 401) {
          setError('Invalid username or password (401)');
        } else if (response.status === 403) {
          setError('Access forbidden (403). User may not exist or is locked.');
        } else if (response.status === 500) {
          setError('Server error (500). Check backend logs.');
        } else {
          setError(data.message || `Login failed with status ${response.status}`);
        }
      }
    } catch (err) {
      console.error('❌ CATCH BLOCK - Network Error:', err);
      console.error('❌ Error Name:', err.name);
      console.error('❌ Error Message:', err.message);
      console.error('❌ Error Stack:', err.stack);
      
      // Specific error handling
      if (err.message.includes('Failed to fetch') || err.name === 'TypeError') {
        setError('❌ Cannot connect to backend. Is it running on http://localhost:8080?');
      } else if (err.message.includes('NetworkError')) {
        setError('❌ Network error. Check CORS configuration on backend.');
      } else {
        setError('❌ Unexpected error: ' + err.message);
      }
    } finally {
      setLoading(false);
      console.log('🏁 Login attempt completed');
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h2 className="text-2xl font-bold mb-6 text-center text-gray-800">
          IntelliQuiz Login
        </h2>
        
        {/* Debug Info */}
        <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded text-sm">
          <p className="text-blue-700 font-semibold">Debug Mode Active</p>
          <p className="text-blue-600 text-xs mt-1">
            Backend: http://localhost:8080
          </p>
          <p className="text-blue-600 text-xs">
            Open DevTools Console (F12) for detailed logs
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Username
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter username"
              required
              disabled={loading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter password"
              required
              disabled={loading}
            />
          </div>

          {error && (
            <div className="p-3 bg-red-50 border border-red-300 rounded">
              <p className="text-red-700 text-sm font-medium">Error:</p>
              <p className="text-red-600 text-sm mt-1">{error}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className={`w-full py-2 px-4 rounded-md text-white font-medium transition-colors ${
              loading
                ? 'bg-gray-400 cursor-not-allowed'
                : 'bg-blue-600 hover:bg-blue-700'
            }`}
          >
            {loading ? '🔄 Logging in...' : '🚀 Login'}
          </button>
        </form>

        {/* Troubleshooting Tips */}
        <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded">
          <p className="font-semibold text-yellow-800 text-sm mb-2">
            🔧 Troubleshooting Checklist:
          </p>
          <ul className="text-yellow-700 text-xs space-y-1">
            <li>✓ Backend running? Test: curl http://localhost:8080/auth/login</li>
            <li>✓ CORS enabled? Add @CrossOrigin to AuthController</li>
            <li>✓ Valid credentials? Use Postman-registered user</li>
            <li>✓ Check browser Console (F12) for errors</li>
            <li>✓ Check Network tab for request details</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;