package com.rms.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.rms.dto.AssignmentView;
import com.rms.dto.OpenShiftView;
import com.rms.entity.Assignment;
import com.rms.entity.AssignmentStatus;
import com.rms.entity.Client;
import com.rms.entity.OpenShift;
import com.rms.entity.OpenShiftStatus;
import com.rms.entity.Role;
import com.rms.entity.User;

class ViewsTest {

    @Test
    void formatLocalAlwaysRendersZeroSeconds() {
        LocalDateTime withSeconds = LocalDateTime.of(2026, 3, 5, 9, 7, 37);
        assertThat(Views.formatLocal(withSeconds)).isEqualTo("2026-03-05T09:07:00");
        assertThat(Views.formatLocal(null)).isNull();
    }

    @Test
    void assignmentProjectionDerivesEndAndLocation() {
        User worker = User.builder().id(1L).username("grace.loke").fullName("Grace Loke")
                .role(Role.WORKER).enabled(true).passwordHash("x").build();
        Client client = Client.builder().id(2L).fullName("Eleanor Whitfield")
                .roomNumber("12B").address("12 Jacaranda Court").active(true).build();
        Assignment a = Assignment.builder()
                .id(10L).workerId(1L).clientId(2L).worker(worker).client(client)
                .scheduledStart(LocalDateTime.of(2026, 3, 5, 9, 0))
                .durationMinutes(60).status(AssignmentStatus.SCHEDULED).notes("check").build();

        AssignmentView v = Views.assignment(a);
        assertThat(v.workerName()).isEqualTo("Grace Loke");
        assertThat(v.clientName()).isEqualTo("Eleanor Whitfield");
        assertThat(v.location()).isEqualTo("12B · 12 Jacaranda Court");
        assertThat(v.start()).isEqualTo("2026-03-05T09:00:00");
        assertThat(v.end()).isEqualTo("2026-03-05T10:00:00");
        assertThat(v.durationMinutes()).isEqualTo(60);
        assertThat(v.status()).isEqualTo(AssignmentStatus.SCHEDULED);
    }

    @Test
    void assignmentProjectionToleratesMissingAssociations() {
        Assignment a = Assignment.builder()
                .id(11L).workerId(1L).clientId(2L)
                .scheduledStart(LocalDateTime.of(2026, 3, 5, 9, 0))
                .durationMinutes(30).status(AssignmentStatus.SCHEDULED).build();
        AssignmentView v = Views.assignment(a);
        assertThat(v.workerName()).isEmpty();
        assertThat(v.location()).isEmpty();
    }

    @Test
    void assignmentToleratesNullClientFields() {
        Client client = Client.builder().id(2L).active(true).build();
        Assignment a = Assignment.builder()
                .id(12L).workerId(1L).clientId(2L).client(client)
                .scheduledStart(LocalDateTime.of(2026, 3, 5, 9, 0))
                .durationMinutes(30).status(AssignmentStatus.SCHEDULED).build();
        AssignmentView v = Views.assignment(a);
        assertThat(v.clientName()).isEmpty();
        assertThat(v.roomNumber()).isEmpty();
        assertThat(v.address()).isEmpty();
    }

    @Test
    void openShiftProjectionWithClientAndTaker() {
        Client client = Client.builder().id(2L).fullName("Harold Nguyen")
                .roomNumber("07A").address("5 Waratah Lane").active(true).build();
        User taker = User.builder().id(3L).fullName("Priya Raman").username("priya")
                .role(Role.WORKER).enabled(true).passwordHash("x").build();
        OpenShift s = OpenShift.builder()
                .id(20L).clientId(2L).client(client)
                .scheduledStart(LocalDateTime.of(2026, 3, 5, 14, 0))
                .durationMinutes(90).status(OpenShiftStatus.TAKEN).notes("n")
                .takenById(3L).takenBy(taker)
                .takenAt(LocalDateTime.of(2026, 3, 5, 8, 0))
                .assignmentId(77L).build();

        OpenShiftView v = Views.openShift(s);
        assertThat(v.clientName()).isEqualTo("Harold Nguyen");
        assertThat(v.location()).isEqualTo("07A · 5 Waratah Lane");
        assertThat(v.takenByName()).isEqualTo("Priya Raman");
        assertThat(v.start()).isEqualTo("2026-03-05T14:00:00");
        assertThat(v.end()).isEqualTo("2026-03-05T15:30:00");
        assertThat(v.assignmentId()).isEqualTo(77L);
    }

    @Test
    void openShiftProjectionWithoutClientOrTaker() {
        OpenShift s = OpenShift.builder()
                .id(21L).clientId(2L)
                .scheduledStart(LocalDateTime.of(2026, 3, 5, 14, 0))
                .durationMinutes(60).status(OpenShiftStatus.OPEN).build();
        OpenShiftView v = Views.openShift(s);
        assertThat(v.clientName()).isEmpty();
        assertThat(v.location()).isEmpty();
        assertThat(v.takenByName()).isEmpty();
        assertThat(v.takenAt()).isNull();
    }

    @Test
    void openShiftExplicitArgsTolerateNulls() {
        OpenShift s = OpenShift.builder()
                .id(22L).clientId(2L)
                .scheduledStart(LocalDateTime.of(2026, 3, 5, 14, 0))
                .durationMinutes(45).status(OpenShiftStatus.TAKEN).takenById(9L).build();

        OpenShiftView nullArgs = Views.openShift(s, null, null);
        assertThat(nullArgs.clientName()).isEmpty();
        assertThat(nullArgs.takenByName()).isEmpty();

        Client partial = Client.builder().id(2L).active(true).build();
        OpenShiftView named = Views.openShift(s, partial, "Grace Loke");
        assertThat(named.clientName()).isEmpty();
        assertThat(named.takenByName()).isEqualTo("Grace Loke");
    }
}
