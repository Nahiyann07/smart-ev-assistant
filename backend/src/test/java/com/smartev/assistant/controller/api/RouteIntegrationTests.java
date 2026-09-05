package com.smartev.assistant.controller.api;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.exception.ServiceUnavailableException;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.service.RouteProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.google-maps.routes-server-key=test-server-key")
@Transactional
class RouteIntegrationTests {
	@Autowired MockMvc mockMvc;
	@Autowired StationRepository stationRepository;
	@MockitoBean RouteProvider routeProvider;
	private Long stationId;

	@BeforeEach
	void station() {
		Station station = new Station(new StationRequest("Route Station", "1 Road", "City",
				new BigDecimal("8.524100"), new BigDecimal("76.936600"), ChargerType.DC_FAST,
				4, 2, 0, new BigDecimal("120.00"), "24 hours", StationStatus.AVAILABLE, "Route target"));
		stationId = stationRepository.saveAndFlush(station).getId();
	}

	@Test
	void routeRequiresAuthenticationAndCsrf() throws Exception {
		mockMvc.perform(post("/api/routes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/routes").with(user("driver").roles("USER")).contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isForbidden());
	}

	@Test
	void validatesCoordinatesAndMissingStations() throws Exception {
		mockMvc.perform(post("/api/routes").with(user("driver").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"stationId\":" + stationId + ",\"originLatitude\":98,\"originLongitude\":76.93}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		mockMvc.perform(post("/api/routes").with(user("driver").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"stationId\":999999,\"originLatitude\":8.52,\"originLongitude\":76.93}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("STATION_NOT_FOUND"));
	}

	@Test
	void returnsMinimalRoutePayload() throws Exception {
		when(routeProvider.compute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString()))
				.thenReturn(new RouteProvider.RouteResult(4200, 720, "encoded-route"));
		mockMvc.perform(post("/api/routes").with(user("driver").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.stationId").value(stationId))
				.andExpect(jsonPath("$.distanceMeters").value(4200)).andExpect(jsonPath("$.durationSeconds").value(720))
				.andExpect(jsonPath("$.encodedPolyline").value("encoded-route"));
	}

	@Test
	void translatesProviderFailuresToServiceUnavailable() throws Exception {
		when(routeProvider.compute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString()))
				.thenThrow(new ServiceUnavailableException("ROUTES_UNAVAILABLE", "Route guidance is temporarily unavailable"));
		mockMvc.perform(post("/api/routes").with(user("driver").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ROUTES_UNAVAILABLE"));
	}

	private String validBody() {
		return "{\"stationId\":" + stationId + ",\"originLatitude\":8.52,\"originLongitude\":76.93}";
	}
}
