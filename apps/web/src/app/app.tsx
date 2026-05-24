import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { CartProvider } from '../context/CartContext';
import { ToastProvider } from '../context/ToastContext';
import Layout from '../components/layout/Layout';
import ProtectedRoute from '../components/ProtectedRoute';
import LandingPage from '../pages/LandingPage';
import GigListingPage from '../pages/GigListingPage';
import GigDetailPage from '../pages/GigDetailPage';
import FreelancerProfilePage from '../pages/FreelancerProfilePage';
import UserDirectoryPage from '../pages/UserDirectoryPage';
import ProfileSettingsPage from '../pages/ProfileSettingsPage';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import DashboardPage from '../pages/DashboardPage';
import MyOrdersPage from '../pages/MyOrdersPage';
import OrderDetailPage from '../pages/OrderDetailPage';
import CreateGigPage from '../pages/CreateGigPage';
import EditGigPage from '../pages/EditGigPage';
import MessagesPage from '../pages/MessagesPage';
import CustomOffersPage from '../pages/CustomOffersPage';
import OrderStatsPage from '../pages/OrderStatsPage';
import OverdueOrdersPage from '../pages/OverdueOrdersPage';
import RevenuePage from '../pages/RevenuePage';
import CartPage from '../pages/CartPage';
import NotFoundPage from '../pages/NotFoundPage';

export function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <ToastProvider>
          <Routes>
            <Route element={<Layout />}>
              <Route path="/" element={<LandingPage />} />
              <Route path="/gigs" element={<GigListingPage />} />
              <Route path="/gigs/:id" element={<GigDetailPage />} />
              <Route path="/freelancers" element={<UserDirectoryPage />} />
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
                path="/dashboard/profile"
                element={
                  <ProtectedRoute>
                    <ProfileSettingsPage />
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
              <Route
                path="/dashboard/custom-offers"
                element={
                  <ProtectedRoute>
                    <CustomOffersPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/dashboard/stats"
                element={
                  <ProtectedRoute>
                    <OrderStatsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/dashboard/overdue"
                element={
                  <ProtectedRoute>
                    <OverdueOrdersPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/dashboard/revenue"
                element={
                  <ProtectedRoute roles={['FREELANCER']}>
                    <RevenuePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cart"
                element={
                  <ProtectedRoute>
                    <CartPage />
                  </ProtectedRoute>
                }
              />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </CartProvider>
    </AuthProvider>
  );
}

export default App;
