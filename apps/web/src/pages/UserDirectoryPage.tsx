import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import SkillTag from '../components/ui/SkillTag';
import { getApiErrorMessage } from '../lib/api';
import { userDisplayName, userInitials, userServiceApi } from '../lib/user-service';
import type { PageMeta, Skill, UserProfile } from '../lib/user-service';

const PAGE_SIZE = 9;

const emptyMeta: PageMeta = { total: 0, page: 1, limit: PAGE_SIZE, totalPages: 1 };

type SortKey = 'createdAt:desc' | 'createdAt:asc' | 'username:asc';
const SORT_OPTIONS = new Set<SortKey>(['createdAt:desc', 'createdAt:asc', 'username:asc']);

export default function UserDirectoryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [meta, setMeta] = useState<PageMeta>(emptyMeta);
  const [loading, setLoading] = useState(true);
  const [skillsLoading, setSkillsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [queryInput, setQueryInput] = useState(searchParams.get('query') || '');
  const [countryInput, setCountryInput] = useState(searchParams.get('country') || '');

  const query = searchParams.get('query') || '';
  const country = searchParams.get('country') || '';
  const skill = searchParams.get('skill') || '';
  const rawSort = searchParams.get('sort') as SortKey | null;
  const sort = rawSort && SORT_OPTIONS.has(rawSort) ? rawSort : 'createdAt:desc';
  const page = Math.max(Number(searchParams.get('page') || '1'), 1);
  const [sortBy, sortDirection] = sort.split(':') as [string, 'asc' | 'desc'];

  useEffect(() => {
    setSkillsLoading(true);
    userServiceApi
      .allSkills()
      .then(setSkills)
      .catch(() => setSkills([]))
      .finally(() => setSkillsLoading(false));
  }, []);

  useEffect(() => {
    setQueryInput(query);
  }, [query]);

  useEffect(() => {
    setCountryInput(country);
  }, [country]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      updateParam('query', queryInput.trim());
    }, 350);

    return () => window.clearTimeout(timeout);
  }, [queryInput]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      updateParam('country', countryInput.trim());
    }, 350);

    return () => window.clearTimeout(timeout);
  }, [countryInput]);

  useEffect(() => {
    setLoading(true);
    setError(null);
    userServiceApi
      .listUsers({
        page,
        limit: PAGE_SIZE,
        query,
        role: 'FREELANCER',
        country,
        skill,
        sortBy,
        sortDirection,
      })
      .then((res) => {
        setUsers(res.data);
        setMeta(res.meta);
      })
      .catch((err: unknown) => {
        setUsers([]);
        setMeta(emptyMeta);
        setError(getApiErrorMessage(err, 'Freelanceri trenutno nisu dostupni.'));
      })
      .finally(() => setLoading(false));
  }, [query, country, skill, page, sortBy, sortDirection]);

  const activeFilters = [query, country, skill].filter(Boolean).length;

  const pageNumbers = useMemo(() => {
    const total = Math.max(meta.totalPages, 1);
    const start = Math.max(1, Math.min(page - 2, total - 4));
    const end = Math.min(total, start + 4);
    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }, [meta.totalPages, page]);

  const updateParam = (key: string, value: string) => {
    const currentValue = searchParams.get(key) || '';
    if (currentValue === value) return;

    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    next.set('page', '1');
    setSearchParams(next);
  };

  const updatePage = (value: number) => {
    const next = new URLSearchParams(searchParams);
    next.set('page', String(value));
    setSearchParams(next);
  };

  const clearFilters = () => {
    setQueryInput('');
    setCountryInput('');
    setSearchParams(new URLSearchParams({ page: '1', sort }));
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Freelanceri</h1>
          <p className="text-sm text-gray-500 mt-1">
            {loading ? 'Ucitavanje profila...' : `${meta.total} profila`}
          </p>
        </div>
        {activeFilters > 0 && (
          <button
            type="button"
            onClick={clearFilters}
            className="self-start md:self-auto px-4 py-2 rounded-lg border border-gray-300 text-sm text-gray-700 hover:bg-gray-50"
          >
            Ocisti filtere
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-8 pb-6 border-b border-gray-200">
        <input
          type="search"
          value={queryInput}
          onChange={(event) => setQueryInput(event.target.value)}
          placeholder="Ime, username ili opis"
          className="border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
        />
        <input
          type="search"
          value={countryInput}
          onChange={(event) => setCountryInput(event.target.value)}
          placeholder="Drzava"
          className="border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
        />
        <select
          value={skill}
          onChange={(event) => updateParam('skill', event.target.value)}
          disabled={skillsLoading}
          className="border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none bg-white disabled:opacity-60"
        >
          <option value="">Sve vjestine</option>
          {skills.map((item) => (
            <option key={item.id} value={item.name}>
              {item.name}
            </option>
          ))}
        </select>
        <select
          value={sort}
          onChange={(event) => updateParam('sort', event.target.value)}
          className="border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none bg-white"
        >
          <option value="createdAt:desc">Najnoviji</option>
          <option value="createdAt:asc">Najstariji</option>
          <option value="username:asc">Username A-Z</option>
        </select>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 text-red-700 px-4 py-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {Array.from({ length: PAGE_SIZE }, (_, index) => (
            <ProfileSkeleton key={index} />
          ))}
        </div>
      ) : users.length > 0 ? (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {users.map((user) => (
              <Link
                key={user.id}
                to={`/freelancer/${user.id}`}
                className="bg-white border border-gray-200 rounded-lg p-5 hover:border-primary-300 hover:shadow-sm transition-all"
              >
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-12 h-12 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center font-semibold overflow-hidden">
                    {user.profilePicture ? (
                      <img src={user.profilePicture} alt={userDisplayName(user)} className="w-full h-full object-cover" />
                    ) : (
                      userInitials(user)
                    )}
                  </div>
                  <div className="min-w-0">
                    <h2 className="font-semibold text-gray-900 truncate">{userDisplayName(user)}</h2>
                    <p className="text-sm text-gray-500 truncate">{user.country || user.email}</p>
                  </div>
                </div>
                <p className="text-sm text-gray-600 line-clamp-3 min-h-[60px]">
                  {user.bio || 'Freelancer jos nije dodao opis profila.'}
                </p>
                {user.skills && user.skills.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-4">
                    {user.skills.slice(0, 4).map((item) => (
                      <SkillTag key={item.id} name={item.name} />
                    ))}
                    {user.skills.length > 4 && (
                      <span className="text-xs text-gray-400 self-center">+{user.skills.length - 4}</span>
                    )}
                  </div>
                )}
              </Link>
            ))}
          </div>

          {meta.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-10">
              <button
                type="button"
                onClick={() => updatePage(page - 1)}
                disabled={page <= 1}
                className="px-3 py-2 rounded-lg text-sm border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                Prethodna
              </button>
              {pageNumbers.map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => updatePage(value)}
                  className={`px-4 py-2 rounded-lg text-sm ${
                    value === page ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {value}
                </button>
              ))}
              <button
                type="button"
                onClick={() => updatePage(page + 1)}
                disabled={page >= meta.totalPages}
                className="px-3 py-2 rounded-lg text-sm border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                Sljedeca
              </button>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-20">
          <p className="text-gray-500 text-lg">Nema pronadjenih freelancera</p>
          <p className="text-gray-400 mt-2">Promijenite pretragu ili uklonite filtere.</p>
        </div>
      )}
    </div>
  );
}

function ProfileSkeleton() {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 animate-pulse">
      <div className="flex items-center gap-3 mb-4">
        <div className="w-12 h-12 rounded-full bg-gray-100" />
        <div className="min-w-0 flex-1 space-y-2">
          <div className="h-4 bg-gray-100 rounded w-2/3" />
          <div className="h-3 bg-gray-100 rounded w-1/2" />
        </div>
      </div>
      <div className="space-y-2">
        <div className="h-3 bg-gray-100 rounded" />
        <div className="h-3 bg-gray-100 rounded" />
        <div className="h-3 bg-gray-100 rounded w-2/3" />
      </div>
    </div>
  );
}
