package com.smartev.assistant.controller.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartev.assistant.dto.request.UserEnabledRequest;
import com.smartev.assistant.dto.response.AdminUserResponse;
import com.smartev.assistant.dto.response.DashboardStatisticsResponse;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.service.AdminService;

import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
	private final AdminService service;
	public AdminDashboardController(AdminService service) { this.service = service; }
	@GetMapping("/dashboard") public DashboardStatisticsResponse dashboard() { return service.dashboard(); }
	@GetMapping("/users") public List<AdminUserResponse> users() { return service.users(); }
	@PatchMapping("/users/{id}/enabled") public AdminUserResponse enabled(@PathVariable Long id,
			@AuthenticationPrincipal AppUserPrincipal admin, @Valid @RequestBody UserEnabledRequest request) {
		return service.setEnabled(id, admin.id(), request.enabled());
	}
}
