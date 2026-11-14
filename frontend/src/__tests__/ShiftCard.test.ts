import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import ShiftCard from "../components/ShiftCard.vue";
import type { AssignmentView } from "../types/api";

const shift: AssignmentView = {
  id: 1,
  workerId: 1,
  workerName: "Grace",
  clientId: 10,
  clientName: "Margaret Smith",
  roomNumber: "201",
  address: "Elm St",
  location: "201 · Elm St",
  start: "2026-11-02T08:00:00",
  end: "2026-11-02T09:00:00",
  durationMinutes: 60,
  status: "COMPLETED",
  notes: "Medication check",
};

describe("ShiftCard", () => {
  it("renders client name", () => {
    const w = mount(ShiftCard, { props: { shift } });
    expect(w.text()).toContain("Margaret Smith");
  });

  it("renders time range", () => {
    const w = mount(ShiftCard, { props: { shift } });
    expect(w.text()).toContain("08:00");
    expect(w.text()).toContain("09:00");
  });

  it("renders location", () => {
    const w = mount(ShiftCard, { props: { shift } });
    expect(w.text()).toContain("201 · Elm St");
  });

  it("renders notes when present", () => {
    const w = mount(ShiftCard, { props: { shift } });
    expect(w.text()).toContain("Medication check");
  });

  it("hides notes section when null", () => {
    const noNotes = { ...shift, notes: null };
    const w = mount(ShiftCard, { props: { shift: noNotes } });
    expect(w.text()).not.toContain("null");
  });

  it("does not show a date by default", () => {
    const w = mount(ShiftCard, { props: { shift } });
    expect(w.text()).not.toContain("2026-11-02");
  });

  it("shows the date when showDate is set", () => {
    const w = mount(ShiftCard, { props: { shift, showDate: true } });
    expect(w.text()).toContain("2026-11-02");
    expect(w.text()).toContain("08:00");
  });

  it("shows a status badge by default", () => {
    const w = mount(ShiftCard, { props: { shift } });
    expect(w.find("button[aria-label='Shift status']").exists()).toBe(false);
    expect(w.text()).toContain("Completed");
  });

  it("shows a status dropdown with the current status when editable", async () => {
    const w = mount(ShiftCard, { props: { shift: { ...shift, status: "SCHEDULED" }, editable: true } });
    const trigger = w.get("button[aria-label='Shift status']");
    expect(trigger.text()).toContain("Scheduled");
    await trigger.trigger("click");
    const items = w.findAll("[role='menuitem']");
    expect(items.map((i) => i.text())).toEqual(["Scheduled", "In progress", "Completed"]);
    await items[1].trigger("click");
    expect(w.emitted("status-change")).toEqual([[expect.objectContaining({ id: 1 }), "IN_PROGRESS"]]);
  });

  it("does not emit for the current status item", async () => {
    const w = mount(ShiftCard, { props: { shift: { ...shift, status: "SCHEDULED" }, editable: true } });
    await w.get("button[aria-label='Shift status']").trigger("click");
    await w.findAll("[role='menuitem']")[0].trigger("click");
    expect(w.emitted("status-change")).toBeUndefined();
  });

  it("disables the dropdown while busy", () => {
    const w = mount(ShiftCard, { props: { shift: { ...shift, status: "SCHEDULED" }, editable: true, busy: true } });
    expect((w.get("button[aria-label='Shift status']").element as HTMLButtonElement).disabled).toBe(true);
  });

  it("falls back to a badge for cancelled shifts even when editable", () => {
    const w = mount(ShiftCard, { props: { shift: { ...shift, status: "CANCELLED" }, editable: true } });
    expect(w.find("button[aria-label='Shift status']").exists()).toBe(false);
    expect(w.text()).toContain("Cancelled");
  });
});
