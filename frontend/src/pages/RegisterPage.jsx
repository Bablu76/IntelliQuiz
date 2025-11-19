import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    role: 'student'
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    console.log('📝 Registration Attempt Started');
    console.log('📍 Backend URL:', 'http://localhost:8080/auth/register');
    console.log('👤 Username:', formData.username);
    console.log('📧 Email:', formData.email);
    console.log('🎭 Role:', formData.role);

    // Prepare payload with role as array
    const payload = {
      username: formData.username,
      email: formData.email,
      password: formData.password,
      role: [formData.role]
    };

    console.log('📦 Payload:', payload);

    try {
      const response = await fetch('http://localhost:8080/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      console.log('📡 Response Status:', response.status);
      console.log('📡 Response Status Text:', response.statusText);

      const contentType = response.headers.get('content-type');
      let data;
      
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
        console.log('📦 Response Data:', data);
      } else {
        const text = await response.text();
        console.log('📦 Response Text:', text);
        data = { message: text };
      }

      if (response.ok) {
        console.log('✅ Registration successful!');
        setSuccess(data.message || 'User registered successfully!');
        
        // Redirect to login after 2 seconds
        setTimeout(() => {
          console.log('🔀 Redirecting to login page...');
          navigate('/login');
        }, 2000);
      } else {
        console.error('❌ Registration failed with status:', response.status);
        setError(data.message || `Registration failed with status ${response.status}`);
      }
    } catch (err) {
      console.error('❌ Network Error:', err);
      console.error('❌ Error Message:', err.message);
      
      if (err.message.includes('Failed to fetch')) {
        setError('Cannot connect to backend. Make sure it\'s running on http://localhost:8080');
      } else {
        setError('Network error: ' + err.message);
      }
    } finally {
      setLoading(false);
      console.log('🏁 Registration attempt completed');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-100 to-purple-100 p-6">
      <div className="bg-white shadow-lg rounded-lg p-8 w-full max-w-md">
        <h2 className="text-3xl font-bold text-center text-gray-800 mb-6">
          📝 Create Account
        </h2>

        {/* Debug Info
        <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded text-sm">
          <p className="text-blue-700 font-semibold">Debug Mode Active</p>
          <p className="text-blue-600 text-xs mt-1">
            Backend: http://localhost:8080
          </p>
          <p className="text-blue-600 text-xs">
            Open Console (F12) for detailed logs
          </p>
        </div> */}

        {/* Success Message */}
        {success && (
          <div className="mb-4 p-3 bg-green-50 border border-green-300 rounded">
            <p className="text-green-700 text-sm font-medium">Success!</p>
            <p className="text-green-600 text-sm mt-1">{success}</p>
            <p className="text-green-500 text-xs mt-2">Redirecting to login...</p>
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-300 rounded">
            <p className="text-red-700 text-sm font-medium">Error:</p>
            <p className="text-red-600 text-sm mt-1">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-700 font-medium mb-1">
              Username
            </label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              placeholder="Enter username"
              required
              disabled={loading}
              minLength={3}
            />
          </div>

          <div>
            <label className="block text-gray-700 font-medium mb-1">
              Email
            </label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              placeholder="Enter email"
              required
              disabled={loading}
            />
          </div>

          <div>
            <label className="block text-gray-700 font-medium mb-1">
              Password
            </label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              placeholder="Enter password"
              required
              disabled={loading}
              minLength={6}
            />
          </div>

          <div>
            <label className="block text-gray-700 font-medium mb-1">
              Role
            </label>
            <select
              name="role"
              value={formData.role}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              required
              disabled={loading}
            >
              <option value="student">Student</option>
              <option value="teacher">Teacher</option>
              <option value="admin">Admin</option>
            </select>
          </div>

          <button
            type="submit"
            disabled={loading || success}
            className={`w-full py-2 px-4 rounded-md text-white font-semibold transition-colors ${
              loading || success
                ? 'bg-gray-400 cursor-not-allowed'
                : 'bg-blue-600 hover:bg-blue-700'
            }`}
          >
            {loading ? '🔄 Registering...' : success ? '✅ Success!' : 'Register'}
          </button>
        </form>

        {/* Login Link */}
        <div className="mt-6 text-center">
          <p className="text-gray-600 text-sm">
            Already have an account?{' '}
            <button
              onClick={() => navigate('/login')}
              className="text-blue-600 hover:text-blue-700 font-semibold hover:underline"
              disabled={loading}
            >
              Login here
            </button>
          </p>
        </div>

        {/* Troubleshooting Tips */}
        <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded">
          <p className="font-semibold text-yellow-800 text-sm mb-2">
            💡 Registration Tips:
          </p>
          <ul className="text-yellow-700 text-xs space-y-1">
            <li>• Username must be at least 3 characters</li>
            <li>• Password must be at least 6 characters</li>
            <li>• Email must be valid format</li>
            <li>• Choose your role carefully (can be changed by admin later)</li>
          </ul>
        </div>
      </div>
    </div>
  );
}