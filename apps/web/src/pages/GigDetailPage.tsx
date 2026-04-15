import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import StarRating from '../components/ui/StarRating';
import api from '../lib/api';

export default function GigDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [gig, setGig] = useState<any>(null);
  const [freelancer, setFreelancer] = useState<any>(null);
  const [ratingData, setRatingData] = useState({ averageRating: 0, totalReviews: 0 });
  const [loading, setLoading] = useState(true);

  const isOwner = user && gig && user.id === gig.freelancerId;

  useEffect(() => {
    api
      .get(`/gigs/${id}`)
      .then(async (res) => {
        const gigData = res.data.data;
        setGig(gigData);
        try {
          const [userRes, ratingRes] = await Promise.all([
            api.get(`/users/${gigData.freelancerId}`),
            api.get(`/reviews/rating/${gigData.freelancerId}`),
          ]);
          setFreelancer(userRes.data.data);
          setRatingData(ratingRes.data.data);
        } catch { /* ignore */ }
      })
      .catch(() => navigate('/gigs'))
      .finally(() => setLoading(false));
  }, [id, navigate]);

  const handleOrder = async () => {
    if (!isAuthenticated) { navigate('/login'); return; }
    if (!confirm(`Potvrdite narudžbu:\n\n${gig.title}\nCijena: ${Number(gig.cost)} €\nRok isporuke: ${gig.deliveryTime} dana\nRevizije: ${gig.revisionCount}\n\nDa li želite naručiti?`)) return;
    try {
      await api.post('/orders', {
        gigId: gig.id,
        totalCost: Number(gig.cost),
        maxRevisions: gig.revisionCount,
        deliveryDays: gig.deliveryTime,
      });
      alert('Narudžba uspješno kreirana!');
      navigate('/dashboard/orders');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Greška pri kreiranju narudžbe';
      alert(msg);
      console.error('Order error:', err.response?.data);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Da li ste sigurni da želite obrisati ovu uslugu?')) return;
    try {
      await api.delete(`/gigs/${id}`);
      navigate('/gigs');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Greška pri brisanju');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (!gig) return null;

  const freelancerName = freelancer
    ? [freelancer.firstName, freelancer.lastName].filter(Boolean).join(' ') || freelancer.username
    : 'Freelancer';
  const freelancerInitial = freelancerName.charAt(0).toUpperCase();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/gigs" className="text-sm text-gray-500 hover:text-gray-700 mb-4 inline-flex items-center gap-1">
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        Nazad na rezultate
      </Link>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-4">
        <div className="lg:col-span-2">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">{gig.title}</h1>
          <div className="flex items-center gap-3 mb-6">
            <Link to={`/freelancer/${gig.freelancerId}`} className="flex items-center gap-2">
              <div className="w-8 h-8 bg-primary-600 text-white rounded-full flex items-center justify-center text-sm font-medium">
                {freelancerInitial}
              </div>
              <span className="text-sm font-medium text-gray-900">{freelancerName}</span>
            </Link>
            {ratingData.totalReviews > 0 && (
              <StarRating rating={ratingData.averageRating} count={ratingData.totalReviews} />
            )}
          </div>
          <div className="aspect-video bg-primary-50 rounded-xl mb-8 flex items-center justify-center overflow-hidden">
            {gig.coverImage ? (
              <img src={gig.coverImage} alt={gig.title} className="w-full h-full object-cover" />
            ) : (
              <div className="text-center text-primary-300">
                <svg className="w-16 h-16 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                <p className="text-sm">Portfolio primjer</p>
              </div>
            )}
          </div>
          {gig.description && (
            <div className="mb-8">
              <h2 className="text-lg font-semibold text-gray-900 mb-3">Opis usluge</h2>
              <p className="text-gray-600 leading-relaxed whitespace-pre-line">{gig.description}</p>
            </div>
          )}
          {gig.tags?.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {gig.tags.map((tag: any) => (
                <span key={tag.id || tag.name} className="px-3 py-1 bg-gray-100 text-gray-600 rounded-full text-sm">
                  {tag.name}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white border border-gray-200 rounded-xl p-6 sticky top-24">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm text-gray-500">Cijena paketa</span>
              <span className="text-3xl font-bold text-gray-900">{Number(gig.cost)} &euro;</span>
            </div>
            <div className="space-y-3 mb-6 text-sm text-gray-600">
              <div className="flex items-center gap-2">
                <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Rok isporuke: <strong>{gig.deliveryTime} dana</strong>
              </div>
              <div className="flex items-center gap-2">
                <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Revizije: <strong>{gig.revisionCount}</strong>
              </div>
            </div>

            {isOwner ? (
              <>
                <button
                  onClick={() => navigate(`/dashboard/gigs/edit/${id}`)}
                  className="w-full bg-primary-600 text-white py-3 rounded-lg font-medium hover:bg-primary-700 transition-colors mb-3"
                >
                  Uredi uslugu
                </button>
                <button
                  onClick={handleDelete}
                  className="w-full border border-red-300 text-red-600 py-3 rounded-lg font-medium hover:bg-red-50 transition-colors"
                >
                  Obriši uslugu
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={handleOrder}
                  className="w-full bg-primary-600 text-white py-3 rounded-lg font-medium hover:bg-primary-700 transition-colors mb-3"
                >
                  Naruči sada
                </button>
                <button
                  onClick={() => {
                    if (!isAuthenticated) { navigate('/login'); return; }
                    navigate(`/dashboard/messages?to=${gig.freelancerId}`);
                  }}
                  className="w-full border border-gray-300 text-gray-700 py-3 rounded-lg font-medium hover:bg-gray-50 transition-colors flex items-center justify-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  Pošalji zahtjev
                </button>
                <p className="text-xs text-gray-400 text-center mt-4 flex items-center justify-center gap-1">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  Sigurna transakcija sa zaštitom kupca
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
