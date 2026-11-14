package com.rms.mapper;

import com.rms.dto.AssignmentView;
import com.rms.dto.OpenShiftView;
import com.rms.entity.Assignment;
import com.rms.entity.Client;
import com.rms.entity.OpenShift;
import com.rms.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Projects entities into their wire (view) shape. Kept in one place so date formatting and
 * the derived {@code location} string stay identical across all endpoints, matching the
 * reference backend (start/end rendered as "YYYY-MM-DDTHH:mm:00" in site time).
 */
public final class Views {

    private static final DateTimeFormatter LOCAL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm':00'");

    private Views() {
    }

    /** Render a date-time as the API's local string, seconds always normalised to :00. */
    public static String formatLocal(LocalDateTime dt) {
        return dt == null ? null : dt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).format(LOCAL);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String locationOf(Client client) {
        return client == null ? "" : client.location();
    }

    /** Build an assignment view. The worker and client associations must be loaded (else names are blank). */
    public static AssignmentView assignment(Assignment a) {
        User worker = a.getWorker();
        Client client = a.getClient();
        return new AssignmentView(
                a.getId(),
                a.getWorkerId(),
                worker == null ? "" : nvl(worker.getFullName()),
                a.getClientId(),
                client == null ? "" : nvl(client.getFullName()),
                client == null ? "" : nvl(client.getRoomNumber()),
                client == null ? "" : nvl(client.getAddress()),
                locationOf(client),
                formatLocal(a.getScheduledStart()),
                formatLocal(a.end()),
                a.getDurationMinutes(),
                a.getStatus(),
                a.getNotes());
    }

    /** Build an open-shift view. The client (and, when taken, the taker) must be loaded. */
    public static OpenShiftView openShift(OpenShift s) {
        Client client = s.getClient();
        User taker = s.getTakenBy();
        return new OpenShiftView(
                s.getId(),
                s.getClientId(),
                client == null ? "" : nvl(client.getFullName()),
                client == null ? "" : nvl(client.getRoomNumber()),
                client == null ? "" : nvl(client.getAddress()),
                locationOf(client),
                formatLocal(s.getScheduledStart()),
                formatLocal(s.end()),
                s.getDurationMinutes(),
                s.getStatus(),
                s.getNotes(),
                s.getTakenById(),
                taker == null ? "" : nvl(taker.getFullName()),
                formatLocal(s.getTakenAt()),
                s.getAssignmentId());
    }

    /**
     * Build an open-shift view with the client and taker name supplied directly. Used by the
     * claim/assign flow, where the caller knows the values without re-reading associations.
     */
    public static OpenShiftView openShift(OpenShift s, Client client, String takenByName) {
        return new OpenShiftView(
                s.getId(),
                s.getClientId(),
                client == null ? "" : nvl(client.getFullName()),
                client == null ? "" : nvl(client.getRoomNumber()),
                client == null ? "" : nvl(client.getAddress()),
                locationOf(client),
                formatLocal(s.getScheduledStart()),
                formatLocal(s.end()),
                s.getDurationMinutes(),
                s.getStatus(),
                s.getNotes(),
                s.getTakenById(),
                takenByName == null ? "" : takenByName,
                formatLocal(s.getTakenAt()),
                s.getAssignmentId());
    }
}
