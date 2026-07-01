import client from "./client";
import type { Review, ReviewCreateRequest } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface ReviewPage {
  records: Review[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface ReviewQueryParams {
  labId?: number;
  userId?: number;
  pageNum?: number;
  pageSize?: number;
}

export function createReview(data: ReviewCreateRequest): Promise<Review> {
  return client
    .post("/reviews", data)
    .then((res: AxiosResponse<ApiResponse<Review>>) => res.data.data);
}

export function getReviews(params: ReviewQueryParams): Promise<ReviewPage> {
  return client
    .get("/reviews", { params })
    .then((res: AxiosResponse<ApiResponse<ReviewPage>>) => res.data.data);
}

export function getReviewByBooking(bookingId: number): Promise<Review | null> {
  return client
    .get(`/reviews/booking/${bookingId}`)
    .then((res: AxiosResponse<ApiResponse<Review | null>>) => res.data.data);
}

export function getReviewsByLab(labId: number, params?: ReviewQueryParams): Promise<ReviewPage> {
  return client
    .get(`/reviews/lab/${labId}`, { params })
    .then((res: AxiosResponse<ApiResponse<ReviewPage>>) => res.data.data);
}

export function deleteReview(id: number): Promise<void> {
  return client.delete(`/reviews/${id}`).then(() => undefined);
}
