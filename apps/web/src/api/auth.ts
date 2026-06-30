import client from "./client";
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  UserInfo,
  ChangePasswordRequest,
} from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export function register(data: RegisterRequest): Promise<void> {
  return client
    .post("/auth/register", data)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}

export function login(data: LoginRequest): Promise<LoginResponse> {
  return client
    .post("/auth/login", data)
    .then((res: AxiosResponse<ApiResponse<LoginResponse>>) => res.data.data);
}

export function getMe(): Promise<UserInfo> {
  return client.get("/auth/me").then((res: AxiosResponse<ApiResponse<UserInfo>>) => res.data.data);
}

export function changePassword(data: ChangePasswordRequest): Promise<void> {
  return client
    .put("/auth/change-password", data)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}
