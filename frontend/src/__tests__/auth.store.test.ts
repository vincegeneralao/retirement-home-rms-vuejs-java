import { describe, it, expect, beforeEach } from "vitest";
import { setActivePinia, createPinia } from "pinia";
import { useAuthStore } from "../stores/auth";
import * as api from "../api/services";
import type { AuthResponse } from "../types/api";

vi.mock("../api/services", () => ({
  login: vi.fn(),
}));

describe("auth store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it("starts unauthenticated when localStorage is empty", () => {
    const store = useAuthStore();
    expect(store.isAuthenticated).toBe(false);
    expect(store.isAdmin).toBe(false);
    expect(store.isWorker).toBe(false);
  });

  it("login sets token and user on success", async () => {
    const mockResp: AuthResponse = {
      token: "tok123",
      tokenType: "Bearer",
      userId: 1,
      username: "admin",
      fullName: "Admin",
      roles: ["ROLE_ADMIN"],
      expiresAt: "2026-11-03T00:00:00",
    };
    (api.login as any).mockResolvedValue(mockResp);

    const store = useAuthStore();
    const user = await store.login("admin", "pass");
    expect(user.username).toBe("admin");
    expect(store.isAuthenticated).toBe(true);
    expect(store.isAdmin).toBe(true);
    expect(store.isWorker).toBe(false);
    expect(localStorage.getItem("rms_token")).toBe("tok123");
  });

  it("logout clears state and localStorage", async () => {
    const mockResp: AuthResponse = {
      token: "tok123",
      tokenType: "Bearer",
      userId: 1,
      username: "admin",
      fullName: "Admin",
      roles: ["ROLE_ADMIN"],
      expiresAt: "2026-11-03T00:00:00",
    };
    (api.login as any).mockResolvedValue(mockResp);

    const store = useAuthStore();
    await store.login("admin", "pass");
    store.logout();

    expect(store.isAuthenticated).toBe(false);
    expect(localStorage.getItem("rms_token")).toBeNull();
    expect(localStorage.getItem("rms_user")).toBeNull();
  });

  it("login throws on invalid credentials", async () => {
    (api.login as any).mockRejectedValue(new Error("401"));
    const store = useAuthStore();
    await expect(store.login("bad", "creds")).rejects.toThrow("401");
    expect(store.isAuthenticated).toBe(false);
  });
});
