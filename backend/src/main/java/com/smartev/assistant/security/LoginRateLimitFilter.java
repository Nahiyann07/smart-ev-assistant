package com.smartev.assistant.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
	private final SecurityRateLimitService rateLimits;

	public LoginRateLimitFilter(SecurityRateLimitService rateLimits) {
		this.rateLimits = rateLimits;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equalsIgnoreCase(request.getMethod()) || !"/login".equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		FixedWindowRateLimiter.Decision decision = rateLimits.loginDecision(request.getRemoteAddr(), request.getParameter("email"));
		if (!decision.allowed()) {
			response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
			response.sendRedirect("/login?rateLimited");
			return;
		}
		filterChain.doFilter(request, response);
	}
}
