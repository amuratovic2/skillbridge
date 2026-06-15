import { describe, expect, it } from 'vitest';
import { getApiErrorMessage } from './api';

function axiosLike(data: unknown) {
  return { isAxiosError: true, response: { data } };
}

describe('getApiErrorMessage', () => {
  it('prefers response message and error strings', () => {
    expect(getApiErrorMessage(axiosLike({ message: 'Poruka sa servera' }), 'Fallback')).toBe('Poruka sa servera');
    expect(getApiErrorMessage(axiosLike({ error: 'Kratka greska' }), 'Fallback')).toBe('Kratka greska');
  });

  it('joins array and object validation errors', () => {
    expect(getApiErrorMessage(axiosLike({ errors: ['Prva', '', 'Druga'] }), 'Fallback')).toBe('Prva Druga');
    expect(getApiErrorMessage(axiosLike({ errors: { email: 'Email nije dobar', password: 'Lozinka je kratka' } }), 'Fallback')).toBe(
      'Email nije dobar Lozinka je kratka',
    );
  });

  it('falls back for empty or non-axios errors', () => {
    expect(getApiErrorMessage(axiosLike({ message: '   ', errors: [] }), 'Fallback')).toBe('Fallback');
    expect(getApiErrorMessage(new Error('boom'), 'Fallback')).toBe('Fallback');
  });
});
