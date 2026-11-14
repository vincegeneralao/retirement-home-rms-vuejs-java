import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("axios", () => {
  const instance = {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  };
  return {
    default: {
      create: vi.fn().mockReturnValue(instance),
    },
  };
});

import { httpClient, getApiBase } from "../api/client";

describe("api/client", () => {
  it("getApiBase returns configured base", () => {
    expect(getApiBase()).toBe("/api");
  });

  it("httpClient is created", () => {
    expect(httpClient).toBeDefined();
  });
});
