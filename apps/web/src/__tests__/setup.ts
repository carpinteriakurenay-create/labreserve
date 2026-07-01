import { vi } from "vitest";

vi.mock("element-plus", () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
  ElNotification: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

export async function flushPromises() {
  return new Promise<void>((resolve) => setTimeout(resolve, 0));
}
