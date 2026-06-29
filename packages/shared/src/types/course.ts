/**
 * 课程相关类型
 */

export interface Course {
  id: string;
  name: string;
  labId: string;
  labName?: string;
  teacherId: string;
  teacherName?: string;
  semester: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  startDate: string;
  endDate: string;
  className: string;
  createdAt: string;
  updatedAt: string;
}

export interface CourseCreateRequest {
  name: string;
  labId: string;
  teacherId: string;
  semester: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  startDate: string;
  endDate: string;
  className: string;
}

export interface CourseUpdateRequest {
  name?: string;
  labId?: string;
  teacherId?: string;
  dayOfWeek?: number;
  startTime?: string;
  endTime?: string;
  startDate?: string;
  endDate?: string;
  className?: string;
}

export interface CourseQuery {
  semester?: string;
  teacherId?: string;
  labId?: string;
  className?: string;
  cursor?: string;
  limit?: number;
}
