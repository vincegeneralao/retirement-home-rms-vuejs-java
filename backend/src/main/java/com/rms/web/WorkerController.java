package com.rms.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rms.config.AppProperties;
import com.rms.dto.AssignmentView;
import com.rms.dto.AuthResponse;
import com.rms.dto.ChangePasswordRequest;
import com.rms.dto.OpenShiftView;
import com.rms.dto.UpdateStatusRequest;
import com.rms.security.AuthPrincipal;
import com.rms.security.CurrentUser;
import com.rms.service.AssignmentQueryService;
import com.rms.service.AssignmentStatusService;
import com.rms.service.AuthService;
import com.rms.service.OpenShiftService;
import com.rms.util.TimeUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/worker")
@Tag(name = "Worker")
public class WorkerController {

    private final AssignmentQueryService assignmentQueryService;
    private final AssignmentStatusService assignmentStatusService;
    private final OpenShiftService openShiftService;
    private final AuthService authService;
    private final AppProperties props;

    public WorkerController(AssignmentQueryService assignmentQueryService,
                           AssignmentStatusService assignmentStatusService,
                           OpenShiftService openShiftService, AuthService authService,
                           AppProperties props) {
        this.assignmentQueryService = assignmentQueryService;
        this.assignmentStatusService = assignmentStatusService;
        this.openShiftService = openShiftService;
        this.authService = authService;
        this.props = props;
    }

    @GetMapping("/assignments/today")
    @Operation(summary = "My shifts on a day")
    public List<AssignmentView> today(@CurrentUser AuthPrincipal me,
                                      @RequestParam(required = false) String date) {
        return assignmentQueryService.workerDay(me.userId(), TimeUtil.resolveDay(date, props.siteZone()));
    }

    @GetMapping("/assignments/upcoming")
    @Operation(summary = "My upcoming shifts")
    public List<AssignmentView> upcoming(@CurrentUser AuthPrincipal me) {
        return assignmentQueryService.workerUpcoming(me.userId());
    }

    @GetMapping("/open-shifts")
    @Operation(summary = "Open shifts I can claim")
    public List<OpenShiftView> openShifts() {
        return openShiftService.listOpenForWorker();
    }

    @PostMapping("/open-shifts/{openShiftId}/claim")
    @Operation(summary = "Claim an open shift")
    public OpenShiftView claim(@CurrentUser AuthPrincipal me, @PathVariable Long openShiftId) {
        return openShiftService.claim(me.userId(), openShiftId);
    }

    @PatchMapping("/assignments/{assignmentId}/status")
    @Operation(summary = "Update the status of one of my shifts")
    public AssignmentView updateStatus(@CurrentUser AuthPrincipal me,
                                       @PathVariable Long assignmentId,
                                       @RequestBody UpdateStatusRequest request) {
        return assignmentStatusService.updateStatus(me.userId(), assignmentId, request.status());
    }

    @PostMapping("/password/change")
    @Operation(summary = "Replace my password after an admin reset")
    public AuthResponse changePassword(@CurrentUser AuthPrincipal me,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(me.userId(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/password/keep")
    @Operation(summary = "Keep the blank password an admin reset me to")
    public AuthResponse keepPassword(@CurrentUser AuthPrincipal me) {
        return authService.keepCurrentPassword(me.userId());
    }
}
