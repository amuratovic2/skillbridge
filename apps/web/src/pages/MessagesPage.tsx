import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';
import { communicationApi, ConversationSummary, Message } from '../lib/communication';
import { userServiceApi, UserProfile } from '../lib/user-service';

const POLL_INTERVAL_MS = 10000;

function getDisplayName(user?: UserProfile | null) {
  if (!user) return '';
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username;
}

function getInitial(name: string) {
  return name.trim().charAt(0).toUpperCase() || '?';
}

export default function MessagesPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [selectedPartner, setSelectedPartner] = useState<number | null>(null);
  const [partnerProfiles, setPartnerProfiles] = useState<Record<number, UserProfile>>({});
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [userQuery, setUserQuery] = useState('');
  const [userResults, setUserResults] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const loadingProfilesRef = useRef(new Set<number>());

  const toParam = searchParams.get('to');

  const selectedProfile = selectedPartner ? partnerProfiles[selectedPartner] : null;
  const selectedName = selectedProfile
    ? getDisplayName(selectedProfile)
    : selectedPartner
      ? `Korisnik #${selectedPartner}`
      : '';
  const selectedInitial = getInitial(selectedName);

  const conversationsByPartner = useMemo(
    () => new Map(conversations.map((conversation) => [conversation.partnerId, conversation])),
    [conversations],
  );

  const loadProfile = useCallback((userId: number) => {
    if (partnerProfiles[userId] || loadingProfilesRef.current.has(userId)) return;

    loadingProfilesRef.current.add(userId);
    api
      .get(`/users/${userId}`)
      .then((response) => {
        const profile = response.data.data as UserProfile;
        setPartnerProfiles((current) => ({ ...current, [userId]: profile }));
      })
      .catch(() => {})
      .finally(() => {
        loadingProfilesRef.current.delete(userId);
      });
  }, [partnerProfiles]);

  const loadConversations = useCallback(async () => {
    const nextConversations = await communicationApi.conversations();
    setConversations(nextConversations);
    nextConversations.forEach((conversation) => loadProfile(conversation.partnerId));
    return nextConversations;
  }, [loadProfile]);

  const loadActiveConversation = useCallback(
    async (partnerId: number) => {
      const response = await communicationApi.conversation(partnerId, { limit: 50 });
      setMessages(response.data);
      await communicationApi.markRead(partnerId).catch(() => null);
      loadProfile(partnerId);
      return response.data;
    },
    [loadProfile],
  );

  const refreshMessages = useCallback(
    async (options?: { initial?: boolean }) => {
      try {
        setError('');
        const nextConversations = await loadConversations();

        if (selectedPartner) {
          await loadActiveConversation(selectedPartner);
        } else if (toParam) {
          const target = Number(toParam);
          if (Number.isFinite(target) && target > 0) {
            setSelectedPartner(target);
            await loadActiveConversation(target);
          }
        } else if (nextConversations.length > 0 && options?.initial) {
          setSelectedPartner(nextConversations[0].partnerId);
          await loadActiveConversation(nextConversations[0].partnerId);
        }
      } catch (err: any) {
        setError(err.response?.data?.message || 'Poruke trenutno nije moguce ucitati.');
      } finally {
        if (options?.initial) setLoading(false);
      }
    },
    [loadActiveConversation, loadConversations, selectedPartner, toParam],
  );

  useEffect(() => {
    refreshMessages({ initial: true });
  }, [refreshMessages]);

  useEffect(() => {
    const tick = window.setInterval(() => {
      if (document.visibilityState === 'visible') {
        refreshMessages();
      }
    }, POLL_INTERVAL_MS);

    return () => window.clearInterval(tick);
  }, [refreshMessages]);

  useEffect(() => {
    if (!selectedPartner) return;
    const next = new URLSearchParams(searchParams);
    next.set('to', String(selectedPartner));
    setSearchParams(next, { replace: true });
  }, [selectedPartner]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages.length, selectedPartner]);

  useEffect(() => {
    const term = userQuery.trim();
    if (term.length < 2) {
      setUserResults([]);
      return;
    }

    const timeout = window.setTimeout(() => {
      userServiceApi
        .listUsers({ page: 1, limit: 8, query: term })
        .then((response) => {
          setUserResults(response.data.filter((profile) => profile.id !== user?.id));
        })
        .catch(() => setUserResults([]));
    }, 250);

    return () => window.clearTimeout(timeout);
  }, [user?.id, userQuery]);

  const selectPartner = (profileOrId: UserProfile | number) => {
    const partnerId = typeof profileOrId === 'number' ? profileOrId : profileOrId.id;
    if (typeof profileOrId !== 'number') {
      setPartnerProfiles((current) => ({ ...current, [profileOrId.id]: profileOrId }));
    }
    setSelectedPartner(partnerId);
    setMessages([]);
    loadActiveConversation(partnerId).catch(() => {});
  };

  const sendMessage = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!newMessage.trim() || !selectedPartner || sending) return;

    setSending(true);
    setError('');

    try {
      await communicationApi.send({
        receiverId: selectedPartner,
        content: newMessage.trim(),
      });
      setNewMessage('');
      await loadActiveConversation(selectedPartner);
      await loadConversations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Slanje poruke nije uspjelo.');
    } finally {
      setSending(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  const showNewConversation =
    selectedPartner && !conversationsByPartner.has(selectedPartner);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-3 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Poruke</h1>
        </div>
      </div>

      {error && (
        <div className="mb-4 px-4 py-3 bg-red-50 text-red-700 rounded-lg text-sm">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 min-h-[68vh]">
        <div className="md:col-span-1 bg-white border border-gray-200 rounded-lg overflow-hidden">
          <div className="p-4 border-b border-gray-100">
            <h2 className="font-medium text-gray-900">Korisnici</h2>
            <input
              type="search"
              value={userQuery}
              onChange={(event) => setUserQuery(event.target.value)}
              placeholder="Pretrazi korisnike..."
              className="mt-3 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
            />
          </div>

          {userResults.length > 0 && (
            <div className="border-b border-gray-100">
              <div className="px-4 py-2 text-xs font-medium uppercase text-gray-400">Rezultati</div>
              {userResults.map((profile) => (
                <button
                  key={profile.id}
                  onClick={() => {
                    setUserQuery('');
                    setUserResults([]);
                    selectPartner(profile);
                  }}
                  className="w-full text-left px-4 py-3 hover:bg-gray-50"
                >
                  <div className="flex items-center gap-3">
                    <Avatar name={getDisplayName(profile)} imageUrl={profile.profilePicture} />
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">{getDisplayName(profile)}</p>
                      <p className="text-xs text-gray-500 truncate">{profile.email}</p>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}

          <div className="divide-y divide-gray-100">
            <div className="px-4 py-3 text-xs font-medium uppercase text-gray-400">Razgovori</div>
            {showNewConversation && (
              <button
                onClick={() => selectedPartner && selectPartner(selectedPartner)}
                className="w-full text-left p-4 bg-primary-50"
              >
                <div className="flex items-center gap-3">
                  <Avatar name={selectedName} imageUrl={selectedProfile?.profilePicture} active />
                  <div>
                    <p className="text-sm font-medium text-gray-900">{selectedName}</p>
                    <p className="text-xs text-primary-600">Novi razgovor</p>
                  </div>
                </div>
              </button>
            )}
            {conversations.length === 0 && !showNewConversation ? (
              <p className="p-4 text-sm text-gray-400 text-center">Nema razgovora</p>
            ) : (
              conversations.map((conversation) => (
                <ConversationItem
                  key={conversation.partnerId}
                  conversation={conversation}
                  profile={partnerProfiles[conversation.partnerId]}
                  isSelected={selectedPartner === conversation.partnerId}
                  onClick={() => selectPartner(conversation.partnerId)}
                />
              ))
            )}
          </div>
        </div>

        <div className="md:col-span-2 bg-white border border-gray-200 rounded-lg flex flex-col min-h-[68vh]">
          {selectedPartner ? (
            <>
              <div className="p-4 border-b border-gray-100 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <Avatar name={selectedName} imageUrl={selectedProfile?.profilePicture} active />
                  <div className="min-w-0">
                    <h2 className="font-medium text-gray-900 truncate">{selectedName}</h2>
                    <p className="text-xs text-gray-400">Korisnik #{selectedPartner}</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => refreshMessages()}
                  className="text-sm text-primary-700 hover:text-primary-800"
                >
                  Osvjezi
                </button>
              </div>

              <div className="flex-1 p-4 space-y-3 overflow-y-auto max-h-[54vh]">
                {messages.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-10">
                    Započnite razgovor slanjem poruke.
                  </p>
                ) : (
                  messages.map((message) => {
                    const own = message.senderId === user?.id;
                    return (
                      <div key={message.id} className={`flex ${own ? 'justify-end' : 'justify-start'}`}>
                        <div
                          className={`max-w-[78%] px-4 py-2 rounded-2xl text-sm ${
                            own ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-900'
                          }`}
                        >
                          <p className="whitespace-pre-line break-words">{message.content}</p>
                          <div className={`text-xs mt-1 ${own ? 'text-primary-100' : 'text-gray-400'}`}>
                            {new Date(message.sentAt).toLocaleTimeString('bs', {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={messagesEndRef} />
              </div>

              <form onSubmit={sendMessage} className="p-4 border-t border-gray-100 flex gap-2">
                <input
                  type="text"
                  value={newMessage}
                  onChange={(event) => setNewMessage(event.target.value)}
                  placeholder="Unesite poruku..."
                  className="flex-1 border border-gray-300 rounded-lg px-4 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                />
                <button
                  type="submit"
                  disabled={sending || !newMessage.trim()}
                  className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700 disabled:opacity-50"
                >
                  {sending ? 'Slanje...' : 'Posalji'}
                </button>
              </form>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-400 px-4 text-center">
              <p>Odaberite razgovor ili pretrazite korisnika za novu poruku.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function ConversationItem({
  conversation,
  profile,
  isSelected,
  onClick,
}: {
  conversation: ConversationSummary;
  profile?: UserProfile;
  isSelected: boolean;
  onClick: () => void;
}) {
  const name = getDisplayName(profile) || `Korisnik #${conversation.partnerId}`;

  return (
    <button
      onClick={onClick}
      className={`w-full text-left p-4 hover:bg-gray-50 transition-colors ${
        isSelected ? 'bg-primary-50' : ''
      }`}
    >
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <Avatar name={name} imageUrl={profile?.profilePicture} />
          <div className="min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate">{name}</p>
            <p className="text-xs text-gray-500 truncate">{conversation.lastMessage}</p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-1 shrink-0">
          {conversation.lastAt && (
            <span className="text-[11px] text-gray-400">
              {new Date(conversation.lastAt).toLocaleTimeString('bs', {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </span>
          )}
          {conversation.unreadCount > 0 && (
            <span className="bg-primary-600 text-white text-xs rounded-full min-w-5 h-5 px-1.5 flex items-center justify-center">
              {conversation.unreadCount}
            </span>
          )}
        </div>
      </div>
    </button>
  );
}

function Avatar({ name, imageUrl, active }: { name: string; imageUrl?: string; active?: boolean }) {
  return (
    <div
      className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-medium overflow-hidden ${
        active ? 'bg-primary-600 text-white' : 'bg-primary-100 text-primary-700'
      }`}
    >
      {imageUrl ? <img src={imageUrl} alt={name} className="w-full h-full object-cover" /> : getInitial(name)}
    </div>
  );
}
