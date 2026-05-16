import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Order, ORDER_STATUS_META, ordersApi } from '../lib/orders';

export default function RevenuePage() {
  const [revenue, setRevenue] = useState<number>(0);
  const [completed, setCompleted] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      ordersApi.revenue().catch(() => 0),
      ordersApi
        .selling({ page: 1, limit: 50 })
        .then((r) => r.data.filter((o) => o.status === 'COMPLETED'))
        .catch(() => [] as Order[]),
    ])
      .then(([r, c]) => {
        setRevenue(r);
        setCompleted(c);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-start justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Moja zarada</h1>
        <Link to="/dashboard" className="text-sm text-primary-600 hover:underline">
          ← Dashboard
        </Link>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-6 mb-6">
        <p className="text-sm text-gray-500 mb-1">Ukupna zarada (završene narudžbe)</p>
        <p className="text-3xl font-bold text-gray-900">{revenue.toFixed(2)} €</p>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-6">
        <h2 className="font-semibold text-gray-900 mb-4">Završene narudžbe</h2>
        {completed.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-8">Još nema završenih narudžbi</p>
        ) : (
          <div className="divide-y divide-gray-100">
            {completed.map((o) => {
              const meta = ORDER_STATUS_META[o.status];
              return (
                <Link
                  key={o.id}
                  to={`/dashboard/orders/${o.id}`}
                  className="flex items-center justify-between py-3 hover:bg-gray-50 -mx-3 px-3 rounded"
                >
                  <div>
                    <p className="font-medium text-gray-900">Narudžba #{o.id}</p>
                    <p className="text-xs text-gray-400">
                      {o.completedAt && new Date(o.completedAt).toLocaleDateString('bs')}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`px-2 py-0.5 text-xs rounded-full ${meta.chip}`}>
                      {meta.label}
                    </span>
                    <span className="font-medium text-gray-900">
                      {Number(o.totalCost).toFixed(2)} €
                    </span>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
