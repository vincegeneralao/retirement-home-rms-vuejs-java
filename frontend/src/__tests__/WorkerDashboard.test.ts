import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import WorkerDashboard from "../pages/WorkerDashboard.vue";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock("../api/services", () => ({
  getWorkerAssignments: vi.fn(),
  getWorkerUpcoming: vi.fn(),
  getOpenShifts: vi.fn(),
  claimOpenShift: vi.fn(),
  updateWorkerAssignmentStatus: vi.fn(),
}));

import * as api from "../api/services";

const openShift = {
  id: 11, clientId: 10, clientName: "Harold Johnson", roomNumber: "105", address: "Oak Ave",
  location: "105 · Oak Ave", start: "2026-11-03T16:00:00", end: "2026-11-03T17:30:00",
  durationMinutes: 90, status: "OPEN", notes: "Evening meds", takenById: null, takenByName: "", takenAt: null, assignmentId: null,
};

describe("WorkerDashboard", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
    (api.getWorkerAssignments as any).mockResolvedValue([]);
    (api.getWorkerUpcoming as any).mockResolvedValue([]);
    (api.getOpenShifts as any).mockResolvedValue([]);
    (api.claimOpenShift as any).mockResolvedValue({});
    (api.updateWorkerAssignmentStatus as any).mockResolvedValue({});
  });

  it("renders shift cards after loading", async () => {
    (api.getWorkerAssignments as any).mockResolvedValue([
      {
        id: 1, workerId: 1, workerName: "Grace", clientId: 10, clientName: "Margaret",
        roomNumber: "201", address: "Elm St", location: "201 · Elm St",
        start: "2026-11-02T08:00:00", end: "2026-11-02T09:00:00",
        durationMinutes: 60, status: "COMPLETED", notes: "Done",
      },
    ]);

    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Margaret"));
    expect(w.text()).toContain("08:00");
  });

  it("shows empty state when no shifts", async () => {
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No records found"));
  });

  it("shows error on failure", async () => {
    (api.getWorkerAssignments as any).mockRejectedValue({
      response: { data: { detail: "Server error" } },
    });
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Server error"));
  });

  it("lists open shifts with a take button", async () => {
    (api.getOpenShifts as any).mockResolvedValue([openShift]);
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Open shifts you can take"));
    await vi.waitFor(() => expect(w.text()).toContain("Harold Johnson"));
    expect(w.text()).toContain("16:00");
    const takeButtons = w.findAllComponents({ name: "UiButton" }).filter((b) => b.text() === "Take shift");
    expect(takeButtons).toHaveLength(1);
  });

  it("shows empty hint when there are no open shifts", async () => {
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No open shifts right now"));
  });

  it("claims a shift and reloads both lists", async () => {
    (api.getOpenShifts as any).mockResolvedValueOnce([openShift]).mockResolvedValue([]);
    (api.claimOpenShift as any).mockResolvedValue({ ...openShift, status: "TAKEN" });
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Harold Johnson"));

    const takeButton = w.findAllComponents({ name: "UiButton" }).find((b) => b.text() === "Take shift")!;
    await takeButton.trigger("click");
    await vi.waitFor(() => expect(api.claimOpenShift).toHaveBeenCalledWith(11));
    // all lists were reloaded (initial load + post-claim refresh)
    expect(api.getOpenShifts).toHaveBeenCalledTimes(2);
    expect(api.getWorkerAssignments).toHaveBeenCalledTimes(2);
    expect(api.getWorkerUpcoming).toHaveBeenCalledTimes(2);
    await vi.waitFor(() => expect(w.text()).toContain("No open shifts right now"));
  });

  it("lists upcoming shifts with dates", async () => {
    (api.getWorkerUpcoming as any).mockResolvedValue([
      {
        id: 7, workerId: 1, workerName: "Ayser", clientId: 10, clientName: "Margaret Smith",
        roomNumber: "201", address: "Elm St", location: "201 · Elm St",
        start: "2026-11-14T09:00:00", end: "2026-11-14T11:00:00",
        durationMinutes: 120, status: "SCHEDULED", notes: "Weekend support",
      },
    ]);
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Margaret Smith"));
    expect(w.text()).toContain("2026-11-14");
    expect(w.text()).toContain("09:00");
  });

  it("shows empty hint when there are no upcoming shifts", async () => {
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No upcoming shifts"));
  });

  it("shows a claim error when the shift was taken by someone else", async () => {
    (api.getOpenShifts as any).mockResolvedValueOnce([openShift]).mockResolvedValue([]);
    (api.claimOpenShift as any).mockRejectedValue({
      response: { data: { detail: "This shift has already been taken" } },
    });
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Harold Johnson"));

    const takeButton = w.findAllComponents({ name: "UiButton" }).find((b) => b.text() === "Take shift")!;
    await takeButton.trigger("click");
    await vi.waitFor(() => expect(w.text()).toContain("This shift has already been taken"));
  });

  it("changes a shift status via the dropdown and reloads", async () => {
    (api.getWorkerAssignments as any).mockResolvedValue([
      {
        id: 1, workerId: 1, workerName: "Grace", clientId: 10, clientName: "Margaret",
        roomNumber: "201", address: "Elm St", location: "201 · Elm St",
        start: "2026-11-02T08:00:00", end: "2026-11-02T09:00:00",
        durationMinutes: 60, status: "SCHEDULED", notes: null,
      },
    ]);
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.find("button[aria-label='Shift status']").exists()).toBe(true));

    await w.get("button[aria-label='Shift status']").trigger("click");
    const item = w.findAll("[role='menuitem']").find((b) => b.text().includes("In progress"))!;
    await item.trigger("click");
    await vi.waitFor(() => expect(api.updateWorkerAssignmentStatus).toHaveBeenCalledWith(1, "IN_PROGRESS"));
    expect(api.getWorkerAssignments).toHaveBeenCalledTimes(2);
    expect(api.getWorkerUpcoming).toHaveBeenCalledTimes(2);
  });

  it("shows an error when the status update fails", async () => {
    (api.getWorkerAssignments as any).mockResolvedValue([
      {
        id: 1, workerId: 1, workerName: "Grace", clientId: 10, clientName: "Margaret",
        roomNumber: "201", address: "Elm St", location: "201 · Elm St",
        start: "2026-11-02T08:00:00", end: "2026-11-02T09:00:00",
        durationMinutes: 60, status: "SCHEDULED", notes: null,
      },
    ]);
    (api.updateWorkerAssignmentStatus as any).mockRejectedValue({
      response: { data: { detail: "This shift has been cancelled" } },
    });
    const w = mount(WorkerDashboard, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.find("button[aria-label='Shift status']").exists()).toBe(true));

    await w.get("button[aria-label='Shift status']").trigger("click");
    const item = w.findAll("[role='menuitem']").find((b) => b.text().includes("Completed"))!;
    await item.trigger("click");
    await vi.waitFor(() => expect(w.text()).toContain("This shift has been cancelled"));
  });
});
