package com.rms.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rms.config.AppProperties;
import com.rms.dto.CreateOpenShiftRequest;
import com.rms.dto.OpenShiftView;
import com.rms.entity.Assignment;
import com.rms.entity.AssignmentStatus;
import com.rms.entity.Client;
import com.rms.entity.OpenShift;
import com.rms.entity.OpenShiftStatus;
import com.rms.entity.Role;
import com.rms.entity.User;
import com.rms.error.ApiErrors;
import com.rms.mapper.Views;
import com.rms.repository.AssignmentRepository;
import com.rms.repository.ClientRepository;
import com.rms.repository.OpenShiftRepository;
import com.rms.repository.UserRepository;

@Service
public class OpenShiftService {

    private final OpenShiftRepository openShiftRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AppProperties props;

    public OpenShiftService(OpenShiftRepository openShiftRepository, AssignmentRepository assignmentRepository,
                            ClientRepository clientRepository, UserRepository userRepository, AppProperties props) {
        this.openShiftRepository = openShiftRepository;
        this.assignmentRepository = assignmentRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.props = props;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(props.siteZone());
    }

    // ---- Admin: publish / board / cancel ---------------------------------------------------

    @Transactional
    public OpenShiftView create(CreateOpenShiftRequest req) {
        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> ApiErrors.clientNotFound(req.clientId()));

        LocalDate day = LocalDate.parse(req.date());
        LocalTime start = LocalTime.parse(req.startTime());
        LocalTime end = LocalTime.parse(req.endTime());
        LocalDateTime startAt = LocalDateTime.of(day, start);
        LocalDateTime endAt = LocalDateTime.of(day, end);
        if (!endAt.isAfter(startAt)) {
            throw ApiErrors.invalidTimeRange();
        }

        OpenShift shift = OpenShift.builder()
                .clientId(client.getId())
                .scheduledStart(startAt)
                .durationMinutes((int) Duration.between(startAt, endAt).toMinutes())
                .notes(req.notes() == null || req.notes().isBlank() ? null : req.notes().trim())
                .status(OpenShiftStatus.OPEN)
                .build();
        openShiftRepository.save(shift);
        return Views.openShift(shift, client, null);
    }

    @Transactional(readOnly = true)
    public List<OpenShiftView> listAll() {
        return openShiftRepository.findAllJoined().stream().map(Views::openShift).toList();
    }

    @Transactional
    public OpenShiftView cancel(Long openShiftId) {
        OpenShift shift = openShiftRepository.findWithRefsById(openShiftId)
                .orElseThrow(() -> ApiErrors.openShiftNotFound(openShiftId));
        shift.setStatus(OpenShiftStatus.CANCELLED);
        return Views.openShift(shift);
    }

    // ---- Worker: board ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OpenShiftView> listOpenForWorker() {
        return openShiftRepository.findOpenUpcoming(OpenShiftStatus.OPEN, now()).stream()
                .map(Views::openShift).toList();
    }

    // ---- Claim / assign --------------------------------------------------------------------

    /** A worker claims an open shift for themselves. */
    @Transactional
    public OpenShiftView claim(Long workerId, Long openShiftId) {
        return take(workerId, openShiftId, userRepository.findById(workerId).orElse(null));
    }

    /** An administrator assigns an open shift to a chosen worker. */
    @Transactional
    public OpenShiftView assignByAdmin(Long openShiftId, Long workerId) {
        if (workerId == null) {
            throw ApiErrors.validation("workerId is required");
        }
        User worker = userRepository.findById(workerId).orElse(null);
        return take(workerId, openShiftId, worker);
    }

    /**
     * Shared claim/assign path. The status flip is a single conditional UPDATE so two racing
     * claims cannot both succeed; the loser (zero rows updated) is told the shift is taken. An
     * overlap with the worker's existing shifts then aborts the whole transaction, restoring the
     * slot to OPEN.
     */
    private OpenShiftView take(Long workerId, Long openShiftId, User worker) {
        if (worker == null || worker.getRole() != Role.WORKER || !worker.isEnabled()) {
            throw ApiErrors.invalidWorker();
        }

        LocalDateTime now = now();
        int updated = openShiftRepository.claim(
                openShiftId, workerId, now, OpenShiftStatus.OPEN, OpenShiftStatus.TAKEN);
        if (updated == 0) {
            boolean exists = openShiftRepository.existsById(openShiftId);
            throw exists ? ApiErrors.shiftAlreadyTaken() : ApiErrors.openShiftNotFound(openShiftId);
        }

        OpenShift shift = openShiftRepository.findWithRefsById(openShiftId)
                .orElseThrow(() -> ApiErrors.openShiftNotFound(openShiftId));

        rejectOverlap(workerId, shift);

        Assignment assignment = Assignment.builder()
                .workerId(workerId)
                .clientId(shift.getClientId())
                .scheduledStart(shift.getScheduledStart())
                .durationMinutes(shift.getDurationMinutes())
                .status(AssignmentStatus.SCHEDULED)
                .notes(shift.getNotes())
                .build();
        assignmentRepository.save(assignment);

        shift.setAssignmentId(assignment.getId());

        return Views.openShift(shift, shift.getClient(), worker.getFullName());
    }

    /** Aborts if the shift window overlaps any existing assignment for the worker that day. */
    private void rejectOverlap(Long workerId, OpenShift shift) {
        LocalDateTime start = shift.getScheduledStart();
        LocalDateTime end = shift.end();
        boolean overlaps = assignmentRepository
                .findByWorkerIdAndScheduledStartBetween(workerId, start.minusDays(1), end.plusDays(1))
                .stream()
                .anyMatch(a -> a.getScheduledStart().isBefore(end) && a.end().isAfter(start));
        if (overlaps) {
            throw ApiErrors.shiftOverlap("This shift overlaps an existing assignment starting "
                    + Views.formatLocal(start));
        }
    }
}
