package com.rms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rms.config.AppProperties;
import com.rms.dto.AssignmentView;
import com.rms.mapper.Views;
import com.rms.repository.AssignmentRepository;
import com.rms.util.TimeUtil;

@Service
public class AssignmentQueryService {

    /** Cap on rows returned by the worker "upcoming" feed. */
    public static final int UPCOMING_LIMIT = 20;

    private final AssignmentRepository assignmentRepository;
    private final AppProperties props;

    public AssignmentQueryService(AssignmentRepository assignmentRepository, AppProperties props) {
        this.assignmentRepository = assignmentRepository;
        this.props = props;
    }

    /** A worker's shifts on a given day. */
    @Transactional(readOnly = true)
    public List<AssignmentView> workerDay(Long workerId, LocalDate day) {
        TimeUtil.DayBounds b = TimeUtil.dayBounds(day);
        return assignmentRepository.findForWorker(workerId, b.start(), b.end()).stream()
                .map(Views::assignment).toList();
    }

    /** A worker's next upcoming shifts, soonest first. */
    @Transactional(readOnly = true)
    public List<AssignmentView> workerUpcoming(Long workerId) {
        LocalDateTime now = LocalDateTime.now(props.siteZone());
        return assignmentRepository.findUpcomingForWorker(workerId, now).stream()
                .limit(UPCOMING_LIMIT).map(Views::assignment).toList();
    }

    /** Every shift scheduled on a given day (admin board). */
    @Transactional(readOnly = true)
    public List<AssignmentView> adminDay(LocalDate day) {
        TimeUtil.DayBounds b = TimeUtil.dayBounds(day);
        return assignmentRepository.findInRange(b.start(), b.end()).stream()
                .map(Views::assignment).toList();
    }
}
