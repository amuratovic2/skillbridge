import { afterEach, vi } from 'vitest';
import { cleanupRendered } from './render';

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

Object.defineProperty(window, 'scrollTo', {
  value: vi.fn(),
  writable: true,
});

Element.prototype.scrollIntoView = vi.fn();

afterEach(() => {
  cleanupRendered();
  vi.clearAllMocks();
  vi.useRealTimers();
  localStorage.clear();
  sessionStorage.clear();
  document.body.innerHTML = '';
});
