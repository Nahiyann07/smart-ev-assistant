package com.smartev.assistant.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"app.google-maps.browser-key=browser-key-sentinel",
		"app.google-maps.map-id=map-id-sentinel",
		"app.google-maps.routes-server-key=server-key-must-not-render"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontendPageIntegrationTests {

	@Autowired MockMvc mockMvc;

	@Test
	void publicPagesRenderTheirPremiumShells() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Find a charger")));
		mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in")));
		mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Create account")));
	}

	@Test
	void driverPagesRenderWithCsrfAwareNavigation() throws Exception {
		for (String path : new String[] { "/dashboard", "/stations", "/recommendations", "/favourites", "/reports", "/profile" }) {
			mockMvc.perform(get(path).with(user("driver").roles("USER"))).andExpect(status().isOk())
					.andExpect(content().string(org.hamcrest.Matchers.containsString("csrf-token")))
					.andExpect(content().string(org.hamcrest.Matchers.containsString("Sign out")));
		}
	}

	@Test
	void mapPageRendersPublicConfigurationAndNeverLeaksServerKey() throws Exception {
		mockMvc.perform(get("/stations").with(user("driver").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("browser-key-sentinel")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("map-id-sentinel")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-map-preview-link")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("api=1")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("server-key-must-not-render"))));
	}

	@Test
	void adminPagesRenderOnlyForAdmin() throws Exception {
		for (String path : new String[] { "/admin", "/admin/stations", "/admin/reports", "/admin/users" }) {
			mockMvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
					.andExpect(content().string(org.hamcrest.Matchers.containsString("Administration")));
			mockMvc.perform(get(path).with(user("driver").roles("USER"))).andExpect(status().isForbidden());
		}
	}
}
