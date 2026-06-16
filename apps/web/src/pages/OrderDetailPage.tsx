import { useState, useEffect, useCallback } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import FeedbackBanner from '../components/ui/FeedbackBanner';
import { useToast } from '../context/ToastContext';
import api, { getApiErrorMessage } from '../lib/api';
import {
  deliveriesApi,
  Delivery,
  Order,
  ORDER_STATUS_META,
  OrderStatus,
  ordersApi,
} from '../lib/orders';
import AdminOrderTools from './AdminOrderTools';
import CancelOrderModal from './order/CancelOrderModal';
import RevisionRequestModal from './order/RevisionRequestModal';
import DeliverWorkModal from './order/DeliverWorkModal';
import DeliveryDetailModal from './order/DeliveryDetailModal';

interface Message {
  id: number;
  senderId: number;
  receiverId: number;
  content: string;
  sentAt: string;
}

type Flash = { type: 'success' | 'error' | 'info'; text: string };

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const toast = useToast();
  const [order, setOrder] = useState<Order | null>(null);
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [revisionOpen, setRevisionOpen] = useState(false);
  const [deliverOpen, setDeliverOpen] = useState(false);
  const [versionOpen, setVersionOpen] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const flash = (location.state as { flash?: Flash } | null)?.flash;

  const fetchData = useCallback(() => {
    if (!id) return;
    Promise.all([
      ordersApi.byId(id).then(setOrder),
      deliveriesApi.forOrder(id).then(setDeliveries),
      api.get(`/messages/order/${id}`).then((res) => setMessages(res.data.data ?? [])),
    ])
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleStatusChange = async (newStatus: Order['status']) => {
    if (!order) return;
    setActionError(null);
    setActionSuccess(null);
    try {
      await ordersApi.updateStatus(order.id, newStatus);
      setActionSuccess('Promjena statusa je započeta. Druga strana će dobiti notifikaciju nakon obrade.');
      fetchData();
    } catch (err: unknown) {
      toast.error(getApiErrorMessage(err, 'Greška pri ažuriranju narudžbe'));
    }
  };

  const sendMessage = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!newMessage.trim() || !order || !user) return;

    const otherUserId = user.id === order.clientId ? order.sellerId : order.clientId;
    setActionError(null);
    setActionSuccess(null);
    try {
      await api.post('/messages', {
        receiverId: otherUserId,
        orderId: order.id,
        content: newMessage,
      });
      setNewMessage('');
      setActionSuccess('Poruka je poslana. Primaoc će dobiti notifikaciju nakon obrade.');
      fetchData();
    } catch (err: unknown) {
      setActionError(getApiErrorMessage(err, 'Slanje poruke nije uspjelo.'));
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (!order) return null;

  const meta = ORDER_STATUS_META[order.status] ?? {
    label: order.status,
    chip: 'bg-gray-100 text-gray-700',
    bar: 'bg-gray-400',
  };
  const isClient = user?.id === order.clientId;
  const isSeller = user?.id === order.sellerId;
  const isAdmin = user?.role === 'ADMIN';
  const revisionsLeft = Math.max(order.maxRevisions - order.usedRevisions, 0);
  const dismissFlash = () => {
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
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
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between mb-5">
              <div>
                <h1 className="text-xl font-bold text-gray-900">Narudžba #{order.id}</h1>
                <p className="text-sm text-gray-500 mt-1">{headlineFor(order.status, isSeller)}</p>
              </div>
              <span className={`self-start px-3 py-1 rounded-full text-xs font-medium ${meta.chip}`}>
                {meta.label}
              </span>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="text-gray-500">Cijena</span>
                <p className="font-medium">{Number(order.totalCost).toFixed(2)} &euro;</p>
              </div>
              <div>
                <span className="text-gray-500">Datum</span>
                <p className="font-medium">{new Date(order.orderDate).toLocaleDateString('bs')}</p>
              </div>
              <div>
                <span className="text-gray-500">Revizije</span>
                <p className="font-medium">{order.usedRevisions} / {order.maxRevisions}</p>
              </div>
              <div>
                <span className="text-gray-500">Rok</span>
                <p className="font-medium">
                  {order.deliveryDeadline
                    ? new Date(order.deliveryDeadline).toLocaleDateString('bs')
                    : '-'}
                </p>
              </div>
            </div>
          </div>

          <WorkflowTracker status={order.status} />

          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <h2 className="font-semibold text-gray-900 mb-2">Zahtjevi klijenta</h2>
            {order.requirements ? (
              <p className="text-sm text-gray-700 whitespace-pre-wrap">{order.requirements}</p>
            ) : (
              <p className="text-sm text-gray-400">Nema zahtjeva</p>
            )}
          </div>

          {isAdmin && <AdminOrderTools order={order} onChange={fetchData} />}

          {deliveries.length > 0 && (
            <div className="bg-white border border-gray-200 rounded-xl p-6">
              <h2 className="font-semibold text-gray-900 mb-4">Isporuke</h2>
              <div className="space-y-4">
                {deliveries.map((delivery) => (
                  <button
                    key={delivery.id}
                    type="button"
                    onClick={() => setVersionOpen(delivery.versionNumber)}
                    className="w-full text-left border border-gray-100 hover:border-primary-300 rounded-lg p-4 transition-colors"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-medium text-sm">Verzija {delivery.versionNumber}</span>
                      <span className="text-xs text-gray-500">
                        {new Date(delivery.createdAt).toLocaleString('bs')}
                      </span>
                    </div>
                    {delivery.message && (
                      <p className="text-sm text-gray-600 line-clamp-2">{delivery.message}</p>
                    )}
                    {delivery.fileName && (
                      <span className="text-xs text-primary-600 mt-1 inline-block">
                        Prilog: {delivery.fileName}
                      </span>
                    )}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <h2 className="font-semibold text-gray-900 mb-4">Poruke</h2>
            <div className="space-y-3 max-h-80 overflow-y-auto mb-4">
              {messages.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">Nema poruka</p>
              ) : (
                messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex ${msg.senderId === user?.id ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-[70%] px-4 py-2 rounded-2xl text-sm ${
                        msg.senderId === user?.id
                          ? 'bg-primary-600 text-white'
                          : 'bg-gray-100 text-gray-900'
                      }`}
                    >
                      {msg.content}
                      <div
                        className={`text-xs mt-1 ${
                          msg.senderId === user?.id ? 'text-primary-200' : 'text-gray-400'
                        }`}
                      >
                        {new Date(msg.sentAt).toLocaleTimeString('bs', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
            <form onSubmit={sendMessage} className="flex gap-2">
              <input
                type="text"
                value={newMessage}
                onChange={(event) => setNewMessage(event.target.value)}
                placeholder="Unesite poruku..."
                className="flex-1 border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
              />
              <button
                type="submit"
                className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
              >
                Pošalji
              </button>
            </form>
          </div>
        </div>

        <div className="lg:col-span-1 space-y-6">
          <OrderActionPanel
            order={order}
            isClient={isClient}
            isSeller={isSeller}
            isAdmin={isAdmin}
            revisionsLeft={revisionsLeft}
            onAccept={() => handleStatusChange('ACCEPTED')}
            onStart={() => handleStatusChange('IN_PROGRESS')}
            onDeliver={() => setDeliverOpen(true)}
            onComplete={() => handleStatusChange('COMPLETED')}
            onRevision={() => setRevisionOpen(true)}
            onCancel={() => setCancelOpen(true)}
          />
          {actionError && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">
              {actionError}
            </p>
          )}
          {actionSuccess && (
            <p className="text-sm text-green-700 bg-green-50 border border-green-100 rounded-lg px-4 py-3">
              {actionSuccess}
            </p>
          )}

          <div className="bg-white border border-gray-200 rounded-xl p-6 lg:sticky lg:top-24">
            <h2 className="font-semibold text-gray-900 mb-4">Historija</h2>
            <div className="space-y-4">
              {(order.history ?? []).length === 0 && (
                <p className="text-sm text-gray-400">Nema unosa</p>
              )}
              {(order.history ?? []).map((h) => {
                const info =
                  ORDER_STATUS_META[h.newStatus as Order['status']] ??
                  { label: h.newStatus, chip: '', bar: '' };
                return (
                  <div key={h.id} className="flex gap-3">
                    <div className="w-2 h-2 bg-primary-400 rounded-full mt-2 shrink-0" />
                    <div>
                      <p className="text-sm font-medium text-gray-900">{info.label}</p>
                      {h.note && <p className="text-xs text-gray-500">{h.note}</p>}
                      <p className="text-xs text-gray-400">
                        {new Date(h.changedAt).toLocaleString('bs')}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {cancelOpen && (
        <CancelOrderModal
          orderId={order.id}
          onClose={() => setCancelOpen(false)}
          onDone={() => {
            setCancelOpen(false);
            setActionSuccess('Otkazivanje je započeto. Druga strana će dobiti notifikaciju nakon obrade.');
            fetchData();
          }}
        />
      )}
      {revisionOpen && (
        <RevisionRequestModal
          orderId={order.id}
          revisionsLeft={revisionsLeft}
          onClose={() => setRevisionOpen(false)}
          onDone={() => {
            setRevisionOpen(false);
            setActionSuccess('Zahtjev za reviziju je započet. Freelancer će dobiti notifikaciju nakon obrade.');
            fetchData();
          }}
        />
      )}
      {deliverOpen && (
        <DeliverWorkModal
          orderId={order.id}
          nextVersion={(deliveries[0]?.versionNumber ?? 0) + 1}
          onClose={() => setDeliverOpen(false)}
          onDone={() => {
            setDeliverOpen(false);
            setActionSuccess('Isporuka je poslana na obradu. Klijent će dobiti notifikaciju nakon obrade.');
            fetchData();
          }}
        />
      )}
      {versionOpen !== null && (
        <DeliveryDetailModal
          orderId={order.id}
          version={versionOpen}
          onClose={() => setVersionOpen(null)}
        />
      )}
    </div>
  );
}

const WORKFLOW_STEPS: { status: OrderStatus; label: string }[] = [
  { status: 'PENDING', label: 'Kreirano' },
  { status: 'ACCEPTED', label: 'Prihvaćeno' },
  { status: 'IN_PROGRESS', label: 'U izradi' },
  { status: 'DELIVERED', label: 'Isporučeno' },
  { status: 'COMPLETED', label: 'Završeno' },
];

function WorkflowTracker({ status }: { status: OrderStatus }) {
  const activeIndex = workflowIndex(status);
  const isTerminalProblem = status === 'CANCELLED' || status === 'DISPUTED';

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-6">
      <div className="flex items-center justify-between mb-5">
        <h2 className="font-semibold text-gray-900">Tok narudžbe</h2>
        {isTerminalProblem && (
          <span className="text-xs font-medium text-red-600">
            {status === 'CANCELLED' ? 'Prekinuto' : 'U sporu'}
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-5 gap-3">
        {WORKFLOW_STEPS.map((step, index) => {
          const reached = !isTerminalProblem && index <= activeIndex;
          const current = !isTerminalProblem && index === activeIndex;
          return (
            <div key={step.status} className="relative flex items-start gap-3 sm:block">
              {index < WORKFLOW_STEPS.length - 1 && (
                <div
                  className={`hidden sm:block absolute left-5 right-0 top-2 h-px ${
                    reached && index < activeIndex ? 'bg-primary-300' : 'bg-gray-200'
                  }`}
                />
              )}
              <div className="relative z-10 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-white mt-0.5 sm:mt-0">
                <span
                  className={`block h-2.5 w-2.5 rounded-full ${
                    reached ? 'bg-primary-600' : 'bg-gray-300'
                  } ${current ? 'ring-4 ring-primary-100' : ''}`}
                />
              </div>
              <div className="min-w-0 sm:mt-3">
                <p className={`text-sm leading-5 font-medium ${current ? 'text-primary-700' : 'text-gray-900'}`}>
                  {step.label}
                </p>
                <p className="text-xs leading-4 text-gray-400">{stepCaption(step.status)}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function OrderActionPanel({
  order,
  isClient,
  isSeller,
  isAdmin,
  revisionsLeft,
  onAccept,
  onStart,
  onDeliver,
  onComplete,
  onRevision,
  onCancel,
}: {
  order: Order;
  isClient: boolean;
  isSeller: boolean;
  isAdmin: boolean;
  revisionsLeft: number;
  onAccept: () => void;
  onStart: () => void;
  onDeliver: () => void;
  onComplete: () => void;
  onRevision: () => void;
  onCancel: () => void;
}) {
  const action = actionCopy(order.status, isClient, isSeller, isAdmin, revisionsLeft);
  const canCancel =
    (isClient || isSeller) &&
    ['PENDING', 'ACCEPTED', 'IN_PROGRESS', 'REVISION_REQUESTED'].includes(order.status);

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-6">
      <p className="text-xs font-semibold uppercase tracking-wide text-primary-600 mb-2">
        Sljedeći korak
      </p>
      <h2 className="font-semibold text-gray-900">{action.title}</h2>
      <p className="text-sm text-gray-600 mt-2">{action.description}</p>

      <div className="mt-5 space-y-2">
        {isSeller && order.status === 'PENDING' && (
          <button
            type="button"
            onClick={onAccept}
            className="w-full bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
          >
            Prihvati narudžbu
          </button>
        )}
        {isSeller && order.status === 'ACCEPTED' && (
          <button
            type="button"
            onClick={onStart}
            className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700"
          >
            Započni rad
          </button>
        )}
        {isSeller && ['ACCEPTED', 'IN_PROGRESS', 'REVISION_REQUESTED'].includes(order.status) && (
          <button
            type="button"
            onClick={onDeliver}
            className="w-full bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700"
          >
            Isporuči rad
          </button>
        )}
        {isClient && order.status === 'DELIVERED' && (
          <>
            <button
              type="button"
              onClick={onComplete}
              className="w-full bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
            >
              Prihvati isporuku
            </button>
            {revisionsLeft > 0 && (
              <button
                type="button"
                onClick={onRevision}
                className="w-full border border-orange-300 text-orange-600 px-4 py-2 rounded-lg text-sm hover:bg-orange-50"
              >
                Traži reviziju ({revisionsLeft} preostalo)
              </button>
            )}
          </>
        )}
        {isAdmin && order.status === 'DISPUTED' && (
          <>
            <button
              type="button"
              onClick={onComplete}
              className="w-full bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
            >
              Riješi: Završi
            </button>
            <button
              type="button"
              onClick={onCancel}
              className="w-full border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm hover:bg-red-50"
            >
              Riješi: Otkaži
            </button>
          </>
        )}
        {canCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="w-full border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm hover:bg-red-50"
          >
            Otkaži narudžbu
          </button>
        )}
      </div>
    </div>
  );
}

function workflowIndex(status: OrderStatus) {
  if (status === 'REVISION_REQUESTED') return 2;
  const index = WORKFLOW_STEPS.findIndex((step) => step.status === status);
  return index >= 0 ? index : 0;
}

function stepCaption(status: OrderStatus) {
  switch (status) {
    case 'PENDING':
      return 'Čeka potvrdu';
    case 'ACCEPTED':
      return 'Dogovoreno';
    case 'IN_PROGRESS':
      return 'Rad u toku';
    case 'DELIVERED':
      return 'Pregled';
    case 'COMPLETED':
      return 'Zatvoreno';
    default:
      return '';
  }
}

function headlineFor(status: OrderStatus, isSeller: boolean) {
  switch (status) {
    case 'PENDING':
      return isSeller ? 'Klijent čeka da prihvatite posao.' : 'Freelancer treba potvrditi narudžbu.';
    case 'ACCEPTED':
      return 'Narudžba je prihvaćena i spremna za rad.';
    case 'IN_PROGRESS':
      return 'Rad je u toku.';
    case 'DELIVERED':
      return 'Isporuka je spremna za pregled.';
    case 'REVISION_REQUESTED':
      return 'Klijent je zatražio dodatne izmjene.';
    case 'COMPLETED':
      return 'Narudžba je završena.';
    case 'CANCELLED':
      return 'Narudžba je otkazana.';
    case 'DISPUTED':
      return 'Narudžba je u sporu.';
    default:
      return '';
  }
}

function actionCopy(
  status: OrderStatus,
  isClient: boolean,
  isSeller: boolean,
  isAdmin: boolean,
  revisionsLeft: number,
) {
  if (isSeller && status === 'PENDING') {
    return {
      title: 'Prihvatite ili otkažite zahtjev',
      description: 'Klijent je poslao narudžbu. Prihvatanjem potvrđujete da možete krenuti sa radom.',
    };
  }
  if (isSeller && status === 'ACCEPTED') {
    return {
      title: 'Započnite rad ili pošaljite isporuku',
      description: 'Ako ste spremni, označite rad kao započet. Isporuku možete poslati odmah kada imate materijale.',
    };
  }
  if (isSeller && status === 'IN_PROGRESS') {
    return {
      title: 'Pošaljite isporuku kada je rad spreman',
      description: 'Klijent će dobiti verziju isporuke i moći će je prihvatiti ili zatražiti reviziju.',
    };
  }
  if (isSeller && status === 'REVISION_REQUESTED') {
    return {
      title: 'Klijent čeka novu verziju',
      description: 'Pregledajte zahtjev za reviziju i pošaljite novu isporuku kada završite izmjene.',
    };
  }
  if (isClient && status === 'DELIVERED') {
    return {
      title: 'Pregledajte isporuku',
      description:
        revisionsLeft > 0
          ? 'Ako je sve uredu, prihvatite isporuku. Ako treba dorada, zatražite reviziju.'
          : 'Ako je sve uredu, prihvatite isporuku. Dostigli ste limit revizija.',
    };
  }
  if (isAdmin && status === 'DISPUTED') {
    return {
      title: 'Riješite spor',
      description: 'Pregledajte historiju i poruke, zatim završite ili otkažite narudžbu.',
    };
  }

  switch (status) {
    case 'PENDING':
      return {
        title: 'Čeka se freelancer',
        description: 'Narudžba je kreirana i čeka da freelancer potvrdi da može preuzeti posao.',
      };
    case 'ACCEPTED':
      return {
        title: 'Čeka se početak rada',
        description: 'Freelancer je prihvatio narudžbu i sljedeći korak je početak rada.',
      };
    case 'IN_PROGRESS':
      return {
        title: 'Rad je u toku',
        description: 'Freelancer trenutno radi na narudžbi. Ovdje možete pratiti poruke i isporuke.',
      };
    case 'DELIVERED':
      return {
        title: 'Isporuka je poslana',
        description: 'Čeka se odluka klijenta: prihvatanje isporuke ili zahtjev za reviziju.',
      };
    case 'REVISION_REQUESTED':
      return {
        title: 'Revizija je u toku',
        description: 'Freelancer treba poslati novu verziju na osnovu zahtjeva klijenta.',
      };
    case 'COMPLETED':
      return {
        title: 'Narudžba je završena',
        description: 'Sve glavne akcije su zatvorene. Poruke i historija ostaju dostupne.',
      };
    case 'CANCELLED':
      return {
        title: 'Narudžba je otkazana',
        description: 'Ova narudžba više nema aktivnih koraka.',
      };
    case 'DISPUTED':
      return {
        title: 'Narudžba je u sporu',
        description: 'Čeka se administrativno rješenje spora.',
      };
    default:
      return {
        title: 'Pregled narudžbe',
        description: 'Otvorite historiju i poruke za više detalja.',
      };
  }
}
