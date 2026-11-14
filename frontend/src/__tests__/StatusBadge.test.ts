import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import StatusBadge from "../components/StatusBadge.vue";

describe("StatusBadge", () => {
  function mountBadge(status: string) {
    return mount(StatusBadge, { props: { status: status as any } });
  }

  it("displays 'Scheduled' for SCHEDULED", () => {
    const w = mountBadge("SCHEDULED");
    expect(w.text()).toBe("Scheduled");
    expect(w.classes()).toContain("bg-gray-100");
  });

  it("displays 'En route' for EN_ROUTE", () => {
    const w = mountBadge("EN_ROUTE");
    expect(w.text()).toBe("En route");
    expect(w.classes()).toContain("bg-amber-100");
  });

  it("displays 'In progress' for IN_PROGRESS", () => {
    const w = mountBadge("IN_PROGRESS");
    expect(w.text()).toBe("In progress");
    expect(w.classes()).toContain("bg-sky-100");
  });

  it("displays 'Completed' for COMPLETED", () => {
    const w = mountBadge("COMPLETED");
    expect(w.text()).toBe("Completed");
    expect(w.classes()).toContain("bg-green-100");
  });

  it("displays 'Cancelled' for CANCELLED", () => {
    const w = mountBadge("CANCELLED");
    expect(w.text()).toBe("Cancelled");
    expect(w.classes()).toContain("bg-red-100");
  });
});
