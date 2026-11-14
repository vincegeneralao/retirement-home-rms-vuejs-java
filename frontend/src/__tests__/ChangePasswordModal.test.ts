import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import ChangePasswordModal from "../components/ChangePasswordModal.vue";
import { useAuthStore } from "../stores/auth";

vi.mock("../api/services", () => ({
  login: vi.fn(),
  changePassword: vi.fn(),
  keepResetPassword: vi.fn(),
}));

function buttonByText(w: ReturnType<typeof mount>, text: string) {
  return w.findAllComponents({ name: "UiButton" }).find((b) => b.text() === text);
}

async function fill(w: ReturnType<typeof mount>, id: string, value: string) {
  await w.get(`#${id}`).setValue(value);
}

describe("ChangePasswordModal", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.resetAllMocks();
  });

  it("submits the chosen password and reports the reset as resolved", async () => {
    const auth = useAuthStore();
    auth.mustChangePassword = true;
    const change = vi.spyOn(auth, "changePassword").mockResolvedValue();

    const w = mount(ChangePasswordModal);
    expect(w.text()).toContain("Your password was reset");

    expect(buttonByText(w, "Change password")!.attributes("disabled")).toBeDefined();

    await fill(w, "cp-new", "Sunny#Day1");
    expect(buttonByText(w, "Change password")!.attributes("disabled")).toBeDefined();

    await fill(w, "cp-confirm", "Sunny#Day1");
    expect(buttonByText(w, "Change password")!.attributes("disabled")).toBeUndefined();

    await w.get("form").trigger("submit");
    expect(change).toHaveBeenCalledWith("", "Sunny#Day1");
    expect(w.emitted("resolved")).toHaveLength(1);
  });

  it("warns about mismatched confirmation and blocks the change", async () => {
    const auth = useAuthStore();
    vi.spyOn(auth, "changePassword").mockResolvedValue();

    const w = mount(ChangePasswordModal);
    await fill(w, "cp-new", "Sunny#Day1");
    await fill(w, "cp-confirm", "Sunny#Day2");
    expect(w.text()).toContain("The passwords do not match.");

    await w.get("form").trigger("submit");
    expect(auth.changePassword).not.toHaveBeenCalled();
    expect(w.emitted("resolved")).toBeUndefined();
  });

  it("asks for confirmation before keeping the blank password", async () => {
    const auth = useAuthStore();
    const keep = vi.spyOn(auth, "keepResetPassword").mockResolvedValue();

    const w = mount(ChangePasswordModal);
    await buttonByText(w, "Keep")!.trigger("click");

    expect(keep).not.toHaveBeenCalled();
    expect(w.emitted("resolved")).toBeUndefined();
    expect(w.text()).toContain("not best practice");
    expect(w.text()).toContain("Anyone who knows your username");
    expect(buttonByText(w, "Keep blank password")).toBeTruthy();

    await buttonByText(w, "Keep blank password")!.trigger("click");
    expect(keep).toHaveBeenCalledTimes(1);
    expect(w.emitted("resolved")).toHaveLength(1);
  });

  it("surfaces a backend rejection", async () => {
    const auth = useAuthStore();
    vi.spyOn(auth, "changePassword").mockRejectedValue({
      response: { data: { detail: "Current password is incorrect" } },
    });

    const w = mount(ChangePasswordModal);
    await fill(w, "cp-current", "guess");
    await fill(w, "cp-new", "Sunny#Day1");
    await fill(w, "cp-confirm", "Sunny#Day1");
    await w.get("form").trigger("submit.prevent");

    await vi.waitFor(() => expect(w.text()).toContain("Current password is incorrect"));
    expect(w.emitted("resolved")).toBeUndefined();
  });

  it("dismisses a stale reset prompt when the server has nothing pending", async () => {
    localStorage.setItem("rms_password_reset_required", "true");
    const auth = useAuthStore();
    vi.spyOn(auth, "keepResetPassword").mockRejectedValue({
      response: { status: 409, data: { code: "PASSWORD_RESET_NOT_PENDING", detail: "nope" } },
    });

    const w = mount(ChangePasswordModal);
    await buttonByText(w, "Keep")!.trigger("click");
    await buttonByText(w, "Keep blank password")!.trigger("click");

    await vi.waitFor(() => expect(w.emitted("resolved")).toHaveLength(1));
    expect(auth.mustChangePassword).toBe(false);
    expect(localStorage.getItem("rms_password_reset_required")).toBe("false");
  });
});
