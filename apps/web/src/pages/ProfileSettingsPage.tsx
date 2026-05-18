import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import SkillTag from '../components/ui/SkillTag';
import { userServiceApi } from '../lib/user-service';
import type { PortfolioItem, Skill, UpdateProfilePayload, UserProfile } from '../lib/user-service';

interface PortfolioFormState {
  id?: number;
  title: string;
  description: string;
  imageUrl: string;
}

const emptyPortfolioForm: PortfolioFormState = { title: '', description: '', imageUrl: '' };

const toForm = (profile: UserProfile): UpdateProfilePayload => ({
  firstName: profile.firstName || '',
  lastName: profile.lastName || '',
  bio: profile.bio || '',
  profilePicture: profile.profilePicture || '',
  country: profile.country || '',
});

export default function ProfileSettingsPage() {
  const { user, setAuthenticatedUser } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [allSkills, setAllSkills] = useState<Skill[]>([]);
  const [profileForm, setProfileForm] = useState<UpdateProfilePayload>({});
  const [portfolioForm, setPortfolioForm] = useState<PortfolioFormState>(emptyPortfolioForm);
  const [loading, setLoading] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPortfolio, setSavingPortfolio] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const selectedSkillIds = useMemo(
    () => new Set((profile?.skills || []).map((skill) => skill.id)),
    [profile?.skills],
  );

  const isFreelancer = profile?.role === 'FREELANCER';

  const loadProfile = () => {
    setLoading(true);
    setError('');
    Promise.all([userServiceApi.me(), userServiceApi.allSkills()])
      .then(([me, skills]) => {
        setProfile(me);
        setAuthenticatedUser(me);
        setProfileForm(toForm(me));
        setAllSkills(skills);
      })
      .catch((err) => setError(err.response?.data?.message || 'Profil nije moguce ucitati.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadProfile();
  }, []);

  const updateProfileField = (key: keyof UpdateProfilePayload, value: string) => {
    setProfileForm((current) => ({ ...current, [key]: value }));
  };

  const handleProfileSave = async (event: FormEvent) => {
    event.preventDefault();
    setSavingProfile(true);
    setMessage('');
    setError('');

    try {
      const updated = await userServiceApi.updateMe(profileForm);
      setProfile(updated);
      setAuthenticatedUser(updated);
      setProfileForm(toForm(updated));
      setMessage('Profil je sacuvan.');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Profil nije sacuvan.');
    } finally {
      setSavingProfile(false);
    }
  };

  const toggleSkill = async (skill: Skill) => {
    if (!profile) return;
    setMessage('');
    setError('');

    const hasSkill = selectedSkillIds.has(skill.id);
    try {
      if (hasSkill) {
        await userServiceApi.removeMySkill(skill.id);
        setProfile({
          ...profile,
          skills: (profile.skills || []).filter((item) => item.id !== skill.id),
        });
      } else {
        await userServiceApi.addMySkill(skill.id);
        setProfile({ ...profile, skills: [...(profile.skills || []), skill] });
      }
      setMessage(hasSkill ? 'Vjestina je uklonjena.' : 'Vjestina je dodana.');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Vjestine nije moguce azurirati.');
    }
  };

  const editPortfolioItem = (item: PortfolioItem) => {
    setPortfolioForm({
      id: item.id,
      title: item.title,
      description: item.description || '',
      imageUrl: item.imageUrl || '',
    });
  };

  const resetPortfolioForm = () => setPortfolioForm(emptyPortfolioForm);

  const handlePortfolioSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!profile || !portfolioForm.title.trim()) return;

    setSavingPortfolio(true);
    setMessage('');
    setError('');

    const payload = {
      title: portfolioForm.title.trim(),
      description: portfolioForm.description.trim(),
      imageUrl: portfolioForm.imageUrl.trim(),
    };

    try {
      if (portfolioForm.id) {
        const updated = await userServiceApi.updatePortfolioItem(portfolioForm.id, payload);
        setProfile({
          ...profile,
          portfolioItems: (profile.portfolioItems || []).map((item) =>
            item.id === updated.id ? updated : item,
          ),
        });
        setMessage('Portfolio stavka je azurirana.');
      } else {
        const created = await userServiceApi.createPortfolioItem(payload);
        setProfile({ ...profile, portfolioItems: [created, ...(profile.portfolioItems || [])] });
        setMessage('Portfolio stavka je dodana.');
      }
      resetPortfolioForm();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Portfolio nije sacuvan.');
    } finally {
      setSavingPortfolio(false);
    }
  };

  const deletePortfolioItem = async (itemId: number) => {
    if (!profile) return;
    setMessage('');
    setError('');

    try {
      await userServiceApi.deletePortfolioItem(itemId);
      setProfile({
        ...profile,
        portfolioItems: (profile.portfolioItems || []).filter((item) => item.id !== itemId),
      });
      setMessage('Portfolio stavka je obrisana.');
      if (portfolioForm.id === itemId) resetPortfolioForm();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Portfolio stavku nije moguce obrisati.');
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
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Moj profil</h1>
        </div>
        {user?.id && (
          <Link
            to={`/freelancer/${user.id}`}
            className="inline-flex items-center justify-center px-4 py-2 rounded-lg border border-gray-300 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Javni prikaz
          </Link>
        )}
      </div>

      {(message || error) && (
        <div
          className={`mb-6 px-4 py-3 rounded-lg text-sm ${
            error ? 'bg-red-50 text-red-700' : 'bg-primary-50 text-primary-700'
          }`}
        >
          {error || message}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <form onSubmit={handleProfileSave} className="lg:col-span-2 bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-5">Osnovni podaci</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label className="block">
              <span className="block text-sm font-medium text-gray-700 mb-1">Ime</span>
              <input
                value={profileForm.firstName || ''}
                onChange={(e) => updateProfileField('firstName', e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-gray-700 mb-1">Prezime</span>
              <input
                value={profileForm.lastName || ''}
                onChange={(e) => updateProfileField('lastName', e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-gray-700 mb-1">Drzava</span>
              <input
                value={profileForm.country || ''}
                onChange={(e) => updateProfileField('country', e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-gray-700 mb-1">Slika profila URL</span>
              <input
                value={profileForm.profilePicture || ''}
                onChange={(e) => updateProfileField('profilePicture', e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </label>
          </div>
          <label className="block mt-4">
            <span className="block text-sm font-medium text-gray-700 mb-1">Bio</span>
            <textarea
              value={profileForm.bio || ''}
              onChange={(e) => updateProfileField('bio', e.target.value)}
              rows={6}
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none resize-none"
            />
          </label>
          <button
            type="submit"
            disabled={savingProfile}
            className="mt-5 bg-primary-600 text-white px-5 py-2.5 rounded-lg font-medium hover:bg-primary-700 transition-colors disabled:opacity-50"
          >
            {savingProfile ? 'Spremanje...' : 'Sacuvaj profil'}
          </button>
        </form>

        <aside className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Nalog</h2>
          <div className="space-y-3 text-sm">
            <div>
              <div className="text-gray-500">Username</div>
              <div className="font-medium text-gray-900">{profile?.username}</div>
            </div>
            <div>
              <div className="text-gray-500">Email</div>
              <div className="font-medium text-gray-900 break-all">{profile?.email}</div>
            </div>
            <div>
              <div className="text-gray-500">Uloga</div>
              <div className="font-medium text-gray-900">{profile?.role}</div>
            </div>
          </div>
        </aside>
      </div>

      {isFreelancer && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
          <section className="bg-white border border-gray-200 rounded-lg p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Vjestine</h2>
            <div className="flex flex-wrap gap-2">
              {allSkills.map((skill) => {
                const active = selectedSkillIds.has(skill.id);
                return (
                  <button
                    key={skill.id}
                    type="button"
                    onClick={() => toggleSkill(skill)}
                    className={`px-3 py-1.5 rounded-full border text-sm transition-colors ${
                      active
                        ? 'border-primary-600 bg-primary-50 text-primary-700'
                        : 'border-gray-200 text-gray-600 hover:border-primary-300'
                    }`}
                  >
                    {skill.name}
                  </button>
                );
              })}
            </div>
            {profile?.skills && profile.skills.length > 0 && (
              <div className="mt-5 pt-4 border-t border-gray-100">
                <div className="text-sm text-gray-500 mb-2">Odabrano</div>
                <div className="flex flex-wrap gap-2">
                  {profile.skills.map((skill) => (
                    <SkillTag key={skill.id} name={skill.name} />
                  ))}
                </div>
              </div>
            )}
          </section>

          <section className="lg:col-span-2 bg-white border border-gray-200 rounded-lg p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Portfolio</h2>
            <form onSubmit={handlePortfolioSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
              <input
                value={portfolioForm.title}
                onChange={(e) => setPortfolioForm({ ...portfolioForm, title: e.target.value })}
                placeholder="Naziv rada"
                required
                className="border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
              <input
                value={portfolioForm.imageUrl}
                onChange={(e) => setPortfolioForm({ ...portfolioForm, imageUrl: e.target.value })}
                placeholder="Slika URL"
                className="border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
              <textarea
                value={portfolioForm.description}
                onChange={(e) => setPortfolioForm({ ...portfolioForm, description: e.target.value })}
                placeholder="Opis"
                rows={3}
                className="md:col-span-2 border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none resize-none"
              />
              <div className="md:col-span-2 flex flex-wrap gap-3">
                <button
                  type="submit"
                  disabled={savingPortfolio}
                  className="bg-primary-600 text-white px-5 py-2.5 rounded-lg font-medium hover:bg-primary-700 transition-colors disabled:opacity-50"
                >
                  {savingPortfolio ? 'Spremanje...' : portfolioForm.id ? 'Azuriraj stavku' : 'Dodaj stavku'}
                </button>
                {portfolioForm.id && (
                  <button
                    type="button"
                    onClick={resetPortfolioForm}
                    className="border border-gray-300 text-gray-700 px-5 py-2.5 rounded-lg font-medium hover:bg-gray-50"
                  >
                    Odustani
                  </button>
                )}
              </div>
            </form>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {(profile?.portfolioItems || []).map((item) => (
                <div key={item.id} className="border border-gray-200 rounded-lg overflow-hidden">
                  {item.imageUrl && <img src={item.imageUrl} alt={item.title} className="w-full h-36 object-cover" />}
                  <div className="p-4">
                    <h3 className="font-semibold text-gray-900">{item.title}</h3>
                    {item.description && <p className="text-sm text-gray-600 mt-1 line-clamp-3">{item.description}</p>}
                    <div className="flex gap-2 mt-4">
                      <button
                        type="button"
                        onClick={() => editPortfolioItem(item)}
                        className="text-sm text-primary-700 font-medium hover:text-primary-800"
                      >
                        Uredi
                      </button>
                      <button
                        type="button"
                        onClick={() => deletePortfolioItem(item.id)}
                        className="text-sm text-red-600 font-medium hover:text-red-700"
                      >
                        Obrisi
                      </button>
                    </div>
                  </div>
                </div>
              ))}
              {(profile?.portfolioItems || []).length === 0 && (
                <p className="text-sm text-gray-500">Dodajte prvi portfolio rad koji ce biti vidljiv na javnom profilu.</p>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
