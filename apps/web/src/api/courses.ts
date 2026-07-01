import client from "./client";
import type { Course, CourseCreateRequest, CourseUpdateRequest } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface CoursePage {
  records: Course[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface CourseQueryParams {
  semester?: string;
  teacherId?: number;
  labId?: number;
  className?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getCourses(params: CourseQueryParams): Promise<CoursePage> {
  return client
    .get("/courses", { params })
    .then((res: AxiosResponse<ApiResponse<CoursePage>>) => res.data.data);
}

export function createCourse(data: CourseCreateRequest): Promise<Course> {
  return client
    .post("/courses", data)
    .then((res: AxiosResponse<ApiResponse<Course>>) => res.data.data);
}

export function getMyCourses(params: {
  className?: string;
  pageNum?: number;
  pageSize?: number;
}): Promise<CoursePage> {
  return client
    .get("/courses/mine", { params })
    .then((res: AxiosResponse<ApiResponse<CoursePage>>) => res.data.data);
}

export function getCourseById(id: number): Promise<Course> {
  return client
    .get(`/courses/${id}`)
    .then((res: AxiosResponse<ApiResponse<Course>>) => res.data.data);
}

export function updateCourse(id: number, data: CourseUpdateRequest): Promise<Course> {
  return client
    .put(`/courses/${id}`, data)
    .then((res: AxiosResponse<ApiResponse<Course>>) => res.data.data);
}

export function deleteCourse(id: number): Promise<void> {
  return client
    .delete(`/courses/${id}`)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}
