package com.smartev.assistant.controller.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class ReportIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired UserRepository users; @Autowired StationRepository stations; @Autowired PasswordEncoder encoder;
	private AppUserPrincipal driver; private AppUserPrincipal other; private AppUserPrincipal admin; private Station station;

	@BeforeEach void setup() {
		driver = principal("Report Driver", Role.USER); other = principal("Other Reporter", Role.USER); admin = principal("Report Admin", Role.ADMIN);
		station = stations.saveAndFlush(new Station(new StationRequest("Report Hub", "4 Test Road", "Bengaluru",
				BigDecimal.valueOf(12.9), BigDecimal.valueOf(77.6), ChargerType.DC_FAST, 3, 1, 0,
				BigDecimal.valueOf(80), "24 hours", StationStatus.AVAILABLE, "Report test")));
	}

	@Test void submitsReportAndShowsOnlyCurrentUsersHistory() throws Exception {
		submit(driver, "CHARGER_NOT_WORKING", "  Connector two is offline  ").andExpect(status().isCreated())
				.andExpect(jsonPath("$.description").value("Connector two is offline")).andExpect(jsonPath("$.status").value("PENDING"));
		submit(other, "PAYMENT_ISSUE", "Card reader failed").andExpect(status().isCreated());
		mockMvc.perform(get("/api/users/me/reports").with(user(driver))).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].userId").value(driver.id()));
	}

	@Test void adminFiltersAndResolvesWithAuditData() throws Exception {
		MvcResult created = submit(driver, "OTHER", "Lighting issue").andExpect(status().isCreated()).andReturn();
		String location = created.getResponse().getHeader("Location"); long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
		mockMvc.perform(get("/api/admin/reports").with(user(admin)).param("status", "PENDING"))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
		mockMvc.perform(patch("/api/admin/reports/{id}", id).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.resolvedByUserId").value(admin.id()))
				.andExpect(jsonPath("$.resolvedAt").isNotEmpty());
		mockMvc.perform(patch("/api/admin/reports/{id}", id).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REPORT_TRANSITION"));
	}

	@Test void driverCannotResolveAndPendingIsNotAResolution() throws Exception {
		MvcResult created = submit(driver, "OTHER", "Signage").andReturn();
		long id = Long.parseLong(created.getResponse().getHeader("Location").replaceAll(".*/", ""));
		mockMvc.perform(patch("/api/admin/reports/{id}", id).with(user(driver)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/admin/reports/{id}", id).with(user(admin)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
				.andExpect(status().isBadRequest());
	}

	private org.springframework.test.web.servlet.ResultActions submit(AppUserPrincipal user, String type, String description) throws Exception {
		return mockMvc.perform(post("/api/stations/{id}/reports", station.getId()).with(user(user)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"issueType\":\"" + type + "\",\"description\":\"" + description + "\"}"));
	}
	private AppUserPrincipal principal(String name, Role role) {
		return AppUserPrincipal.from(users.saveAndFlush(new User(name, name.replace(" ", "").toLowerCase() + System.nanoTime() + "@example.com", encoder.encode("Charge123"), role)));
	}
}
