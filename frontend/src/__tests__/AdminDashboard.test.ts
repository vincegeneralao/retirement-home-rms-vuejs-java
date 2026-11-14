import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import AdminDashboard from "../pages/AdminDashboard.vue";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { template: '<a><slot /></a>' },
}));

vi.mock("../api/services", () => ({
  getAdminAssignments: vi.fn(),
  downloadDailyReport: vi.fn(),
}));

import * as api from "../api/services";

describe("AdminDashboard", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("renders shift cards", async () => {
    (api.getAdminAssignments as any).mockResolvedValue([
      {
        id: 1, workerId: 1, workerName: "Grace", clientId: 10, clientName: "Margaret",
        roomNumber: "201", address: "Elm St", location: "201 · Elm St",
        start: "2026-11-02T08:00:00", end: "2026-11-02T09:00:00",
        durationMinutes: 60, status: "COMPLETED", notes: null,
      },
    ]);

    const w = mount(AdminDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Margaret"));
    expect(w.text()).toContain("Download Report");
  });

  it("shows empty state", async () => {
    (api.getAdminAssignments as any).mockResolvedValue([]);
    const w = mount(AdminDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No records found"));
  });

  it("shows error on failure", async () => {
    (api.getAdminAssignments as any).mockRejectedValue({
      response: { data: { detail: "Load failed" } },
    });
    const w = mount(AdminDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Load failed"));
  });

  it("downloads the report through the authenticated API when clicked", async () => {
    (api.getAdminAssignments as any).mockResolvedValue([]);
    (api.downloadDailyReport as any).mockResolvedValue({ blob: new Blob(["x"]), filename: "daily-report.xlsx" });
    (URL as any).createObjectURL = vi.fn().mockReturnValue("blob:test");
    (URL as any).revokeObjectURL = vi.fn();

    const w = mount(AdminDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No records found"));

    const button = w.findAll("button").find((b) => b.text().includes("Download Report"))!;
    await button.trigger("click");
    await vi.waitFor(() => expect(api.downloadDailyReport).toHaveBeenCalledTimes(1));
    expect((URL as any).createObjectURL).toHaveBeenCalled();
    expect((URL as any).revokeObjectURL).toHaveBeenCalledWith("blob:test");
  });

  it("shows an error when the download fails", async () => {
    (api.getAdminAssignments as any).mockResolvedValue([]);
    (api.downloadDailyReport as any).mockRejectedValue({
      response: { status: 401, data: new Blob([JSON.stringify({ detail: "Authentication required" })]) },
    });

    const w = mount(AdminDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No records found"));

    const button = w.findAll("button").find((b) => b.text().includes("Download Report"))!;
    await button.trigger("click");
    await vi.waitFor(() => expect(w.text()).toContain("Authentication required"));
  });
});
