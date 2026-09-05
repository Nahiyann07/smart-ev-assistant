package com.smartev.assistant.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class AuthPageController {
	@GetMapping("/") String landing() { return "landing"; }

	@GetMapping("/login")
	String login() { return "auth/login"; }

	@GetMapping("/dashboard")
	String dashboard(Authentication authentication) {
		boolean admin = authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		return admin ? "redirect:/admin" : "user/dashboard";
	}

	@GetMapping("/admin")
	String admin() { return "admin/dashboard"; }

	@GetMapping("/stations") String stations() { return "stations/list"; }
	@GetMapping("/favourites") String favourites() { return "user/favourites"; }
	@GetMapping("/reports") String reports() { return "user/reports"; }
	@GetMapping("/recommendations") String recommendations() { return "user/recommendations"; }
	@GetMapping("/profile") String profile() { return "user/profile"; }
	@GetMapping("/admin/stations") String adminStations() { return "admin/stations"; }
	@GetMapping("/admin/reports") String adminReports() { return "admin/reports"; }
	@GetMapping("/admin/users") String adminUsers() { return "admin/users"; }
}
