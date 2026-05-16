import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';
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

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [order, setOrder] = useState<Order | null>(null);
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [revisionOpen, setRevisionOpen] = useState(false);
  const [deliverOpen, setDeliverOpen] = useState(false);
  const [versionOpen, setVersionOpen] = useState<number | null>(null);

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
    try {
      await ordersApi.updateStatus(order.id, newStatus);
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Greška');
    }
  };

  const sendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || !order || !user) return;

    const otherUserId = user.id === order.clientId ? order.sellerId : order.clientId;
    try {
      await api.post('/messages', {
        receiverId: otherUserId,
        orderId: order.id,
        content: newMessage,
      });
      setNewMessage('');
      fetchData();
    } catch {
      /* ignore */
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
  const inDispute = order.status === 'DISPUTED';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h1 className="text-xl font-bold text-gray-900">Narudžba #{order.id}</h1>
              <span className={`px-3 py-1 rounded-full text-xs font-medium ${meta.chip}`}>
                {meta.label}
              </span>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="text-gray-500">Cijena</span>
                <p className="font-medium">{Number(order.totalCost)} &euro;</p>
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
                    : '—'}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <h2 className="font-semibold text-gray-900 mb-2">Zahtjevi klijenta</h2>
            {order.requirements ? (
              <p className="text-sm text-gray-700 whitespace-pre-wrap">{order.requirements}</p>
            ) : (
              <p className="text-sm text-gray-400">Nema zahtjeva</p>
            )}
          </div>

          <div className="flex flex-wrap gap-3">
            {isSeller && order.status === 'PENDING' && (
              <button
                onClick={() => handleStatusChange('ACCEPTED')}
                className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
              >
                Prihvati narudžbu
              </button>
            )}
            {isSeller && order.status === 'ACCEPTED' && (
              <button
                onClick={() => handleStatusChange('IN_PROGRESS')}
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700"
              >
                Započni rad
              </button>
            )}
            {isSeller && ['ACCEPTED', 'IN_PROGRESS', 'REVISION_REQUESTED'].includes(order.status) && (
              <button
                onClick={() => setDeliverOpen(true)}
                className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700"
              >
                Isporuči rad
              </button>
            )}
            {isClient && order.status === 'DELIVERED' && (
              <>
                <button
                  onClick={() => handleStatusChange('COMPLETED')}
                  className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
                >
                  Prihvati isporuku
                </button>
                {revisionsLeft > 0 && (
                  <button
                    onClick={() => setRevisionOpen(true)}
                    className="border border-orange-300 text-orange-600 px-4 py-2 rounded-lg text-sm hover:bg-orange-50"
                  >
                    Traži reviziju ({revisionsLeft} preostalo)
                  </button>
                )}
              </>
            )}
            {(isClient || isSeller) &&
              ['PENDING', 'ACCEPTED', 'IN_PROGRESS', 'REVISION_REQUESTED'].includes(order.status) && (
                <button
                  onClick={() => setCancelOpen(true)}
                  className="border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm hover:bg-red-50"
                >
                  Otkaži
                </button>
              )}
            {(isClient || isSeller || isAdmin) &&
              ['IN_PROGRESS', 'DELIVERED', 'REVISION_REQUESTED'].includes(order.status) && (
                <button
                  onClick={() => handleStatusChange('DISPUTED')}
                  className="border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm hover:bg-red-50"
                >
                  Prijavi spor
                </button>
              )}
            {isAdmin && inDispute && (
              <>
                <button
                  onClick={() => handleStatusChange('COMPLETED')}
                  className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
                >
                  Riješi: Završi
                </button>
                <button
                  onClick={() => handleStatusChange('CANCELLED')}
                  className="border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm hover:bg-red-50"
                >
                  Riješi: Otkaži
                </button>
              </>
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
                        📎 {delivery.fileName}
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
                onChange={(e) => setNewMessage(e.target.value)}
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

        <div className="lg:col-span-1">
          <div className="bg-white border border-gray-200 rounded-xl p-6 sticky top-24">
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
