package com.smartev.assistant.security;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FixedWindowRateLimiter {
	private final int maximumAttempts;
	private final long windowMillis;
	private final Clock clock;
	private final Map<String, Window> windows;

	public FixedWindowRateLimiter(int maximumAttempts, Duration window, int maximumEntries) {
		this(maximumAttempts, window, maximumEntries, Clock.systemUTC());
	}

	FixedWindowRateLimiter(int maximumAttempts, Duration window, int maximumEntries, Clock clock) {
		if (maximumAttempts < 1 || window == null || window.isZero() || window.isNegative() || maximumEntries < 1)
			throw new IllegalArgumentException("Rate-limit values must be positive");
		this.maximumAttempts = maximumAttempts;
		this.windowMillis = window.toMillis();
		this.clock = clock;
		this.windows = new BoundedWindowMap(maximumEntries);
	}

	public synchronized Decision tryAcquire(String key) {
		long now = clock.millis();
		Window current = current(key, now);
		if (current.attempts >= maximumAttempts) return denied(current, now);
		current.attempts++;
		return Decision.granted();
	}

	public synchronized Decision currentDecision(String key) {
		long now = clock.millis();
		Window current = windows.get(key);
		if (current == null || expired(current, now)) {
			windows.remove(key);
			return Decision.granted();
		}
		return current.attempts >= maximumAttempts ? denied(current, now) : Decision.granted();
	}

	public synchronized void record(String key) {
		Window current = current(key, clock.millis());
		if (current.attempts < Integer.MAX_VALUE) current.attempts++;
	}

	public synchronized void reset(String key) {
		windows.remove(key);
	}

	private Window current(String key, long now) {
		Window current = windows.get(key);
		if (current == null || expired(current, now)) {
			current = new Window(now);
			windows.put(key, current);
		}
		return current;
	}

	private boolean expired(Window window, long now) {
		return now - window.startedAtMillis >= windowMillis;
	}

	private Decision denied(Window window, long now) {
		long remainingMillis = Math.max(1, windowMillis - (now - window.startedAtMillis));
		return Decision.blocked(Math.max(1, (remainingMillis + 999) / 1000));
	}

	private static final class Window {
		private final long startedAtMillis;
		private int attempts;
		private Window(long startedAtMillis) { this.startedAtMillis = startedAtMillis; }
	}

	private static final class BoundedWindowMap extends LinkedHashMap<String, Window> {
		private static final long serialVersionUID = 1L;
		private final int maximumEntries;

		private BoundedWindowMap(int maximumEntries) {
			super(16, 0.75f, true);
			this.maximumEntries = maximumEntries;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Window> eldest) {
			return size() > maximumEntries;
		}
	}

	public record Decision(boolean allowed, long retryAfterSeconds) {
		static Decision granted() { return new Decision(true, 0); }
		static Decision blocked(long retryAfterSeconds) { return new Decision(false, retryAfterSeconds); }
	}
}
