package com.schwab.notification.api;

import com.schwab.notification.api.dto.*;
import com.schwab.notification.service.NotificationStatusService;
import com.schwab.notification.service.NotificationSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** The two required APIs: submission and status (requirements 4.1, 4.2). */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationSubmissionService submissionService;
    private final NotificationStatusService statusService;

    public NotificationController(NotificationSubmissionService submissionService,
                                   NotificationStatusService statusService) {
        this.submissionService = submissionService;
        this.statusService = statusService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> submit(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(submissionService.submit(request));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<NotificationStatusResponse> status(@PathVariable UUID id) {
        return ResponseEntity.ok(statusService.getStatus(id));
    }
}
