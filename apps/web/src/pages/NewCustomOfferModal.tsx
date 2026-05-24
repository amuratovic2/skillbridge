import { useEffect, useState } from 'react';
import Modal from '../components/ui/Modal';
import { useAuth } from '../context/AuthContext';
import api from '../lib/api';
import { customOffersApi } from '../lib/orders';
import { userServiceApi, UserProfile } from '../lib/user-service';

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

interface GigOption {
  id: number;
  title: string;
  cost: number;
}

export default function NewCustomOfferModal({ onClose, onCreated }: Props) {
  const { user } = useAuth();
  const [receiverQuery, setReceiverQuery] = useState('');
  const [receiverResults, setReceiverResults] = useState<UserProfile[]>([]);
  const [receiver, setReceiver] = useState<UserProfile | null>(null);
  const [gigs, setGigs] = useState<GigOption[]>([]);
  const [gigId, setGigId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [deliveryDays, setDeliveryDays] = useState('7');
  const [revisionCount, setRevisionCount] = useState('1');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user?.id) return;
    api
      .get(`/gigs/freelancer/${user.id}`)
      .then((response) => setGigs(response.data.data ?? []))
      .catch(() => setGigs([]));
  }, [user?.id]);

  useEffect(() => {
    const term = receiverQuery.trim();
    if (receiver || term.length < 2) {
      setReceiverResults([]);
      return;
    }

    const timeout = window.setTimeout(() => {
      userServiceApi
        .listUsers({ page: 1, limit: 6, query: term, role: 'CLIENT' })
        .then((response) => setReceiverResults(response.data.filter((profile) => profile.id !== user?.id)))
        .catch(() => setReceiverResults([]));
    }, 250);

    return () => window.clearTimeout(timeout);
  }, [receiver, receiverQuery, user?.id]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!receiver) {
      setError('Odaberite klijenta kojem šaljete ponudu.');
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      await customOffersApi.create({
        receiverId: receiver.id,
        gigId: gigId ? Number(gigId) : null,
        title,
        description: description || undefined,
        price: Number(price),
        deliveryDays: Number(deliveryDays),
        revisionCount: Number(revisionCount),
      });
      onCreated();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Slanje nije uspjelo');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal title="Nova prilagođena ponuda" onClose={onClose} size="lg">
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-700 mb-1">Klijent</label>
          {receiver ? (
            <div className="flex items-center justify-between gap-3 border border-primary-200 bg-primary-50 rounded-lg px-3 py-2">
              <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{displayName(receiver)}</p>
                <p className="text-xs text-gray-500 truncate">{receiver.email}</p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setReceiver(null);
                  setReceiverQuery('');
                }}
                className="text-sm text-primary-700 hover:underline shrink-0"
              >
                Promijeni
              </button>
            </div>
          ) : (
            <div className="relative">
              <input
                type="search"
                value={receiverQuery}
                onChange={(event) => setReceiverQuery(event.target.value)}
                placeholder="Pretražite klijenta po imenu ili emailu"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
              {receiverResults.length > 0 && (
                <div className="absolute z-10 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden">
                  {receiverResults.map((profile) => (
                    <button
                      key={profile.id}
                      type="button"
                      onClick={() => {
                        setReceiver(profile);
                        setReceiverQuery(displayName(profile));
                      }}
                      className="w-full text-left px-3 py-2 hover:bg-gray-50"
                    >
                      <p className="text-sm font-medium text-gray-900">{displayName(profile)}</p>
                      <p className="text-xs text-gray-500">{profile.email}</p>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div>
          <label className="block text-sm text-gray-700 mb-1">Vezani gig</label>
          <select
            value={gigId}
            onChange={(event) => setGigId(event.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white"
          >
            <option value="">Bez vezanog giga</option>
            {gigs.map((gig) => (
              <option key={gig.id} value={gig.id}>
                {gig.title} ({Number(gig.cost).toFixed(2)} €)
              </option>
            ))}
          </select>
          <p className="text-xs text-gray-400 mt-1">
            Ako je gig vezan, prihvatanje ponude automatski kreira narudžbu iz ponude.
          </p>
        </div>

        <div>
          <label className="block text-sm text-gray-700 mb-1">Naslov ponude</label>
          <input
            type="text"
            required
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label className="block text-sm text-gray-700 mb-1">Opis</label>
          <textarea
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={4}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-sm text-gray-700 mb-1">Cijena (€)</label>
            <input
              type="number"
              step="0.01"
              required
              min={0.01}
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-700 mb-1">Rok (dana)</label>
            <input
              type="number"
              required
              min={1}
              value={deliveryDays}
              onChange={(event) => setDeliveryDays(event.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-700 mb-1">Revizije</label>
            <input
              type="number"
              required
              min={0}
              value={revisionCount}
              onChange={(event) => setRevisionCount(event.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            Otkaži
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700 disabled:opacity-60"
          >
            {submitting ? 'Slanje...' : 'Pošalji ponudu'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function displayName(profile: UserProfile) {
  return [profile.firstName, profile.lastName].filter(Boolean).join(' ') || profile.username;
}
