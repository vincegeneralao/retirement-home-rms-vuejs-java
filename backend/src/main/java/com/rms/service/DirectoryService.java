package com.rms.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rms.dto.AvailabilityRequest;
import com.rms.dto.AvailabilityView;
import com.rms.dto.ClientView;
import com.rms.dto.CreateClientRequest;
import com.rms.dto.CreateWorkerRequest;
import com.rms.dto.WorkerView;
import com.rms.entity.Client;
import com.rms.entity.DayOfWeek;
import com.rms.entity.Role;
import com.rms.entity.User;
import com.rms.entity.WorkerAvailability;
import com.rms.error.ApiErrors;
import com.rms.repository.AssignmentRepository;
import com.rms.repository.ClientRepository;
import com.rms.repository.UserRepository;
import com.rms.repository.WorkerAvailabilityRepository;
import com.rms.util.TimeUtil;

@Service
public class DirectoryService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AssignmentRepository assignmentRepository;
    private final WorkerAvailabilityRepository availabilityRepository;
    private final PasswordEncoder passwordEncoder;

    public DirectoryService(UserRepository userRepository, ClientRepository clientRepository,
                            AssignmentRepository assignmentRepository,
                            WorkerAvailabilityRepository availabilityRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.assignmentRepository = assignmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final Comparator<WorkerAvailability> AVAIL_ORDER =
            Comparator.comparingInt((WorkerAvailability a) -> a.getDayOfWeek().order())
                    .thenComparing(WorkerAvailability::getStartTime);

    // ---- Workers ---------------------------------------------------------------------------

    /** Every support worker with today's shift count and their availability windows. */
    @Transactional(readOnly = true)
    public List<WorkerView> listWorkers(LocalDate day) {
        List<User> workers = userRepository.findByRoleOrderByIdAsc(Role.WORKER);
        TimeUtil.DayBounds bounds = TimeUtil.dayBounds(day);

        List<Long> ids = workers.stream().map(User::getId).toList();
        Map<Long, List<WorkerAvailability>> byWorker = ids.isEmpty() ? Map.of()
                : availabilityRepository.findByWorkerIdInOrderByWorkerIdAscIdAsc(ids).stream()
                        .collect(Collectors.groupingBy(WorkerAvailability::getWorkerId));

        return workers.stream().map(w -> new WorkerView(
                w.getId(),
                w.getFullName(),
                w.getUsername(),
                w.getRole().name(),
                w.isEnabled(),
                assignmentRepository.countByWorkerIdAndScheduledStartBetween(w.getId(), bounds.start(), bounds.end()),
                byWorker.getOrDefault(w.getId(), List.of()).stream().sorted(AVAIL_ORDER)
                        .map(DirectoryService::toAvailabilityView).toList()))
                .toList();
    }

    @Transactional
    public WorkerView createWorker(CreateWorkerRequest req) {
        String username = req.username().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw ApiErrors.duplicateUsername(username);
        }
        User worker = User.builder()
                .username(username)
                .fullName(req.fullName().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.WORKER)
                .enabled(true)
                .build();
        userRepository.save(worker);
        return new WorkerView(worker.getId(), worker.getFullName(), worker.getUsername(),
                worker.getRole().name(), worker.isEnabled(), 0, List.of());
    }

    /** Soft-delete: the worker can no longer sign in but historical shifts are preserved. */
    @Transactional
    public void deactivateWorker(Long workerId) {
        User worker = requireWorker(workerId);
        worker.setEnabled(false);
    }

    private User requireWorker(Long workerId) {
        User user = userRepository.findById(workerId).orElse(null);
        if (user == null || user.getRole() != Role.WORKER) {
            throw ApiErrors.workerNotFound(workerId);
        }
        return user;
    }

    // ---- Clients ---------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ClientView> listClients() {
        return clientRepository.findAllByOrderByIdAsc().stream()
                .map(DirectoryService::toClientView).toList();
    }

    @Transactional
    public ClientView createClient(CreateClientRequest req) {
        Client client = Client.builder()
                .fullName(req.fullName().trim())
                .roomNumber(req.roomNumber().trim())
                .address(req.address().trim())
                .active(true)
                .build();
        clientRepository.save(client);
        return toClientView(client);
    }

    // ---- Availability ----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AvailabilityView> listAvailability(Long workerId) {
        requireWorker(workerId);
        return availabilityRepository.findByWorkerIdOrderByIdAsc(workerId).stream()
                .sorted(AVAIL_ORDER).map(DirectoryService::toAvailabilityView).toList();
    }

    @Transactional
    public AvailabilityView addAvailability(Long workerId, AvailabilityRequest req) {
        requireWorker(workerId);
        String start = req.startTime().trim();
        String end = req.endTime().trim();

        if (end.compareTo(start) <= 0) {
            throw ApiErrors.availabilityConflict(
                    req.dayOfWeek().name() + " " + start + "-" + end + " must end after it starts");
        }

        WorkerAvailability proposed = WorkerAvailability.builder()
                .workerId(workerId).dayOfWeek(req.dayOfWeek()).startTime(start).endTime(end).build();

        for (WorkerAvailability existing :
                availabilityRepository.findByWorkerIdAndDayOfWeek(workerId, req.dayOfWeek())) {
            if (proposed.overlaps(existing)) {
                throw ApiErrors.availabilityConflict(req.dayOfWeek().name() + " " + start + "-" + end
                        + " overlaps existing availability " + existing.getStartTime() + "-" + existing.getEndTime());
            }
        }

        availabilityRepository.save(proposed);
        return toAvailabilityView(proposed);
    }

    @Transactional
    public void deleteAvailability(Long workerId, Long availabilityId) {
        requireWorker(workerId);
        availabilityRepository.findByWorkerIdOrderByIdAsc(workerId).stream()
                .filter(a -> a.getId().equals(availabilityId))
                .findFirst()
                .ifPresent(availabilityRepository::delete);
    }

    // ---- Mappers ---------------------------------------------------------------------------

    private static AvailabilityView toAvailabilityView(WorkerAvailability a) {
        return new AvailabilityView(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime());
    }

    private static ClientView toClientView(Client c) {
        return new ClientView(c.getId(), c.getFullName(), c.getRoomNumber(), c.getAddress(),
                c.location(), c.isActive());
    }
}
