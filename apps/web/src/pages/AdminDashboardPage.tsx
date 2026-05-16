import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  customOffersApi,
  Order,
  ORDER_STATUS_META,
  OrderStatus,
  ordersApi,
} from '../lib/orders';

const STATUS_ORDER: OrderStatus[] = [
  'PENDING',
  'ACCEPTED',
  'IN_PROGRESS',
  'DELIVERED',
  'REVISION_REQUESTED',
  'COMPLETED',
  'CANCELLED',
  'DISPUTED',
];

function daysOverdue(deadline: string | null): number {
  if (!deadline) return 0;
  const diff = Date.now() - new Date(deadline).getTime();
  return Math.max(0, Math.floor(diff / (1000 * 60 * 60 * 24)));
}

export default function AdminDashboardPage() {
  const navigate = useNavigate();
  const [overdue, setOverdue] = useState<Order[]>([]);
  const [stats, setStats] = useState<Partial<Record<OrderStatus, number>>>({});
  const [offerCounts, setOfferCounts] = useState({ received: 0, sent: 0 });
  const [lookupId, setLookupId] = useState('');
  const [lookupErr, setLookupErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      ordersApi.overdue().catch(() => [] as Order[]),
      ordersApi.statistics().catch(() => ({} as Partial<Record<OrderStatus, number>>)),
      customOffersApi.received().catch(() => []),
      customOffersApi.sent().catch(() => []),
    ])
      .then(([o, s, recv, sent]) => {
        setOverdue(o);
        setStats(s);
        setOfferCounts({ received: recv.length, sent: sent.length });
      })
      .finally(() => setLoading(false));
  }, []);

  const totalOrders = STATUS_ORDER.reduce((acc, s) => acc + (stats[s] ?? 0), 0);

  const openLookup = (e: React.FormEvent) => {
    e.preventDefault();
    setLookupErr(null);
    const id = Number(lookupId.trim());
    if (!id || Number.isNaN(id)) {
      setLookupErr('Unesite ispravan ID');
      return;
    }
    navigate(`/dashboard/orders/${id}`);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Admin dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">
          Pregled svih narudžbi, rokova i prilagođenih ponuda
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <div className="text-sm text-gray-500 mb-1">Ukupno narudžbi</div>
          <div className="text-2xl font-bold text-gray-900">{totalOrders}</div>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <div className="text-sm text-gray-500 mb-1">Probijeni rokovi</div>
          <div className="text-2xl font-bold text-red-600">{overdue.length}</div>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <div className="text-sm text-gray-500 mb-1">Aktivne ponude (primljene)</div>
          <div className="text-2xl font-bold text-gray-900">{offerCounts.received}</div>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <div className="text-sm text-gray-500 mb-1">Aktivne ponude (poslate)</div>
          <div className="text-2xl font-bold text-gray-900">{offerCounts.sent}</div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white border border-gray-200 rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-900">Probijeni rokovi</h2>
            <Link to="/dashboard/overdue" className="text-sm text-primary-600 hover:underline">
              Vidi sve →
            </Link>
          </div>
          {overdue.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Nema narudžbi sa kašnjenjem</p>
          ) : (
            <table className="w-full text-sm">
              <thead className="text-xs uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="text-left pb-2">ID</th>
                  <th className="text-left pb-2">Status</th>
                  <th className="text-left pb-2">Rok</th>
                  <th className="text-left pb-2">Kašnjenje</th>
                  <th />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {overdue.slice(0, 6).map((o) => {
                  const meta = ORDER_STATUS_META[o.status];
                  return (
                    <tr key={o.id}>
                      <td className="py-2 font-medium">#{o.id}</td>
                      <td className="py-2">
                        <span className={`px-2 py-0.5 text-xs rounded-full ${meta.chip}`}>
                          {meta.label}
                        </span>
                      </td>
                      <td className="py-2 text-gray-600">
                        {o.deliveryDeadline
                          ? new Date(o.deliveryDeadline).toLocaleDateString('bs')
                          : '—'}
                      </td>
                      <td className="py-2 text-red-600 font-medium">
                        {daysOverdue(o.deliveryDeadline)} dana
                      </td>
                      <td className="py-2 text-right">
                        <Link
                          to={`/dashboard/orders/${o.id}`}
                          className="text-primary-600 hover:underline text-xs"
                        >
                          Detalji
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h2 className="font-semibold text-gray-900 mb-4">Raspodjela po statusu</h2>
          {totalOrders === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Nema podataka</p>
          ) : (
            <div className="space-y-3">
              {STATUS_ORDER.filter((s) => stats[s]).map((s) => {
                const count = stats[s] ?? 0;
                const pct = totalOrders === 0 ? 0 : (count / totalOrders) * 100;
                const meta = ORDER_STATUS_META[s];
                return (
                  <div key={s}>
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="font-medium text-gray-700">{meta.label}</span>
                      <span className="text-gray-500">
                        {count} ({pct.toFixed(0)}%)
                      </span>
                    </div>
                    <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                      <div className={`h-full ${meta.bar}`} style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
          <Link
            to="/dashboard/stats"
            className="mt-4 inline-block text-sm text-primary-600 hover:underline"
          >
            Detaljna statistika →
          </Link>
        </div>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-6">
        <h2 className="font-semibold text-gray-900 mb-3">Otvori narudžbu po ID</h2>
        <form onSubmit={openLookup} className="flex gap-2 items-end">
          <div className="flex-1 max-w-xs">
            <label className="block text-sm text-gray-700 mb-1">ID narudžbe</label>
            <input
              type="number"
              value={lookupId}
              onChange={(e) => setLookupId(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              placeholder="npr. 21"
            />
          </div>
          <button
            type="submit"
            className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
          >
            Otvori
          </button>
        </form>
        {lookupErr && <p className="text-sm text-red-600 mt-2">{lookupErr}</p>}
      </div>
    </div>
  );
}
