import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import StatusSelect from "../components/StatusSelect.vue";

describe("StatusSelect", () => {
  it("shows the current status with an icon", () => {
    const w = mount(StatusSelect, { props: { status: "IN_PROGRESS" } });
    const trigger = w.get("button[aria-label='Shift status']");
    expect(trigger.text()).toContain("In progress");
    expect(trigger.find("svg").exists()).toBe(true);
  });

  it("opens a menu with the three worker statuses", async () => {
    const w = mount(StatusSelect, { props: { status: "SCHEDULED" } });
    const trigger = w.get("button[aria-label='Shift status']");
    expect(trigger.attributes("aria-expanded")).toBe("false");
    await trigger.trigger("click");
    expect(trigger.attributes("aria-expanded")).toBe("true");
    const items = w.findAll("[role='menuitem']");
    expect(items.map((i) => i.text())).toEqual(["Scheduled", "In progress", "Completed"]);
    expect(items.every((i) => i.find("svg").exists())).toBe(true);
  });

  it("marks the current status disabled in the menu", async () => {
    const w = mount(StatusSelect, { props: { status: "SCHEDULED" } });
    await w.get("button[aria-label='Shift status']").trigger("click");
    const items = w.findAll("[role='menuitem']");
    expect((items[0].element as HTMLButtonElement).disabled).toBe(true);
    expect((items[1].element as HTMLButtonElement).disabled).toBe(false);
  });

  it("emits change and closes when another status is picked", async () => {
    const w = mount(StatusSelect, { props: { status: "SCHEDULED" } });
    await w.get("button[aria-label='Shift status']").trigger("click");
    await w.findAll("[role='menuitem']")[2].trigger("click");
    expect(w.emitted("change")).toEqual([["COMPLETED"]]);
    expect(w.find("[role='menuitem']").exists()).toBe(false);
  });

  it("closes when the backdrop is clicked", async () => {
    const w = mount(StatusSelect, { props: { status: "SCHEDULED" } });
    await w.get("button[aria-label='Shift status']").trigger("click");
    await w.get("div[aria-hidden='true']").trigger("click");
    expect(w.find("[role='menuitem']").exists()).toBe(false);
    expect(w.emitted("change")).toBeUndefined();
  });

  it("closes on Escape", async () => {
    const w = mount(StatusSelect, { props: { status: "SCHEDULED" } });
    await w.get("button[aria-label='Shift status']").trigger("click");
    await w.get("div").trigger("keydown.esc");
    expect(w.find("[role='menuitem']").exists()).toBe(false);
  });

  it("is disabled while busy", () => {
    const w = mount(StatusSelect, { props: { status: "SCHEDULED", busy: true } });
    expect((w.get("button[aria-label='Shift status']").element as HTMLButtonElement).disabled).toBe(true);
  });
});
