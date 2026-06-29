/**
 * @labreserve/shared — 前后端共享类型和工具
 */

// ── 枚举 ──
export {
  UserRole,
  USER_ROLE_LABELS,
  BookingStatus,
  BOOKING_STATUS_LABELS,
  BorrowStatus,
  BORROW_STATUS_LABELS,
  LabStatus,
  LAB_STATUS_LABELS,
  EquipmentStatus,
  EQUIPMENT_STATUS_LABELS,
  NoticeType,
  NOTICE_TYPE_LABELS,
  NoticePriority,
  NOTICE_PRIORITY_LABELS,
  RepairStatus,
  REPAIR_STATUS_LABELS,
} from "./types/enums";

// ── API 通用类型 ──
export type {
  CursorPageQuery,
  CursorPageResult,
  ApiError,
  ApiResponse,
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  UserInfo,
} from "./types/api";
export { SUCCESS_CODE } from "./types/api";

// ── 用户 ──
export type {
  User,
  UserCreateRequest,
  UserUpdateRequest,
  ChangePasswordRequest,
} from "./types/user";

// ── 实验室 ──
export type {
  Lab,
  LabCreateRequest,
  LabUpdateRequest,
  LabHours,
  LabHoursBatchRequest,
  LabQuery,
} from "./types/lab";

// ── 预约 ──
export type {
  Booking,
  BookingCreateRequest,
  BookingUpdateRequest,
  ApprovalRequest,
  AvailableSlotQuery,
  TimeSlot,
  BookingQuery,
} from "./types/booking";

// ── 设备 & 借用 ──
export type {
  Equipment,
  EquipmentCreateRequest,
  EquipmentUpdateRequest,
  EquipmentQuery,
  Borrow,
  BorrowCreateRequest,
  BorrowQuery,
} from "./types/equipment";

// ── 课程 ──
export type { Course, CourseCreateRequest, CourseUpdateRequest, CourseQuery } from "./types/course";

// ── 通知公告 ──
export type { Notice, NoticeCreateRequest, NoticeUpdateRequest, NoticeQuery } from "./types/notice";

// ── 评价 ──
export type { Review, ReviewCreateRequest, ReviewQuery } from "./types/review";

// ── 统计 & 杂项 ──
export type {
  DashboardKpi,
  LabUsageStat,
  EquipmentUsageStat,
  StudentRanking,
  UsageRecord,
  UsageRecordQuery,
  Student,
  StudentCreateRequest,
  StudentUpdateRequest,
  StudentQuery,
  RepairLog,
  RepairLogCreateRequest,
  RepairLogQuery,
  DateRangeQuery,
  ExportRequest,
} from "./types/misc";

// ── 工具函数 ──
export { isValidSemester, formatDate, formatTime } from "./utils";
