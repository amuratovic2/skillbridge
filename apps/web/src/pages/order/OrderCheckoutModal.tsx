import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../../components/ui/Modal';
import { Order, ordersApi } from '../../lib/orders';

interface GigSummary {
  id: number;
  title: string;
  cost: number;
  deliveryTime: number;
  revisionCount: number;
}

interface Props {
  gig: GigSummary;
  onClose: () => void;
}

export default function OrderCheckoutModal({ gig, onClose }: Props) {
  const navigate = useNavigate();
  const [requirements, setRequirements] = useState('');
  const [createdOrder, setCreatedOrder] = useState<Order | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (busy || createdOrder) return;

    setError(null);
    setBusy(true);
    try {
      const order = await ordersApi.create(gig.id, requirements);
      setCreatedOrder(order);
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
          err.response?.data?.error ||
          'Narudžbu trenutno nije moguće kreirati.',
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={createdOrder ? 'Narudžba je kreirana' : 'Potvrda narudžbe'} onClose={onClose} size="lg">
      {createdOrder ? (
        <div className="space-y-5">
          <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3">
            <p className="text-sm font-medium text-green-800">
              Narudžba #{createdOrder.id} je poslana freelanceru.
            </p>
            <p className="text-sm text-green-700 mt-1">
              Status je trenutno na čekanju. Freelancer treba prihvatiti narudžbu prije početka rada.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row justify-end gap-2">
            <button
              type="button"
              onClick={() => navigate('/dashboard/orders')}
              className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              Moje narudžbe
            </button>
            <button
              type="button"
              onClick={() => navigate(`/dashboard/orders/${createdOrder.id}`)}
              className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700"
            >
              Otvori detalje
            </button>
          </div>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-5">
          <div className="rounded-lg border border-gray-200 divide-y divide-gray-100">
            <div className="p-4">
              <p className="text-sm text-gray-500">Usluga</p>
              <p className="font-medium text-gray-900 mt-1">{gig.title}</p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 divide-y sm:divide-y-0 sm:divide-x divide-gray-100">
              <div className="p-4">
                <p className="text-xs text-gray-500">Cijena</p>
                <p className="font-semibold text-gray-900 mt-1">{Number(gig.cost).toFixed(2)} €</p>
              </div>
              <div className="p-4">
                <p className="text-xs text-gray-500">Rok</p>
                <p className="font-semibold text-gray-900 mt-1">{gig.deliveryTime} dana</p>
              </div>
              <div className="p-4">
                <p className="text-xs text-gray-500">Revizije</p>
                <p className="font-semibold text-gray-900 mt-1">{gig.revisionCount}</p>
              </div>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-800 mb-1">
              Zahtjevi za freelancera
            </label>
            <textarea
              value={requirements}
              onChange={(event) => setRequirements(event.target.value)}
              rows={5}
              maxLength={2000}
              placeholder="Opišite šta vam treba, linkove, preferencije, rokove ili materijale koje freelancer treba znati."
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
            />
            <div className="flex justify-between gap-3 mt-1">
              <p className="text-xs text-gray-400">
                Možete dopuniti detalje i kasnije kroz poruke.
              </p>
              <p className="text-xs text-gray-400 shrink-0">{requirements.length}/2000</p>
            </div>
          </div>

          <div className="rounded-lg border border-yellow-200 bg-yellow-50 px-4 py-3">
            <p className="text-sm text-yellow-800">
              Nakon potvrde narudžba ide u status na čekanju. Freelancer je zatim prihvata i započinje rad.
            </p>
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex flex-col-reverse sm:flex-row justify-end gap-2 pt-1">
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
              className="bg-primary-600 text-white px-5 py-2 rounded-lg text-sm hover:bg-primary-700 disabled:opacity-60"
            >
              {busy ? 'Kreiranje...' : 'Potvrdi narudžbu'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
