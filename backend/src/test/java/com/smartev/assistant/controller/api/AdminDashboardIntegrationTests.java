package com.smartev.assistant.controller.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.transaction.annotation.Transactional;
import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.entity.Report;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.IssueType;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.repository.ReportRepository;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;
import com.smartev.assistant.security.AppUserPrincipal;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AdminDashboardIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired UserRepository users; @Autowired StationRepository stations;
	@Autowired ReportRepository reports; @Autowired PasswordEncoder encoder;
	private AppUserPrincipal admin; private User driver;
	@BeforeEach void setup() {
		User adminUser = users.saveAndFlush(new User("Dashboard Admin", unique("dashadmin"), encoder.encode("Charge123"), Role.ADMIN));
		admin = AppUserPrincipal.from(adminUser);
		driver = users.saveAndFlush(new User("Dashboard Driver", unique("dashdriver"), encoder.encode("Charge123"), Role.USER));
		Station station = stations.saveAndFlush(new Station(new StationRequest("Dashboard Hub", "7 Test Road", "Bengaluru",
				BigDecimal.valueOf(12.9), BigDecimal.valueOf(77.6), ChargerType.DC_FAST, 4, 2, 0,
				BigDecimal.valueOf(120), "24 hours", StationStatus.AVAILABLE, "Dashboard test")));
		reports.saveAndFlush(new Report(driver, station, IssueType.OTHER, "Test issue"));
	}

	@Test void exposesUsefulOperationalCounts() throws Exception {
		mockMvc.perform(get("/api/admin/dashboard").with(user(admin))).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalUsers").isNumber()).andExpect(jsonPath("$.activeStations").isNumber())
				.andExpect(jsonPath("$.availableStations").isNumber()).andExpect(jsonPath("$.pendingReports").isNumber())
				.andExpect(jsonPath("$.pendingReports").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
	}

	@Test void canDisableAnotherUserButCannotDisableSelf() throws Exception {
		mockMvc.perform(patch("/api/admin/users/{id}/enabled", driver.getId()).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
		mockMvc.perform(get("/api/admin/users").with(user(admin))).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + driver.getId() + ")].enabled").value(false));
		mockMvc.perform(patch("/api/admin/users/{id}/enabled", admin.id()).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_DISABLE_NOT_ALLOWED"));
	}

	private String unique(String prefix) { return prefix + System.nanoTime() + "@example.com"; }
}
