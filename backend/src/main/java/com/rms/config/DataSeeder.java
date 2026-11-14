package com.rms.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rms.entity.Assignment;
import com.rms.entity.AssignmentStatus;
import com.rms.entity.Client;
import com.rms.entity.OpenShift;
import com.rms.entity.OpenShiftStatus;
import com.rms.entity.Role;
import com.rms.entity.User;
import com.rms.repository.AssignmentRepository;
import com.rms.repository.ClientRepository;
import com.rms.repository.OpenShiftRepository;
import com.rms.repository.UserRepository;

/**
 * Seeds the demo dataset on first startup (only when the users table is empty), mirroring the
 * reference project: 1 admin, 2 workers, 3 clients, 4 shifts today and 3 open shifts.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AssignmentRepository assignmentRepository;
    private final OpenShiftRepository openShiftRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DataSeeder(UserRepository userRepository, ClientRepository clientRepository,
                      AssignmentRepository assignmentRepository, OpenShiftRepository openShiftRepository,
                      PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.assignmentRepository = assignmentRepository;
        this.openShiftRepository = openShiftRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .username("admin").fullName("Site Administrator")
                .passwordHash(passwordEncoder.encode("Admin#2026")).role(Role.ADMIN).enabled(true).build());
        User grace = userRepository.save(User.builder()
                .username("grace.loke").fullName("Grace Loke")
                .passwordHash(passwordEncoder.encode("Worker#2026")).role(Role.WORKER).enabled(true).build());
        User priya = userRepository.save(User.builder()
                .username("priya.raman").fullName("Priya Raman")
                .passwordHash(passwordEncoder.encode("Worker#2026")).role(Role.WORKER).enabled(true).build());
        log.info("Seeded users: {} (admin), {} & {} (workers)",
                admin.getUsername(), grace.getUsername(), priya.getUsername());

        Client eleanor = clientRepository.save(Client.builder()
                .fullName("Eleanor Whitfield").roomNumber("12B").address("12 Jacaranda Court").active(true).build());
        Client harold = clientRepository.save(Client.builder()
                .fullName("Harold Nguyen").roomNumber("07A").address("5 Waratah Lane").active(true).build());
        Client margaret = clientRepository.save(Client.builder()
                .fullName("Margaret Osei").roomNumber("21C").address("88 Flinders Way").active(true).build());

        LocalDate today = LocalDate.now(props.siteZone());

        assignmentRepository.save(assign(grace.getId(), eleanor.getId(), today.atTime(9, 0), 60,
                "Medication reminder and mobility support."));
        assignmentRepository.save(assign(grace.getId(), harold.getId(), today.atTime(11, 30), 45,
                "Companion visit and medication check."));
        assignmentRepository.save(assign(priya.getId(), margaret.getId(), today.atTime(14, 0), 90,
                "Post-lunch assistance and physiotherapy escort."));
        assignmentRepository.save(assign(priya.getId(), eleanor.getId(), today.atTime(16, 0), 30,
                "Evening wellbeing check."));

        openShiftRepository.save(openShift(harold.getId(), today.atTime(18, 30), 60, "Overnight readiness check."));
        openShiftRepository.save(openShift(margaret.getId(), today.plusDays(1).atTime(8, 0), 120,
                "Weekend morning support."));
        openShiftRepository.save(openShift(eleanor.getId(), today.plusDays(2).atTime(13, 0), 60,
                "Afternoon companion shift."));

        log.info("Seeded {} clients, 4 assignments today and 3 open shifts.", clientRepository.count());
    }

    private static Assignment assign(Long workerId, Long clientId, LocalDateTime start, int minutes, String notes) {
        return Assignment.builder()
                .workerId(workerId).clientId(clientId).scheduledStart(start)
                .durationMinutes(minutes).status(AssignmentStatus.SCHEDULED).notes(notes).build();
    }

    private static OpenShift openShift(Long clientId, LocalDateTime start, int minutes, String notes) {
        return OpenShift.builder()
                .clientId(clientId).scheduledStart(start).durationMinutes(minutes)
                .notes(notes).status(OpenShiftStatus.OPEN).build();
    }
}
