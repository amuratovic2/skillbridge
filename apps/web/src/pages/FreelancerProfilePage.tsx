import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import StarRating from '../components/ui/StarRating';
import SkillTag from '../components/ui/SkillTag';
import GigCard from '../components/ui/GigCard';
import api, { getApiErrorMessage } from '../lib/api';
import { userDisplayName, userInitials, userServiceApi } from '../lib/user-service';
import type { UserProfile } from '../lib/user-service';

interface GigSummary {
  id: number;
  title: string;
  cost: number;
  deliveryTime: number;
  coverImage?: string | null;
}

interface RatingSummary {
  averageRating: number;
  totalReviews: number;
}

export default function FreelancerProfilePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [gigs, setGigs] = useState<GigSummary[]>([]);
  const [ratingData, setRatingData] = useState<RatingSummary>({ averageRating: 0, totalReviews: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);

    Promise.all([
      userServiceApi.byId(id),
      userServiceApi.userSkills(id).catch(() => []),
      userServiceApi.userPortfolio(id).catch(() => []),
      api
        .get(`/gigs/freelancer/${id}`)
        .then((res) => setGigs((res.data.data || []) as GigSummary[]))
        .catch(() => setGigs([])),
      api.get(`/reviews/rating/${id}`).then((res) => setRatingData(res.data.data as RatingSummary)).catch(() => {
        setRatingData({ averageRating: 0, totalReviews: 0 });
      }),
    ])
      .then(([profileData, skills, portfolioItems]) => {
        setProfile({
          ...profileData,
          skills: skills.length > 0 ? skills : profileData.skills,
          portfolioItems: portfolioItems.length > 0 ? portfolioItems : profileData.portfolioItems,
        });
      })
      .catch((err: unknown) => {
        setProfile(null);
        setGigs([]);
        setError(getApiErrorMessage(err, 'Profil nije moguce ucitati.'));
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <h1 className="text-xl font-semibold text-gray-900">Profil nije dostupan</h1>
        <p className="text-sm text-gray-500 mt-2">{error || 'Korisnik nije pronadjen ili profil nije aktivan.'}</p>
        <Link
          to="/freelancers"
          className="inline-flex mt-6 px-4 py-2 rounded-lg bg-primary-600 text-white text-sm font-medium hover:bg-primary-700"
        >
          Nazad na freelancere
        </Link>
      </div>
    );
  }

  const skills = profile.skills ?? [];
  const portfolioItems = profile.portfolioItems ?? [];
  const initials = userInitials(profile);
  const displayName = userDisplayName(profile);
  const memberSince = profile.createdAt
    ? new Date(profile.createdAt).toLocaleDateString('bs', { month: 'short', year: 'numeric' })
    : 'nedavno';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/gigs" className="text-sm text-gray-500 hover:text-gray-700 mb-6 inline-flex items-center gap-1">
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        Nazad
      </Link>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-4">
        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white border border-gray-200 rounded-xl p-6 text-center">
            <div className="w-24 h-24 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center text-3xl font-bold mx-auto mb-4">
              {profile.profilePicture ? (
                <img src={profile.profilePicture} alt={displayName} className="w-full h-full rounded-full object-cover" />
              ) : (
                initials
              )}
            </div>
            <h1 className="text-xl font-bold text-gray-900 mb-1">{displayName}</h1>
            <span className="inline-block px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-xs font-medium mb-3">
              Top Rated
            </span>
            <div className="flex items-center justify-center mb-4">
              <StarRating rating={ratingData.averageRating} count={ratingData.totalReviews} size="md" />
            </div>
            <div className="space-y-2 text-sm text-gray-500 mb-6">
              {profile.country && (
                <p className="flex items-center justify-center gap-1">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  {profile.country}
                </p>
              )}
              <p className="flex items-center justify-center gap-1">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                Član od {memberSince}
              </p>
              <p className="flex items-center justify-center gap-1">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {ratingData.totalReviews} završenih narudžbi
              </p>
            </div>
            <button
              onClick={() => {
                if (!isAuthenticated) { navigate('/login'); return; }
                if (user?.id === profile.id) { navigate('/dashboard/profile'); return; }
                navigate(`/dashboard/messages?to=${id}`);
              }}
              className="w-full bg-primary-600 text-white py-2.5 rounded-lg font-medium hover:bg-primary-700 transition-colors"
            >
              {user?.id === profile.id ? 'Uredi profil' : 'Kontaktirajte'}
            </button>
          </div>

          {/* Skills */}
          {skills.length > 0 && (
            <div className="bg-white border border-gray-200 rounded-xl p-6 mt-4">
              <h3 className="font-semibold text-gray-900 mb-3">Vještine</h3>
              <div className="flex flex-wrap gap-2">
                {skills.map((skill) => (
                  <SkillTag key={skill.id} name={skill.name} />
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Main */}
        <div className="lg:col-span-2 space-y-8">
          {/* Bio */}
          {profile.bio && (
            <div>
              <h2 className="text-lg font-semibold text-gray-900 mb-3">O meni</h2>
              <p className="text-gray-600 leading-relaxed whitespace-pre-line">{profile.bio}</p>
            </div>
          )}

          {/* Portfolio */}
          {portfolioItems.length > 0 && (
            <div>
              <h2 className="text-lg font-semibold text-gray-900 mb-3">Portfolio</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {portfolioItems.map((item) => (
                  <div key={item.id} className="bg-white border border-gray-200 rounded-lg overflow-hidden">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.title} className="w-full aspect-square object-cover bg-primary-50" />
                    ) : (
                      <div className="aspect-square bg-primary-50 text-center p-4 flex flex-col items-center justify-center">
                        <svg className="w-8 h-8 text-primary-200 mx-auto mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        <p className="text-xs text-primary-400">{item.title}</p>
                      </div>
                    )}
                    <div className="p-3">
                      <h3 className="font-medium text-sm text-gray-900">{item.title}</h3>
                      {item.description && (
                        <p className="text-xs text-gray-500 mt-1 line-clamp-3">{item.description}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Active Gigs */}
          {gigs.length > 0 && (
            <div>
              <h2 className="text-lg font-semibold text-gray-900 mb-3">Aktivne usluge</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {gigs.map((gig) => (
                  <GigCard
                    key={gig.id}
                    id={gig.id}
                    title={gig.title}
                    cost={Number(gig.cost)}
                    deliveryTime={gig.deliveryTime}
                    coverImage={gig.coverImage ?? undefined}
                    freelancerName={displayName}
                    rating={ratingData.averageRating}
                    reviewCount={ratingData.totalReviews}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
