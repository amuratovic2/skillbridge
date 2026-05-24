import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import SkillTag from '../components/ui/SkillTag';
import Modal from '../components/ui/Modal';
import { getApiErrorMessage } from '../lib/api';
import { userServiceApi } from '../lib/user-service';
import type { PortfolioItem, Skill, UpdateProfilePayload, UserProfile } from '../lib/user-service';

interface PortfolioFormState {
  id?: number;
  title: string;
  description: string;
  imageUrl: string;
}

type Feedback = { type: 'success' | 'error'; text: string } | null;

const emptyPortfolioForm: PortfolioFormState = { title: '', description: '', imageUrl: '' };

const toForm = (profile: UserProfile): UpdateProfilePayload => ({
  firstName: profile.firstName || '',
  lastName: profile.lastName || '',
  bio: profile.bio || '',
  profilePicture: profile.profilePicture || '',
  country: profile.country || '',
});

function isValidOptionalUrl(value?: string) {
  const trimmed = value?.trim();
  if (!trimmed) return true;

  try {
    const url = new URL(trimmed);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

function isWithinLimit(value: string | undefined, limit: number) {
  return (value || '').trim().length <= limit;
}

export default function ProfileSettingsPage() {
  const navigate = useNavigate();
  const { user, setAuthenticatedUser, logout } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [allSkills, setAllSkills] = useState<Skill[]>([]);
  const [profileForm, setProfileForm] = useState<UpdateProfilePayload>({});
  const [portfolioForm, setPortfolioForm] = useState<PortfolioFormState>(emptyPortfolioForm);
  const [newSkillName, setNewSkillName] = useState('');
  const [loading, setLoading] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPortfolio, setSavingPortfolio] = useState(false);
  const [creatingSkill, setCreatingSkill] = useState(false);
  const [deactivateOpen, setDeactivateOpen] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [feedback, setFeedback] = useState<Feedback>(null);

  const selectedSkillIds = useMemo(
    () => new Set((profile?.skills || []).map((skill) => skill.id)),
    [profile?.skills],
  );

  const isFreelancer = profile?.role === 'FREELANCER';

  const loadProfile = () => {
    setLoading(true);
    setFeedback(null);
    Promise.all([userServiceApi.me(), userServiceApi.allSkills()])
      .then(([me, skills]) => {
        setProfile(me);
        setAuthenticatedUser(me);
        setProfileForm(toForm(me));
        setAllSkills(skills);
      })
      .catch((err: unknown) =>
        setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Profil nije moguce ucitati.') }),
      )
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
    setFeedback(null);

    if (!isValidOptionalUrl(profileForm.profilePicture)) {
      setFeedback({ type: 'error', text: 'URL slike profila mora biti validan http ili https link.' });
      setSavingProfile(false);
      return;
    }

    if (
      !isWithinLimit(profileForm.firstName, 255) ||
      !isWithinLimit(profileForm.lastName, 255) ||
      !isWithinLimit(profileForm.bio, 2000) ||
      !isWithinLimit(profileForm.profilePicture, 500) ||
      !isWithinLimit(profileForm.country, 100)
    ) {
      setFeedback({ type: 'error', text: 'Provjerite duzinu unesenih polja.' });
      setSavingProfile(false);
      return;
    }

    try {
      const updated = await userServiceApi.updateMe({
        firstName: profileForm.firstName?.trim(),
        lastName: profileForm.lastName?.trim(),
        bio: profileForm.bio?.trim(),
        profilePicture: profileForm.profilePicture?.trim(),
        country: profileForm.country?.trim(),
      });
      setProfile(updated);
      setAuthenticatedUser(updated);
      setProfileForm(toForm(updated));
      setFeedback({ type: 'success', text: 'Profil je sacuvan.' });
    } catch (err: unknown) {
      setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Profil nije sacuvan.') });
    } finally {
      setSavingProfile(false);
    }
  };

  const toggleSkill = async (skill: Skill) => {
    if (!profile) return;
    setFeedback(null);

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
      setFeedback({ type: 'success', text: hasSkill ? 'Vjestina je uklonjena.' : 'Vjestina je dodana.' });
    } catch (err: unknown) {
      setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Vjestine nije moguce azurirati.') });
    }
  };

  const createSkill = async (event: FormEvent) => {
    event.preventDefault();
    const name = newSkillName.trim();
    setFeedback(null);

    if (!name || name.length > 100) {
      setFeedback({ type: 'error', text: 'Naziv vjestine mora imati 1-100 karaktera.' });
      return;
    }

    const existing = allSkills.find((skill) => skill.name.toLowerCase() === name.toLowerCase());
    if (existing) {
      if (selectedSkillIds.has(existing.id)) {
        setFeedback({ type: 'success', text: 'Vjestina je vec dodana profilu.' });
      } else if (profile) {
        setCreatingSkill(true);
        try {
          await userServiceApi.addMySkill(existing.id);
          setProfile({ ...profile, skills: [...(profile.skills || []), existing] });
          setFeedback({ type: 'success', text: 'Vjestina je dodana profilu.' });
        } catch (err: unknown) {
          setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Vjestinu nije moguce dodati.') });
        } finally {
          setCreatingSkill(false);
        }
      }
      setNewSkillName('');
      return;
    }

    setCreatingSkill(true);
    try {
      const created = await userServiceApi.createSkill(name);
      setAllSkills((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)));
      if (profile) {
        await userServiceApi.addMySkill(created.id);
        setProfile({ ...profile, skills: [...(profile.skills || []), created] });
      }
      setNewSkillName('');
      setFeedback({ type: 'success', text: 'Vjestina je kreirana i dodana profilu.' });
    } catch (err: unknown) {
      setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Vjestinu nije moguce kreirati.') });
    } finally {
      setCreatingSkill(false);
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
    setFeedback(null);

    const payload = {
      title: portfolioForm.title.trim(),
      description: portfolioForm.description.trim(),
      imageUrl: portfolioForm.imageUrl.trim(),
    };

    if (!payload.title || payload.title.length > 255 || payload.description.length > 2000 || payload.imageUrl.length > 500) {
      setFeedback({ type: 'error', text: 'Provjerite duzinu portfolio polja.' });
      setSavingPortfolio(false);
      return;
    }

    if (!isValidOptionalUrl(payload.imageUrl)) {
      setFeedback({ type: 'error', text: 'URL slike portfolio stavke mora biti validan http ili https link.' });
      setSavingPortfolio(false);
      return;
    }

    try {
      if (portfolioForm.id) {
        const updated = await userServiceApi.updatePortfolioItem(portfolioForm.id, payload);
        setProfile({
          ...profile,
          portfolioItems: (profile.portfolioItems || []).map((item) =>
            item.id === updated.id ? updated : item,
          ),
        });
        setFeedback({ type: 'success', text: 'Portfolio stavka je azurirana.' });
      } else {
        const created = await userServiceApi.createPortfolioItem(payload);
        setProfile({ ...profile, portfolioItems: [created, ...(profile.portfolioItems || [])] });
        setFeedback({ type: 'success', text: 'Portfolio stavka je dodana.' });
      }
      resetPortfolioForm();
    } catch (err: unknown) {
      setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Portfolio nije sacuvan.') });
    } finally {
      setSavingPortfolio(false);
    }
  };

  const deactivateAccount = async () => {
    setDeactivating(true);
    setFeedback(null);
    try {
      await userServiceApi.deactivateMe();
      logout();
      navigate('/', { replace: true });
    } catch (err: unknown) {
      setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Nalog nije moguce deaktivirati.') });
      setDeactivateOpen(false);
    } finally {
      setDeactivating(false);
    }
  };

  const deletePortfolioItem = async (itemId: number) => {
    if (!profile) return;
    setFeedback(null);

    try {
      await userServiceApi.deletePortfolioItem(itemId);
      setProfile({
        ...profile,
        portfolioItems: (profile.portfolioItems || []).filter((item) => item.id !== itemId),
      });
      setFeedback({ type: 'success', text: 'Portfolio stavka je obrisana.' });
      if (portfolioForm.id === itemId) resetPortfolioForm();
    } catch (err: unknown) {
      setFeedback({ type: 'error', text: getApiErrorMessage(err, 'Portfolio stavku nije moguce obrisati.') });
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

      {feedback && (
        <div
          className={`mb-6 px-4 py-3 rounded-lg text-sm ${
            feedback.type === 'error' ? 'bg-red-50 text-red-700' : 'bg-primary-50 text-primary-700'
          }`}
        >
          {feedback.text}
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
                maxLength={255}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-gray-700 mb-1">Prezime</span>
              <input
                value={profileForm.lastName || ''}
                onChange={(e) => updateProfileField('lastName', e.target.value)}
                maxLength={255}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-gray-700 mb-1">Drzava</span>
              <input
                value={profileForm.country || ''}
                onChange={(e) => updateProfileField('country', e.target.value)}
                maxLength={100}
                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-gray-700 mb-1">Slika profila URL</span>
              <input
                value={profileForm.profilePicture || ''}
                onChange={(e) => updateProfileField('profilePicture', e.target.value)}
                type="url"
                maxLength={500}
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
              maxLength={2000}
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
            <div>
              <div className="text-gray-500">Status</div>
              <div className="font-medium text-gray-900">{profile?.isActive === false ? 'Neaktivan' : 'Aktivan'}</div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setDeactivateOpen(true)}
            className="mt-6 w-full border border-red-300 text-red-600 px-4 py-2 rounded-lg text-sm font-medium hover:bg-red-50"
          >
            Deaktiviraj nalog
          </button>
        </aside>
      </div>

      {isFreelancer && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
          <section className="bg-white border border-gray-200 rounded-lg p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Vjestine</h2>
            <form onSubmit={createSkill} className="flex gap-2 mb-4">
              <input
                value={newSkillName}
                onChange={(event) => setNewSkillName(event.target.value)}
                maxLength={100}
                placeholder="Nova vjestina"
                className="min-w-0 flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
              <button
                type="submit"
                disabled={creatingSkill}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-50"
              >
                Dodaj
              </button>
            </form>
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
                maxLength={255}
                className="border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
              <input
                value={portfolioForm.imageUrl}
                onChange={(e) => setPortfolioForm({ ...portfolioForm, imageUrl: e.target.value })}
                placeholder="Slika URL"
                type="url"
                maxLength={500}
                className="border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
              <textarea
                value={portfolioForm.description}
                onChange={(e) => setPortfolioForm({ ...portfolioForm, description: e.target.value })}
                placeholder="Opis"
                rows={3}
                maxLength={2000}
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

      {deactivateOpen && (
        <Modal title="Deaktiviraj nalog" onClose={() => setDeactivateOpen(false)}>
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Profil ce biti oznacen kao neaktivan i necete moci nastaviti rad pod ovim nalogom.
            </p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setDeactivateOpen(false)}
                className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Odustani
              </button>
              <button
                type="button"
                onClick={deactivateAccount}
                disabled={deactivating}
                className="bg-red-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-red-700 disabled:opacity-60"
              >
                {deactivating ? 'Deaktivacija...' : 'Deaktiviraj'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
