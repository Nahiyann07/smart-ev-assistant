package com.smartev.assistant.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;

@Configuration
public class DemoDataConfiguration {
	@Bean
	@ConditionalOnProperty(prefix = "app.preview", name = "seed-enabled", havingValue = "true")
	ApplicationRunner demoData(UserRepository users, StationRepository stations, PasswordEncoder encoder,
			@Value("${app.preview.driver-email:}") String driverEmail,
			@Value("${app.preview.driver-password:}") String driverPassword,
			@Value("${app.preview.admin-email:}") String adminEmail,
			@Value("${app.preview.admin-password:}") String adminPassword) {
		return arguments -> {
			validateDemoCredentials(driverEmail, driverPassword, adminEmail, adminPassword);
			String normalizedDriverEmail = driverEmail.trim().toLowerCase(Locale.ROOT);
			String normalizedAdminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
			if (!users.existsByEmail(normalizedDriverEmail)) users.save(new User("Demo Driver", normalizedDriverEmail, encoder.encode(driverPassword), Role.USER));
			if (!users.existsByEmail(normalizedAdminEmail)) users.save(new User("Network Admin", normalizedAdminEmail, encoder.encode(adminPassword), Role.ADMIN));
			if (stations.countByDeletedAtIsNull() == 0) stations.saveAll(sampleStations());
		};
	}

	private void validateDemoCredentials(String driverEmail, String driverPassword, String adminEmail, String adminPassword) {
		if (driverEmail.isBlank() || adminEmail.isBlank() || driverPassword.length() < 8 || adminPassword.length() < 8) {
			throw new IllegalStateException("Demo seeding requires both emails and passwords of at least 8 characters through environment variables");
		}
		if (driverEmail.equalsIgnoreCase(adminEmail)) {
			throw new IllegalStateException("Demo driver and administrator must use different email addresses");
		}
	}

	private List<Station> sampleStations() {
		return List.of(
			station("Technopark Pulse", "Phase 1 Campus Road", "Thiruvananthapuram", 8.5583, 76.8819, ChargerType.DC_FAST, 8, 5, 0, 120, StationStatus.AVAILABLE, "Covered high-speed bays near the main campus entrance."),
			station("Lulu Charge Deck", "Edappally Junction", "Kochi", 10.0272, 76.3089, ChargerType.DC_FAST, 10, 3, 1, 150, StationStatus.AVAILABLE, "Fast charging on the east parking deck with round-the-clock access."),
			station("Marine Drive Current", "Shanmugham Road", "Kochi", 9.9816, 76.2756, ChargerType.AC, 6, 0, 0, 22, StationStatus.OCCUPIED, "Convenient waterfront destination charging."),
			station("Cyberpark Volt", "Nellikode", "Kozhikode", 11.2588, 75.8290, ChargerType.DC_FAST, 6, 2, 1, 90, StationStatus.AVAILABLE, "Mixed-speed campus charging with security on site."),
			station("Palakkad Gateway", "NH 544 Bypass", "Palakkad", 10.7867, 76.6548, ChargerType.DC_FAST, 4, 0, 2, 60, StationStatus.UNDER_MAINTENANCE, "Highway stop currently undergoing partial maintenance."),
			station("Alappuzha Lakeside", "Beach Road", "Alappuzha", 9.4981, 76.3388, ChargerType.AC, 4, 1, 0, 22, StationStatus.AVAILABLE, "Quiet destination chargers close to the beach."));
	}

	private Station station(String name, String address, String city, double latitude, double longitude,
			ChargerType type, int total, int available, int offline, double speed, StationStatus status, String description) {
		return new Station(new StationRequest(name, address, city, BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude),
				type, total, available, offline, BigDecimal.valueOf(speed), "24 hours", status, description));
	}
}
