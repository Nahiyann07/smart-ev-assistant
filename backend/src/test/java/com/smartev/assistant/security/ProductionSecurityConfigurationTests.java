package com.smartev.assistant.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"prod", "test"})
class ProductionSecurityConfigurationTests {
	@Autowired MockMvc mockMvc;
	@Autowired Environment environment;

	@Test
	void productionProfileRequiresHttpsAndSecureCookies() throws Exception {
		assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class)).isTrue();
		assertThat(environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class)).isTrue();
		assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
		assertThat(environment.getProperty("app.security.require-https", Boolean.class)).isTrue();
		mockMvc.perform(get("/api/health")).andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/api/health").with(request -> {
			request.setScheme("https");
			request.setSecure(true);
			request.setServerPort(443);
			return request;
		})).andExpect(status().isOk());
	}
}
