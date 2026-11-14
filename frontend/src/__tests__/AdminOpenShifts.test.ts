import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import AdminOpenShifts from "../pages/AdminOpenShifts.vue";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { props: ["to"], template: '<a :href="to"><slot /></a>' },
}));

vi.mock("../api/services", () => ({
  getAdminClients: vi.fn(),
  getAdminWorkers: vi.fn(),
  getAdminOpenShifts: vi.fn(),
  createOpenShift: vi.fn(),
  assignOpenShift: vi.fn(),
  cancelOpenShift: vi.fn(),
}));

import * as api from "../api/services";

const openShift = {
  id: 11, clientId: 10, clientName: "Harold Johnson", roomNumber: "105", address: "Oak Ave",
  location: "105 · Oak Ave", start: "2026-11-03T16:00:00", end: "2026-11-03T17:30:00",
  durationMinutes: 90, status: "OPEN", notes: "Evening meds", takenById: null, takenByName: "", takenAt: null, assignmentId: null,
};

const takenShift = {
  ...openShift, id: 12, status: "TAKEN", takenById: 3, takenByName: "Grace Loke",
  takenAt: "2026-11-02T09:00:00", assignmentId: 42,
};

describe("AdminOpenShifts", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    (api.getAdminClients as any).mockResolvedValue([
      { id: 10, fullName: "Harold Johnson", roomNumber: "105", address: "Oak Ave", location: "105 · Oak Ave", active: true },
      { id: 11, fullName: "Retired Person", roomNumber: "999", address: "Old Rd", location: "999 · Old Rd", active: false },
    ]);
    (api.getAdminWorkers as any).mockResolvedValue([
      { id: 3, fullName: "Grace Loke", username: "grace.loke", role: "WORKER", enabled: true, shiftsToday: 0, availability: [] },
      { id: 4, fullName: "Off Duty Nurse", username: "off.duty", role: "WORKER", enabled: false, shiftsToday: 0, availability: [] },
    ]);
    (api.getAdminOpenShifts as any).mockResolvedValue([openShift, takenShift]);
  });

  function mountPage() {
    return mount(AdminOpenShifts, {
      global: { plugins: [createPinia()], stubs: { "router-link": true } },
    });
  }

  it("renders the create form and the open shift list", async () => {
    const w = mountPage();
    await vi.waitFor(() => expect(w.text()).toContain("Harold Johnson"));
    expect(w.text()).toContain("Open shifts");
    expect(w.text()).toContain("Create an open shift");
    expect(w.text()).toContain("by Grace Loke");

    const clientOptions = w.get("#os-client").findAll("option");
    expect(clientOptions).toHaveLength(2); // placeholder + only active client
    expect(w.text()).not.toContain("Retired Person");

    const cardSelects = w.findAllComponents({ name: "OpenShiftCard" })
      .flatMap((c) => c.findAll("select"));
    expect(cardSelects).toHaveLength(1); // only the OPEN shift row has a select
    const workerOptions = cardSelects[0].findAll("option");
    expect(workerOptions).toHaveLength(2); // placeholder + only enabled worker
  });

  it("creates an open shift and refreshes the list", async () => {
    (api.createOpenShift as any).mockResolvedValue(openShift);
    const w = mountPage();
    await vi.waitFor(() => expect(w.findAll("#os-client option").length).toBe(2));

    await w.get("#os-client").setValue("10");
    await w.get("#os-date").setValue("2026-11-05");
    await w.get("#os-start").setValue("07:00");
    await w.get("#os-end").setValue("15:30");
    await w.get("#os-notes").setValue("Morning care");
    await w.get("#open-shift-form").trigger("submit");

    await vi.waitFor(() =>
      expect(api.createOpenShift).toHaveBeenCalledWith({
        clientId: 10, date: "2026-11-05", startTime: "07:00", endTime: "15:30", notes: "Morning care",
      }),
    );
    await vi.waitFor(() => expect(w.text()).toContain("Open shift created"));
    expect(api.getAdminOpenShifts).toHaveBeenCalledTimes(2);
  });

  it("shows an error when creating fails", async () => {
    (api.createOpenShift as any).mockRejectedValue({
      response: { data: { detail: "End time must be after start time" } },
    });
    const w = mountPage();
    await vi.waitFor(() => expect(w.text()).toContain("Create an open shift"));

    await w.get("#os-client").setValue("10");
    await w.get("#open-shift-form").trigger("submit");
    await vi.waitFor(() => expect(w.text()).toContain("End time must be after start time"));
  });

  it("assigns a worker from the card and refreshes", async () => {
    (api.assignOpenShift as any).mockResolvedValue({ ...openShift, status: "TAKEN" });
    const w = mountPage();
    await vi.waitFor(() => expect(w.text()).toContain("Harold Johnson"));

    const cardSelect = w.findAll("select").find((s) => s.attributes("aria-label") === "Worker to assign")!;
    await cardSelect.setValue("3");
    const assignButton = w.findAll("button").find((b) => b.text() === "Assign")!;
    await assignButton.trigger("click");

    await vi.waitFor(() => expect(api.assignOpenShift).toHaveBeenCalledWith(11, 3));
    await vi.waitFor(() => expect(api.getAdminOpenShifts).toHaveBeenCalledTimes(2));
  });

  it("cancels an open shift", async () => {
    (api.cancelOpenShift as any).mockResolvedValue({ ...openShift, status: "CANCELLED" });
    const w = mountPage();
    await vi.waitFor(() => expect(w.text()).toContain("Harold Johnson"));

    const cancelButton = w.findAll("button").find((b) => b.text() === "Cancel")!;
    await cancelButton.trigger("click");
    await vi.waitFor(() => expect(api.cancelOpenShift).toHaveBeenCalledWith(11));
  });

  it("shows an error when an action fails", async () => {
    (api.cancelOpenShift as any).mockRejectedValue({
      response: { data: { detail: "Only open shifts that nobody has taken can be cancelled" } },
    });
    const w = mountPage();
    await vi.waitFor(() => expect(w.text()).toContain("Harold Johnson"));

    const cancelButton = w.findAll("button").find((b) => b.text() === "Cancel")!;
    await cancelButton.trigger("click");
    await vi.waitFor(() =>
      expect(w.text()).toContain("Only open shifts that nobody has taken can be cancelled"),
    );
  });

  it("shows error panel when initial load fails", async () => {
    (api.getAdminOpenShifts as any).mockRejectedValue({
      response: { data: { detail: "Forbidden" } },
    });
    const w = mountPage();
    await vi.waitFor(() => expect(w.text()).toContain("Forbidden"));
  });
});
