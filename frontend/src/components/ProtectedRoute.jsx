import { Navigate } from 'react-router-dom';

const ProtectedRoute = ({ children }) => {
  // Check if JWT token exists in localStorage
  const token = localStorage.getItem('token');
  
  // If no token found, redirect to login page
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  
  // If token exists, render the protected component
  return children;
};

export default ProtectedRoute;