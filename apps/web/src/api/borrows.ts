import client from "./client";
import type { Borrow, BorrowCreateRequest } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface BorrowPage {
  records: Borrow[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface BorrowQueryParams {
  status?: string;
  equipmentId?: number;
  userId?: number;
  pageNum?: number;
  pageSize?: number;
}

export function getBorrows(params: BorrowQueryParams): Promise<BorrowPage> {
  return client
    .get("/borrows", { params })
    .then((res: AxiosResponse<ApiResponse<BorrowPage>>) => res.data.data);
}

export function createBorrow(data: BorrowCreateRequest): Promise<Borrow> {
  return client
    .post("/borrows", data)
    .then((res: AxiosResponse<ApiResponse<Borrow>>) => res.data.data);
}

export function getMyBorrows(params: BorrowQueryParams): Promise<BorrowPage> {
  return client
    .get("/borrows/mine", { params })
    .then((res: AxiosResponse<ApiResponse<BorrowPage>>) => res.data.data);
}

export function getBorrowById(id: number): Promise<Borrow> {
  return client
    .get(`/borrows/${id}`)
    .then((res: AxiosResponse<ApiResponse<Borrow>>) => res.data.data);
}

export function approveBorrow(
  id: number,
  data: { approved: boolean; rejectReason?: string },
): Promise<Borrow> {
  return client
    .put(`/borrows/${id}/approve`, data)
    .then((res: AxiosResponse<ApiResponse<Borrow>>) => res.data.data);
}

export function returnBorrow(id: number, actualReturn?: string): Promise<void> {
  return client
    .put(`/borrows/${id}/return`, { actualReturn })
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}
