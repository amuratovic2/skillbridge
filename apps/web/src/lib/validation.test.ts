import { describe, expect, it } from 'vitest';
import {
  isValidEmail,
  isValidHttpUrl,
  parseNonNegativeInteger,
  parsePositiveInteger,
  parsePositiveNumber,
  validateEmail,
  validateGigForm,
} from './validation';

const validGigForm = {
  title: 'Profesionalni logo dizajn',
  description: 'Kreiram moderan logo paket spreman za web i print upotrebu.',
  categoryId: '3',
  cost: '120.50',
  deliveryTime: '5',
  revisionCount: '2',
  tags: 'logo, branding, design',
};

describe('validation helpers', () => {
  it('validates email and optional http urls', () => {
    expect(isValidEmail(' user@example.com ')).toBe(true);
    expect(validateEmail('wrong-address')).toBe(false);
    expect(isValidHttpUrl('')).toBe(true);
    expect(isValidHttpUrl(' https://skillbridge.example/gig ')).toBe(true);
    expect(isValidHttpUrl('ftp://skillbridge.example/file')).toBe(false);
    expect(isValidHttpUrl('not a url')).toBe(false);
  });

  it('parses numeric fields with strict positive and non-negative rules', () => {
    expect(parsePositiveInteger('7', 'Rok')).toBe(7);
    expect(parseNonNegativeInteger('0', 'Revizije')).toBe(0);
    expect(parsePositiveNumber('9.75', 'Cijena')).toBe(9.75);

    expect(() => parsePositiveInteger('0', 'Rok')).toThrow('Rok mora biti cijeli broj veci od 0.');
    expect(() => parseNonNegativeInteger('-1', 'Revizije')).toThrow('Revizije mora biti 0 ili veci broj.');
    expect(() => parsePositiveNumber('abc', 'Cijena')).toThrow('Cijena mora biti broj veci od 0.');
  });

  it('normalizes valid gig form data into an API payload', () => {
    expect(validateGigForm(validGigForm)).toEqual({
      title: 'Profesionalni logo dizajn',
      description: 'Kreiram moderan logo paket spreman za web i print upotrebu.',
      categoryId: 3,
      cost: 120.5,
      deliveryTime: 5,
      revisionCount: 2,
      tags: ['logo', 'branding', 'design'],
    });
  });

  it('rejects invalid gig form boundaries', () => {
    expect(() => validateGigForm({ ...validGigForm, title: 'Logo' })).toThrow(
      'Naslov mora imati izmedju 5 i 120 karaktera.',
    );
    expect(() => validateGigForm({ ...validGigForm, description: 'Prekratko' })).toThrow(
      'Opis mora imati izmedju 20 i 2000 karaktera.',
    );
    expect(() => validateGigForm({ ...validGigForm, tags: '1,2,3,4,5,6,7,8,9,10,11' })).toThrow(
      'Mozete dodati najvise 10 tagova.',
    );
    expect(() => validateGigForm({ ...validGigForm, tags: 'x'.repeat(41) })).toThrow(
      'Maksimalno je 40 karaktera.',
    );
  });
});
