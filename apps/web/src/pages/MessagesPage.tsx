import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';

export default function MessagesPage() {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const [conversations, setConversations] = useState<any[]>([]);
  const [selectedPartner, setSelectedPartner] = useState<number | null>(null);
  const [partnerName, setPartnerName] = useState<string>('');
  const [messages, setMessages] = useState<any[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const toParam = searchParams.get('to');

  useEffect(() => {
    api
      .get('/messages/conversations')
      .then((res) => {
        setConversations(res.data.data || []);
        if (toParam) {
          setSelectedPartner(parseInt(toParam, 10));
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [toParam]);

  useEffect(() => {
    if (!selectedPartner) return;

    api
      .get(`/messages/conversation/${selectedPartner}`)
      .then((res) => setMessages(res.data.data || []))
      .catch(() => setMessages([]));

    api.patch(`/messages/read/${selectedPartner}`).catch(() => {});

    api
      .get(`/users/${selectedPartner}`)
      .then((res) => {
        const u = res.data.data;
        setPartnerName(
          [u.firstName, u.lastName].filter(Boolean).join(' ') || u.username,
        );
      })
      .catch(() => setPartnerName(`Korisnik #${selectedPartner}`));
  }, [selectedPartner]);

  const sendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedPartner) return;

    try {
      await api.post('/messages', {
        receiverId: selectedPartner,
        content: newMessage,
      });
      setNewMessage('');

      const res = await api.get(`/messages/conversation/${selectedPartner}`);
      setMessages(res.data.data || []);

      const convRes = await api.get('/messages/conversations');
      setConversations(convRes.data.data || []);
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

  const partnerInitial = partnerName?.charAt(0)?.toUpperCase() || '?';
  const showNewConversation =
    selectedPartner && !conversations.some((c) => c.partnerId === selectedPartner);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Poruke</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 min-h-[60vh]">
        {/* Conversation list */}
        <div className="md:col-span-1 bg-white border border-gray-200 rounded-xl overflow-hidden">
          <div className="p-4 border-b border-gray-100">
            <h2 className="font-medium text-gray-900">Razgovori</h2>
          </div>
          <div className="divide-y divide-gray-100">
            {showNewConversation && (
              <button
                onClick={() => setSelectedPartner(selectedPartner)}
                className="w-full text-left p-4 bg-primary-50"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 bg-primary-600 text-white rounded-full flex items-center justify-center text-sm font-medium">
                    {partnerInitial}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{partnerName}</p>
                    <p className="text-xs text-primary-600">Novi razgovor</p>
                  </div>
                </div>
              </button>
            )}
            {conversations.length === 0 && !showNewConversation ? (
              <p className="p-4 text-sm text-gray-400 text-center">Nema razgovora</p>
            ) : (
              conversations.map((conv: any) => (
                <ConversationItem
                  key={conv.partnerId}
                  conv={conv}
                  isSelected={selectedPartner === conv.partnerId}
                  onClick={() => setSelectedPartner(conv.partnerId)}
                />
              ))
            )}
          </div>
        </div>

        {/* Messages */}
        <div className="md:col-span-2 bg-white border border-gray-200 rounded-xl flex flex-col">
          {selectedPartner ? (
            <>
              <div className="p-4 border-b border-gray-100 flex items-center gap-3">
                <div className="w-8 h-8 bg-primary-600 text-white rounded-full flex items-center justify-center text-sm font-medium">
                  {partnerInitial}
                </div>
                <h2 className="font-medium text-gray-900">{partnerName}</h2>
              </div>
              <div className="flex-1 p-4 space-y-3 overflow-y-auto max-h-[50vh]">
                {messages.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-8">
                    Započnite razgovor slanjem poruke
                  </p>
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
                        <div
                          className={`text-xs mt-1 ${msg.senderId === user?.id ? 'text-primary-200' : 'text-gray-400'}`}
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
              <form onSubmit={sendMessage} className="p-4 border-t border-gray-100 flex gap-2">
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
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-400">
              <p>Odaberite razgovor</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function ConversationItem({
  conv,
  isSelected,
  onClick,
}: {
  conv: any;
  isSelected: boolean;
  onClick: () => void;
}) {
  const [name, setName] = useState(`Korisnik #${conv.partnerId}`);

  useEffect(() => {
    api
      .get(`/users/${conv.partnerId}`)
      .then((res) => {
        const u = res.data.data;
        setName([u.firstName, u.lastName].filter(Boolean).join(' ') || u.username);
      })
      .catch(() => {});
  }, [conv.partnerId]);

  const initial = name.charAt(0).toUpperCase();

  return (
    <button
      onClick={onClick}
      className={`w-full text-left p-4 hover:bg-gray-50 transition-colors ${
        isSelected ? 'bg-primary-50' : ''
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center text-sm font-medium">
            {initial}
          </div>
          <div>
            <p className="text-sm font-medium text-gray-900">{name}</p>
            <p className="text-xs text-gray-500 line-clamp-1">{conv.lastMessage}</p>
          </div>
        </div>
        {conv.unreadCount > 0 && (
          <span className="bg-primary-600 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
            {conv.unreadCount}
          </span>
        )}
      </div>
    </button>
  );
}
