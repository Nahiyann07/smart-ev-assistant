package com.smartev.assistant.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;

class DemoDataConfigurationTests {
	private final UserRepository users = mock(UserRepository.class);
	private final StationRepository stations = mock(StationRepository.class);
	private final PasswordEncoder encoder = mock(PasswordEncoder.class);

	@Test
	void acceptsEnvironmentSuppliedDemoCredentials() throws Exception {
		when(users.existsByEmail("driver@demo.test")).thenReturn(true);
		when(users.existsByEmail("admin@demo.test")).thenReturn(true);
		when(stations.countByDeletedAtIsNull()).thenReturn(1L);

		new DemoDataConfiguration().demoData(users, stations, encoder,
				"driver@demo.test", "driver-secret", "admin@demo.test", "admin-secret")
				.run(mock(ApplicationArguments.class));
	}

	@Test
	void refusesEnabledSeedingWithoutStrongDistinctCredentials() {
		assertThatThrownBy(() -> new DemoDataConfiguration().demoData(users, stations, encoder,
				"", "short", "", "short").run(mock(ApplicationArguments.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("environment variables");
	}
}
