import { describe, expect, it, vi } from 'vitest';
import { act } from 'react';
import { ToastProvider, useToast } from './ToastContext';
import { click, render } from '../test/render';

function ToastProbe() {
  const toast = useToast();

  return (
    <div>
      <button type="button" onClick={() => toast.success('Sacuvano')}>Success</button>
      <button type="button" onClick={() => toast.error('Nije uspjelo')}>Error</button>
      <button type="button" onClick={() => toast.info('Novo obavjestenje')}>Info</button>
      <button type="button" onClick={() => toast.showToast('info', 'Direktno')}>Direct</button>
    </div>
  );
}

describe('ToastProvider', () => {
  it('shows success, error and info toasts and supports manual close', () => {
    vi.useFakeTimers();

    const view = render(
      <ToastProvider>
        <ToastProbe />
      </ToastProvider>,
      { router: false },
    );

    click(view.container.querySelector('button:nth-of-type(1)'));
    click(view.container.querySelector('button:nth-of-type(2)'));
    click(view.container.querySelector('button:nth-of-type(3)'));
    click(view.container.querySelector('button:nth-of-type(4)'));

    expect(view.text()).toContain('Sacuvano');
    expect(view.text()).toContain('Nije uspjelo');
    expect(view.text()).toContain('Novo obavjestenje');
    expect(view.text()).toContain('Direktno');

    click(view.container.querySelector('[aria-label="Zatvori obavijest"]'));
    expect(view.text()).not.toContain('Sacuvano');
  });

  it('auto-removes toast after five seconds', () => {
    vi.useFakeTimers();

    const view = render(
      <ToastProvider>
        <ToastProbe />
      </ToastProvider>,
      { router: false },
    );

    click(view.container.querySelector('button:nth-of-type(1)'));
    expect(view.text()).toContain('Sacuvano');

    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(view.text()).not.toContain('Sacuvano');
  });
});
