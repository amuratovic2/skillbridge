interface BadgeProps {
  label: string;
  variant?: 'primary' | 'secondary' | 'warning';
}

export default function Badge({ label, variant = 'primary' }: BadgeProps) {
  const variants = {
    primary: 'bg-primary-100 text-primary-700',
    secondary: 'bg-gray-100 text-gray-700',
    warning: 'bg-yellow-100 text-yellow-700',
  };

  return (
    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${variants[variant]}`}>
      {label}
    </span>
  );
}
