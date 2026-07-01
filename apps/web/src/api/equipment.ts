import client from "./client";
import type { Equipment, EquipmentCreateRequest, EquipmentUpdateRequest } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface EquipmentPage {
  records: Equipment[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface EquipmentQueryParams {
  labId?: number;
  status?: string;
  name?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getEquipments(params: EquipmentQueryParams): Promise<EquipmentPage> {
  return client
    .get("/equipment", { params })
    .then((res: AxiosResponse<ApiResponse<EquipmentPage>>) => res.data.data);
}

export function createEquipment(data: EquipmentCreateRequest): Promise<Equipment> {
  return client
    .post("/equipment", data)
    .then((res: AxiosResponse<ApiResponse<Equipment>>) => res.data.data);
}

export function getEquipmentById(id: number): Promise<Equipment> {
  return client
    .get(`/equipment/${id}`)
    .then((res: AxiosResponse<ApiResponse<Equipment>>) => res.data.data);
}

export function updateEquipment(id: number, data: EquipmentUpdateRequest): Promise<Equipment> {
  return client
    .put(`/equipment/${id}`, data)
    .then((res: AxiosResponse<ApiResponse<Equipment>>) => res.data.data);
}

export function deleteEquipment(id: number): Promise<void> {
  return client
    .delete(`/equipment/${id}`)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}

export function updateEquipmentStatus(id: number, status: string): Promise<void> {
  return client
    .put(`/equipment/${id}/status`, null, { params: { status } })
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}
