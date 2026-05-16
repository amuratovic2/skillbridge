import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Order, ORDER_STATUS_META, OrderStatus, ordersApi } from '../lib/orders';

type Filter = 'ALL' | OrderStatus;

const FILTERS: { id: Filter; label: string }[] = [
  { id: 'ALL', label: 'Sve' },
  { id: 'PENDING', label: 'Na čekanju' },
  { id: 'ACCEPTED', label: 'Prihvaćene' },
  { id: 'IN_PROGRESS', label: 'U izradi' },
  { id: 'DELIVERED', label: 'Isporučene' },
  { id: 'REVISION_REQUESTED', label: 'Na reviziji' },
  { id: 'COMPLETED', label: 'Završene' },
  { id: 'CANCELLED', label: 'Otkazane' },
];

export default function MyOrdersPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>('ALL');

  const isFreelancer = user?.role === 'FREELANCER';

  useEffect(() => {
    setLoading(true);

    // For buyers we use the dedicated GET /orders/my/buying/status/{status}
    // endpoint when filtering by a specific status, so the server-side route
    // is actually exercised. Sellers fall back to client-side filter since
    // there is no /orders/my/selling/status/{status} on the backend.
    let loader: Promise<Order[]>;
    if (filter !== 'ALL' && !isFreelancer) {
      loader = ordersApi.byBuyingStatus(filter as OrderStatus);
    } else {
      const base = isFreelancer ? ordersApi.selling() : ordersApi.buying();
      loader = base.then((r) =>
        filter === 'ALL' ? r.data : r.data.filter((o) => o.status === filter),
      );
    }

    loader
      .then(setOrders)
      .catch(() => setOrders([]))
      .finally(() => setLoading(false));
  }, [isFreelancer, filter]);

  const visible = orders;

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Moje narudžbe</h1>
        {isFreelancer && (
          <Link to="/dashboard/revenue" className="text-sm text-primary-600 hover:underline">
            Moja zarada →
          </Link>
        )}
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {FILTERS.map((f) => (
          <button
            key={f.id}
            onClick={() => setFilter(f.id)}
            className={`px-4 py-1.5 rounded-full text-sm border transition-colors ${
              filter === f.id
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-700 border-gray-200 hover:border-gray-300'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-gray-500 text-lg">
            {filter === 'ALL' ? 'Nemate narudžbi' : 'Nema narudžbi za odabrani status'}
          </p>
          {!isFreelancer && filter === 'ALL' && (
            <Link
              to="/gigs"
              className="inline-block mt-4 bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700"
            >
              Pretraži usluge
            </Link>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {visible.map((order) => {
            const meta = ORDER_STATUS_META[order.status] ?? {
              label: order.status,
              chip: 'bg-gray-100 text-gray-700',
              bar: 'bg-gray-400',
            };
            return (
              <Link
                key={order.id}
                to={`/dashboard/orders/${order.id}`}
                className="block bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm text-gray-500 mb-1">Narudžba #{order.id}</div>
                    <div className="font-medium text-gray-900">
                      {Number(order.totalCost)} &euro;
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${meta.chip}`}>
                      {meta.label}
                    </span>
                    <div className="text-xs text-gray-400 mt-1">
                      {new Date(order.orderDate).toLocaleDateString('bs')}
                    </div>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
