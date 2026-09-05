package com.smartev.assistant.controller.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.request.ReportRequest;
import com.smartev.assistant.dto.response.ReportResponse;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.service.ReportService;

import jakarta.validation.Valid;

@RestController
public class ReportController {
	private final ReportService service;
	public ReportController(ReportService service) { this.service = service; }
	@PostMapping("/api/stations/{stationId}/reports")
	public ResponseEntity<ReportResponse> submit(@PathVariable Long stationId,
			@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody ReportRequest request) {
		ReportResponse response = service.submit(stationId, user.id(), request);
		return ResponseEntity.created(URI.create("/api/users/me/reports/" + response.id())).body(response);
	}
	@GetMapping("/api/users/me/reports")
	public List<ReportResponse> history(@AuthenticationPrincipal AppUserPrincipal user) { return service.userHistory(user.id()); }
}
