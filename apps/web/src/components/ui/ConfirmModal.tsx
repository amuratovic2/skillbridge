import Modal from './Modal';

interface ConfirmModalProps {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: 'danger' | 'primary';
  busy?: boolean;
  error?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

const TONE_CLASSES: Record<NonNullable<ConfirmModalProps['tone']>, string> = {
  danger: 'bg-red-600 hover:bg-red-700',
  primary: 'bg-primary-600 hover:bg-primary-700',
};

export default function ConfirmModal({
  title,
  message,
  confirmLabel,
  cancelLabel = 'Odustani',
  tone = 'primary',
  busy = false,
  error,
  onCancel,
  onConfirm,
}: ConfirmModalProps) {
  return (
    <Modal title={title} onClose={onCancel} size="sm">
      <div className="space-y-4">
        <p className="text-sm text-gray-600 leading-relaxed">{message}</p>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-60"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className={`${TONE_CLASSES[tone]} text-white px-4 py-2 rounded-lg text-sm disabled:opacity-60`}
          >
            {busy ? 'Obrada...' : confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  );
}
