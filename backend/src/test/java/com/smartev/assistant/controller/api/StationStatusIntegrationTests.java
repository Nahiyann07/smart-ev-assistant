package com.smartev.assistant.controller.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class StationStatusIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired StationRepository stations; @Autowired UserRepository users;
	@Autowired PasswordEncoder encoder;
	private AppUserPrincipal admin; private AppUserPrincipal driver; private Station station;

	@BeforeEach void setup() {
		admin = principal("Status Admin", Role.ADMIN); driver = principal("Status Driver", Role.USER);
		station = stations.saveAndFlush(new Station(new StationRequest("Status Hub", "2 Test Road", "Bengaluru",
				BigDecimal.valueOf(12.97), BigDecimal.valueOf(77.59), ChargerType.DC_FAST, 4, 2, 0,
				BigDecimal.valueOf(100), "24 hours", StationStatus.AVAILABLE, "Status test")));
	}

	@Test void updatesStatusAndPortCountsAtomically() throws Exception {
		mockMvc.perform(patch("/api/admin/stations/{id}/status", station.getId()).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"UNDER_MAINTENANCE\",\"availablePorts\":0,\"outOfServicePorts\":2}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"))
				.andExpect(jsonPath("$.occupiedPorts").value(2))
				.andExpect(jsonPath("$.availabilitySource").value("SIMULATED_DATABASE_STATE"));
	}

	@Test void rejectsAnInconsistentAtomicStatusUpdate() throws Exception {
		mockMvc.perform(patch("/api/admin/stations/{id}/status", station.getId()).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"OUT_OF_SERVICE\",\"availablePorts\":0,\"outOfServicePorts\":3}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_STATION_STATUS"));
	}

	@Test void detailPageDisclosesSimulationAndOffersRefresh() throws Exception {
		mockMvc.perform(get("/stations/{id}", station.getId()).with(user(driver)))
				.andExpect(status().isOk()).andExpect(view().name("stations/details"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("simulated database state")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Refresh status")));
	}

	private AppUserPrincipal principal(String name, Role role) {
		return AppUserPrincipal.from(users.saveAndFlush(new User(name,
				name.replace(" ", "").toLowerCase() + System.nanoTime() + "@example.com", encoder.encode("Charge123"), role)));
	}
}
