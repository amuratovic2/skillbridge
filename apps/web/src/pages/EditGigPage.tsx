import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../lib/api';

export default function EditGigPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [categories, setCategories] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    categoryId: '',
    cost: '',
    deliveryTime: '',
    revisionCount: '',
    tags: '',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      api.get(`/gigs/${id}`),
      api.get('/categories'),
    ])
      .then(([gigRes, catRes]) => {
        const gig = gigRes.data.data;
        setFormData({
          title: gig.title || '',
          description: gig.description || '',
          categoryId: String(gig.categoryId || gig.category?.id || ''),
          cost: String(gig.cost || ''),
          deliveryTime: String(gig.deliveryTime || ''),
          revisionCount: String(gig.revisionCount || ''),
          tags: (gig.tags || []).map((t: any) => t.name).join(', '),
        });
        setCategories(catRes.data.data || []);
      })
      .catch(() => navigate('/dashboard'))
      .finally(() => setLoading(false));
  }, [id, navigate]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSaving(true);

    try {
      const tags = formData.tags.split(',').map((t) => t.trim()).filter(Boolean);
      await api.patch(`/gigs/${id}`, {
        title: formData.title,
        description: formData.description,
        categoryId: parseInt(formData.categoryId, 10),
        cost: parseFloat(formData.cost),
        deliveryTime: parseInt(formData.deliveryTime, 10),
        revisionCount: parseInt(formData.revisionCount, 10),
        tags,
      });
      navigate(`/gigs/${id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri ažuriranju usluge');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">Uredi uslugu</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        {error && <div className="bg-red-50 text-red-600 px-4 py-3 rounded-lg text-sm">{error}</div>}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Naslov usluge</label>
          <input type="text" name="title" value={formData.title} onChange={handleChange} required
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Opis</label>
          <textarea name="description" value={formData.description} onChange={handleChange} rows={5}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none resize-none" />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Kategorija</label>
          <select name="categoryId" value={formData.categoryId} onChange={handleChange} required
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none">
            <option value="">Izaberite kategoriju</option>
            {categories.map((cat: any) => (
              <option key={cat.id} value={cat.id}>{cat.title}</option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Cijena (&euro;)</label>
            <input type="number" name="cost" value={formData.cost} onChange={handleChange} required min="1"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Rok (dani)</label>
            <input type="number" name="deliveryTime" value={formData.deliveryTime} onChange={handleChange} required min="1"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Revizije</label>
            <input type="number" name="revisionCount" value={formData.revisionCount} onChange={handleChange} required min="0"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none" />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Tagovi <span className="text-gray-400">(odvojeni zarezom)</span>
          </label>
          <input type="text" name="tags" value={formData.tags} onChange={handleChange}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none" />
        </div>

        <div className="flex gap-3">
          <button type="submit" disabled={saving}
            className="flex-1 bg-primary-600 text-white py-3 rounded-lg font-medium hover:bg-primary-700 transition-colors disabled:opacity-50">
            {saving ? 'Spremanje...' : 'Spremi promjene'}
          </button>
          <button type="button" onClick={() => navigate(`/gigs/${id}`)}
            className="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition-colors">
            Otkaži
          </button>
        </div>
      </form>
    </div>
  );
}
