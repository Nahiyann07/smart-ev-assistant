package com.smartev.assistant.controller.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.request.ReviewRequest;
import com.smartev.assistant.dto.response.ReviewResponse;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.service.ReviewService;

import jakarta.validation.Valid;

@RestController
public class ReviewController {
	private final ReviewService service;
	public ReviewController(ReviewService service) { this.service = service; }

	@GetMapping("/api/stations/{stationId}/reviews")
	public List<ReviewResponse> list(@PathVariable Long stationId) { return service.list(stationId); }

	@PostMapping("/api/stations/{stationId}/reviews")
	public ResponseEntity<ReviewResponse> create(@PathVariable Long stationId,
			@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody ReviewRequest request) {
		ReviewResponse response = service.create(stationId, user.id(), request);
		return ResponseEntity.created(URI.create("/api/reviews/" + response.id())).body(response);
	}

	@PutMapping("/api/reviews/{id}")
	public ReviewResponse update(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user,
			@Valid @RequestBody ReviewRequest request) { return service.update(id, user.id(), request); }

	@DeleteMapping("/api/reviews/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
		service.delete(id, user.id()); return ResponseEntity.noContent().build();
	}
}
