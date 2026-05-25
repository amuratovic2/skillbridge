import type { ReactNode } from 'react';

type FeedbackBannerProps = {
  type: 'success' | 'error' | 'info';
  children: ReactNode;
  className?: string;
};

const styles = {
  success: 'bg-green-50 text-green-700 border-green-100',
  error: 'bg-red-50 text-red-700 border-red-100',
  info: 'bg-blue-50 text-blue-700 border-blue-100',
};

export default function FeedbackBanner({ type, children, className = '' }: FeedbackBannerProps) {
  return (
    <div className={`rounded-lg border px-4 py-3 text-sm ${styles[type]} ${className}`}>
      {children}
    </div>
  );
}
