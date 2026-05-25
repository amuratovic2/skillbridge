export type GigFormValues = {
  title: string;
  description: string;
  categoryId: string;
  cost: string;
  deliveryTime: string;
  revisionCount: string;
  tags: string;
};

export type GigPayload = {
  title: string;
  description: string;
  categoryId: number;
  cost: number;
  deliveryTime: number;
  revisionCount: number;
  tags: string[];
};

export function isValidHttpUrl(value: string) {
  if (!value.trim()) return true;

  try {
    const url = new URL(value.trim());
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export function validateEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

export function parsePositiveInteger(value: string, fieldLabel: string) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${fieldLabel} mora biti cijeli broj veci od 0.`);
  }
  return parsed;
}

export function parseNonNegativeInteger(value: string, fieldLabel: string) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed < 0) {
    throw new Error(`${fieldLabel} mora biti 0 ili veci broj.`);
  }
  return parsed;
}

export function parsePositiveNumber(value: string, fieldLabel: string) {
  const parsed = Number.parseFloat(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${fieldLabel} mora biti broj veci od 0.`);
  }
  return parsed;
}

export function validateGigForm(values: GigFormValues): GigPayload {
  const title = values.title.trim();
  const description = values.description.trim();

  if (title.length < 5 || title.length > 120) {
    throw new Error('Naslov mora imati izmedju 5 i 120 karaktera.');
  }

  if (description.length < 20 || description.length > 2000) {
    throw new Error('Opis mora imati izmedju 20 i 2000 karaktera.');
  }

  const tags = values.tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean);

  if (tags.length > 10) {
    throw new Error('Mozete dodati najvise 10 tagova.');
  }

  const invalidTag = tags.find((tag) => tag.length > 40);
  if (invalidTag) {
    throw new Error(`Tag "${invalidTag}" je predug. Maksimalno je 40 karaktera.`);
  }

  return {
    title,
    description,
    categoryId: parsePositiveInteger(values.categoryId, 'Kategorija'),
    cost: parsePositiveNumber(values.cost, 'Cijena'),
    deliveryTime: parsePositiveInteger(values.deliveryTime, 'Rok isporuke'),
    revisionCount: parseNonNegativeInteger(values.revisionCount, 'Broj revizija'),
    tags,
  };
}
