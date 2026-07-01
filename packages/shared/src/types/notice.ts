/**
 * 通知公告相关类型
 */

import type { NoticePriority, NoticeType } from "./enums";

export interface Notice {
  id: string;
  title: string;
  content: string;
  type: NoticeType;
  priority: NoticePriority;
  publisherId: string;
  publisherName?: string;
  labId: string | null;
  labName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface NoticeCreateRequest {
  title: string;
  content: string;
  type?: NoticeType;
  priority?: NoticePriority;
  labId?: string;
}

export interface NoticeUpdateRequest {
  title?: string;
  content?: string;
  type?: NoticeType;
  priority?: NoticePriority;
  labId?: string;
}

export interface NoticeQuery {
  type?: NoticeType;
  priority?: NoticePriority;
  cursor?: string;
  limit?: number;
}
