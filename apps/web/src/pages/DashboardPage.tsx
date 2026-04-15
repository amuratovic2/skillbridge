import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';

export default function DashboardPage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<any>(null);
  const [stats, setStats] = useState({ orders: 0, rating: 0, reviews: 0 });

  useEffect(() => {
    if (user) {
      api.get(`/users/${user.id}`).then((res) => setProfile(res.data.data)).catch(() => {});
      api.get(`/reviews/rating/${user.id}`).then((res) => {
        setStats((s) => ({
          ...s,
          rating: res.data.data.averageRating,
          reviews: res.data.data.totalReviews,
        }));
      }).catch(() => {});
    }
  }, [user]);

  const isFreelancer = user?.role === 'FREELANCER';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <div className="text-sm text-gray-500 mb-1">
            {isFreelancer ? 'Primljene narudžbe' : 'Moje narudžbe'}
          </div>
          <div className="text-2xl font-bold text-gray-900">{stats.orders}</div>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <div className="text-sm text-gray-500 mb-1">Prosječna ocjena</div>
          <div className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            {stats.rating > 0 ? stats.rating : '—'}
            {stats.rating > 0 && (
              <svg className="w-5 h-5 text-yellow-400 fill-current" viewBox="0 0 20 20">
                <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
              </svg>
            )}
          </div>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <div className="text-sm text-gray-500 mb-1">Recenzije</div>
          <div className="text-2xl font-bold text-gray-900">{stats.reviews}</div>
        </div>
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Link
          to="/dashboard/orders"
          className="bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 hover:shadow-sm transition-all"
        >
          <svg className="w-8 h-8 text-primary-600 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <h3 className="font-medium text-gray-900">Moje narudžbe</h3>
          <p className="text-sm text-gray-500 mt-1">Pregledajte status narudžbi</p>
        </Link>

        <Link
          to="/dashboard/messages"
          className="bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 hover:shadow-sm transition-all"
        >
          <svg className="w-8 h-8 text-primary-600 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <h3 className="font-medium text-gray-900">Poruke</h3>
          <p className="text-sm text-gray-500 mt-1">Komunikacija sa korisnicima</p>
        </Link>

        {isFreelancer && (
          <>
            <Link
              to="/dashboard/gigs/create"
              className="bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 hover:shadow-sm transition-all"
            >
              <svg className="w-8 h-8 text-primary-600 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              <h3 className="font-medium text-gray-900">Kreiraj uslugu</h3>
              <p className="text-sm text-gray-500 mt-1">Objavi novu uslugu</p>
            </Link>

            <Link
              to={`/freelancer/${user?.id}`}
              className="bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 hover:shadow-sm transition-all"
            >
              <svg className="w-8 h-8 text-primary-600 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <h3 className="font-medium text-gray-900">Moj profil</h3>
              <p className="text-sm text-gray-500 mt-1">Pogledajte javni profil</p>
            </Link>
          </>
        )}

        {!isFreelancer && (
          <Link
            to="/gigs"
            className="bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 hover:shadow-sm transition-all"
          >
            <svg className="w-8 h-8 text-primary-600 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <h3 className="font-medium text-gray-900">Pretraži usluge</h3>
            <p className="text-sm text-gray-500 mt-1">Pronađite freelancera</p>
          </Link>
        )}
      </div>
    </div>
  );
}
