import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { ordersApi } from '../lib/orders';

export default function CartPage() {
  const { items, remove, clear, total } = useCart();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const checkout = async () => {
    if (items.length === 0) return;
    setError(null);
    setBusy(true);
    try {
      await ordersApi.batchCreate(items.map((i) => i.gigId));
      clear();
      navigate('/dashboard/orders');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri kreiranju narudžbi');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Korpa</h1>
        <Link to="/gigs" className="text-sm text-primary-600 hover:underline">
          ← Nastavi pretragu
        </Link>
      </div>

      {items.length === 0 ? (
        <div className="text-center py-16 bg-white border border-gray-200 rounded-xl">
          <p className="text-gray-500 mb-4">Vaša korpa je prazna</p>
          <Link
            to="/gigs"
            className="inline-block bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700"
          >
            Pretraži usluge
          </Link>
        </div>
      ) : (
        <>
          <div className="bg-white border border-gray-200 rounded-xl divide-y divide-gray-100">
            {items.map((i) => (
              <div key={i.gigId} className="flex items-center justify-between p-4">
                <div className="min-w-0">
                  <Link
                    to={`/gigs/${i.gigId}`}
                    className="font-medium text-gray-900 hover:text-primary-600"
                  >
                    {i.title}
                  </Link>
                  <p className="text-xs text-gray-500 mt-1">
                    Rok isporuke: {i.deliveryTime} dana
                  </p>
                </div>
                <div className="flex items-center gap-4 shrink-0">
                  <span className="font-medium text-gray-900">
                    {Number(i.cost).toFixed(2)} €
                  </span>
                  <button
                    onClick={() => remove(i.gigId)}
                    className="text-sm text-red-600 hover:underline"
                  >
                    Ukloni
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="bg-white border border-gray-200 rounded-xl p-6 mt-6">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm text-gray-500">
                Stavki: <strong>{items.length}</strong>
              </span>
              <span className="text-xl font-bold text-gray-900">
                Ukupno: {total.toFixed(2)} €
              </span>
            </div>

            {error && <p className="text-sm text-red-600 mb-3">{error}</p>}

            <div className="flex gap-2">
              <button
                onClick={clear}
                className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Isprazni korpu
              </button>
              <button
                onClick={checkout}
                disabled={busy}
                className="bg-primary-600 text-white px-6 py-2 rounded-lg text-sm hover:bg-primary-700 disabled:opacity-60 ml-auto"
              >
                {busy ? 'Naručujem...' : 'Naruči sve'}
              </button>
            </div>
            <p className="text-xs text-gray-400 mt-3">
              Svaki gig postaje zasebna narudžba u PENDING statusu — freelancer mora svaku
              pojedinačno prihvatiti.
            </p>
          </div>
        </>
      )}
    </div>
  );
}
