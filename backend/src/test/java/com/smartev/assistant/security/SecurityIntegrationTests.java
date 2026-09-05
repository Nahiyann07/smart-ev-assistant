package com.smartev.assistant.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTests {

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;

	private User driver;
	private User admin;

	@BeforeEach
	void setUp() {
		driver = userRepository.saveAndFlush(new User("Test Driver", "driver@example.com",
				passwordEncoder.encode("Charge123"), Role.USER));
		admin = userRepository.saveAndFlush(new User("Test Admin", "admin@example.com",
				passwordEncoder.encode("Charge123"), Role.ADMIN));
	}

	@Test
	void protectsPagesAndApisWithAppropriateResponse() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void driverLoginCreatesSessionAndReturnsIdentity() throws Exception {
		MvcResult login = mockMvc.perform(post("/login").with(csrf())
				.param("email", " DRIVER@EXAMPLE.COM ")
				.param("password", "Charge123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dashboard"))
				.andReturn();

		mockMvc.perform(get("/api/auth/me").session((MockHttpSession) login.getRequest().getSession(false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(driver.getId()))
				.andExpect(jsonPath("$.email").value("driver@example.com"))
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void adminLoginUsesAdminDestinationAndRoleGuard() throws Exception {
		mockMvc.perform(post("/login").with(csrf())
				.param("email", "admin@example.com").param("password", "Charge123"))
				.andExpect(redirectedUrl("/admin"));

		mockMvc.perform(get("/admin").with(user(AppUserPrincipal.from(driver))))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/admin").with(user(AppUserPrincipal.from(admin))))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsInvalidAndDisabledCredentialsWithoutDisclosingWhich() throws Exception {
		driver.setEnabled(false);
		userRepository.saveAndFlush(driver);
		mockMvc.perform(post("/login").with(csrf())
				.param("email", "driver@example.com").param("password", "Charge123"))
				.andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(post("/login").with(csrf())
				.param("email", "missing@example.com").param("password", "Charge123"))
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void csrfProtectsMutationsAndLogoutInvalidatesSession() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType("application/json")
				.content("{\"name\":\"Test User\",\"email\":\"test@example.com\",\"password\":\"Charge123\"}"))
				.andExpect(status().isForbidden());

		MvcResult login = mockMvc.perform(post("/login").with(csrf())
				.param("email", "driver@example.com").param("password", "Charge123"))
				.andReturn();
		mockMvc.perform(post("/logout").with(csrf()).session((MockHttpSession) login.getRequest().getSession(false)))
				.andExpect(redirectedUrl("/login?logout"));
	}
}
