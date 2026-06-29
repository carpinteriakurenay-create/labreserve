/**
 * 通用枚举定义 — LabReserve
 */

/** 用户角色 */
export enum UserRole {
  STUDENT = "STUDENT",
  TEACHER = "TEACHER",
  ADMIN = "ADMIN",
}

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  [UserRole.STUDENT]: "学生",
  [UserRole.TEACHER]: "教师",
  [UserRole.ADMIN]: "管理员",
};

/** 预约状态 */
export enum BookingStatus {
  PENDING = "PENDING",
  APPROVED = "APPROVED",
  REJECTED = "REJECTED",
  CANCELLED = "CANCELLED",
  COMPLETED = "COMPLETED",
}

export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  [BookingStatus.PENDING]: "待审核",
  [BookingStatus.APPROVED]: "已通过",
  [BookingStatus.REJECTED]: "已拒绝",
  [BookingStatus.CANCELLED]: "已取消",
  [BookingStatus.COMPLETED]: "已完成",
};

/** 借用状态 */
export enum BorrowStatus {
  PENDING = "PENDING",
  APPROVED = "APPROVED",
  REJECTED = "REJECTED",
  BORROWING = "BORROWING",
  RETURNED = "RETURNED",
}

export const BORROW_STATUS_LABELS: Record<BorrowStatus, string> = {
  [BorrowStatus.PENDING]: "待审核",
  [BorrowStatus.APPROVED]: "已通过",
  [BorrowStatus.REJECTED]: "已拒绝",
  [BorrowStatus.BORROWING]: "借用中",
  [BorrowStatus.RETURNED]: "已归还",
};

/** 实验室状态 */
export enum LabStatus {
  AVAILABLE = "AVAILABLE",
  MAINTENANCE = "MAINTENANCE",
  CLOSED = "CLOSED",
}

export const LAB_STATUS_LABELS: Record<LabStatus, string> = {
  [LabStatus.AVAILABLE]: "可用",
  [LabStatus.MAINTENANCE]: "维护中",
  [LabStatus.CLOSED]: "关闭",
};

/** 设备状态 */
export enum EquipmentStatus {
  AVAILABLE = "AVAILABLE",
  BORROWED = "BORROWED",
  MAINTENANCE = "MAINTENANCE",
}

export const EQUIPMENT_STATUS_LABELS: Record<EquipmentStatus, string> = {
  [EquipmentStatus.AVAILABLE]: "可用",
  [EquipmentStatus.BORROWED]: "借出",
  [EquipmentStatus.MAINTENANCE]: "维修中",
};

/** 通知类型 */
export enum NoticeType {
  GENERAL = "GENERAL",
  LAB = "LAB",
  EQUIPMENT = "EQUIPMENT",
}

export const NOTICE_TYPE_LABELS: Record<NoticeType, string> = {
  [NoticeType.GENERAL]: "通用",
  [NoticeType.LAB]: "实验室",
  [NoticeType.EQUIPMENT]: "设备",
};

/** 通知优先级 */
export enum NoticePriority {
  LOW = "LOW",
  NORMAL = "NORMAL",
  HIGH = "HIGH",
  URGENT = "URGENT",
}

export const NOTICE_PRIORITY_LABELS: Record<NoticePriority, string> = {
  [NoticePriority.LOW]: "低",
  [NoticePriority.NORMAL]: "普通",
  [NoticePriority.HIGH]: "高",
  [NoticePriority.URGENT]: "紧急",
};

/** 报修状态 */
export enum RepairStatus {
  PENDING = "PENDING",
  IN_PROGRESS = "IN_PROGRESS",
  COMPLETED = "COMPLETED",
}

export const REPAIR_STATUS_LABELS: Record<RepairStatus, string> = {
  [RepairStatus.PENDING]: "待处理",
  [RepairStatus.IN_PROGRESS]: "处理中",
  [RepairStatus.COMPLETED]: "已完成",
};
