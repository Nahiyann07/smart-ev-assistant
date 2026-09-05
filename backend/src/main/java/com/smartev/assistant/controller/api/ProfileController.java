package com.smartev.assistant.controller.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartev.assistant.dto.response.ProfileResponse;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.service.ProfileService;

@RestController
public class ProfileController {
	private final ProfileService service;
	public ProfileController(ProfileService service) { this.service = service; }
	@GetMapping("/api/users/me/profile")
	public ProfileResponse profile(@AuthenticationPrincipal AppUserPrincipal user) { return service.get(user.id()); }
}
