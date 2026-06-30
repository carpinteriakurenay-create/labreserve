import client from "./client";
import type { Booking, BookingCreateRequest, TimeSlot, ApprovalRequest } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface BookingPage {
  records: Booking[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface BookingQueryParams {
  status?: string;
  labId?: number;
  userId?: number;
  dateFrom?: string;
  dateTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getAvailableSlots(labId: number | string, date: string): Promise<TimeSlot[]> {
  return client
    .get("/bookings/available-slots", { params: { labId, date } })
    .then((res: AxiosResponse<ApiResponse<TimeSlot[]>>) => res.data.data);
}

export function createBooking(data: BookingCreateRequest): Promise<Booking> {
  return client
    .post("/bookings", data)
    .then((res: AxiosResponse<ApiResponse<Booking>>) => res.data.data);
}

export function getBookings(params: BookingQueryParams): Promise<BookingPage> {
  return client
    .get("/bookings", { params })
    .then((res: AxiosResponse<ApiResponse<BookingPage>>) => res.data.data);
}

export function getMyBookings(params: BookingQueryParams): Promise<BookingPage> {
  return client
    .get("/bookings/mine", { params })
    .then((res: AxiosResponse<ApiResponse<BookingPage>>) => res.data.data);
}

export function getPendingApprovals(params: BookingQueryParams): Promise<BookingPage> {
  return client
    .get("/bookings/pending", { params })
    .then((res: AxiosResponse<ApiResponse<BookingPage>>) => res.data.data);
}

export function cancelBooking(id: number): Promise<void> {
  return client.put(`/bookings/${id}/cancel`).then(() => undefined);
}

export function approveBooking(id: number, data: ApprovalRequest): Promise<Booking> {
  return client
    .put(`/bookings/${id}/approve`, data)
    .then((res: AxiosResponse<ApiResponse<Booking>>) => res.data.data);
}

export function completeBooking(id: number): Promise<void> {
  return client.put(`/bookings/${id}/complete`).then(() => undefined);
}
