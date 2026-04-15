import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
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

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [order, setOrder] = useState<any>(null);
  const [deliveries, setDeliveries] = useState<any[]>([]);
  const [messages, setMessages] = useState<any[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchData = () => {
    Promise.all([
      api.get(`/orders/${id}`).then((res) => setOrder(res.data.data)),
      api.get(`/deliveries/order/${id}`).then((res) => setDeliveries(res.data.data || [])),
      api.get(`/messages/order/${id}`).then((res) => setMessages(res.data.data || [])),
    ])
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchData();
  }, [id]);

  const handleStatusChange = async (newStatus: string) => {
    try {
      await api.patch(`/orders/${id}/status`, { status: newStatus });
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Greška');
    }
  };

  const handleDeliver = async () => {
    try {
      await api.post(`/deliveries/order/${id}`, { message: 'Rad je isporučen' });
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Greška');
    }
  };

  const handleRevision = async () => {
    try {
      await api.post(`/orders/${id}/revision`, { message: 'Potrebne su izmjene' });
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Greška');
    }
  };

  const sendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || !order) return;

    const otherUserId = user?.id === order.clientId ? order.gigId : order.clientId;
    try {
      await api.post('/messages', {
        receiverId: otherUserId,
        orderId: parseInt(id!, 10),
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

  const statusInfo = STATUS_LABELS[order.status] || { label: order.status, color: 'bg-gray-100 text-gray-700' };
  const isClient = user?.id === order.clientId;
  const isFreelancer = user?.role === 'FREELANCER';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main */}
        <div className="lg:col-span-2 space-y-6">
          {/* Order info */}
          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h1 className="text-xl font-bold text-gray-900">Narudžba #{order.id}</h1>
              <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusInfo.color}`}>
                {statusInfo.label}
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

          {/* Actions */}
          <div className="flex flex-wrap gap-3">
            {isFreelancer && order.status === 'PENDING' && (
              <button
                onClick={() => handleStatusChange('ACCEPTED')}
                className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
              >
                Prihvati narudžbu
              </button>
            )}
            {isFreelancer && ['ACCEPTED', 'IN_PROGRESS', 'REVISION_REQUESTED'].includes(order.status) && (
              <>
                {order.status === 'ACCEPTED' && (
                  <button
                    onClick={() => handleStatusChange('IN_PROGRESS')}
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700"
                  >
                    Započni rad
                  </button>
                )}
                <button
                  onClick={handleDeliver}
                  className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700"
                >
                  Isporuči rad
                </button>
              </>
            )}
            {isClient && order.status === 'DELIVERED' && (
              <>
                <button
                  onClick={() => handleStatusChange('COMPLETED')}
                  className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
                >
                  Prihvati isporuku
                </button>
                {order.usedRevisions < order.maxRevisions && (
                  <button
                    onClick={handleRevision}
                    className="border border-orange-300 text-orange-600 px-4 py-2 rounded-lg text-sm hover:bg-orange-50"
                  >
                    Traži reviziju ({order.maxRevisions - order.usedRevisions} preostalo)
                  </button>
                )}
              </>
            )}
            {['PENDING', 'ACCEPTED', 'IN_PROGRESS'].includes(order.status) && (
              <button
                onClick={() => handleStatusChange('CANCELLED')}
                className="border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm hover:bg-red-50"
              >
                Otkaži
              </button>
            )}
          </div>

          {/* Deliveries */}
          {deliveries.length > 0 && (
            <div className="bg-white border border-gray-200 rounded-xl p-6">
              <h2 className="font-semibold text-gray-900 mb-4">Isporuke</h2>
              <div className="space-y-4">
                {deliveries.map((delivery: any) => (
                  <div key={delivery.id} className="border border-gray-100 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-medium text-sm">Verzija {delivery.versionNumber}</span>
                      <span className="text-xs text-gray-500">
                        {new Date(delivery.createdAt).toLocaleString('bs')}
                      </span>
                    </div>
                    {delivery.message && (
                      <p className="text-sm text-gray-600">{delivery.message}</p>
                    )}
                    {delivery.fileName && (
                      <a href={delivery.fileUrl} className="text-sm text-primary-600 hover:underline mt-1 inline-block">
                        {delivery.fileName}
                      </a>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Chat */}
          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <h2 className="font-semibold text-gray-900 mb-4">Poruke</h2>
            <div className="space-y-3 max-h-80 overflow-y-auto mb-4">
              {messages.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">Nema poruka</p>
              ) : (
                messages.map((msg: any) => (
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
                      <div className={`text-xs mt-1 ${msg.senderId === user?.id ? 'text-primary-200' : 'text-gray-400'}`}>
                        {new Date(msg.sentAt).toLocaleTimeString('bs', { hour: '2-digit', minute: '2-digit' })}
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

        {/* Sidebar - History */}
        <div className="lg:col-span-1">
          <div className="bg-white border border-gray-200 rounded-xl p-6 sticky top-24">
            <h2 className="font-semibold text-gray-900 mb-4">Historija</h2>
            <div className="space-y-4">
              {(order.history || []).map((h: any) => {
                const info = STATUS_LABELS[h.newStatus] || { label: h.newStatus, color: '' };
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
    </div>
  );
}
