import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Order, ORDER_STATUS_META, ordersApi } from '../lib/orders';

function daysOverdue(deadline: string | null): number {
  if (!deadline) return 0;
  const diff = Date.now() - new Date(deadline).getTime();
  return Math.max(0, Math.floor(diff / (1000 * 60 * 60 * 24)));
}

export default function OverdueOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ordersApi
      .overdue()
      .then(setOrders)
      .catch(() => setOrders([]))
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
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Narudžbe sa probijenim rokom</h1>
          <p className="text-sm text-gray-500 mt-1">
            Administratorski pregled svih narudžbi sa zakašnjenjem
          </p>
        </div>
        <Link to="/dashboard" className="text-sm text-primary-600 hover:underline">
          ← Dashboard
        </Link>
      </div>

      {orders.length === 0 ? (
        <p className="text-center text-gray-500 py-16">Nema narudžbi sa probijenim rokom</p>
      ) : (
        <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
              <tr>
                <th className="px-4 py-3 text-left">ID</th>
                <th className="px-4 py-3 text-left">Status</th>
                <th className="px-4 py-3 text-left">Cijena</th>
                <th className="px-4 py-3 text-left">Rok</th>
                <th className="px-4 py-3 text-left">Kašnjenje</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {orders.map((o) => {
                const meta = ORDER_STATUS_META[o.status] ?? {
                  label: o.status,
                  chip: 'bg-gray-100 text-gray-700',
                  bar: 'bg-gray-400',
                };
                const days = daysOverdue(o.deliveryDeadline);
                return (
                  <tr key={o.id}>
                    <td className="px-4 py-3 font-medium text-gray-900">#{o.id}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 text-xs rounded-full ${meta.chip}`}>
                        {meta.label}
                      </span>
                    </td>
                    <td className="px-4 py-3">{Number(o.totalCost).toFixed(2)} €</td>
                    <td className="px-4 py-3 text-gray-600">
                      {o.deliveryDeadline
                        ? new Date(o.deliveryDeadline).toLocaleDateString('bs')
                        : '—'}
                    </td>
                    <td className="px-4 py-3 text-red-600 font-medium">{days} dana</td>
                    <td className="px-4 py-3 text-right">
                      <Link
                        to={`/dashboard/orders/${o.id}`}
                        className="text-primary-600 hover:underline"
                      >
                        Detalji
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
