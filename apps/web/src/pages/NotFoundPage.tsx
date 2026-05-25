import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4 py-16">
      <div className="max-w-lg text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-3">404</h1>
        <h1 className="text-3xl font-bold text-gray-900 mb-3">Stranica nije pronađena</h1>
        <p className="text-gray-500 mb-8">
          Link koji ste otvorili ne postoji ili je stranica premještena.
        </p>
        <Link
          to="/"
          className="inline-flex items-center justify-center bg-primary-600 text-white px-5 py-3 rounded-lg text-sm font-medium hover:bg-primary-700 transition-colors"
        >
          Nazad na početnu
        </Link>
      </div>
    </div>
  );
}
