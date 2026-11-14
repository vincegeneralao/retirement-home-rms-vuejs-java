import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import StatePanels from "../components/StatePanels.vue";

describe("StatePanels", () => {
  it("shows spinner when loading", () => {
    const w = mount(StatePanels, { props: { loading: true } });
    expect(w.find(".animate-spin").exists()).toBe(true);
    expect(w.text()).not.toContain("No records");
  });

  it("shows error message when error is set", () => {
    const w = mount(StatePanels, { props: { error: "Something broke" } });
    expect(w.text()).toContain("Something broke");
    expect(w.classes()).toContain("bg-red-50");
  });

  it("shows empty message when empty is true", () => {
    const w = mount(StatePanels, { props: { empty: true } });
    expect(w.text()).toContain("No records found.");
  });

  it("renders nothing when no state flags are set", () => {
    const w = mount(StatePanels, { props: {} });
    expect(w.text()).toBe("");
  });
});
