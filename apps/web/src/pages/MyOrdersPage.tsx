import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';
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
  const [gigTitles, setGigTitles] = useState<Record<number, string>>({});
  const [partyNames, setPartyNames] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>('ALL');

  const isFreelancer = user?.role === 'FREELANCER';

  useEffect(() => {
    let active = true;
    setLoading(true);

    let loader: Promise<Order[]>;
    if (filter !== 'ALL' && !isFreelancer) {
      loader = ordersApi.byBuyingStatus(filter as OrderStatus);
    } else {
      const base = isFreelancer ? ordersApi.selling() : ordersApi.buying();
      loader = base.then((response) =>
        filter === 'ALL' ? response.data : response.data.filter((order) => order.status === filter),
      );
    }

    loader
      .then(async (nextOrders) => {
        if (!active) return;
        setOrders(nextOrders);

        const gigIds = [...new Set(nextOrders.map((order) => order.gigId))];
        const partyIds = [
          ...new Set(nextOrders.map((order) => (isFreelancer ? order.clientId : order.sellerId))),
        ];
        const [nextGigTitles, nextPartyNames] = await Promise.all([
          loadGigTitles(gigIds),
          loadPartyNames(partyIds),
        ]);

        if (!active) return;
        setGigTitles(nextGigTitles);
        setPartyNames(nextPartyNames);
      })
      .catch(() => {
        if (!active) return;
        setOrders([]);
        setGigTitles({});
        setPartyNames({});
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [isFreelancer, filter]);

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
        <h1 className="text-2xl font-bold text-gray-900">
          {isFreelancer ? 'Primljene narudžbe' : 'Moje narudžbe'}
        </h1>
        {isFreelancer && (
          <Link to="/dashboard/revenue" className="text-sm text-primary-600 hover:underline">
            Moja zarada →
          </Link>
        )}
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {FILTERS.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => setFilter(item.id)}
            className={`px-4 py-1.5 rounded-full text-sm border transition-colors ${
              filter === item.id
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-gray-700 border-gray-200 hover:border-gray-300'
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      {orders.length === 0 ? (
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
          {orders.map((order) => (
            <OrderCard
              key={order.id}
              order={order}
              isFreelancer={isFreelancer}
              gigTitle={gigTitles[order.gigId]}
              partyName={partyNames[isFreelancer ? order.clientId : order.sellerId]}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function OrderCard({
  order,
  isFreelancer,
  gigTitle,
  partyName,
}: {
  order: Order;
  isFreelancer: boolean;
  gigTitle?: string;
  partyName?: string;
}) {
  const meta = ORDER_STATUS_META[order.status] ?? {
    label: order.status,
    chip: 'bg-gray-100 text-gray-700',
    bar: 'bg-gray-400',
  };
  const partyId = isFreelancer ? order.clientId : order.sellerId;
  const partyLabel = isFreelancer ? 'Klijent' : 'Freelancer';
  const deadline = order.deliveryDeadline
    ? new Date(order.deliveryDeadline).toLocaleDateString('bs')
    : null;

  return (
    <Link
      to={`/dashboard/orders/${order.id}`}
      className="block bg-white border border-gray-200 rounded-xl p-5 hover:border-primary-300 hover:shadow-sm transition-all"
    >
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2 mb-2">
            <span className="text-xs text-gray-500">Narudžba #{order.id}</span>
            <span className={`inline-flex items-center px-2 py-1 rounded-md text-xs leading-none font-medium ${meta.chip}`}>
              {meta.label}
            </span>
          </div>
          <h2 className="font-semibold text-gray-900 truncate">
            {gigTitle || `Gig #${order.gigId}`}
          </h2>
          <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm text-gray-500">
            <span>
              {partyLabel}: <span className="text-gray-800">{partyName || `#${partyId}`}</span>
            </span>
            {deadline && (
              <span>
                Rok: <span className="text-gray-800">{deadline}</span>
              </span>
            )}
            <span>
              Revizije: <span className="text-gray-800">{order.usedRevisions}/{order.maxRevisions}</span>
            </span>
          </div>
        </div>

        <div className="md:text-right shrink-0">
          <div className="font-semibold text-gray-900">
            {Number(order.totalCost).toFixed(2)} &euro;
          </div>
          <div className="text-xs text-gray-400 mt-1">
            Kreirano {new Date(order.orderDate).toLocaleDateString('bs')}
          </div>
        </div>
      </div>

      <div className="mt-4 pt-4 border-t border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
        <p className="text-sm text-gray-600">{nextStepFor(order.status, isFreelancer)}</p>
        <span className="text-sm text-primary-600 font-medium">Otvori detalje</span>
      </div>
    </Link>
  );
}

async function loadGigTitles(gigIds: number[]) {
  const entries = await Promise.all(
    gigIds.map((id) =>
      api
        .get(`/gigs/${id}`)
        .then((response) => [id, response.data.data?.title || `Gig #${id}`] as const)
        .catch(() => [id, `Gig #${id}`] as const),
    ),
  );
  return Object.fromEntries(entries);
}

async function loadPartyNames(userIds: number[]) {
  const entries = await Promise.all(
    userIds.map((id) =>
      api
        .get(`/users/${id}`)
        .then((response) => {
          const profile = response.data.data;
          const name = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ') || profile?.username;
          return [id, name || `#${id}`] as const;
        })
        .catch(() => [id, `#${id}`] as const),
    ),
  );
  return Object.fromEntries(entries);
}

function nextStepFor(status: OrderStatus, isFreelancer: boolean) {
  switch (status) {
    case 'PENDING':
      return isFreelancer ? 'Čeka da prihvatite narudžbu.' : 'Čeka potvrdu freelancera.';
    case 'ACCEPTED':
      return isFreelancer ? 'Spremno za početak rada.' : 'Freelancer je prihvatio narudžbu.';
    case 'IN_PROGRESS':
      return isFreelancer ? 'Rad je u toku, sljedeći korak je isporuka.' : 'Freelancer trenutno radi na isporuci.';
    case 'DELIVERED':
      return isFreelancer ? 'Isporuka je poslana, čeka odgovor klijenta.' : 'Pregledajte isporuku i prihvatite ili zatražite reviziju.';
    case 'REVISION_REQUESTED':
      return isFreelancer ? 'Klijent je zatražio reviziju.' : 'Revizija je poslana freelanceru.';
    case 'COMPLETED':
      return 'Narudžba je završena.';
    case 'CANCELLED':
      return 'Narudžba je otkazana.';
    case 'DISPUTED':
      return 'Narudžba je u sporu i čeka rješavanje.';
    default:
      return 'Otvori detalje za sljedeće korake.';
  }
}
