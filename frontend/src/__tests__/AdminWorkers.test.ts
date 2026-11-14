import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import AdminWorkers from "../pages/AdminWorkers.vue";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { template: '<a><slot /></a>' },
}));

vi.mock("../api/services", () => ({
  getAdminWorkers: vi.fn(),
  createWorker: vi.fn(),
  deactivateWorker: vi.fn(),
}));

import * as api from "../api/services";

const workersData = [
  {
    id: 1, fullName: "Grace Loke", username: "grace.loke", role: "WORKER",
    enabled: true, shiftsToday: 2,
    availability: [{ id: 1, dayOfWeek: "MONDAY", startTime: "08:00", endTime: "12:00" }],
  },
  {
    id: 2, fullName: "Priya Raman", username: "priya.raman", role: "WORKER",
    enabled: false, shiftsToday: 0,
    availability: [],
  },
];

describe("AdminWorkers", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("renders workers table", async () => {
    (api.getAdminWorkers as any).mockResolvedValue(workersData);
    const w = mount(AdminWorkers, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Grace Loke"));
    expect(w.text()).toContain("Priya Raman");
    expect(w.text()).toContain("2");
  });

  it("shows Deactivate button only for enabled workers", async () => {
    (api.getAdminWorkers as any).mockResolvedValue(workersData);
    const w = mount(AdminWorkers, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Grace Loke"));
    const deactivateButtons = w.findAll("button").filter(b => b.text().trim() === "Deactivate");
    expect(deactivateButtons.length).toBe(1);
  });

  it("shows empty state when no workers", async () => {
    (api.getAdminWorkers as any).mockResolvedValue([]);
    const w = mount(AdminWorkers, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No records found"));
  });

  it("shows error on failure", async () => {
    (api.getAdminWorkers as any).mockRejectedValue({
      response: { data: { detail: "Failed to load" } },
    });
    const w = mount(AdminWorkers, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Failed to load"));
  });

  it("adds a worker via form", async () => {
    (api.getAdminWorkers as any).mockResolvedValue(workersData);
    (api.createWorker as any).mockResolvedValue(workersData[0]);

    const w = mount(AdminWorkers, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Grace Loke"));

    // Fill form fields
    const inputs = w.findAll("input");
    await inputs[0].setValue("new.worker");
    await inputs[1].setValue("New Worker");
    await inputs[2].setValue("Passw0rd!");

    await w.find("form").trigger("submit.prevent");
    await vi.waitFor(() => expect(w.text()).toContain("Worker added"));
  });

  it("shows error when adding worker fails", async () => {
    (api.getAdminWorkers as any).mockResolvedValue(workersData);
    (api.createWorker as any).mockRejectedValue({
      response: { data: { detail: "Username taken" } },
    });

    const w = mount(AdminWorkers, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Grace Loke"));

    const inputs = w.findAll("input");
    await inputs[0].setValue("taken");
    await inputs[1].setValue("Takes");
    await inputs[2].setValue("Passw0rd!");

    await w.find("form").trigger("submit.prevent");
    await vi.waitFor(() => expect(w.text()).toContain("Username taken"));
  });

  it("deactivates a worker", async () => {
    (api.getAdminWorkers as any).mockResolvedValue(workersData);
    (api.deactivateWorker as any).mockResolvedValue({ ...workersData[0], enabled: false });

    const w = mount(AdminWorkers, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Grace Loke"));

    const deactivateBtn = w.findAll("button").find(b => b.text() === "Deactivate");
    await deactivateBtn!.trigger("click");
    await vi.waitFor(() => expect(api.deactivateWorker).toHaveBeenCalledWith(1));
  });
});
