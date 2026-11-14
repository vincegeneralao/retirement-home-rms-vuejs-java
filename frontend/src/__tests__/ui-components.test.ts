import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import UiButton from "../components/ui/UiButton.vue";
import UiInput from "../components/ui/UiInput.vue";
import UiLabel from "../components/ui/UiLabel.vue";
import UiCard from "../components/ui/UiCard.vue";
import UiBadge from "../components/ui/UiBadge.vue";
import UiAlert from "../components/ui/UiAlert.vue";
import { cn } from "../lib/utils";

describe("cn utility", () => {
  it("merges conflicting tailwind classes, last wins", () => {
    expect(cn("p-2", "p-4")).toBe("p-4");
  });

  it("joins conditional classes", () => {
    expect(cn("a", false && "b", "c")).toBe("a c");
  });
});

describe("UiButton", () => {
  it("renders slot content in a button", () => {
    const w = mount(UiButton, { slots: { default: "Click me" } });
    expect(w.element.tagName).toBe("BUTTON");
    expect(w.text()).toBe("Click me");
    expect(w.classes()).toContain("bg-primary");
  });

  it("applies variant classes", () => {
    for (const [variant, cls] of [
      ["default", "bg-primary"],
      ["destructive", "bg-destructive"],
      ["outline", "border-input"],
      ["secondary", "bg-secondary"],
      ["ghost", "bg-white"],
      ["link", "underline-offset-4"],
    ] as const) {
      const w = mount(UiButton, { props: { variant } });
      expect(w.classes()).toContain(cls);
    }
  });

  it("applies size classes", () => {
    for (const [size, cls] of [
      ["default", "h-9"],
      ["sm", "h-8"],
      ["lg", "h-10"],
      ["icon", "w-9"],
    ] as const) {
      const w = mount(UiButton, { props: { size } });
      expect(w.classes()).toContain(cls);
    }
  });

  it("merges a custom class", () => {
    const w = mount(UiButton, { props: { class: "w-full" } });
    expect(w.classes()).toContain("w-full");
  });

  it("emits click events", async () => {
    const w = mount(UiButton);
    await w.trigger("click");
    expect(w.emitted("click")).toHaveLength(1);
  });
});

describe("UiInput", () => {
  it("renders an input with v-model support", async () => {
    const w = mount(UiInput, { props: { modelValue: "hello" } });
    expect((w.element as HTMLInputElement).value).toBe("hello");
    await w.setValue("typed");
    expect(w.emitted("update:modelValue")?.at(-1)).toEqual(["typed"]);
  });

  it("passes through type and placeholder", () => {
    const w = mount(UiInput, { props: { type: "password", placeholder: "secret" } });
    expect(w.attributes("type")).toBe("password");
    expect(w.attributes("placeholder")).toBe("secret");
  });

  it("supports disabled state", () => {
    const w = mount(UiInput, { props: { disabled: true } });
    expect((w.element as HTMLInputElement).disabled).toBe(true);
  });

  it("merges a custom class", () => {
    const w = mount(UiInput, { props: { class: "mt-2" } });
    expect(w.classes()).toContain("mt-2");
  });
});

describe("UiLabel", () => {
  it("renders with for attribute", () => {
    const w = mount(UiLabel, { props: { for: "email" }, slots: { default: "Email" } });
    expect(w.element.tagName).toBe("LABEL");
    expect(w.attributes("for")).toBe("email");
    expect(w.text()).toBe("Email");
  });
});

describe("UiCard", () => {
  it("renders slot content in a styled div", () => {
    const w = mount(UiCard, { slots: { default: "Body" } });
    expect(w.element.tagName).toBe("DIV");
    expect(w.classes()).toContain("bg-white");
    expect(w.text()).toBe("Body");
  });

  it("merges a custom class", () => {
    const w = mount(UiCard, { props: { class: "p-0" } });
    expect(w.classes()).toContain("p-0");
  });
});

describe("UiBadge", () => {
  it("applies variant classes", () => {
    for (const [variant, cls] of [
      ["default", "bg-primary"],
      ["secondary", "bg-secondary"],
      ["success", "bg-green-100"],
      ["warning", "bg-amber-100"],
      ["destructive", "bg-red-100"],
      ["outline", "text-slate-700"],
    ] as const) {
      const w = mount(UiBadge, { props: { variant } });
      expect(w.classes()).toContain(cls);
    }
  });
});

describe("UiAlert", () => {
  it("has alert role and default styling", () => {
    const w = mount(UiAlert, { slots: { default: "Heads up" } });
    expect(w.attributes("role")).toBe("alert");
    expect(w.classes()).toContain("bg-card");
  });

  it("applies variant classes", () => {
    for (const [variant, cls] of [
      ["destructive", "bg-red-50"],
      ["success", "bg-green-50"],
      ["info", "bg-brand-50"],
    ] as const) {
      const w = mount(UiAlert, { props: { variant } });
      expect(w.classes()).toContain(cls);
    }
  });
});
