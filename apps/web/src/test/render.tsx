import { type ReactElement } from 'react';
import { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';

const cleanupCallbacks: Array<() => void> = [];

export function cleanupRendered() {
  while (cleanupCallbacks.length > 0) {
    const cleanup = cleanupCallbacks.pop();
    cleanup?.();
  }
}

export function render(
  ui: ReactElement,
  options: { route?: string; router?: boolean } = {},
) {
  const container = document.createElement('div');
  document.body.appendChild(container);

  const root: Root = createRoot(container);
  const element = options.router === false ? ui : (
    <MemoryRouter initialEntries={[options.route ?? '/']}>{ui}</MemoryRouter>
  );

  act(() => {
    root.render(element);
  });

  let cleaned = false;
  const cleanup = () => {
    if (cleaned) return;
    cleaned = true;
    act(() => {
      root.unmount();
    });
    container.remove();
  };
  cleanupCallbacks.push(cleanup);

  return {
    container,
    root,
    text: () => container.textContent ?? '',
    unmount: cleanup,
  };
}

export async function flushPromises() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });
}

export function click(element: Element | null) {
  if (!element) {
    throw new Error('Element not found for click');
  }

  act(() => {
    element.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
  });
}

export function input(element: HTMLInputElement | HTMLTextAreaElement | null, value: string) {
  if (!element) {
    throw new Error('Element not found for input');
  }

  act(() => {
    setNativeValue(element, value);
    element.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
  });
}

export function change(element: HTMLInputElement | HTMLSelectElement | null, value: string) {
  if (!element) {
    throw new Error('Element not found for change');
  }

  act(() => {
    setNativeValue(element, value);
    element.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));
  });
}

function setNativeValue(
  element: HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement,
  value: string,
) {
  const descriptor = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(element), 'value');
  descriptor?.set?.call(element, value);
}

export async function submit(element: HTMLFormElement | null) {
  if (!element) {
    throw new Error('Element not found for submit');
  }

  await act(async () => {
    element.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await Promise.resolve();
  });
}
