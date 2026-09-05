package com.smartev.assistant.controller.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;
import com.smartev.assistant.security.AppUserPrincipal;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class RecommendationIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired StationRepository stations; @Autowired UserRepository users; @Autowired PasswordEncoder encoder;
	private AppUserPrincipal driver;
	@BeforeEach void setup() {
		driver = AppUserPrincipal.from(users.saveAndFlush(new User("Recommendation Driver", "recommend" + System.nanoTime() + "@example.com", encoder.encode("Charge123"), Role.USER)));
		stations.save(station("Best Operational Hub", StationStatus.AVAILABLE, 4, 0, 150));
		stations.save(station("Maintenance Hub", StationStatus.UNDER_MAINTENANCE, 0, 1, 150));
		stations.saveAndFlush(station("Slower Hub", StationStatus.AVAILABLE, 1, 0, 22));
	}

	@Test void ranksDeterministicallyAndExcludesUnavailableStatuses() throws Exception {
		mockMvc.perform(get("/api/recommendations").with(user(driver)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].stationName").value("Best Operational Hub"))
				.andExpect(jsonPath("$[0].breakdown.distanceScore").doesNotExist())
				.andExpect(jsonPath("$[0].reasons").isArray());
	}

	@Test void validatesOptionalCoordinatePair() throws Exception {
		mockMvc.perform(get("/api/recommendations").with(user(driver)).param("latitude", "12"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
	}

	private Station station(String name, StationStatus status, int available, int out, double speed) {
		return new Station(new StationRequest(name, "6 Test Road", "Bengaluru", BigDecimal.valueOf(12.97), BigDecimal.valueOf(77.59),
				ChargerType.DC_FAST, 4, available, out, BigDecimal.valueOf(speed), "24 hours", status, "Recommendation test"));
	}
}
