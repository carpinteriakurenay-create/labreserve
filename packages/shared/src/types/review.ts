/**
 * 评价相关类型
 *
 * 对应后端：com.labreserve.controller.ReviewController
 * @packageDocumentation
 */

/**
 * 实验室评价
 *
 * 对应后端 DTO：com.labreserve.dto.ReviewVO
 */
export interface Review {
  /** 评价 ID */
  id: string;
  /** 关联预约 ID */
  bookingId: string;
  /** 评价人用户 ID */
  userId: string;
  /** 评价人用户名 */
  userName?: string;
  /** 评价人真实姓名 */
  userRealName?: string;
  /** 被评价实验室 ID */
  labId: string;
  /** 实验室名称 */
  labName?: string;
  /** 评分（1-5 星） */
  rating: number;
  /** 评价内容 */
  comment: string | null;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/**
 * 提交评价请求
 *
 * 需要角色：STUDENT/TEACHER/ADMIN
 * 限制：仅可评价自己已完成的预约，每个预约仅可评价一次。
 * 可能错误：BOOKING_NOT_COMPLETED, ALREADY_REVIEWED
 */
export interface ReviewCreateRequest {
  /** 预约 ID（必须是已完成状态的预约） */
  bookingId: string;
  /** 评分（1-5） */
  rating: number;
  /** 评价内容（可选，最长 1000 字符） */
  comment?: string;
}

/**
 * 评价列表查询参数
 */
export interface ReviewQuery {
  /** 按实验室筛选 */
  labId?: string;
  /** 按用户筛选（"我的评价"） */
  userId?: string;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}
