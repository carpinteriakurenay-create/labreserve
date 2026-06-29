/**
 * 评价相关类型
 */

export interface Review {
  id: string;
  bookingId: string;
  userId: string;
  userName?: string;
  userRealName?: string;
  labId: string;
  labName?: string;
  rating: number;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewCreateRequest {
  bookingId: string;
  rating: number;
  comment?: string;
}

export interface ReviewQuery {
  labId?: string;
  userId?: string;
  cursor?: string;
  limit?: number;
}
