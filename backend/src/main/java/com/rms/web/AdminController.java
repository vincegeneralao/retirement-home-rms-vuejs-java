package com.rms.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rms.config.AppProperties;
import com.rms.dto.AssignOpenShiftRequest;
import com.rms.dto.AssignmentView;
import com.rms.dto.AvailabilityRequest;
import com.rms.dto.AvailabilityView;
import com.rms.dto.ClientView;
import com.rms.dto.CreateClientRequest;
import com.rms.dto.CreateOpenShiftRequest;
import com.rms.dto.CreateWorkerRequest;
import com.rms.dto.OpenShiftView;
import com.rms.dto.WorkerView;
import com.rms.service.DailyReportService;
import com.rms.service.AssignmentQueryService;
import com.rms.service.DirectoryService;
import com.rms.service.OpenShiftService;
import com.rms.util.TimeUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminController {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DirectoryService directoryService;
    private final AssignmentQueryService assignmentQueryService;
    private final OpenShiftService openShiftService;
    private final DailyReportService dailyReportService;
    private final AppProperties props;

    public AdminController(DirectoryService directoryService, AssignmentQueryService assignmentQueryService,
                           OpenShiftService openShiftService, DailyReportService dailyReportService,
                           AppProperties props) {
        this.directoryService = directoryService;
        this.assignmentQueryService = assignmentQueryService;
        this.openShiftService = openShiftService;
        this.dailyReportService = dailyReportService;
        this.props = props;
    }

    // ---- Workers ---------------------------------------------------------------------------

    @GetMapping("/workers")
    @Operation(summary = "List support workers with today's load and availability")
    public List<WorkerView> workers(@RequestParam(required = false) String date) {
        return directoryService.listWorkers(TimeUtil.resolveDay(date, props.siteZone()));
    }

    @PostMapping("/workers")
    @Operation(summary = "Create a support worker")
    public ResponseEntity<WorkerView> createWorker(@Valid @RequestBody CreateWorkerRequest request) {
        return ResponseEntity.status(201).body(directoryService.createWorker(request));
    }

    @DeleteMapping("/workers/{workerId}")
    @Operation(summary = "Deactivate a support worker")
    public ResponseEntity<Void> deactivateWorker(@PathVariable Long workerId) {
        directoryService.deactivateWorker(workerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/workers/{workerId}/reset-password")
    @Operation(summary = "Reset a support worker's password to blank")
    public ResponseEntity<Void> resetWorkerPassword(@PathVariable Long workerId) {
        directoryService.resetWorkerPassword(workerId);
        return ResponseEntity.noContent().build();
    }

    // ---- Availability ----------------------------------------------------------------------

    @GetMapping("/workers/{workerId}/availability")
    @Operation(summary = "List a worker's availability windows")
    public List<AvailabilityView> availability(@PathVariable Long workerId) {
        return directoryService.listAvailability(workerId);
    }

    @PostMapping("/workers/{workerId}/availability")
    @Operation(summary = "Add an availability window")
    public ResponseEntity<AvailabilityView> addAvailability(@PathVariable Long workerId,
                                                            @Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.status(201).body(directoryService.addAvailability(workerId, request));
    }

    @DeleteMapping("/workers/{workerId}/availability/{availabilityId}")
    @Operation(summary = "Remove an availability window")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long workerId, @PathVariable Long availabilityId) {
        directoryService.deleteAvailability(workerId, availabilityId);
        return ResponseEntity.noContent().build();
    }

    // ---- Clients ---------------------------------------------------------------------------

    @GetMapping("/clients")
    @Operation(summary = "List clients")
    public List<ClientView> clients() {
        return directoryService.listClients();
    }

    @PostMapping("/clients")
    @Operation(summary = "Create a client")
    public ResponseEntity<ClientView> createClient(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(201).body(directoryService.createClient(request));
    }

    // ---- Assignments -----------------------------------------------------------------------

    @GetMapping("/assignments/today")
    @Operation(summary = "All shifts scheduled on a day")
    public List<AssignmentView> today(@RequestParam(required = false) String date) {
        return assignmentQueryService.adminDay(TimeUtil.resolveDay(date, props.siteZone()));
    }

    // ---- Open shifts -----------------------------------------------------------------------

    @PostMapping("/open-shifts")
    @Operation(summary = "Publish an open shift")
    public ResponseEntity<OpenShiftView> createOpenShift(@Valid @RequestBody CreateOpenShiftRequest request) {
        return ResponseEntity.status(201).body(openShiftService.create(request));
    }

    @GetMapping("/open-shifts")
    @Operation(summary = "List all open shifts")
    public List<OpenShiftView> openShifts() {
        return openShiftService.listAll();
    }

    @PostMapping("/open-shifts/{openShiftId}/assign")
    @Operation(summary = "Assign an open shift to a worker")
    public OpenShiftView assign(@PathVariable Long openShiftId, @RequestBody AssignOpenShiftRequest request) {
        return openShiftService.assignByAdmin(openShiftId, request.workerId());
    }

    @DeleteMapping("/open-shifts/{openShiftId}")
    @Operation(summary = "Cancel an open shift")
    public OpenShiftView cancel(@PathVariable Long openShiftId) {
        return openShiftService.cancel(openShiftId);
    }

    // ---- Reports ---------------------------------------------------------------------------

    @GetMapping("/reports/daily/download")
    @Operation(summary = "Download the daily schedule as an XLSX workbook")
    public ResponseEntity<byte[]> dailyReport(@RequestParam(required = false) String date) {
        LocalDate day = TimeUtil.resolveDay(date, props.siteZone());
        byte[] content = dailyReportService.generate(day);
        String filename = "daily-schedule-" + day + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .body(content);
    }
}
