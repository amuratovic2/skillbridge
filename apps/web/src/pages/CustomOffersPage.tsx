import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  CustomOffer,
  CUSTOM_OFFER_STATUS_META,
  customOffersApi,
} from '../lib/orders';
import { getApiErrorMessage } from '../lib/api';
import { userServiceApi, UserProfile } from '../lib/user-service';
import { useAuth } from '../context/AuthContext';
import NewCustomOfferModal from './NewCustomOfferModal';

type Tab = 'received' | 'sent';

export default function CustomOffersPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('received');
  const [offers, setOffers] = useState<CustomOffer[]>([]);
  const [participantNames, setParticipantNames] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isFreelancer = user?.role === 'FREELANCER';

  const load = (which: Tab) => {
    setLoading(true);
    setError(null);
    const fetcher = which === 'received' ? customOffersApi.received() : customOffersApi.sent();
    fetcher
      .then((data) => {
        setOffers(data);
        loadParticipantNames(data);
      })
      .catch(() => {
        setOffers([]);
        setParticipantNames({});
      })
      .finally(() => setLoading(false));
  };

  const loadParticipantNames = async (data: CustomOffer[]) => {
    const ids = Array.from(new Set(data.flatMap((offer) => [offer.senderId, offer.receiverId])));
    const entries = await Promise.all(
      ids.map((id) =>
        userServiceApi
          .byId(id)
          .then((profile) => [id, displayName(profile)] as const)
          .catch(() => [id, `#${id}`] as const),
      ),
    );

    setParticipantNames(Object.fromEntries(entries));
  };

  useEffect(() => load(tab), [tab]);

  const respond = async (offer: CustomOffer, status: 'ACCEPTED' | 'REJECTED') => {
    try {
      const updated = await customOffersApi.respond(offer.id, status);
      if (status === 'ACCEPTED' && updated.orderId) {
        navigate(`/dashboard/orders/${updated.orderId}`);
        return;
      }
      load(tab);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Greška'));
    }
  };

  const withdraw = async (offer: CustomOffer) => {
    try {
      await customOffersApi.withdraw(offer.id);
      load(tab);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Greška'));
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Prilagođene ponude</h1>
        {isFreelancer && (
          <button
            onClick={() => setCreating(true)}
            className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
          >
            + Nova ponuda
          </button>
        )}
      </div>

      <div className="border-b border-gray-200 mb-6">
        <div className="flex gap-6">
          {(['received', 'sent'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`pb-3 -mb-px text-sm font-medium border-b-2 transition-colors ${
                tab === t
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {t === 'received' ? 'Primljene' : 'Poslate'}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 text-red-700 px-4 py-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center min-h-[30vh]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
        </div>
      ) : offers.length === 0 ? (
        <p className="text-center text-gray-500 py-16">
          {tab === 'received' ? 'Nemate primljenih ponuda' : 'Nemate poslatih ponuda'}
        </p>
      ) : (
        <div className="space-y-4">
          {offers.map((offer) => {
            const meta = CUSTOM_OFFER_STATUS_META[offer.status];
            const otherParty = tab === 'received' ? offer.senderId : offer.receiverId;
            return (
              <div
                key={offer.id}
                className="bg-white border border-gray-200 rounded-xl p-6"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-3 flex-wrap">
                      <h2 className="font-semibold text-gray-900">{offer.title}</h2>
                      <span className={`px-2 py-0.5 text-xs rounded-full ${meta.chip}`}>
                        {meta.label}
                      </span>
                    </div>
                    {offer.description && (
                      <p className="text-sm text-gray-600 mt-1 line-clamp-2">
                        {offer.description}
                      </p>
                    )}
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-xl font-bold text-gray-900">
                      {Number(offer.price).toFixed(2)} &euro;
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mt-4 pt-4 border-t border-gray-100">
                  <div>
                    <p className="text-gray-500">Rok</p>
                    <p className="font-medium">{offer.deliveryDays} dana</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Revizije</p>
                    <p className="font-medium">{offer.revisionCount}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Vezano za gig</p>
                    <p className="font-medium text-primary-600">
                      {offer.gigId ? `#${offer.gigId}` : '—'}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-500">{tab === 'received' ? 'Od' : 'Za'}</p>
                    <p className="font-medium">{participantNames[otherParty] ?? `#${otherParty}`}</p>
                  </div>
                </div>

                {offer.status === 'PENDING' && tab === 'received' && (
                  <div className="flex gap-2 mt-4">
                    <button
                      onClick={() => respond(offer, 'ACCEPTED')}
                      className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
                    >
                      Prihvati
                    </button>
                    <button
                      onClick={() => respond(offer, 'REJECTED')}
                      className="border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm hover:bg-red-50"
                    >
                      Odbij
                    </button>
                  </div>
                )}
                {offer.status === 'PENDING' && tab === 'sent' && (
                  <div className="mt-4">
                    <button
                      onClick={() => withdraw(offer)}
                      className="border border-gray-300 text-gray-700 px-4 py-2 rounded-lg text-sm hover:bg-gray-50"
                    >
                      Povuci ponudu
                    </button>
                  </div>
                )}
                {offer.status === 'ACCEPTED' && offer.orderId && (
                  <div className="mt-4">
                    <Link
                      to={`/dashboard/orders/${offer.orderId}`}
                      className="inline-flex text-sm text-primary-600 font-medium hover:underline"
                    >
                      Otvori narudžbu #{offer.orderId}
                    </Link>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {creating && (
        <NewCustomOfferModal
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
            setTab('sent');
            load('sent');
          }}
        />
      )}
    </div>
  );
}

function displayName(profile: UserProfile) {
  return [profile.firstName, profile.lastName].filter(Boolean).join(' ') || profile.username || profile.email;
}
