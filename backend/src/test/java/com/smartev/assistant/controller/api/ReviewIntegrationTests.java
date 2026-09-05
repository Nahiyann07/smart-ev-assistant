package com.smartev.assistant.controller.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;
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
class ReviewIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired UserRepository users; @Autowired StationRepository stations; @Autowired PasswordEncoder encoder;
	private AppUserPrincipal author; private AppUserPrincipal other; private Station station;

	@BeforeEach void setup() {
		author = principal("Review Author"); other = principal("Other Driver");
		station = stations.saveAndFlush(new Station(new StationRequest("Review Hub", "3 Test Road", "Bengaluru",
				BigDecimal.valueOf(12.9), BigDecimal.valueOf(77.6), ChargerType.AC, 2, 1, 0,
				BigDecimal.valueOf(22), "06:00–23:00", StationStatus.AVAILABLE, "Review test")));
	}

	@Test void supportsReviewLifecycleAndAuthorOwnership() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/stations/{id}/reviews", station.getId()).with(user(author)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"  Excellent charger  \"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.comment").value("Excellent charger")).andReturn();
		String location = result.getResponse().getHeader("Location"); long reviewId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

		mockMvc.perform(put("/api/reviews/{id}", reviewId).with(user(other)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":2,\"comment\":\"No\"}"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("REVIEW_NOT_OWNED"));
		mockMvc.perform(put("/api/reviews/{id}", reviewId).with(user(author)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4,\"comment\":\"Updated\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(4));
		mockMvc.perform(delete("/api/reviews/{id}", reviewId).with(user(author)).with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test void enforcesOneReviewPerUserAndRatingValidation() throws Exception {
		postReview(author, 5, "First").andExpect(status().isCreated());
		postReview(author, 4, "Second").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
		postReview(other, 6, "Invalid").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.rating").exists());
	}

	@Test void returnsAnEmptyReviewListForAStation() throws Exception {
		mockMvc.perform(get("/api/stations/{id}/reviews", station.getId()).with(user(author)))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
	}

	private org.springframework.test.web.servlet.ResultActions postReview(AppUserPrincipal principal, int rating, String comment) throws Exception {
		return mockMvc.perform(post("/api/stations/{id}/reviews", station.getId()).with(user(principal)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":" + rating + ",\"comment\":\"" + comment + "\"}"));
	}
	private AppUserPrincipal principal(String name) {
		return AppUserPrincipal.from(users.saveAndFlush(new User(name, name.replace(" ", "").toLowerCase() + System.nanoTime() + "@example.com", encoder.encode("Charge123"), Role.USER)));
	}
}
