/**
 * 通知公告相关类型
 *
 * 对应后端：com.labreserve.controller.NoticeController
 * @packageDocumentation
 */

import type { NoticePriority, NoticeType } from "./enums";

/**
 * 通知公告
 *
 * 对应后端 DTO：com.labreserve.dto.NoticeVO
 */
export interface Notice {
  /** 通知 ID */
  id: string;
  /** 通知标题 */
  title: string;
  /** 通知正文（支持纯文本） */
  content: string;
  /** 通知类型 */
  type: NoticeType;
  /** 优先级 */
  priority: NoticePriority;
  /** 发布人 ID */
  publisherId: string;
  /** 发布人姓名（后端 JOIN 填充） */
  publisherName?: string;
  /** 关联实验室 ID（类型为 LAB 时有值） */
  labId: string | null;
  /** 关联实验室名称 */
  labName?: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/**
 * 创建/发布通知请求
 *
 * 需要角色：TEACHER/ADMIN
 */
export interface NoticeCreateRequest {
  /** 通知标题（必填） */
  title: string;
  /** 通知正文（必填） */
  content: string;
  /** 通知类型（默认 GENERAL） */
  type?: NoticeType;
  /** 优先级（默认 NORMAL） */
  priority?: NoticePriority;
  /** 关联实验室 ID（实验室通知时必填） */
  labId?: string;
}

/**
 * 更新通知请求
 *
 * 需要角色：TEACHER/ADMIN，所有字段可选。
 */
export interface NoticeUpdateRequest {
  /** 新标题 */
  title?: string;
  /** 新正文 */
  content?: string;
  /** 新类型 */
  type?: NoticeType;
  /** 新优先级 */
  priority?: NoticePriority;
  /** 新关联实验室 */
  labId?: string;
}

/**
 * 通知列表查询参数
 */
export interface NoticeQuery {
  /** 按类型筛选 */
  type?: NoticeType;
  /** 按优先级筛选 */
  priority?: NoticePriority;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}
