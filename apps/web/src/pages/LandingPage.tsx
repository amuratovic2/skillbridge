import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import GigCard from '../components/ui/GigCard';
import api from '../lib/api';

export default function LandingPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [featuredGigs, setFeaturedGigs] = useState<any[]>([]);

  useEffect(() => {
    api
      .get('/gigs/featured?limit=6')
      .then((res) => setFeaturedGigs(res.data.data || []))
      .catch(() => {});
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/gigs?q=${encodeURIComponent(searchQuery)}`);
    }
  };

  const popularTags = ['Web dizajn', 'React developer', 'Logo', 'SEO'];

  return (
    <div>
      <section className="bg-gradient-to-b from-primary-50/50 to-white py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="max-w-2xl">
            <h1 className="text-4xl md:text-5xl font-bold text-gray-900 leading-tight mb-4">
              Pronađite pravi<br />talent za vaš projekat
            </h1>
            <p className="text-lg text-gray-600 mb-8">
              SkillBridge povezuje klijente sa vrhunskim freelancerima. Od
              dizajna do programiranja — pokrenite saradnju u par klikova.
            </p>
            <form onSubmit={handleSearch} className="flex gap-3 mb-4">
              <div className="flex-1 relative">
                <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input type="text" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Šta vam treba? npr. logo dizajn"
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none" />
              </div>
              <button type="submit" className="bg-primary-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-primary-700 transition-colors">
                Traži
              </button>
            </form>
            <div className="flex items-center gap-2 text-sm">
              <span className="text-gray-500">Popularno:</span>
              {popularTags.map((tag) => (
                <button key={tag} onClick={() => navigate(`/gigs?q=${encodeURIComponent(tag)}`)}
                  className="text-gray-700 hover:text-primary-600 border border-gray-200 rounded-full px-3 py-1 hover:border-primary-300 transition-colors">
                  {tag}
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="py-12 border-b border-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {[
              { value: '2,847', label: 'Aktivnih freelancera' },
              { value: '12,430', label: 'Završenih projekata' },
              { value: '4.8', label: 'Prosječna ocjena' },
              { value: '98%', label: 'Zadovoljnih klijenata' },
            ].map((stat) => (
              <div key={stat.label} className="text-center">
                <div className="text-3xl font-bold text-primary-600 mb-1">{stat.value}</div>
                <div className="text-sm text-gray-500">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Istaknute usluge</h2>
              <p className="text-gray-500 mt-1">Preporučene na osnovu kvaliteta i ocjena</p>
            </div>
            <button onClick={() => navigate('/gigs')} className="text-primary-600 hover:text-primary-700 font-medium text-sm flex items-center gap-1">
              Vidi sve
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {featuredGigs.map((gig: any) => (
              <GigCard
                key={gig.id}
                id={gig.id}
                title={gig.title}
                cost={Number(gig.cost)}
                deliveryTime={gig.deliveryTime}
                coverImage={gig.coverImage}
                freelancerName={gig.freelancerName || 'Freelancer'}
                badge="Top Rated"
              />
            ))}
          </div>
        </div>
      </section>

      {!isAuthenticated && (
        <section className="bg-primary-600 py-16">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            <h2 className="text-3xl font-bold text-white mb-4">Spremni da pokrenete projekat?</h2>
            <p className="text-primary-100 mb-8 max-w-xl mx-auto">
              Registrujte se besplatno i počnite sarađivati sa najboljim freelancerima.
            </p>
            <button onClick={() => navigate('/register')}
              className="bg-white text-primary-600 px-8 py-3 rounded-full font-medium hover:bg-primary-50 transition-colors">
              Započni besplatno
            </button>
          </div>
        </section>
      )}
    </div>
  );
}
