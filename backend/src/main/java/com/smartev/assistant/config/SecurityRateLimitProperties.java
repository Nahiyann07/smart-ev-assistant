package com.smartev.assistant.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.rate-limit")
public class SecurityRateLimitProperties {
	private boolean enabled = true;
	private int maxEntries = 10_000;
	private Limit login = new Limit(5, Duration.ofMinutes(15));
	private Limit registration = new Limit(10, Duration.ofHours(1));
	private Limit routes = new Limit(30, Duration.ofMinutes(1));

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public int getMaxEntries() { return maxEntries; }
	public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }
	public Limit getLogin() { return login; }
	public void setLogin(Limit login) { this.login = login; }
	public Limit getRegistration() { return registration; }
	public void setRegistration(Limit registration) { this.registration = registration; }
	public Limit getRoutes() { return routes; }
	public void setRoutes(Limit routes) { this.routes = routes; }

	public static class Limit {
		private int maxAttempts;
		private Duration window;

		public Limit() {}
		public Limit(int maxAttempts, Duration window) {
			this.maxAttempts = maxAttempts;
			this.window = window;
		}
		public int getMaxAttempts() { return maxAttempts; }
		public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
		public Duration getWindow() { return window; }
		public void setWindow(Duration window) { this.window = window; }
	}
}
