import client from "./client";
import type { Notice, NoticeCreateRequest, NoticeUpdateRequest } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface NoticePage {
  records: Notice[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface NoticeQueryParams {
  type?: string;
  priority?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getNotices(params: NoticeQueryParams): Promise<NoticePage> {
  return client
    .get("/notices", { params })
    .then((res: AxiosResponse<ApiResponse<NoticePage>>) => res.data.data);
}

export function getNoticeById(id: number): Promise<Notice> {
  return client
    .get(`/notices/${id}`)
    .then((res: AxiosResponse<ApiResponse<Notice>>) => res.data.data);
}

export function createNotice(data: NoticeCreateRequest): Promise<Notice> {
  return client
    .post("/notices", data)
    .then((res: AxiosResponse<ApiResponse<Notice>>) => res.data.data);
}

export function updateNotice(id: number, data: NoticeUpdateRequest): Promise<Notice> {
  return client
    .put(`/notices/${id}`, data)
    .then((res: AxiosResponse<ApiResponse<Notice>>) => res.data.data);
}

export function deleteNotice(id: number): Promise<void> {
  return client
    .delete(`/notices/${id}`)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}
