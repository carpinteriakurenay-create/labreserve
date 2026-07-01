import client from "./client";
import type {
  Student,
  StudentCreateRequest,
  StudentUpdateRequest,
  ApiResponse,
} from "@labreserve/shared";
import type { AxiosResponse } from "axios";

export interface StudentPage {
  records: Student[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface StudentQueryParams {
  labId?: number;
  name?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getStudents(params: StudentQueryParams): Promise<StudentPage> {
  return client
    .get("/students", { params })
    .then((res: AxiosResponse<ApiResponse<StudentPage>>) => res.data.data);
}

export function getStudentById(id: number): Promise<Student> {
  return client
    .get(`/students/${id}`)
    .then((res: AxiosResponse<ApiResponse<Student>>) => res.data.data);
}

export function createStudent(data: StudentCreateRequest): Promise<Student> {
  return client
    .post("/students", data)
    .then((res: AxiosResponse<ApiResponse<Student>>) => res.data.data);
}

export function updateStudent(id: number, data: StudentUpdateRequest): Promise<Student> {
  return client
    .put(`/students/${id}`, data)
    .then((res: AxiosResponse<ApiResponse<Student>>) => res.data.data);
}

export function deleteStudent(id: number): Promise<void> {
  return client.delete(`/students/${id}`);
}
