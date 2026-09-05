package com.smartev.assistant.controller.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.request.ReportResolutionRequest;
import com.smartev.assistant.dto.response.ReportResponse;
import com.smartev.assistant.enums.ReportStatus;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.service.ReportService;

import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin/reports") @PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {
	private final ReportService service;
	public AdminReportController(ReportService service) { this.service = service; }
	@GetMapping public List<ReportResponse> list(@RequestParam(required = false) ReportStatus status) { return service.adminQueue(status); }
	@PatchMapping("/{id}") public ReportResponse resolve(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal admin, @Valid @RequestBody ReportResolutionRequest request) {
		return service.resolve(id, admin.id(), request.status());
	}
}
