import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { notificationsApi, Notification } from '../../lib/orders';

const NAVIGATION_BY_TYPE: Record<string, (refId: number | null) => string | null> = {
  ORDER_UPDATE: (refId) => (refId ? `/dashboard/orders/${refId}` : '/dashboard/orders'),
  CUSTOM_OFFER: () => '/dashboard/custom-offers',
  NEW_MESSAGE: (refId) => (refId ? `/dashboard/orders/${refId}` : '/dashboard/messages'),
  DISPUTE_UPDATE: (refId) => (refId ? `/dashboard/orders/${refId}` : '/dashboard'),
  REVIEW_RECEIVED: () => '/dashboard',
  SYSTEM: () => null,
};

export default function NotificationsBell() {
  const { isAuthenticated } = useAuth();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const popoverRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const refresh = () => {
    if (!isAuthenticated) {
      setItems([]);
      setUnread(0);
      return;
    }
    notificationsApi.list({ limit: 15 }).then((r) => setItems(r.data)).catch(() => {});
    notificationsApi.unreadCount().then(setUnread).catch(() => {});
  };

  useEffect(() => {
    if (!isAuthenticated) {
      setItems([]);
      setUnread(0);
      setOpen(false);
      return;
    }

    refresh();
    const tick = setInterval(refresh, 15000);
    return () => clearInterval(tick);
  }, [isAuthenticated]);

  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [open]);

  const markAllRead = async () => {
    if (!isAuthenticated) return;
    await notificationsApi.markAllRead().catch(() => {});
    refresh();
  };

  return (
    <div className="relative" ref={popoverRef}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="relative p-1 rounded-full hover:bg-gray-100"
        aria-label="Obavještenja"
      >
        <svg className="w-6 h-6 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.6}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>
        {unread > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] rounded-full w-4 h-4 flex items-center justify-center">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-96 max-w-[calc(100vw-2rem)] bg-white border border-gray-200 rounded-lg shadow-lg z-50">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <h3 className="font-semibold text-gray-900">Obavještenja</h3>
            <button
              onClick={markAllRead}
              className="text-xs text-primary-600 hover:underline"
              disabled={unread === 0}
            >
              Označi sve kao pročitano
            </button>
          </div>
          <div className="max-h-96 overflow-y-auto">
            {items.length === 0 ? (
              <p className="text-center text-sm text-gray-400 py-8">Nema obavještenja</p>
            ) : (
              items.map((n) => (
                <button
                  key={n.id}
                  onClick={async () => {
                    if (!n.isRead) await notificationsApi.markRead(n.id).catch(() => {});
                    const target = NAVIGATION_BY_TYPE[n.type]?.(n.referenceId);
                    refresh();
                    if (target) {
                      setOpen(false);
                      navigate(target);
                    }
                  }}
                  className={`w-full text-left px-4 py-3 border-b border-gray-50 hover:bg-gray-50 ${
                    n.isRead ? '' : 'bg-primary-50/40'
                  }`}
                >
                  <div className="flex items-start gap-2">
                    <span
                      className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${
                        n.isRead ? 'bg-gray-200' : 'bg-primary-500'
                      }`}
                    />
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-900">{n.title}</p>
                      <p className="text-xs text-gray-600 mt-0.5 break-words">{n.content}</p>
                      <p className="text-xs text-gray-400 mt-1">
                        {new Date(n.createdAt).toLocaleString('bs')}
                      </p>
                    </div>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
