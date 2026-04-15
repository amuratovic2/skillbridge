export interface RegisterDto {
  username: string;
  email: string;
  password: string;
  role: 'CLIENT' | 'FREELANCER';
  firstName?: string;
  lastName?: string;
}

export interface LoginDto {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: number;
    username: string;
    email: string;
    role: string;
    profilePicture?: string;
  };
}

export interface RefreshTokenDto {
  refreshToken: string;
}
