export interface UpdateUserDto {
  firstName?: string;
  lastName?: string;
  bio?: string;
  profilePicture?: string;
  country?: string;
}

export interface CreatePortfolioItemDto {
  title: string;
  description?: string;
  imageUrl?: string;
}

export interface UpdatePortfolioItemDto {
  title?: string;
  description?: string;
  imageUrl?: string;
}
