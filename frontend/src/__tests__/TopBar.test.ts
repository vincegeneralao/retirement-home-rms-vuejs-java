import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { useAuthStore } from "../stores/auth";
import TopBar from "../components/TopBar.vue";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe("TopBar", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it("shows app title", () => {
    const w = mount(TopBar, { global: { plugins: [createPinia()] } });
    expect(w.text()).toContain("Retirement Home RMS");
  });

  it("shows Logout button when authenticated", () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const auth = useAuthStore();
    auth.token = "tok";
    auth.user = { userId: 1, username: "u", fullName: "User One", roles: ["ROLE_WORKER"] };
    const w = mount(TopBar, { global: { plugins: [pinia] } });
    expect(w.text()).toContain("Logout");
    expect(w.text()).toContain("User One");
  });

  it("hides Logout when not authenticated", () => {
    const w = mount(TopBar, { global: { plugins: [createPinia()] } });
    expect(w.text()).not.toContain("Logout");
  });

  it("clicking Logout clears the store", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const auth = useAuthStore();
    auth.token = "tok";
    auth.user = { userId: 1, username: "u", fullName: "U", roles: ["ROLE_WORKER"] };
    const w = mount(TopBar, { global: { plugins: [pinia] } });
    await w.find("button").trigger("click");
    expect(auth.isAuthenticated).toBe(false);
  });
});
