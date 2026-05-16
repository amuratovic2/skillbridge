import { useState } from 'react';
import Modal from '../../components/ui/Modal';
import { ordersApi } from '../../lib/orders';

interface Props {
  orderId: number;
  onClose: () => void;
  onDone: () => void;
}

export default function CancelOrderModal({ orderId, onClose, onDone }: Props) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await ordersApi.updateStatus(orderId, 'CANCELLED', reason || undefined);
      onDone();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri otkazivanju');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={`Otkaži narudžbu #${orderId}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-700 mb-1">Razlog otkazivanja</label>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            placeholder="Kratko opišite zašto otkazujete narudžbu (vidjet će ga druga strana)..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
          />
          <p className="text-xs text-gray-400 mt-1">
            Razlog se sprema u historiju i šalje se kao notifikacija drugoj strani.
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
            className="bg-red-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-red-700 disabled:opacity-60"
          >
            {busy ? 'Otkazivanje...' : 'Otkaži narudžbu'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
