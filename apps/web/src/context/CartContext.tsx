import { createContext, useContext, useEffect, useState, ReactNode } from 'react';

export interface CartItem {
  gigId: number;
  title: string;
  cost: number;
  deliveryTime: number;
}

interface CartContextType {
  items: CartItem[];
  add: (item: CartItem) => void;
  remove: (gigId: number) => void;
  clear: () => void;
  total: number;
}

const STORAGE_KEY = 'skillbridge_cart';

const CartContext = createContext<CartContextType | undefined>(undefined);

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as CartItem[]) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }, [items]);

  const add = (item: CartItem) => {
    setItems((current) =>
      current.some((c) => c.gigId === item.gigId) ? current : [...current, item],
    );
  };

  const remove = (gigId: number) => {
    setItems((current) => current.filter((c) => c.gigId !== gigId));
  };

  const clear = () => setItems([]);

  const total = items.reduce((sum, i) => sum + Number(i.cost || 0), 0);

  return (
    <CartContext.Provider value={{ items, add, remove, clear, total }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within a CartProvider');
  return ctx;
}
