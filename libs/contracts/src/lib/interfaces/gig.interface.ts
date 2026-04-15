import { GigStatus } from '../enums';

export interface IGig {
  id: number;
  freelancerId: number;
  categoryId: number;
  title: string;
  description?: string;
  cost: number;
  deliveryTime: number;
  revisionCount: number;
  status: GigStatus;
  coverImage?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface ICategory {
  id: number;
  title: string;
  slug: string;
}

export interface ITag {
  id: number;
  name: string;
  slug: string;
}

export interface IGigImage {
  id: number;
  gigId: number;
  imageUrl: string;
  sortOrder: number;
}

export interface IGigDetail extends IGig {
  category?: ICategory;
  tags: ITag[];
  images: IGigImage[];
  freelancerName?: string;
  freelancerAvatar?: string;
  freelancerRating?: number;
  freelancerReviewCount?: number;
}
