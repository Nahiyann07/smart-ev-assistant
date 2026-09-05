package com.smartev.assistant.controller.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class FavouriteIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired UserRepository users; @Autowired StationRepository stations; @Autowired PasswordEncoder encoder;
	private AppUserPrincipal owner; private AppUserPrincipal other; private Station station;
	@BeforeEach void setup() {
		owner = principal("Favourite Owner"); other = principal("Another Owner");
		station = stations.saveAndFlush(new Station(new StationRequest("Favourite Hub", "5 Test Road", "Bengaluru",
				BigDecimal.valueOf(12.9), BigDecimal.valueOf(77.6), ChargerType.DC_FAST, 4, 2, 0,
				BigDecimal.valueOf(120), "24 hours", StationStatus.AVAILABLE, "Favourite test")));
	}

	@Test void addsListsAndRemovesAnOwnedFavourite() throws Exception {
		mockMvc.perform(post("/api/favourites/{id}", station.getId()).with(user(owner)).with(csrf()))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.stationName").value("Favourite Hub"));
		mockMvc.perform(get("/api/favourites").with(user(owner))).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].stationId").value(station.getId()));
		mockMvc.perform(get("/api/favourites").with(user(other))).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(delete("/api/favourites/{id}", station.getId()).with(user(owner)).with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test void duplicateAndMissingRemovalHaveStableErrors() throws Exception {
		mockMvc.perform(post("/api/favourites/{id}", station.getId()).with(user(owner)).with(csrf())).andExpect(status().isCreated());
		mockMvc.perform(post("/api/favourites/{id}", station.getId()).with(user(owner)).with(csrf()))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FAVOURITE_ALREADY_EXISTS"));
		mockMvc.perform(delete("/api/favourites/{id}", station.getId()).with(user(other)).with(csrf()))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FAVOURITE_NOT_FOUND"));
	}

	private AppUserPrincipal principal(String name) {
		return AppUserPrincipal.from(users.saveAndFlush(new User(name, name.replace(" ", "").toLowerCase() + System.nanoTime() + "@example.com", encoder.encode("Charge123"), Role.USER)));
	}
}
