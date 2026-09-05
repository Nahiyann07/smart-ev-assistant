package com.smartev.assistant.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RoleAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	private final SecurityRateLimitService rateLimits;

	public RoleAwareAuthenticationSuccessHandler(SecurityRateLimitService rateLimits) {
		this.rateLimits = rateLimits;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		boolean admin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		rateLimits.recordLoginSuccess(request.getRemoteAddr(), request.getParameter("email"));
		response.sendRedirect(admin ? "/admin" : "/dashboard");
	}
}
