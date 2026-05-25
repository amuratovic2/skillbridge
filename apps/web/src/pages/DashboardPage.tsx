import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import FeedbackBanner from '../components/ui/FeedbackBanner';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';
import { ordersApi } from '../lib/orders';

interface CardDef {
  to: string;
  title: string;
  subtitle: string;
  iconPath: string;
  variant?: 'default' | 'danger';
}

type Flash = { type: 'success' | 'error' | 'info'; text: string };

export default function DashboardPage() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [stats, setStats] = useState({ orders: 0, rating: 0, reviews: 0 });

  const isFreelancer = user?.role === 'FREELANCER';
  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    if (!user) return;
    api
      .get(`/reviews/rating/${user.id}`)
      .then((res) => {
        setStats((s) => ({
          ...s,
          rating: res.data.data.averageRating,
          reviews: res.data.data.totalReviews,
        }));
      })
      .catch(() => {});

    const orderLoader = isFreelancer ? ordersApi.selling() : ordersApi.buying();
    orderLoader
      .then((r) => setStats((s) => ({ ...s, orders: r.meta?.total ?? r.data.length })))
      .catch(() => {});
  }, [user, isFreelancer]);

  const baseCards: CardDef[] = [
    {
      to: '/dashboard/profile',
      title: 'Moj profil',
      subtitle: 'Azurirajte podatke, vjestine i portfolio',
      iconPath: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
    },
    {
      to: '/dashboard/orders',
      title: 'Moje narudžbe',
      subtitle: 'Pregledajte status narudžbi',
      iconPath:
        'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2',
    },
    {
      to: '/dashboard/messages',
      title: 'Poruke',
      subtitle: 'Komunikacija sa korisnicima',
      iconPath:
        'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
    },
    {
      to: '/dashboard/custom-offers',
      title: 'Prilagođene ponude',
      subtitle: isFreelancer ? 'Šaljite i pratite ponude' : 'Pregledajte primljene ponude',
      iconPath:
        'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
    },
  ];

  const freelancerCards: CardDef[] = [
    {
      to: '/dashboard/gigs/create',
      title: 'Kreiraj uslugu',
      subtitle: 'Objavi novu uslugu',
      iconPath: 'M12 4v16m8-8H4',
    },
    {
      to: '/dashboard/revenue',
      title: 'Moja zarada',
      subtitle: 'Pregled prihoda',
      iconPath:
        'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1',
    },
    {
      to: `/freelancer/${user?.id ?? ''}`,
      title: 'Javni profil',
      subtitle: 'Pogledajte javni profil',
      iconPath: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
    },
  ];

  const adminCards: CardDef[] = [
    {
      to: '/dashboard/stats',
      title: 'Statistika narudžbi',
      subtitle: 'Pregled po statusu',
      iconPath: 'M3 3v18h18M7 16V8m4 8V12m4 4V6',
    },
    {
      to: '/dashboard/overdue',
      title: 'Probijeni rokovi',
      subtitle: 'Narudžbe sa kašnjenjem',
      iconPath:
        'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
      variant: 'danger',
    },
    {
      to: '/gigs',
      title: 'Pretraži usluge',
      subtitle: 'Pronađite freelancera',
      iconPath: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z',
    },
  ];

  const clientCards: CardDef[] = [
    {
      to: '/gigs',
      title: 'Pretraži usluge',
      subtitle: 'Pronađite freelancera',
      iconPath: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z',
    },
  ];

  const cards = isFreelancer
    ? [...baseCards, ...freelancerCards]
    : isAdmin
      ? [...baseCards, ...adminCards]
      : [...baseCards, ...clientCards];
  const flash = (location.state as { flash?: Flash } | null)?.flash;
  const dismissFlash = () => {
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">Dashboard</h1>

      {flash && (
        <FeedbackBanner type={flash.type} className="mb-6">
          <div className="flex items-center justify-between gap-3">
            <span>{flash.text}</span>
            <button type="button" onClick={dismissFlash} className="text-xs font-medium underline">
              Zatvori
            </button>
          </div>
        </FeedbackBanner>
      )}

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

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((c) => (
          <Link
            key={c.to + c.title}
            to={c.to}
            className="bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 hover:shadow-sm transition-all"
          >
            <svg
              className={`w-8 h-8 mb-3 ${
                c.variant === 'danger' ? 'text-red-500' : 'text-primary-600'
              }`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={c.iconPath} />
            </svg>
            <h3 className="font-medium text-gray-900">{c.title}</h3>
            <p className="text-sm text-gray-500 mt-1">{c.subtitle}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
