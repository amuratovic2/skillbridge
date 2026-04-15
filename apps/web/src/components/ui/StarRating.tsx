interface StarRatingProps {
  rating: number;
  count?: number;
  size?: 'sm' | 'md' | 'lg';
}

export default function StarRating({ rating, count, size = 'sm' }: StarRatingProps) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-6 h-6',
  };

  return (
    <div className="flex items-center gap-1">
      <svg className={`${sizeClasses[size]} text-yellow-400 fill-current`} viewBox="0 0 20 20">
        <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
      </svg>
      <span className="font-medium text-gray-900">{rating}</span>
      {count !== undefined && (
        <span className="text-gray-500">({count})</span>
      )}
    </div>
  );
}
