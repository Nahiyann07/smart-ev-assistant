package com.smartev.assistant.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTests {
	@Test
	void enforcesAWindowAndAllowsReset() {
		MutableClock clock = new MutableClock();
		FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, Duration.ofMinutes(1), 10, clock);

		assertThat(limiter.tryAcquire("user").allowed()).isTrue();
		assertThat(limiter.tryAcquire("user").allowed()).isTrue();
		FixedWindowRateLimiter.Decision blocked = limiter.tryAcquire("user");
		assertThat(blocked.allowed()).isFalse();
		assertThat(blocked.retryAfterSeconds()).isEqualTo(60);

		limiter.reset("user");
		assertThat(limiter.tryAcquire("user").allowed()).isTrue();
		clock.advance(Duration.ofMinutes(1));
		assertThat(limiter.tryAcquire("user").allowed()).isTrue();
	}

	private static final class MutableClock extends Clock {
		private Instant current = Instant.parse("2026-09-05T00:00:00Z");
		@Override public ZoneId getZone() { return ZoneId.of("UTC"); }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return current; }
		void advance(Duration duration) { current = current.plus(duration); }
	}
}
