export interface CreateGigDto {
  title: string;
  description?: string;
  categoryId: number;
  cost: number;
  deliveryTime: number;
  revisionCount: number;
  coverImage?: string;
  tags?: string[];
}

export interface UpdateGigDto {
  title?: string;
  description?: string;
  categoryId?: number;
  cost?: number;
  deliveryTime?: number;
  revisionCount?: number;
  coverImage?: string;
  status?: string;
  tags?: string[];
}

export interface SearchGigsDto {
  q?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  deliveryTime?: number;
  sortBy?: 'price_asc' | 'price_desc' | 'rating' | 'newest' | 'popular';
  page?: number;
  limit?: number;
}

export interface CreateCategoryDto {
  title: string;
}

export interface UpdateCategoryDto {
  title?: string;
}
