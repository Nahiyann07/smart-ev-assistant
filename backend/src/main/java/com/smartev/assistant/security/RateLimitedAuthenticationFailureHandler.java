package com.smartev.assistant.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitedAuthenticationFailureHandler implements AuthenticationFailureHandler {
	private final SecurityRateLimitService rateLimits;

	public RateLimitedAuthenticationFailureHandler(SecurityRateLimitService rateLimits) {
		this.rateLimits = rateLimits;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		rateLimits.recordLoginFailure(request.getRemoteAddr(), request.getParameter("email"));
		RelativeRedirects.send(response, "/login?error");
	}
}
