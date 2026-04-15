export interface SendMessageDto {
  receiverId: number;
  orderId?: number;
  content: string;
}

export interface CreateReviewDto {
  orderId: number;
  revieweeId: number;
  rating: number;
  comment?: string;
}

export interface CreateDisputeDto {
  orderId: number;
  reason: string;
  description?: string;
}

export interface ResolveDisputeDto {
  resolution: string;
  status: 'RESOLVED_BUYER' | 'RESOLVED_SELLER';
}
