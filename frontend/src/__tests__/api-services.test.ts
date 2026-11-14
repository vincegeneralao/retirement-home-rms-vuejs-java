import { describe, it, expect, vi, beforeEach } from "vitest";

const { mockGet, mockPost, mockDelete, mockPatch } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockDelete: vi.fn(),
  mockPatch: vi.fn(),
}));

vi.mock("../api/client", () => ({
  httpClient: {
    get: mockGet,
    post: mockPost,
    delete: mockDelete,
    patch: mockPatch,
  },
}));

import * as services from "../api/services";

describe("api/services", () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPost.mockReset();
    mockDelete.mockReset();
    mockPatch.mockReset();
  });

  it("login calls POST /auth/login", async () => {
    mockPost.mockResolvedValue({ data: { token: "t" } });
    const result = await services.login("user", "pass");
    expect(mockPost).toHaveBeenCalledWith("/auth/login", { username: "user", password: "pass" });
    expect(result).toEqual({ token: "t" });
  });

  it("getWorkerAssignments calls GET /worker/assignments/today", async () => {
    mockGet.mockResolvedValue({ data: [] });
    await services.getWorkerAssignments("2026-11-02");
    expect(mockGet).toHaveBeenCalledWith("/worker/assignments/today", { params: { date: "2026-11-02" } });
  });

  it("getAdminWorkers calls GET /admin/workers", async () => {
    mockGet.mockResolvedValue({ data: [] });
    await services.getAdminWorkers();
    expect(mockGet).toHaveBeenCalledWith("/admin/workers", { params: {} });
  });

  it("getAdminClients calls GET /admin/clients", async () => {
    mockGet.mockResolvedValue({ data: [] });
    await services.getAdminClients();
    expect(mockGet).toHaveBeenCalledWith("/admin/clients");
  });

  it("getAdminAssignments calls GET /admin/assignments/today", async () => {
    mockGet.mockResolvedValue({ data: [] });
    await services.getAdminAssignments("2026-11-02");
    expect(mockGet).toHaveBeenCalledWith("/admin/assignments/today", { params: { date: "2026-11-02" } });
  });

  it("createWorker calls POST /admin/workers", async () => {
    const body = { username: "u", fullName: "U", password: "p" };
    mockPost.mockResolvedValue({ data: { id: 1 } });
    await services.createWorker(body);
    expect(mockPost).toHaveBeenCalledWith("/admin/workers", body);
  });

  it("deactivateWorker calls DELETE /admin/workers/:id", async () => {
    mockDelete.mockResolvedValue({ data: {} });
    await services.deactivateWorker(5);
    expect(mockDelete).toHaveBeenCalledWith("/admin/workers/5");
  });

  it("getWorkerAvailability calls GET", async () => {
    mockGet.mockResolvedValue({ data: [] });
    await services.getWorkerAvailability(5);
    expect(mockGet).toHaveBeenCalledWith("/admin/workers/5/availability");
  });

  it("addWorkerAvailability calls POST", async () => {
    mockPost.mockResolvedValue({ data: { id: 1 } });
    await services.addWorkerAvailability(5, { dayOfWeek: "MONDAY", startTime: "08:00", endTime: "12:00" });
    expect(mockPost).toHaveBeenCalledWith("/admin/workers/5/availability", { dayOfWeek: "MONDAY", startTime: "08:00", endTime: "12:00" });
  });

  it("removeWorkerAvailability calls DELETE", async () => {
    mockDelete.mockResolvedValue({ data: {} });
    await services.removeWorkerAvailability(5, 10);
    expect(mockDelete).toHaveBeenCalledWith("/admin/workers/5/availability/10");
  });

  it("createClient calls POST /admin/clients", async () => {
    mockPost.mockResolvedValue({ data: { id: 2 } });
    await services.createClient({ fullName: "New Client", roomNumber: "305", address: "Oak Ave" });
    expect(mockPost).toHaveBeenCalledWith("/admin/clients", { fullName: "New Client", roomNumber: "305", address: "Oak Ave" });
  });

  it("downloadDailyReport requests a blob and parses the filename", async () => {
    mockGet.mockResolvedValue({
      data: new Blob(["x"]),
      headers: {
        "content-disposition":
          'attachment; filename="daily-report-2026-11-02.xlsx"; filename*=UTF-8\'\'daily-report-2026-11-02.xlsx',
      },
    });
    const result = await services.downloadDailyReport("2026-11-02");
    expect(mockGet).toHaveBeenCalledWith("/admin/reports/daily/download", {
      params: { date: "2026-11-02" },
      responseType: "blob",
    });
    expect(result.filename).toBe("daily-report-2026-11-02.xlsx");
    expect(result.blob).toBeInstanceOf(Blob);
  });

  it("downloadDailyReport parses plain filename header", async () => {
    mockGet.mockResolvedValue({ data: new Blob(["x"]), headers: { "content-disposition": 'attachment; filename="plain.xlsx"' } });
    const result = await services.downloadDailyReport();
    expect(result.filename).toBe("plain.xlsx");
  });

  it("downloadDailyReport falls back to a default filename", async () => {
    mockGet.mockResolvedValue({ data: new Blob(["x"]), headers: {} });
    const result = await services.downloadDailyReport();
    expect(mockGet).toHaveBeenCalledWith("/admin/reports/daily/download", { params: {}, responseType: "blob" });
    expect(result.filename).toBe("daily-report.xlsx");
  });

  it("getWorkerUpcoming calls GET /worker/assignments/upcoming", async () => {
    mockGet.mockResolvedValue({ data: [] });
    await services.getWorkerUpcoming();
    expect(mockGet).toHaveBeenCalledWith("/worker/assignments/upcoming");
  });

  it("getOpenShifts calls GET /worker/open-shifts", async () => {
    mockGet.mockResolvedValue({ data: [{ id: 1 }] });
    const result = await services.getOpenShifts();
    expect(mockGet).toHaveBeenCalledWith("/worker/open-shifts");
    expect(result).toEqual([{ id: 1 }]);
  });

  it("claimOpenShift calls POST /worker/open-shifts/:id/claim", async () => {
    mockPost.mockResolvedValue({ data: { id: 5, status: "TAKEN" } });
    const result = await services.claimOpenShift(5);
    expect(mockPost).toHaveBeenCalledWith("/worker/open-shifts/5/claim");
    expect(result).toEqual({ id: 5, status: "TAKEN" });
  });

  it("getAdminOpenShifts calls GET /admin/open-shifts", async () => {
    mockGet.mockResolvedValue({ data: [] });
    await services.getAdminOpenShifts();
    expect(mockGet).toHaveBeenCalledWith("/admin/open-shifts");
  });

  it("createOpenShift calls POST /admin/open-shifts", async () => {
    const body = { clientId: 2, date: "2026-11-05", startTime: "08:00", endTime: "16:00", notes: "n" };
    mockPost.mockResolvedValue({ data: { id: 1 } });
    await services.createOpenShift(body);
    expect(mockPost).toHaveBeenCalledWith("/admin/open-shifts", body);
  });

  it("assignOpenShift calls POST /admin/open-shifts/:id/assign with workerId", async () => {
    mockPost.mockResolvedValue({ data: { id: 5, status: "TAKEN" } });
    await services.assignOpenShift(5, 3);
    expect(mockPost).toHaveBeenCalledWith("/admin/open-shifts/5/assign", { workerId: 3 });
  });

  it("cancelOpenShift calls DELETE /admin/open-shifts/:id", async () => {
    mockDelete.mockResolvedValue({ data: { id: 5, status: "CANCELLED" } });
    const result = await services.cancelOpenShift(5);
    expect(mockDelete).toHaveBeenCalledWith("/admin/open-shifts/5");
    expect(result).toEqual({ id: 5, status: "CANCELLED" });
  });

  it("updateWorkerAssignmentStatus calls PATCH /worker/assignments/:id/status", async () => {
    mockPatch.mockResolvedValue({ data: { id: 7, status: "IN_PROGRESS" } });
    const result = await services.updateWorkerAssignmentStatus(7, "IN_PROGRESS");
    expect(mockPatch).toHaveBeenCalledWith("/worker/assignments/7/status", { status: "IN_PROGRESS" });
    expect(result).toEqual({ id: 7, status: "IN_PROGRESS" });
  });
});
