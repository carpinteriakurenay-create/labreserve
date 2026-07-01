import { describe, it, expect, vi } from "vitest";
import client, { activeRequests } from "@/api/client";

describe("API client", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("should add Bearer token from localStorage", async () => {
    localStorage.setItem("token", "test-jwt-token");

    const config = { headers: {} as Record<string, string> };
    const handlers = (
      client.interceptors.request as unknown as {
        handlers: Array<{ fulfilled: (c: typeof config) => void }>;
      }
    ).handlers;
    if (handlers?.[0]) {
      handlers[0].fulfilled(config);
      expect(config.headers.Authorization).toBe("Bearer test-jwt-token");
    }
  });

  it("should not add Authorization header when no token", () => {
    const config = { headers: {} as Record<string, string> };
    const handlers = (
      client.interceptors.request as unknown as {
        handlers: Array<{ fulfilled: (c: typeof config) => void }>;
      }
    ).handlers;
    if (handlers?.[0]) {
      handlers[0].fulfilled(config);
      expect(config.headers.Authorization).toBeUndefined();
    }
  });

  it("should increment active requests on request", () => {
    const countBefore = activeRequests;
    const handlers = (
      client.interceptors.request as unknown as {
        handlers: Array<{ fulfilled: (c: Record<string, unknown>) => void }>;
      }
    ).handlers;
    if (handlers?.[0]) {
      handlers[0].fulfilled({ headers: {} });
    }
    expect(activeRequests).toBe(countBefore + 1);
  });

  it("should handle 401 by clearing token", () => {
    const error = {
      response: { status: 401, data: { message: "Unauthorized" } },
      config: { headers: {} },
    };

    const handlers = (
      client.interceptors.response as unknown as {
        handlers: Array<null | { rejected: (e: unknown) => Promise<never> }>;
      }
    ).handlers;
    if (handlers?.[1]) {
      handlers[1].rejected(error).catch(() => {});
      expect(localStorage.getItem("token")).toBeNull();
    }
  });
});
