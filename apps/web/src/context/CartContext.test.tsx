import { describe, expect, it } from 'vitest';
import { CartProvider, useCart } from './CartContext';
import { click, render } from '../test/render';

function CartProbe() {
  const cart = useCart();

  return (
    <div>
      <p data-testid="summary">{cart.items.length}:{cart.total.toFixed(2)}</p>
      <button type="button" onClick={() => cart.add({ gigId: 1, title: 'Logo', cost: 100, deliveryTime: 3 })}>
        Add logo
      </button>
      <button type="button" onClick={() => cart.add({ gigId: 1, title: 'Logo duplicate', cost: 150, deliveryTime: 4 })}>
        Add duplicate
      </button>
      <button type="button" onClick={() => cart.add({ gigId: 2, title: 'SEO', cost: 50, deliveryTime: 2 })}>
        Add SEO
      </button>
      <button type="button" onClick={() => cart.remove(1)}>Remove logo</button>
      <button type="button" onClick={cart.clear}>Clear</button>
    </div>
  );
}

describe('CartProvider', () => {
  it('loads saved items, avoids duplicates, totals, removes and clears', () => {
    localStorage.setItem('skillbridge_cart', JSON.stringify([{ gigId: 9, title: 'Saved', cost: 25, deliveryTime: 1 }]));

    const view = render(
      <CartProvider>
        <CartProbe />
      </CartProvider>,
      { router: false },
    );

    expect(view.text()).toContain('1:25.00');

    click(view.container.querySelector('button:nth-of-type(1)'));
    click(view.container.querySelector('button:nth-of-type(2)'));
    click(view.container.querySelector('button:nth-of-type(3)'));
    expect(view.text()).toContain('3:175.00');

    click(view.container.querySelector('button:nth-of-type(4)'));
    expect(view.text()).toContain('2:75.00');

    click(view.container.querySelector('button:nth-of-type(5)'));
    expect(view.text()).toContain('0:0.00');
    expect(localStorage.getItem('skillbridge_cart')).toBe('[]');
  });

  it('falls back to an empty cart when storage is corrupt', () => {
    localStorage.setItem('skillbridge_cart', '{bad json');

    const view = render(
      <CartProvider>
        <CartProbe />
      </CartProvider>,
      { router: false },
    );

    expect(view.text()).toContain('0:0.00');
  });
});
