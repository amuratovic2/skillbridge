import { Link } from 'react-router-dom';

interface GigCardProps {
  id: number;
  title: string;
  cost: number;
  deliveryTime: number;
  coverImage?: string;
  freelancerName?: string;
  freelancerAvatar?: string;
  rating?: number;
  reviewCount?: number;
  badge?: string;
}

export default function GigCard({
  id,
  title,
  cost,
  deliveryTime,
  coverImage,
  freelancerName,
  freelancerAvatar,
  rating,
  reviewCount,
  badge,
}: GigCardProps) {
  const initials = freelancerName
    ?.split(' ')
    .map((n) => n.charAt(0))
    .join('')
    .toUpperCase() || '?';

  return (
    <Link to={`/gigs/${id}`} className="group">
      <div className="bg-white border border-gray-200 rounded-xl overflow-hidden hover:shadow-lg transition-shadow">
        <div className="aspect-[4/3] bg-primary-50 flex items-center justify-center">
          {coverImage ? (
            <img src={coverImage} alt={title} className="w-full h-full object-cover" />
          ) : (
            <span className="text-6xl font-light text-primary-200">{initials}</span>
          )}
        </div>
        <div className="p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="w-7 h-7 bg-primary-600 text-white rounded-full flex items-center justify-center text-xs font-medium">
              {initials}
            </div>
            <div>
              <span className="text-sm font-medium text-gray-900">{freelancerName}</span>
              {badge && (
                <span className="ml-2 text-xs text-primary-600 font-medium">{badge}</span>
              )}
            </div>
          </div>
          <h3 className="text-sm text-gray-700 line-clamp-2 mb-3 group-hover:text-primary-600 transition-colors">
            {title}
          </h3>
          <div className="flex items-center gap-3 text-xs text-gray-500 mb-3">
            {rating !== undefined && (
              <span className="flex items-center gap-1">
                <svg className="w-3.5 h-3.5 text-yellow-400 fill-current" viewBox="0 0 20 20">
                  <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
                </svg>
                {rating} ({reviewCount})
              </span>
            )}
            <span className="flex items-center gap-1">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {deliveryTime} dana
            </span>
          </div>
          <div className="flex items-center justify-between border-t border-gray-100 pt-3">
            <span className="text-xs text-gray-500 uppercase">Cijena od</span>
            <span className="text-lg font-bold text-gray-900">{cost} &euro;</span>
          </div>
        </div>
      </div>
    </Link>
  );
}
