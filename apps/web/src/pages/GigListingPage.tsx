import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import FeedbackBanner from '../components/ui/FeedbackBanner';
import GigCard from '../components/ui/GigCard';
import api, { getApiErrorMessage } from '../lib/api';

type Flash = { type: 'success' | 'error' | 'info'; text: string };

export default function GigListingPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [gigs, setGigs] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [meta, setMeta] = useState({ total: 0, page: 1, totalPages: 1 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [queryInput, setQueryInput] = useState(searchParams.get('q') || '');

  const flash = (location.state as { flash?: Flash } | null)?.flash;
  const q = searchParams.get('q') || '';
  const categoryId = searchParams.get('categoryId') || '';
  const sortBy = searchParams.get('sortBy') || 'newest';
  const page = Math.max(Number.parseInt(searchParams.get('page') || '1', 10), 1);

  useEffect(() => {
    api
      .get('/categories')
      .then((res) => setCategories(res.data.data || []))
      .catch(() => setError('Kategorije trenutno nije moguce ucitati.'));
  }, []);

  useEffect(() => {
    setQueryInput(q);
  }, [q]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      updateParam('q', queryInput.trim());
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [queryInput]);

  useEffect(() => {
    setLoading(true);
    setError('');
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
      .catch((err: unknown) => {
        setGigs([]);
        setMeta({ total: 0, page: 1, totalPages: 1 });
        setError(getApiErrorMessage(err, 'Usluge trenutno nije moguce ucitati.'));
      })
      .finally(() => setLoading(false));
  }, [q, categoryId, sortBy, page]);

  const pageNumbers = useMemo(() => {
    const total = Math.max(meta.totalPages, 1);
    const start = Math.max(1, Math.min(page - 2, total - 4));
    const end = Math.min(total, start + 4);
    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [meta.totalPages, page]);

  const updateParam = (key: string, value: string) => {
    const p = new URLSearchParams(searchParams);
    if (value) p.set(key, value);
    else p.delete(key);
    p.set('page', '1');
    setSearchParams(p);
  };

  const updatePage = (value: number) => {
    const p = new URLSearchParams(searchParams);
    p.set('page', String(value));
    setSearchParams(p);
  };

  const dismissFlash = () => {
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">
        {q ? `Rezultati za "${q}"` : 'Pretrazi usluge'}
      </h1>

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

      <div className="flex flex-wrap items-center gap-4 mb-8 pb-6 border-b border-gray-200">
        <input
          type="search"
          value={queryInput}
          onChange={(e) => setQueryInput(e.target.value)}
          placeholder="Pretrazi..."
          className="border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
        />
        <select
          value={categoryId}
          onChange={(e) => updateParam('categoryId', e.target.value)}
          className="border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
        >
          <option value="">Sve kategorije</option>
          {categories.map((cat: any) => (
            <option key={cat.id} value={cat.id}>
              {cat.title}
            </option>
          ))}
        </select>
        <select
          value={sortBy}
          onChange={(e) => updateParam('sortBy', e.target.value)}
          className="border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
        >
          <option value="newest">Najnovije</option>
          <option value="price_asc">Cijena: niska - visoka</option>
          <option value="price_desc">Cijena: visoka - niska</option>
        </select>
        <span className="text-sm text-gray-500 ml-auto">{meta.total} rezultata</span>
      </div>

      {error && <FeedbackBanner type="error" className="mb-6">{error}</FeedbackBanner>}

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
              <button
                type="button"
                onClick={() => updatePage(page - 1)}
                disabled={page <= 1}
                className="px-3 py-2 rounded-lg text-sm border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                Prethodna
              </button>
              {pageNumbers.map((p) => (
                <button
                  key={p}
                  onClick={() => updatePage(p)}
                  className={`px-4 py-2 rounded-lg text-sm ${
                    p === page ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {p}
                </button>
              ))}
              <button
                type="button"
                onClick={() => updatePage(page + 1)}
                disabled={page >= meta.totalPages}
                className="px-3 py-2 rounded-lg text-sm border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                Sljedeca
              </button>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-20">
          <p className="text-gray-500 text-lg">Nema rezultata</p>
          <p className="text-gray-400 mt-2">Pokusajte sa drugim pojmom pretrage</p>
        </div>
      )}
    </div>
  );
}
