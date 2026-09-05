package com.smartev.assistant.security;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.smartev.assistant.config.SecurityRateLimitProperties;
import com.smartev.assistant.exception.TooManyRequestsException;

@Service
public class SecurityRateLimitService {
	private final boolean enabled;
	private final FixedWindowRateLimiter loginFailures;
	private final FixedWindowRateLimiter registrations;
	private final FixedWindowRateLimiter routes;

	public SecurityRateLimitService(SecurityRateLimitProperties properties) {
		enabled = properties.isEnabled();
		int maximumEntries = properties.getMaxEntries();
		loginFailures = limiter(properties.getLogin(), maximumEntries);
		registrations = limiter(properties.getRegistration(), maximumEntries);
		routes = limiter(properties.getRoutes(), maximumEntries);
	}

	public FixedWindowRateLimiter.Decision loginDecision(String remoteAddress, String email) {
		return enabled ? loginFailures.currentDecision(loginKey(remoteAddress, email))
				: new FixedWindowRateLimiter.Decision(true, 0);
	}

	public void recordLoginFailure(String remoteAddress, String email) {
		if (enabled) loginFailures.record(loginKey(remoteAddress, email));
	}

	public void recordLoginSuccess(String remoteAddress, String email) {
		if (enabled) loginFailures.reset(loginKey(remoteAddress, email));
	}

	public void requireRegistrationPermit(String remoteAddress) {
		if (enabled) require(registrations.tryAcquire(safe(remoteAddress)), "REGISTRATION_RATE_LIMITED",
				"Too many registration attempts. Please try again later");
	}

	public void requireRoutePermit(String subject) {
		if (enabled) require(routes.tryAcquire(safe(subject)), "ROUTE_RATE_LIMITED",
				"Too many route requests. Please try again shortly");
	}

	private FixedWindowRateLimiter limiter(SecurityRateLimitProperties.Limit limit, int maximumEntries) {
		return new FixedWindowRateLimiter(limit.getMaxAttempts(), limit.getWindow(), maximumEntries);
	}

	private void require(FixedWindowRateLimiter.Decision decision, String code, String message) {
		if (!decision.allowed()) throw new TooManyRequestsException(code, message, decision.retryAfterSeconds());
	}

	private String loginKey(String remoteAddress, String email) {
		return safe(remoteAddress) + '|' + safe(email).trim().toLowerCase(Locale.ROOT);
	}

	private String safe(String value) { return value == null ? "" : value; }
}
