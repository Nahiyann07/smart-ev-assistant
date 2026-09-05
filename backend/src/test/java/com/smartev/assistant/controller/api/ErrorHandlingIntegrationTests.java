package com.smartev.assistant.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.smartev.assistant.service.StationService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingIntegrationTests {
	@Autowired MockMvc mockMvc;
	@MockitoBean StationService stationService;

	@Test
	void malformedParametersUseStableApiErrorContract() throws Exception {
		mockMvc.perform(get("/api/stations").with(user("driver").roles("USER")).param("chargerType", "CHADEMO_PLUS"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.fieldErrors").isMap()).andExpect(jsonPath("$.path").value("/api/stations"));
	}

	@Test
	void securityFailuresUseJsonForApiRoutes() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
		mockMvc.perform(get("/api/admin/dashboard").with(user("driver").roles("USER")))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void databaseFailuresDoNotLeakImplementationDetails() throws Exception {
		when(stationService.get(999L)).thenThrow(new CannotGetJdbcConnectionException("secret jdbc detail", new SQLException("password")));
		mockMvc.perform(get("/api/stations/999").with(user("driver").roles("USER")))
				.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("The data service is temporarily unavailable. Please try again"));
	}
}
