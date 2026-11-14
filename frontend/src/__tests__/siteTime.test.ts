import { describe, it, expect } from "vitest";
import { timeLabel, dateLabel, toIsoDate, formatDateLabel, todayIso } from "../utils/siteTime";

describe("siteTime", () => {
  describe("timeLabel", () => {
    it("extracts HH:mm from ISO string", () => {
      expect(timeLabel("2026-11-02T08:30:00")).toBe("08:30");
    });

    it("handles midnight", () => {
      expect(timeLabel("2026-01-01T00:00:00")).toBe("00:00");
    });
  });

  describe("dateLabel", () => {
    it("extracts yyyy-MM-dd from ISO string", () => {
      expect(dateLabel("2026-11-02T08:30:00")).toBe("2026-11-02");
    });
  });

  describe("toIsoDate", () => {
    it("formats a Date to yyyy-MM-dd", () => {
      const d = new Date(2026, 10, 2);
      expect(toIsoDate(d)).toBe("2026-11-02");
    });

    it("pads single-digit month and day", () => {
      const d = new Date(2026, 0, 5);
      expect(toIsoDate(d)).toBe("2026-01-05");
    });
  });

  describe("formatDateLabel", () => {
    it("formats a full human-readable label", () => {
      const d = new Date(2026, 10, 2); // Nov 2, 2026 is Monday
      expect(formatDateLabel(d)).toBe("Mon 02 Nov 2026");
    });
  });

  describe("todayIso", () => {
    it("returns today in yyyy-MM-dd format", () => {
      const result = todayIso();
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });
  });
});
