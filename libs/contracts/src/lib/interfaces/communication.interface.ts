import { DisputeStatus, NotificationType } from '../enums';

export interface IMessage {
  id: number;
  senderId: number;
  receiverId: number;
  orderId?: number;
  content: string;
  isRead: boolean;
  sentAt: Date;
}

export interface IReview {
  id: number;
  orderId: number;
  reviewerId: number;
  revieweeId: number;
  rating: number;
  comment?: string;
  createdAt: Date;
}

export interface IDispute {
  id: number;
  orderId: number;
  initiatorId: number;
  reason?: string;
  description?: string;
  status: DisputeStatus;
  adminId?: number;
  resolution?: string;
  createdAt: Date;
  resolvedAt?: Date;
}

export interface INotification {
  id: number;
  userId: number;
  type: NotificationType;
  title: string;
  content?: string;
  referenceId?: number;
  isRead: boolean;
  createdAt: Date;
}
