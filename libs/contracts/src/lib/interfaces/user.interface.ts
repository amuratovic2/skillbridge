import { UserRole } from '../enums';

export interface IUser {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  firstName?: string;
  lastName?: string;
  bio?: string;
  profilePicture?: string;
  country?: string;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface ISkill {
  id: number;
  name: string;
}

export interface IPortfolioItem {
  id: number;
  userId: number;
  title: string;
  description?: string;
  imageUrl?: string;
  createdAt: Date;
}

export interface IUserProfile extends IUser {
  skills: ISkill[];
  portfolio: IPortfolioItem[];
  completedOrders?: number;
  averageRating?: number;
}
