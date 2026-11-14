import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import LoginPage from "../pages/LoginPage.vue";

const mockPush = vi.fn();
vi.mock("vue-router", () => ({
  useRouter: () => ({ push: mockPush }),
}));

vi.mock("../api/services", () => ({
  login: vi.fn(),
}));

import * as api from "../api/services";

describe("LoginPage", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    mockPush.mockClear();
  });

  it("renders the form", () => {
    const w = mount(LoginPage, { global: { plugins: [createPinia()] } });
    expect(w.find("#username").exists()).toBe(true);
    expect(w.find("#password").exists()).toBe(true);
    expect(w.text()).toContain("Sign In");
  });

  it("submits login and redirects to admin on success", async () => {
    (api.login as any).mockResolvedValue({
      token: "tok",
      tokenType: "Bearer",
      userId: 1,
      username: "admin",
      fullName: "Admin",
      roles: ["ROLE_ADMIN"],
      expiresAt: "2026-11-03T00:00:00",
    });

    const w = mount(LoginPage, { global: { plugins: [createPinia()] } });
    await w.find("#username").setValue("admin");
    await w.find("#password").setValue("pass");
    await w.find("form").trigger("submit.prevent");
    await vi.waitFor(() => expect(mockPush).toHaveBeenCalledWith("/admin"));
  });

  it("submits login and redirects to worker on success", async () => {
    (api.login as any).mockResolvedValue({
      token: "tok",
      tokenType: "Bearer",
      userId: 2,
      username: "worker",
      fullName: "Worker",
      roles: ["ROLE_WORKER"],
      expiresAt: "2026-11-03T00:00:00",
    });

    const w = mount(LoginPage, { global: { plugins: [createPinia()] } });
    await w.find("#username").setValue("worker");
    await w.find("#password").setValue("pass");
    await w.find("form").trigger("submit.prevent");
    await vi.waitFor(() => expect(mockPush).toHaveBeenCalledWith("/worker"));
  });

  it("shows error on failed login", async () => {
    (api.login as any).mockRejectedValue({
      response: { data: { detail: "Invalid username or password" } },
    });

    const w = mount(LoginPage, { global: { plugins: [createPinia()] } });
    await w.find("#username").setValue("bad");
    await w.find("#password").setValue("bad");
    await w.find("form").trigger("submit.prevent");
    await vi.waitFor(() => expect(w.text()).toContain("Invalid username or password"));
  });
});
