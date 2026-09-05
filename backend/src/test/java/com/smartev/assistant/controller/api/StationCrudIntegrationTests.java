package com.smartev.assistant.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;
import com.smartev.assistant.security.AppUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StationCrudIntegrationTests {
	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired StationRepository stationRepository;
	@Autowired PasswordEncoder passwordEncoder;
	private AppUserPrincipal admin;
	private AppUserPrincipal driver;

	@BeforeEach
	void users() {
		admin = AppUserPrincipal.from(userRepository.saveAndFlush(new User("Admin", unique("admin"), passwordEncoder.encode("Charge123"), Role.ADMIN)));
		driver = AppUserPrincipal.from(userRepository.saveAndFlush(new User("Driver", unique("driver"), passwordEncoder.encode("Charge123"), Role.USER)));
	}

	@Test
	void adminCanCreateUpdateAndSoftDeleteStation() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/admin/stations").with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validStation("AVAILABLE", 4, 2, 0)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Voltway Central"))
				.andExpect(jsonPath("$.occupiedPorts").value(2)).andReturn();
		String location = created.getResponse().getHeader("Location");
		long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

		mockMvc.perform(put("/api/admin/stations/{id}", id).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validStation("OCCUPIED", 4, 0, 1)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.occupiedPorts").value(3));

		mockMvc.perform(delete("/api/admin/stations/{id}", id).with(user(admin)).with(csrf()))
				.andExpect(status().isNoContent());
		assertThat(stationRepository.findById(id).orElseThrow().getDeletedAt()).isNotNull();
		mockMvc.perform(get("/api/admin/stations/{id}", id).with(user(admin)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("STATION_NOT_FOUND"));
	}

	@Test
	void validatesPortSumAndStatusInvariants() throws Exception {
		mockMvc.perform(post("/api/admin/stations").with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validStation("AVAILABLE", 4, 0, 0)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_STATION_STATUS"));
		mockMvc.perform(post("/api/admin/stations").with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validStation("AVAILABLE", 4, 3, 2)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void driverCannotUseAdminStationApi() throws Exception {
		mockMvc.perform(post("/api/admin/stations").with(user(driver)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validStation("AVAILABLE", 4, 2, 0)))
				.andExpect(status().isForbidden());
	}

	private String validStation(String status, int total, int available, int out) {
		return """
			{"name":"Voltway Central","address":"1 MG Road","city":"Bengaluru","latitude":12.971599,
			"longitude":77.594566,"chargerType":"DC_FAST","totalPorts":%d,"availablePorts":%d,
			"outOfServicePorts":%d,"chargingSpeedKw":120.50,"operatingHours":"24 hours",
			"status":"%s","description":"Covered fast-charging station"}
			""".formatted(total, available, out, status);
	}

	private String unique(String prefix) { return prefix + System.nanoTime() + "@example.com"; }
}
