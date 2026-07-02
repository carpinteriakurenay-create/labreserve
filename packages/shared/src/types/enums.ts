/**
 * 通用枚举定义 — LabReserve
 *
 * 定义系统中所有领域枚举及其中文显示标签。
 * 前后端各自维护枚举值（类型安全），通过 shared 包保证一致性。
 *
 * @packageDocumentation
 */

/**
 * 用户角色
 *
 * 权限层级：STUDENT < TEACHER < ADMIN
 * 后端对应：com.labreserve.enums.UserRole
 */
export enum UserRole {
  /** 学生 — 可浏览实验室、预约/取消、借用设备、评价 */
  STUDENT = "STUDENT",
  /** 教师 — 学生全部权限 + 审批预约/借用、发布通知、查看统计 */
  TEACHER = "TEACHER",
  /** 管理员 — 全部权限：用户管理、实验室/设备/课程 CRUD、数据导出 */
  ADMIN = "ADMIN",
}

/** 用户角色中文显示名 */
export const USER_ROLE_LABELS: Record<UserRole, string> = {
  [UserRole.STUDENT]: "学生",
  [UserRole.TEACHER]: "教师",
  [UserRole.ADMIN]: "管理员",
};

/**
 * 预约状态
 *
 * 状态机：PENDING → APPROVED → COMPLETED, PENDING → REJECTED, PENDING/APPROVED → CANCELLED
 * 后端对应：com.labreserve.enums.BookingStatus
 */
export enum BookingStatus {
  /** 待审核 — 学生提交预约后的初始状态 */
  PENDING = "PENDING",
  /** 已通过 — 教师/管理员审批通过 */
  APPROVED = "APPROVED",
  /** 已拒绝 — 教师/管理员驳回预约 */
  REJECTED = "REJECTED",
  /** 已取消 — 学生自行取消或管理员取消 */
  CANCELLED = "CANCELLED",
  /** 已完成 — 预约时段结束后教师确认完成 */
  COMPLETED = "COMPLETED",
}

/** 预约状态中文显示名 */
export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  [BookingStatus.PENDING]: "待审核",
  [BookingStatus.APPROVED]: "已通过",
  [BookingStatus.REJECTED]: "已拒绝",
  [BookingStatus.CANCELLED]: "已取消",
  [BookingStatus.COMPLETED]: "已完成",
};

/**
 * 借用状态
 *
 * 状态机：PENDING → APPROVED → BORROWING → RETURNED, PENDING → REJECTED
 * 后端对应：com.labreserve.enums.BorrowStatus
 */
export enum BorrowStatus {
  /** 待审核 — 学生提交借用申请后的初始状态 */
  PENDING = "PENDING",
  /** 已通过 — 教师/管理员审批通过 */
  APPROVED = "APPROVED",
  /** 已拒绝 — 教师/管理员驳回借用 */
  REJECTED = "REJECTED",
  /** 借用中 — 设备已借出，管理员确认归还前一直为该状态 */
  BORROWING = "BORROWING",
  /** 已归还 — 管理员确认归还，设备恢复为 AVAILABLE */
  RETURNED = "RETURNED",
}

/** 借用状态中文显示名 */
export const BORROW_STATUS_LABELS: Record<BorrowStatus, string> = {
  [BorrowStatus.PENDING]: "待审核",
  [BorrowStatus.APPROVED]: "已通过",
  [BorrowStatus.REJECTED]: "已拒绝",
  [BorrowStatus.BORROWING]: "借用中",
  [BorrowStatus.RETURNED]: "已归还",
};

/**
 * 实验室状态
 *
 * 后端对应：com.labreserve.enums.LabStatus
 */
export enum LabStatus {
  /** 可用 — 正常开放，可预约 */
  AVAILABLE = "AVAILABLE",
  /** 维护中 — 临时关闭，不可预约 */
  MAINTENANCE = "MAINTENANCE",
  /** 关闭 — 长期关闭，不可预约 */
  CLOSED = "CLOSED",
}

/** 实验室状态中文显示名 */
export const LAB_STATUS_LABELS: Record<LabStatus, string> = {
  [LabStatus.AVAILABLE]: "可用",
  [LabStatus.MAINTENANCE]: "维护中",
  [LabStatus.CLOSED]: "关闭",
};

/**
 * 设备状态
 *
 * 后端对应：com.labreserve.enums.EquipmentStatus
 */
export enum EquipmentStatus {
  /** 可用 — 可借用 */
  AVAILABLE = "AVAILABLE",
  /** 借出 — 已被借用，不可再借 */
  BORROWED = "BORROWED",
  /** 维修中 — 不可借用 */
  MAINTENANCE = "MAINTENANCE",
}

/** 设备状态中文显示名 */
export const EQUIPMENT_STATUS_LABELS: Record<EquipmentStatus, string> = {
  [EquipmentStatus.AVAILABLE]: "可用",
  [EquipmentStatus.BORROWED]: "借出",
  [EquipmentStatus.MAINTENANCE]: "维修中",
};

/**
 * 通知类型
 *
 * 后端对应：com.labreserve.enums.NoticeType
 */
export enum NoticeType {
  /** 通用通知 — 系统公告 */
  GENERAL = "GENERAL",
  /** 实验室通知 — 特定实验室相关 */
  LAB = "LAB",
  /** 设备通知 — 设备相关 */
  EQUIPMENT = "EQUIPMENT",
}

/** 通知类型中文显示名 */
export const NOTICE_TYPE_LABELS: Record<NoticeType, string> = {
  [NoticeType.GENERAL]: "通用",
  [NoticeType.LAB]: "实验室",
  [NoticeType.EQUIPMENT]: "设备",
};

/**
 * 通知优先级
 *
 * 后端对应：com.labreserve.enums.NoticePriority
 */
export enum NoticePriority {
  /** 低优先级 */
  LOW = "LOW",
  /** 普通优先级 */
  NORMAL = "NORMAL",
  /** 高优先级 */
  HIGH = "HIGH",
  /** 紧急 — 需立即关注 */
  URGENT = "URGENT",
}

/** 通知优先级中文显示名 */
export const NOTICE_PRIORITY_LABELS: Record<NoticePriority, string> = {
  [NoticePriority.LOW]: "低",
  [NoticePriority.NORMAL]: "普通",
  [NoticePriority.HIGH]: "高",
  [NoticePriority.URGENT]: "紧急",
};

/**
 * 报修状态
 *
 * 后端对应：com.labreserve.enums.RepairStatus
 */
export enum RepairStatus {
  /** 待处理 — 报修已提交 */
  PENDING = "PENDING",
  /** 处理中 — 管理员已开始处理 */
  IN_PROGRESS = "IN_PROGRESS",
  /** 已完成 — 修复完成 */
  COMPLETED = "COMPLETED",
}

/** 报修状态中文显示名 */
export const REPAIR_STATUS_LABELS: Record<RepairStatus, string> = {
  [RepairStatus.PENDING]: "待处理",
  [RepairStatus.IN_PROGRESS]: "处理中",
  [RepairStatus.COMPLETED]: "已完成",
};
