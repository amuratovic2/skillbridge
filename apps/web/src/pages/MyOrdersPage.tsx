import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  PENDING: { label: 'Na čekanju', color: 'bg-yellow-100 text-yellow-700' },
  ACCEPTED: { label: 'Prihvaćeno', color: 'bg-blue-100 text-blue-700' },
  IN_PROGRESS: { label: 'U izradi', color: 'bg-blue-100 text-blue-700' },
  DELIVERED: { label: 'Isporučeno', color: 'bg-green-100 text-green-700' },
  REVISION_REQUESTED: { label: 'Na reviziji', color: 'bg-orange-100 text-orange-700' },
  COMPLETED: { label: 'Završeno', color: 'bg-green-100 text-green-700' },
  CANCELLED: { label: 'Otkazano', color: 'bg-red-100 text-red-700' },
  DISPUTED: { label: 'Spor', color: 'bg-red-100 text-red-700' },
};

export default function MyOrdersPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const endpoint = user?.role === 'FREELANCER' ? '/orders/my/selling' : '/orders/my/buying';
    api
      .get(endpoint)
      .then((res) => setOrders(res.data.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Moje narudžbe</h1>

      {orders.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-gray-500 text-lg">Nemate narudžbi</p>
          <Link
            to="/gigs"
            className="inline-block mt-4 bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700"
          >
            Pretraži usluge
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order: any) => {
            const statusInfo = STATUS_LABELS[order.status] || { label: order.status, color: 'bg-gray-100 text-gray-700' };
            return (
              <Link
                key={order.id}
                to={`/dashboard/orders/${order.id}`}
                className="block bg-white border border-gray-200 rounded-xl p-6 hover:border-primary-300 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm text-gray-500 mb-1">
                      Narudžba #{order.id}
                    </div>
                    <div className="font-medium text-gray-900">
                      {Number(order.totalCost)} &euro;
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${statusInfo.color}`}>
                      {statusInfo.label}
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
