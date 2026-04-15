import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import Layout from '../components/layout/Layout';
import ProtectedRoute from '../components/ProtectedRoute';
import LandingPage from '../pages/LandingPage';
import GigListingPage from '../pages/GigListingPage';
import GigDetailPage from '../pages/GigDetailPage';
import FreelancerProfilePage from '../pages/FreelancerProfilePage';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import DashboardPage from '../pages/DashboardPage';
import MyOrdersPage from '../pages/MyOrdersPage';
import OrderDetailPage from '../pages/OrderDetailPage';
import CreateGigPage from '../pages/CreateGigPage';
import EditGigPage from '../pages/EditGigPage';
import MessagesPage from '../pages/MessagesPage';

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<LandingPage />} />
          <Route path="/gigs" element={<GigListingPage />} />
          <Route path="/gigs/:id" element={<GigDetailPage />} />
          <Route path="/freelancer/:id" element={<FreelancerProfilePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard/orders"
            element={
              <ProtectedRoute>
                <MyOrdersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard/orders/:id"
            element={
              <ProtectedRoute>
                <OrderDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard/gigs/create"
            element={
              <ProtectedRoute roles={['FREELANCER']}>
                <CreateGigPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard/gigs/edit/:id"
            element={
              <ProtectedRoute roles={['FREELANCER']}>
                <EditGigPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard/messages"
            element={
              <ProtectedRoute>
                <MessagesPage />
              </ProtectedRoute>
            }
          />
        </Route>
      </Routes>
    </AuthProvider>
  );
}

export default App;
