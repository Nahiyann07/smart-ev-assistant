package com.smartev.assistant.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.repository.UserRepository;

@SpringBootTest(properties = {
		"app.security.rate-limit.login.max-attempts=2",
		"app.security.rate-limit.registration.max-attempts=2",
		"app.security.rate-limit.routes.max-attempts=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityHardeningIntegrationTests {
	@Autowired MockMvc mockMvc;
	@Autowired UserRepository users;
	@Autowired PasswordEncoder encoder;

	@Test
	void disabledAccountLosesItsExistingSession() throws Exception {
		User driver = users.saveAndFlush(new User("Session Driver", unique("session"), encoder.encode("Charge123"), Role.USER));
		MvcResult login = mockMvc.perform(post("/login").with(csrf()).param("email", driver.getEmail())
				.param("password", "Charge123").with(remoteAddress("192.0.2.10")))
				.andExpect(redirectedUrl("/dashboard")).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		driver.setEnabled(false);
		users.saveAndFlush(driver);
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void missingEnabledFlagIsRejected() throws Exception {
		User admin = users.saveAndFlush(new User("Hardening Admin", unique("admin"), encoder.encode("Charge123"), Role.ADMIN));
		User target = users.saveAndFlush(new User("Target Driver", unique("target"), encoder.encode("Charge123"), Role.USER));
		mockMvc.perform(patch("/api/admin/users/{id}/enabled", target.getId()).with(user(AppUserPrincipal.from(admin)))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.enabled").exists());
		assertThat(users.findById(target.getId()).orElseThrow().isEnabled()).isTrue();
	}

	@Test
	void htmlUsesTheCspNonceAndSecurityHeaders() throws Exception {
		MvcResult result = mockMvc.perform(get("/login")).andExpect(status().isOk())
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
				.andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)"))
				.andReturn();
		String policy = result.getResponse().getHeader("Content-Security-Policy");
		Matcher matcher = Pattern.compile("'nonce-([^']+)'").matcher(policy);
		assertThat(matcher.find()).isTrue();
		assertThat(result.getResponse().getContentAsString()).contains("nonce=\"" + matcher.group(1) + "\"");
		assertThat(policy).contains("script-src-attr 'none'", "object-src 'none'", "frame-ancestors 'none'");
	}

	@Test
	void loginFailuresAreBoundedAndSuccessWouldNotBypassTheBlock() throws Exception {
		String email = unique("limited-login");
		users.saveAndFlush(new User("Limited Login", email, encoder.encode("Charge123"), Role.USER));
		for (int attempt = 0; attempt < 2; attempt++) {
			mockMvc.perform(post("/login").with(csrf()).with(remoteAddress("192.0.2.20"))
					.param("email", email).param("password", "wrong-password"))
					.andExpect(redirectedUrl("/login?error"));
		}
		mockMvc.perform(post("/login").with(csrf()).with(remoteAddress("192.0.2.20"))
				.param("email", email).param("password", "Charge123"))
				.andExpect(redirectedUrl("/login?rateLimited")).andExpect(header().exists("Retry-After"));
	}

	@Test
	void registrationAndRouteApisReturn429WithRetryAfter() throws Exception {
		for (int attempt = 0; attempt < 2; attempt++) {
			mockMvc.perform(post("/api/auth/register").with(csrf()).with(remoteAddress("192.0.2.30"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Rate User\",\"email\":\"rate" + attempt + "@example.com\",\"password\":\"Charge123\"}"))
					.andExpect(status().isCreated());
		}
		mockMvc.perform(post("/api/auth/register").with(csrf()).with(remoteAddress("192.0.2.30"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Rate User\",\"email\":\"rate2@example.com\",\"password\":\"Charge123\"}"))
				.andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.code").value("REGISTRATION_RATE_LIMITED"));

		for (int attempt = 0; attempt < 2; attempt++) {
			mockMvc.perform(post("/api/routes").with(user("route-user")).with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"stationId\":999999,\"originLatitude\":8.52,\"originLongitude\":76.93}"))
					.andExpect(status().isNotFound());
		}
		mockMvc.perform(post("/api/routes").with(user("route-user")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"stationId\":999999,\"originLatitude\":8.52,\"originLongitude\":76.93}"))
				.andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.code").value("ROUTE_RATE_LIMITED"));
	}

	@Test
	void nonFiniteAndOverflowingInputsAreHandledWithoutServerErrors() throws Exception {
		mockMvc.perform(get("/api/stations").with(user("driver"))
				.param("latitude", "NaN").param("longitude", "NaN"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
		mockMvc.perform(get("/api/stations").with(user("driver"))
				.param("page", String.valueOf(Integer.MAX_VALUE)).param("size", "50"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());

		String station = "{\"name\":\"Overflow Hub\",\"address\":\"Road\",\"city\":\"City\","
				+ "\"chargerType\":\"AC\",\"totalPorts\":2147483647,\"availablePorts\":2147483647,"
				+ "\"outOfServicePorts\":2147483647,\"chargingSpeedKw\":22,\"operatingHours\":\"24 hours\","
				+ "\"status\":\"AVAILABLE\",\"description\":\"Overflow test\"}";
		mockMvc.perform(post("/api/admin/stations").with(user("admin").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(station))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_STATION_STATUS"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor remoteAddress(String address) {
		return request -> { request.setRemoteAddr(address); return request; };
	}

	private String unique(String prefix) { return prefix + System.nanoTime() + "@example.com"; }
}
