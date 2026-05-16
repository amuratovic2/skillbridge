import { useEffect, useState } from 'react';
import Modal from '../../components/ui/Modal';
import { deliveriesApi, Delivery } from '../../lib/orders';

interface Props {
  orderId: number;
  version: number;
  onClose: () => void;
}

export default function DeliveryDetailModal({ orderId, version, onClose }: Props) {
  const [delivery, setDelivery] = useState<Delivery | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    deliveriesApi
      .byVersion(orderId, version)
      .then(setDelivery)
      .catch((err) => setError(err.response?.data?.message || 'Verzija nije pronađena'))
      .finally(() => setLoading(false));
  }, [orderId, version]);

  return (
    <Modal title={`Isporuka — verzija ${version}`} onClose={onClose} size="lg">
      {loading ? (
        <div className="flex justify-center py-8">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600" />
        </div>
      ) : error ? (
        <p className="text-sm text-red-600">{error}</p>
      ) : delivery ? (
        <div className="space-y-4">
          <div className="text-xs text-gray-400">
            Poslano: {new Date(delivery.createdAt).toLocaleString('bs')}
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-700 mb-1">Poruka</h3>
            <p className="text-sm text-gray-900 whitespace-pre-wrap">
              {delivery.message || <span className="text-gray-400">Nema poruke</span>}
            </p>
          </div>
          {delivery.fileName && delivery.fileUrl && (
            <div>
              <h3 className="text-sm font-medium text-gray-700 mb-1">Fajl</h3>
              <a
                href={delivery.fileUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-primary-600 hover:underline inline-flex items-center gap-1"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
                {delivery.fileName}
              </a>
            </div>
          )}
        </div>
      ) : null}
    </Modal>
  );
}
