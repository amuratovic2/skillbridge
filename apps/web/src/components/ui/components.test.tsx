import { describe, expect, it, vi } from 'vitest';
import Badge from './Badge';
import ConfirmModal from './ConfirmModal';
import FeedbackBanner from './FeedbackBanner';
import GigCard from './GigCard';
import Modal from './Modal';
import SkillTag from './SkillTag';
import StarRating from './StarRating';
import { click, render } from '../../test/render';

describe('shared UI components', () => {
  it('renders badge variants, feedback banners and star ratings', () => {
    const view = render(
      <div>
        <Badge label="Primary" />
        <Badge label="Secondary" variant="secondary" />
        <Badge label="Warning" variant="warning" />
        <FeedbackBanner type="success">Saved</FeedbackBanner>
        <FeedbackBanner type="error">Broken</FeedbackBanner>
        <FeedbackBanner type="info">Heads up</FeedbackBanner>
        <StarRating rating={4.8} count={12} size="lg" />
      </div>,
      { router: false },
    );

    expect(view.text()).toContain('Primary');
    expect(view.text()).toContain('Secondary');
    expect(view.text()).toContain('Warning');
    expect(view.text()).toContain('Saved');
    expect(view.text()).toContain('Broken');
    expect(view.text()).toContain('Heads up');
    expect(view.text()).toContain('4.8');
    expect(view.text()).toContain('(12)');
  });

  it('renders skill tags as static labels or clickable controls', () => {
    const onClick = vi.fn();
    const view = render(
      <div>
        <SkillTag name="React" />
        <SkillTag name="TypeScript" onClick={onClick} />
      </div>,
      { router: false },
    );

    click(view.container.querySelectorAll('span')[1]);

    expect(view.text()).toContain('React');
    expect(view.text()).toContain('TypeScript');
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('renders gig cards with fallback initials, images, badges and rating', () => {
    const view = render(
      <div>
        <GigCard id={1} title="Logo paket" cost={80} deliveryTime={3} freelancerName="Mila Kovac" badge="Top" rating={4.9} reviewCount={20} />
        <GigCard id={2} title="SEO audit" cost={120} deliveryTime={5} freelancerName="SEO Pro" coverImage="https://example.com/cover.jpg" />
      </div>,
    );

    expect(view.text()).toContain('Logo paket');
    expect(view.text()).toContain('MK');
    expect(view.text()).toContain('Top');
    expect(view.text()).toContain('4.9 (20)');
    expect(view.container.querySelector('img')?.getAttribute('alt')).toBe('SEO audit');
  });

  it('closes modals by close button, backdrop and escape', () => {
    const onClose = vi.fn();
    const view = render(
      <Modal title="Detalji" onClose={onClose} size="lg">
        <p>Sadrzaj</p>
      </Modal>,
      { router: false },
    );

    click(view.container.querySelector('[aria-label="Zatvori"]'));
    click(view.container.firstElementChild);
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));

    expect(onClose).toHaveBeenCalledTimes(3);
  });

  it('runs confirm modal actions and respects busy state', () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    const view = render(
      <ConfirmModal
        title="Potvrda"
        message="Nastaviti?"
        confirmLabel="Da"
        cancelLabel="Ne"
        tone="danger"
        error="Nesto nije uredu"
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
      { router: false },
    );

    click(view.container.querySelectorAll('button')[1]);
    click(view.container.querySelectorAll('button')[2]);
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(view.text()).toContain('Nesto nije uredu');

    view.unmount();
    const busyView = render(
      <ConfirmModal title="Busy" message="Wait" confirmLabel="Save" busy onCancel={onCancel} onConfirm={onConfirm} />,
      { router: false },
    );
    expect(busyView.text()).toContain('Obrada...');
    expect((busyView.container.querySelectorAll('button')[1] as HTMLButtonElement).disabled).toBe(true);
  });
});
