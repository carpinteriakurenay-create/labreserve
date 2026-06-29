/**
 * 验证学期格式，如 "2025-2026-1"
 */
export function isValidSemester(value: string): boolean {
  return /^\d{4}-\d{4}-[12]$/.test(value);
}

/**
 * 格式化日期为 yyyy-MM-dd
 */
export function formatDate(date: Date | string): string {
  const d = typeof date === "string" ? new Date(date) : date;
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * 格式化时间 HH:mm
 */
export function formatTime(date: Date | string): string {
  const d = typeof date === "string" ? new Date(date) : date;
  const hh = String(d.getHours()).padStart(2, "0");
  const min = String(d.getMinutes()).padStart(2, "0");
  return `${hh}:${min}`;
}
