import client from "./client";
import type { User, UserCreateRequest, UserUpdateRequest } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface UserPage {
  records: User[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface UserQuery {
  role?: string;
  enabled?: boolean;
  pageNum?: number;
  pageSize?: number;
}

export function getUsers(params: UserQuery): Promise<UserPage> {
  return client
    .get("/users", { params })
    .then((res: AxiosResponse<ApiResponse<UserPage>>) => res.data.data);
}

export function createUser(data: UserCreateRequest): Promise<User> {
  return client.post("/users", data).then((res: AxiosResponse<ApiResponse<User>>) => res.data.data);
}

export function getUserById(id: number): Promise<User> {
  return client.get(`/users/${id}`).then((res: AxiosResponse<ApiResponse<User>>) => res.data.data);
}

export function updateUser(id: number, data: UserUpdateRequest): Promise<User> {
  return client
    .put(`/users/${id}`, data)
    .then((res: AxiosResponse<ApiResponse<User>>) => res.data.data);
}

export function deleteUser(id: number): Promise<void> {
  return client
    .delete(`/users/${id}`)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}

export function toggleEnabled(id: number): Promise<void> {
  return client
    .put(`/users/${id}/toggle-enabled`)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}
