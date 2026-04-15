export interface CreateOrderDto {
  gigId: number;
}

export interface CreateDeliveryDto {
  message?: string;
  fileUrl?: string;
  fileName?: string;
}

export interface RequestRevisionDto {
  message: string;
}

export interface CreateCustomOfferDto {
  gigId?: number;
  receiverId: number;
  title: string;
  description?: string;
  price: number;
  deliveryDays: number;
  revisionCount: number;
}

export interface RespondCustomOfferDto {
  status: 'ACCEPTED' | 'REJECTED';
}
