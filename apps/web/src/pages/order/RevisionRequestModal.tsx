import { useState } from 'react';
import Modal from '../../components/ui/Modal';
import { ordersApi } from '../../lib/orders';

interface Props {
  orderId: number;
  revisionsLeft: number;
  onClose: () => void;
  onDone: () => void;
}

export default function RevisionRequestModal({ orderId, revisionsLeft, onClose, onDone }: Props) {
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim()) {
      setError('Molimo opišite šta treba ispraviti');
      return;
    }
    setError(null);
    setBusy(true);
    try {
      await ordersApi.requestRevision(orderId, message.trim());
      onDone();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri slanju zahtjeva');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={`Traži reviziju (#${orderId})`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <p className="text-sm text-gray-600">
          Preostalo revizija: <strong>{revisionsLeft}</strong>
        </p>
        <div>
          <label className="block text-sm text-gray-700 mb-1">Šta treba ispraviti?</label>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={5}
            required
            placeholder="Što konkretnije opišite šta treba promijeniti..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
          />
          <p className="text-xs text-gray-400 mt-1">
            Freelancer vidi tačno ovaj tekst u svojoj notifikaciji i historiji narudžbe.
          </p>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            Odustani
          </button>
          <button
            type="submit"
            disabled={busy}
            className="bg-orange-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-orange-700 disabled:opacity-60"
          >
            {busy ? 'Slanje...' : 'Pošalji zahtjev'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
