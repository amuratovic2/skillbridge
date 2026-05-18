import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import SkillTag from '../components/ui/SkillTag';
import { userServiceApi } from '../lib/user-service';
import type { Skill, UserProfile } from '../lib/user-service';

function displayName(user: UserProfile) {
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username;
}

function initials(user: UserProfile) {
  const value =
    [user.firstName, user.lastName]
      .filter(Boolean)
      .map((part) => part?.charAt(0))
      .join('') || user.username.charAt(0);
  return value.toUpperCase();
}

export default function UserDirectoryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [meta, setMeta] = useState({ total: 0, page: 1, limit: 9, totalPages: 1 });
  const [loading, setLoading] = useState(true);

  const query = searchParams.get('query') || '';
  const country = searchParams.get('country') || '';
  const skill = searchParams.get('skill') || '';
  const page = Number(searchParams.get('page') || '1');

  useEffect(() => {
    userServiceApi.allSkills().then(setSkills).catch(() => setSkills([]));
  }, []);

  useEffect(() => {
    setLoading(true);
    userServiceApi
      .listUsers({ page, limit: 9, query, role: 'FREELANCER', country, skill })
      .then((res) => {
        setUsers(res.data);
        setMeta(res.meta);
      })
      .catch(() => {
        setUsers([]);
        setMeta({ total: 0, page: 1, limit: 9, totalPages: 1 });
      })
      .finally(() => setLoading(false));
  }, [query, country, skill, page]);

  const updateParam = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    next.set('page', '1');
    setSearchParams(next);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Freelanceri</h1>
          <p className="text-gray-500 mt-1">Pretraga javnih profila iz user-service baze.</p>
        </div>
        <span className="text-sm text-gray-500">{meta.total} profila</span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-8 pb-6 border-b border-gray-200">
        <input
          type="text"
          value={query}
          onChange={(e) => updateParam('query', e.target.value)}
          placeholder="Ime, username ili opis"
          className="border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
        />
        <input
          type="text"
          value={country}
          onChange={(e) => updateParam('country', e.target.value)}
          placeholder="Drzava"
          className="border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
        />
        <select
          value={skill}
          onChange={(e) => updateParam('skill', e.target.value)}
          className="border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
        >
          <option value="">Sve vjestine</option>
          {skills.map((item) => (
            <option key={item.id} value={item.name}>
              {item.name}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
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
                      <img src={user.profilePicture} alt={displayName(user)} className="w-full h-full object-cover" />
                    ) : (
                      initials(user)
                    )}
                  </div>
                  <div className="min-w-0">
                    <h2 className="font-semibold text-gray-900 truncate">{displayName(user)}</h2>
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
                  </div>
                )}
              </Link>
            ))}
          </div>

          {meta.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-10">
              {Array.from({ length: meta.totalPages }, (_, index) => index + 1).map((value) => (
                <button
                  key={value}
                  onClick={() => {
                    const next = new URLSearchParams(searchParams);
                    next.set('page', String(value));
                    setSearchParams(next);
                  }}
                  className={`px-4 py-2 rounded-lg text-sm ${
                    value === page ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {value}
                </button>
              ))}
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
