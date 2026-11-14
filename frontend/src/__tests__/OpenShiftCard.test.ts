import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import OpenShiftCard from "../components/OpenShiftCard.vue";
import type { OpenShiftView, WorkerView } from "../types/api";

const openShift: OpenShiftView = {
  id: 11,
  clientId: 10,
  clientName: "Harold Johnson",
  roomNumber: "105",
  address: "Oak Ave",
  location: "105 · Oak Ave",
  start: "2026-11-03T16:00:00",
  end: "2026-11-03T17:30:00",
  durationMinutes: 90,
  status: "OPEN",
  notes: "Evening meds",
  takenById: null,
  takenByName: "",
  takenAt: null,
  assignmentId: null,
};

const workers: WorkerView[] = [
  { id: 3, fullName: "Grace Loke", username: "grace.loke", role: "WORKER", enabled: true, shiftsToday: 0, availability: [] },
  { id: 4, fullName: "Priya Raman", username: "priya.raman", role: "WORKER", enabled: true, shiftsToday: 0, availability: [] },
];

describe("OpenShiftCard", () => {
  it("shows the time, client and notes", () => {
    const w = mount(OpenShiftCard, { props: { shift: openShift, mode: "worker" } });
    expect(w.text()).toContain("2026-11-03");
    expect(w.text()).toContain("16:00");
    expect(w.text()).toContain("17:30");
    expect(w.text()).toContain("Harold Johnson");
    expect(w.text()).toContain("Evening meds");
    expect(w.text()).toContain("Open");
  });

  it("worker mode shows a Take shift button that emits take", async () => {
    const w = mount(OpenShiftCard, { props: { shift: openShift, mode: "worker" } });
    const button = w.findAll("button").find((b) => b.text() === "Take shift")!;
    expect(button).toBeTruthy();
    await button.trigger("click");
    expect(w.emitted("take")).toEqual([[11]]);
  });

  it("worker mode disables the take button while busy", () => {
    const w = mount(OpenShiftCard, { props: { shift: openShift, mode: "worker", busy: true } });
    const button = w.findAll("button").find((b) => b.text() === "Taking…")!;
    expect(button).toBeTruthy();
    expect((button.element as HTMLButtonElement).disabled).toBe(true);
  });

  it("admin open shift shows worker select, Assign and Cancel", async () => {
    const w = mount(OpenShiftCard, { props: { shift: openShift, mode: "admin", workers } });
    const select = w.get("select");
    const options = select.findAll("option");
    expect(options).toHaveLength(3); // placeholder + 2 workers

    const assignButton = w.findAll("button").find((b) => b.text() === "Assign")!;
    expect((assignButton.element as HTMLButtonElement).disabled).toBe(true);

    await select.setValue("3");
    expect((assignButton.element as HTMLButtonElement).disabled).toBe(false);
    await assignButton.trigger("click");
    expect(w.emitted("assign")).toEqual([[11, 3]]);

    const cancelButton = w.findAll("button").find((b) => b.text() === "Cancel")!;
    await cancelButton.trigger("click");
    expect(w.emitted("cancel")).toEqual([[11]]);
  });

  it("taken shift shows who took it and no actions", () => {
    const taken: OpenShiftView = {
      ...openShift,
      status: "TAKEN",
      takenById: 3,
      takenByName: "Grace Loke",
      takenAt: "2026-11-02T09:00:00",
      assignmentId: 42,
    };
    const w = mount(OpenShiftCard, { props: { shift: taken, mode: "admin", workers } });
    expect(w.text()).toContain("Taken");
    expect(w.text()).toContain("by Grace Loke");
    expect(w.find("select").exists()).toBe(false);
    expect(w.findAll("button")).toHaveLength(0);
  });

  it("worker mode does not show assign controls", () => {
    const w = mount(OpenShiftCard, { props: { shift: openShift, mode: "worker" } });
    expect(w.find("select").exists()).toBe(false);
  });
});
