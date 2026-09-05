package com.smartev.assistant.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.request.RouteRequest;
import com.smartev.assistant.dto.response.RouteResponse;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.security.SecurityRateLimitService;
import com.smartev.assistant.service.RouteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
	private final RouteService service;
	private final SecurityRateLimitService rateLimits;
	public RouteController(RouteService service, SecurityRateLimitService rateLimits) {
		this.service = service;
		this.rateLimits = rateLimits;
	}

	@PostMapping
	ResponseEntity<RouteResponse> compute(Authentication authentication,
			@Valid @RequestBody RouteRequest request) {
		Object principal = authentication.getPrincipal();
		String subject = principal instanceof AppUserPrincipal user ? "id:" + user.id() : "name:" + authentication.getName();
		rateLimits.requireRoutePermit(subject);
		return ResponseEntity.ok(service.compute(request));
	}
}
