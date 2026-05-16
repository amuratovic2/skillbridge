import { useState } from 'react';
import Modal from '../../components/ui/Modal';
import { deliveriesApi } from '../../lib/orders';

interface Props {
  orderId: number;
  nextVersion: number;
  onClose: () => void;
  onDone: () => void;
}

export default function DeliverWorkModal({ orderId, nextVersion, onClose, onDone }: Props) {
  const [message, setMessage] = useState('');
  const [fileUrl, setFileUrl] = useState('');
  const [fileName, setFileName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim() && !fileUrl.trim()) {
      setError('Dodajte poruku ili URL fajla');
      return;
    }
    setError(null);
    setBusy(true);
    try {
      await deliveriesApi.create(orderId, {
        message: message.trim() || undefined,
        fileUrl: fileUrl.trim() || undefined,
        fileName: fileName.trim() || undefined,
      });
      onDone();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri isporuci');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={`Isporuči rad — verzija ${nextVersion} (#${orderId})`} onClose={onClose} size="lg">
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-700 mb-1">Poruka klijentu</label>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={4}
            placeholder="Šta ste uradili u ovoj verziji? Napomene za klijenta..."
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 outline-none"
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm text-gray-700 mb-1">URL fajla (opcionalno)</label>
            <input
              type="url"
              value={fileUrl}
              onChange={(e) => setFileUrl(e.target.value)}
              placeholder="https://drive.google.com/..."
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-700 mb-1">Naziv fajla</label>
            <input
              type="text"
              value={fileName}
              onChange={(e) => setFileName(e.target.value)}
              placeholder="dizajn-v1.zip"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
        </div>
        <p className="text-xs text-gray-400">
          Klijent dobija notifikaciju i može pregledati ovu verziju iz Isporuka.
        </p>

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
            className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700 disabled:opacity-60"
          >
            {busy ? 'Isporuka...' : 'Pošalji isporuku'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
