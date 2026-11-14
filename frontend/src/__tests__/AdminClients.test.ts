import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import AdminClients from "../pages/AdminClients.vue";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
  RouterLink: { template: '<a><slot /></a>' },
}));

vi.mock("../api/services", () => ({
  getAdminClients: vi.fn(),
  createClient: vi.fn(),
}));

import * as api from "../api/services";

const client = { id: 10, fullName: "Margaret Smith", roomNumber: "201", address: "Elm St", location: "201 · Elm St", active: true };

describe("AdminClients", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
    (api.getAdminClients as any).mockResolvedValue([client]);
  });

  it("lists clients with room and address", async () => {
    const w = mount(AdminClients, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Margaret Smith"));
    expect(w.text()).toContain("Room 201");
    expect(w.text()).toContain("Elm St");
    expect(w.text()).toContain("Add client");
  });

  it("shows empty state", async () => {
    (api.getAdminClients as any).mockResolvedValue([]);
    const w = mount(AdminClients, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("No records found"));
  });

  it("shows load error", async () => {
    (api.getAdminClients as any).mockRejectedValue({ response: { data: { detail: "Clients unavailable" } } });
    const w = mount(AdminClients, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Clients unavailable"));
  });

  it("adds a client and refreshes the list", async () => {
    (api.createClient as any).mockResolvedValue({ ...client, id: 11, fullName: "New Person" });
    const w = mount(AdminClients, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Margaret Smith"));

    await w.get("#c-fullName").setValue("New Person");
    await w.get("#c-roomNumber").setValue("305");
    await w.get("#c-address").setValue("Oak Ave");
    await w.get("form").trigger("submit");

    await vi.waitFor(() =>
      expect(api.createClient).toHaveBeenCalledWith({ fullName: "New Person", roomNumber: "305", address: "Oak Ave" }),
    );
    await vi.waitFor(() => expect(w.text()).toContain("Client added"));
    expect(api.getAdminClients).toHaveBeenCalledTimes(2);
  });

  it("shows an error when adding fails", async () => {
    (api.createClient as any).mockRejectedValue({ response: { data: { detail: "address is required" } } });
    const w = mount(AdminClients, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Margaret Smith"));

    await w.get("#c-fullName").setValue("X");
    await w.get("form").trigger("submit");
    await vi.waitFor(() => expect(w.text()).toContain("address is required"));
  });

  it("falls back to a generic add error", async () => {
    (api.createClient as any).mockRejectedValue(new Error("network"));
    const w = mount(AdminClients, { global: { plugins: [createPinia()] } });
    await vi.waitFor(() => expect(w.text()).toContain("Margaret Smith"));

    await w.get("form").trigger("submit");
    await vi.waitFor(() => expect(w.text()).toContain("Failed to add client"));
  });
});
