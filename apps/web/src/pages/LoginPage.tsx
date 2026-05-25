import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import FeedbackBanner from '../components/ui/FeedbackBanner';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../lib/api';
import { isValidEmail } from '../lib/validation';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: { pathname?: string; search?: string } } | null)?.from;
  const redirectPath = from?.pathname ? `${from.pathname}${from.search ?? ''}` : '/dashboard';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    const trimmedEmail = email.trim();

    if (!isValidEmail(trimmedEmail)) {
      setError('Unesite validan email, npr. korisnik@gmail.com');
      return;
    }

    if (!password.trim()) {
      setError('Unesite lozinku.');
      return;
    }

    setLoading(true);

    try {
      await login(trimmedEmail, password);
      navigate(redirectPath, { replace: true });
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Pogresan email ili lozinka.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center py-12 px-4">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Prijava</h1>
          <p className="text-gray-500 mt-2">Dobrodosli nazad na SkillBridge</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5" noValidate>
          {error && <FeedbackBanner type="error">{error}</FeedbackBanner>}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              placeholder="vas@email.com"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Lozinka</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              placeholder="Unesite lozinku"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary-600 text-white py-3 rounded-lg font-medium hover:bg-primary-700 transition-colors disabled:opacity-50"
          >
            {loading ? 'Prijavljivanje...' : 'Prijavi se'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          Nemate nalog?{' '}
          <Link to="/register" className="text-primary-600 font-medium hover:text-primary-700">
            Registrujte se
          </Link>
        </p>
      </div>
    </div>
  );
}
