import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import GigCard from '../components/ui/GigCard';
import api from '../lib/api';

export default function GigListingPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [gigs, setGigs] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [meta, setMeta] = useState({ total: 0, page: 1, totalPages: 1 });
  const [loading, setLoading] = useState(true);

  const q = searchParams.get('q') || '';
  const categoryId = searchParams.get('categoryId') || '';
  const sortBy = searchParams.get('sortBy') || 'newest';
  const page = parseInt(searchParams.get('page') || '1', 10);

  useEffect(() => {
    api.get('/categories').then((res) => setCategories(res.data.data || [])).catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (categoryId) params.set('categoryId', categoryId);
    params.set('sortBy', sortBy);
    params.set('page', String(page));
    params.set('limit', '12');

    api
      .get(`/gigs/search?${params.toString()}`)
      .then((res) => {
        setGigs(res.data.data || []);
        setMeta(res.data.meta || { total: 0, page: 1, totalPages: 1 });
      })
      .catch(() => setGigs([]))
      .finally(() => setLoading(false));
  }, [q, categoryId, sortBy, page]);

  const updateParam = (key: string, value: string) => {
    const p = new URLSearchParams(searchParams);
    if (value) p.set(key, value); else p.delete(key);
    p.set('page', '1');
    setSearchParams(p);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">
        {q ? `Rezultati za "${q}"` : 'Pretraži usluge'}
      </h1>

      <div className="flex flex-wrap items-center gap-4 mb-8 pb-6 border-b border-gray-200">
        <input type="text" value={q} onChange={(e) => updateParam('q', e.target.value)}
          placeholder="Pretraži..."
          className="border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none" />
        <select value={categoryId} onChange={(e) => updateParam('categoryId', e.target.value)}
          className="border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none">
          <option value="">Sve kategorije</option>
          {categories.map((cat: any) => (
            <option key={cat.id} value={cat.id}>{cat.title}</option>
          ))}
        </select>
        <select value={sortBy} onChange={(e) => updateParam('sortBy', e.target.value)}
          className="border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none">
          <option value="newest">Najnovije</option>
          <option value="price_asc">Cijena: niska &rarr; visoka</option>
          <option value="price_desc">Cijena: visoka &rarr; niska</option>
        </select>
        <span className="text-sm text-gray-500 ml-auto">{meta.total} rezultata</span>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
        </div>
      ) : gigs.length > 0 ? (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {gigs.map((gig: any) => (
              <GigCard
                key={gig.id}
                id={gig.id}
                title={gig.title}
                cost={Number(gig.cost)}
                deliveryTime={gig.deliveryTime}
                coverImage={gig.coverImage}
                freelancerName={gig.freelancerName || 'Freelancer'}
              />
            ))}
          </div>
          {meta.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-10">
              {Array.from({ length: meta.totalPages }, (_, i) => i + 1).map((p) => (
                <button key={p} onClick={() => updateParam('page', String(p))}
                  className={`px-4 py-2 rounded-lg text-sm ${p === page ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>
                  {p}
                </button>
              ))}
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-20">
          <p className="text-gray-500 text-lg">Nema rezultata</p>
          <p className="text-gray-400 mt-2">Pokušajte sa drugim pojmom pretrage</p>
        </div>
      )}
    </div>
  );
}
