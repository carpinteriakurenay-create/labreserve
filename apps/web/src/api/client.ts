import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import { ElMessage } from "element-plus";
import type { ApiResponse } from "@labreserve/shared";

declare module "axios" {
  interface AxiosRequestConfig {
    _silent?: boolean;
  }
}

const client = axios.create({
  baseURL: "/api",
  timeout: 10000,
  headers: { "Content-Type": "application/json" },
});

let activeRequests = 0;

client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  activeRequests++;
  return config;
});

client.interceptors.response.use(
  (response: AxiosResponse) => {
    activeRequests--;
    const body = response.data as ApiResponse;
    if (body.code !== "SUCCESS") {
      if (!(response.config as unknown as Record<string, unknown>)._silent) {
        ElMessage.error(body.message || "请求失败");
      }
      return Promise.reject(new Error(body.message || "请求失败"));
    }
    return response;
  },
  (error: AxiosError<ApiResponse>) => {
    activeRequests--;
    const silent = (error.config as unknown as Record<string, unknown>)?._silent;
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("userInfo");
      if (!silent) {
        ElMessage.error("登录已过期，请重新登录");
      }
      window.location.href = "/login";
    } else if (!silent) {
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message);
      } else {
        ElMessage.error("网络错误，请稍后重试");
      }
    }
    return Promise.reject(error);
  },
);

export { activeRequests };

export default client;
