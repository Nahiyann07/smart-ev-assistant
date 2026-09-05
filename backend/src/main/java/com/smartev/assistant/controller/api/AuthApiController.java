package com.smartev.assistant.controller.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.request.RegistrationRequest;
import com.smartev.assistant.dto.response.RegistrationResponse;
import com.smartev.assistant.dto.response.CurrentUserResponse;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.security.SecurityRateLimitService;
import com.smartev.assistant.service.RegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

	private final RegistrationService registrationService;
	private final SecurityRateLimitService rateLimits;

	public AuthApiController(RegistrationService registrationService, SecurityRateLimitService rateLimits) {
		this.registrationService = registrationService;
		this.rateLimits = rateLimits;
	}

	@PostMapping("/register")
	public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request,
			HttpServletRequest servletRequest) {
		rateLimits.requireRegistrationPermit(servletRequest.getRemoteAddr());
		RegistrationResponse response = registrationService.register(request);
		return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
	}

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return new CurrentUserResponse(principal.id(), principal.name(), principal.username(), principal.role());
	}
}
