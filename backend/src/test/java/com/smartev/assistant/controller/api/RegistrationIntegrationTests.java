package com.smartev.assistant.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.Role;
import com.smartev.assistant.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registrationPageIsAvailable() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/register"));
	}

	@Test
	void registersAUserWithNormalizedEmailAndHashedPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Nikhil Test","email":"  Nikhil@Example.COM ","password":"Charge123"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("nikhil@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		User saved = userRepository.findByEmail("nikhil@example.com").orElseThrow();
		assertThat(saved.getRole()).isEqualTo(Role.USER);
		assertThat(saved.getPasswordHash()).isNotEqualTo("Charge123");
		assertThat(passwordEncoder.matches("Charge123", saved.getPasswordHash())).isTrue();
	}

	@Test
	void rejectsDuplicateEmail() throws Exception {
		userRepository.saveAndFlush(new User("Existing User", "existing@example.com", "hash", Role.USER));

		mockMvc.perform(post("/api/auth/register")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Another User","email":"EXISTING@example.com","password":"Charge123"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void rejectsInvalidRegistrationFields() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"","email":"not-an-email","password":"weak"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.name").exists())
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.password").exists());
	}
}
