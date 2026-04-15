import { OrderStatus, CustomOfferStatus } from '../enums';

export interface IOrder {
  id: number;
  clientId: number;
  gigId: number;
  orderDate: Date;
  status: OrderStatus;
  totalCost: number;
  deliveryDeadline?: Date;
  maxRevisions: number;
  usedRevisions: number;
  completedAt?: Date;
  cancelledAt?: Date;
}

export interface IOrderHistory {
  id: number;
  orderId: number;
  changedByUserId: number;
  actionType: string;
  oldStatus: string;
  newStatus: string;
  note?: string;
  changedAt: Date;
}

export interface IDelivery {
  id: number;
  orderId: number;
  versionNumber: number;
  message?: string;
  fileUrl?: string;
  fileName?: string;
  createdAt: Date;
}

export interface ICustomOffer {
  id: number;
  gigId?: number;
  senderId: number;
  receiverId: number;
  title: string;
  description?: string;
  price: number;
  deliveryDays: number;
  revisionCount: number;
  status: CustomOfferStatus;
  expiresAt?: Date;
  createdAt: Date;
}
