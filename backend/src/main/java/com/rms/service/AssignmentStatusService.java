package com.rms.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rms.dto.AssignmentView;
import com.rms.entity.Assignment;
import com.rms.entity.AssignmentStatus;
import com.rms.error.ApiErrors;
import com.rms.mapper.Views;
import com.rms.repository.AssignmentRepository;

@Service
public class AssignmentStatusService {

    private static final Set<String> ALLOWED = Set.of("SCHEDULED", "IN_PROGRESS", "COMPLETED");

    private final AssignmentRepository assignmentRepository;

    public AssignmentStatusService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * A worker moves one of their own shifts through its lifecycle. Rejects shifts belonging to
     * another worker (403), cancelled shifts (409) and unknown statuses (400, VALIDATION code).
     */
    @Transactional
    public AssignmentView updateStatus(Long workerId, Long assignmentId, String status) {
        Assignment assignment = assignmentRepository.findWithRefsById(assignmentId)
                .orElseThrow(() -> ApiErrors.assignmentNotFound(assignmentId));

        if (!assignment.getWorkerId().equals(workerId)) {
            throw ApiErrors.notYourAssignment();
        }
        if (assignment.getStatus() == AssignmentStatus.CANCELLED) {
            throw ApiErrors.shiftCancelled();
        }
        if (status == null || !ALLOWED.contains(status)) {
            throw ApiErrors.validation("status must be one of: SCHEDULED, IN_PROGRESS, COMPLETED");
        }

        assignment.setStatus(AssignmentStatus.valueOf(status));
        return Views.assignment(assignment);
    }
}
