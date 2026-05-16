import { useState } from 'react';
import { customOffersApi } from '../lib/orders';

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

export default function NewCustomOfferModal({ onClose, onCreated }: Props) {
  const [receiverId, setReceiverId] = useState('');
  const [gigId, setGigId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [deliveryDays, setDeliveryDays] = useState('7');
  const [revisionCount, setRevisionCount] = useState('1');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await customOffersApi.create({
        receiverId: Number(receiverId),
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
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 px-4">
      <form
        onSubmit={submit}
        className="bg-white rounded-xl w-full max-w-md p-6 space-y-4"
      >
        <h2 className="text-lg font-semibold text-gray-900">Nova prilagođena ponuda</h2>

        <div>
          <label className="block text-sm text-gray-700 mb-1">ID primaoca</label>
          <input
            type="number"
            required
            value={receiverId}
            onChange={(e) => setReceiverId(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-700 mb-1">ID gig-a (opcionalno)</label>
          <input
            type="number"
            value={gigId}
            onChange={(e) => setGigId(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-700 mb-1">Naslov</label>
          <input
            type="text"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-700 mb-1">Opis</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div className="grid grid-cols-3 gap-3">
          <div>
            <label className="block text-sm text-gray-700 mb-1">Cijena (€)</label>
            <input
              type="number"
              step="0.01"
              required
              min={0.01}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
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
              onChange={(e) => setDeliveryDays(e.target.value)}
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
              onChange={(e) => setRevisionCount(e.target.value)}
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
    </div>
  );
}
