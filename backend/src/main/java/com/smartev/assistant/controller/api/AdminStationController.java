package com.smartev.assistant.controller.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.dto.request.StationStatusRequest;
import com.smartev.assistant.dto.response.StationResponse;
import com.smartev.assistant.service.StationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/stations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStationController {
	private final StationService service;
	public AdminStationController(StationService service) { this.service = service; }

	@GetMapping public List<StationResponse> list() { return service.list(); }
	@GetMapping("/{id}") public StationResponse get(@PathVariable Long id) { return service.get(id); }

	@PostMapping
	public ResponseEntity<StationResponse> create(@Valid @RequestBody StationRequest request) {
		StationResponse response = service.create(request);
		return ResponseEntity.created(URI.create("/api/admin/stations/" + response.id())).body(response);
	}

	@PutMapping("/{id}") public StationResponse update(@PathVariable Long id, @Valid @RequestBody StationRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id); return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/status")
	public StationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StationStatusRequest request) {
		return service.updateStatus(id, request);
	}
}
