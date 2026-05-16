import { useState } from 'react';
import { Order, ORDER_STATUS_META, OrderStatus, ordersApi } from '../lib/orders';

const FORCEABLE: OrderStatus[] = [
  'PENDING',
  'ACCEPTED',
  'IN_PROGRESS',
  'DELIVERED',
  'REVISION_REQUESTED',
  'COMPLETED',
  'CANCELLED',
  'DISPUTED',
];

interface Props {
  order: Order;
  onChange: () => void;
}

export default function AdminOrderTools({ order, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [totalCost, setTotalCost] = useState(String(order.totalCost));
  const [maxRevisions, setMaxRevisions] = useState(String(order.maxRevisions));
  const [forceStatus, setForceStatus] = useState<OrderStatus>(order.status);
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submitPatch = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);

    const ops: Array<{ op: 'replace'; path: string; value: unknown }> = [];
    const cost = Number(totalCost);
    const rev = Number(maxRevisions);
    if (!Number.isNaN(cost) && cost !== Number(order.totalCost)) {
      ops.push({ op: 'replace', path: '/totalCost', value: cost });
    }
    if (!Number.isNaN(rev) && rev !== order.maxRevisions) {
      ops.push({ op: 'replace', path: '/maxRevisions', value: rev });
    }
    if (ops.length === 0) {
      setError('Nijedna vrijednost se nije promijenila');
      setBusy(false);
      return;
    }

    try {
      await ordersApi.patch(order.id, ops);
      onChange();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška');
    } finally {
      setBusy(false);
    }
  };

  const submitForce = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await ordersApi.updateStatus(order.id, forceStatus, note || undefined);
      setNote('');
      onChange();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex items-center justify-between w-full text-left"
      >
        <span className="text-sm font-medium text-amber-900">
          Admin alati {open ? '▾' : '▸'}
        </span>
      </button>

      {open && (
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-6">
          <form onSubmit={submitPatch} className="space-y-3">
            <h3 className="font-medium text-sm text-amber-900">Uredi narudžbu</h3>
            <div>
              <label className="block text-xs text-gray-700 mb-1">Cijena (€)</label>
              <input
                type="number"
                step="0.01"
                value={totalCost}
                onChange={(e) => setTotalCost(e.target.value)}
                className="w-full border border-amber-300 bg-white rounded-lg px-3 py-1.5 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-700 mb-1">Maks. revizije</label>
              <input
                type="number"
                min={0}
                value={maxRevisions}
                onChange={(e) => setMaxRevisions(e.target.value)}
                className="w-full border border-amber-300 bg-white rounded-lg px-3 py-1.5 text-sm"
              />
            </div>
            <button
              type="submit"
              disabled={busy}
              className="bg-amber-600 text-white px-4 py-1.5 rounded-lg text-sm hover:bg-amber-700 disabled:opacity-60"
            >
              Spremi izmjene
            </button>
          </form>

          <form onSubmit={submitForce} className="space-y-3">
            <h3 className="font-medium text-sm text-amber-900">Promijeni status</h3>
            <div>
              <label className="block text-xs text-gray-700 mb-1">Novi status</label>
              <select
                value={forceStatus}
                onChange={(e) => setForceStatus(e.target.value as OrderStatus)}
                className="w-full border border-amber-300 bg-white rounded-lg px-3 py-1.5 text-sm"
              >
                {FORCEABLE.map((s) => (
                  <option key={s} value={s}>
                    {s} – {ORDER_STATUS_META[s].label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-700 mb-1">Napomena</label>
              <input
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className="w-full border border-amber-300 bg-white rounded-lg px-3 py-1.5 text-sm"
                placeholder="Razlog promjene..."
              />
            </div>
            <button
              type="submit"
              disabled={busy}
              className="bg-amber-600 text-white px-4 py-1.5 rounded-lg text-sm hover:bg-amber-700 disabled:opacity-60"
            >
              Primijeni status
            </button>
          </form>
        </div>
      )}

      {error && <p className="text-sm text-red-600 mt-3">{error}</p>}
    </div>
  );
}
