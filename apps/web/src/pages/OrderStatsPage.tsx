import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ordersApi, ORDER_STATUS_META, OrderStatus } from '../lib/orders';

const STATUS_ORDER: OrderStatus[] = [
  'CANCELLED',
  'COMPLETED',
  'ACCEPTED',
  'PENDING',
  'DELIVERED',
  'IN_PROGRESS',
  'REVISION_REQUESTED',
  'DISPUTED',
];

const TEXT_COLOR: Record<OrderStatus, string> = {
  PENDING: 'text-yellow-700',
  ACCEPTED: 'text-blue-700',
  IN_PROGRESS: 'text-blue-700',
  DELIVERED: 'text-green-700',
  REVISION_REQUESTED: 'text-orange-700',
  COMPLETED: 'text-green-700',
  CANCELLED: 'text-red-700',
  DISPUTED: 'text-red-700',
};

export default function OrderStatsPage() {
  const [stats, setStats] = useState<Partial<Record<OrderStatus, number>>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ordersApi
      .statistics()
      .then((s) => setStats(s ?? {}))
      .catch(() => setStats({}))
      .finally(() => setLoading(false));
  }, []);

  const total = STATUS_ORDER.reduce((acc, s) => acc + (stats[s] ?? 0), 0);

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
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Statistika narudžbi</h1>
          <p className="text-sm text-gray-500 mt-1">Pregled svih narudžbi po statusu</p>
        </div>
        <Link to="/dashboard" className="text-sm text-primary-600 hover:underline">
          ← Dashboard
        </Link>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-6 mb-6">
        <p className="text-sm text-gray-500 mb-1">Ukupno narudžbi</p>
        <p className="text-3xl font-bold text-gray-900">{total}</p>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-6">
        <h2 className="font-semibold text-gray-900 mb-6">Raspodjela po statusu</h2>
        {total === 0 ? (
          <p className="text-sm text-gray-400 text-center py-8">Nema narudžbi za prikaz</p>
        ) : (
          <div className="space-y-5">
            {STATUS_ORDER.filter((s) => stats[s]).map((s) => {
              const count = stats[s] ?? 0;
              const pct = total === 0 ? 0 : (count / total) * 100;
              const meta = ORDER_STATUS_META[s];
              return (
                <div key={s}>
                  <div className="flex items-center justify-between text-sm mb-1">
                    <span className={`font-medium ${TEXT_COLOR[s]}`}>{meta.label}</span>
                    <span className="text-gray-600">
                      {count} ({pct.toFixed(1)}%)
                    </span>
                  </div>
                  <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className={`h-full ${meta.bar}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
