/**
 * 课程相关类型
 *
 * 对应后端：com.labreserve.controller.CourseController
 * @packageDocumentation
 */

/**
 * 课程信息
 *
 * 对应后端 DTO：com.labreserve.dto.CourseVO
 */
export interface Course {
  /** 课程 ID */
  id: string;
  /** 课程名称 */
  name: string;
  /** 上课实验室 ID */
  labId: string;
  /** 实验室名称（后端 JOIN 填充） */
  labName?: string;
  /** 任课教师 ID */
  teacherId: string;
  /** 教师姓名（后端 JOIN 填充） */
  teacherName?: string;
  /** 学期（如 "2025-2026-2"） */
  semester: string;
  /** 星期几（1=周一，7=周日） */
  dayOfWeek: number;
  /** 上课时间，格式 HH:mm */
  startTime: string;
  /** 下课时间，格式 HH:mm */
  endTime: string;
  /** 课程开始日期，格式 yyyy-MM-dd */
  startDate: string;
  /** 课程结束日期，格式 yyyy-MM-dd */
  endDate: string;
  /** 班级名称（如 "计科2101"） */
  className: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/**
 * 创建课程请求
 *
 * 需要角色：TEACHER/ADMIN
 */
export interface CourseCreateRequest {
  /** 课程名称 */
  name: string;
  /** 上课实验室 ID */
  labId: string;
  /** 任课教师 ID */
  teacherId: string;
  /** 学期 */
  semester: string;
  /** 星期几（1-7） */
  dayOfWeek: number;
  /** 上课时间，格式 HH:mm */
  startTime: string;
  /** 下课时间，格式 HH:mm */
  endTime: string;
  /** 课程开始日期 */
  startDate: string;
  /** 课程结束日期 */
  endDate: string;
  /** 班级名称 */
  className: string;
}

/**
 * 更新课程请求
 *
 * 需要角色：TEACHER/ADMIN，所有字段可选。
 */
export interface CourseUpdateRequest {
  /** 新名称 */
  name?: string;
  /** 新实验室 */
  labId?: string;
  /** 新教师 */
  teacherId?: string;
  /** 新星期 */
  dayOfWeek?: number;
  /** 新开始时间 */
  startTime?: string;
  /** 新结束时间 */
  endTime?: string;
  /** 新开始日期 */
  startDate?: string;
  /** 新结束日期 */
  endDate?: string;
  /** 新班级 */
  className?: string;
}

/**
 * 课程列表查询参数
 */
export interface CourseQuery {
  /** 按学期筛选 */
  semester?: string;
  /** 按教师筛选 */
  teacherId?: string;
  /** 按实验室筛选 */
  labId?: string;
  /** 按班级筛选 */
  className?: string;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}
