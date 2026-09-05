package com.smartev.assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.smartev.assistant.security.ActiveAccountFilter;
import com.smartev.assistant.security.CspNonceFilter;
import com.smartev.assistant.security.LoginRateLimitFilter;
import com.smartev.assistant.security.RateLimitedAuthenticationFailureHandler;
import com.smartev.assistant.security.RelativeRedirects;
import com.smartev.assistant.security.RoleAwareAuthenticationSuccessHandler;
import com.smartev.assistant.security.SecurityErrorWriter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			RoleAwareAuthenticationSuccessHandler successHandler,
			RateLimitedAuthenticationFailureHandler failureHandler,
			LoginRateLimitFilter loginRateLimitFilter,
			ActiveAccountFilter activeAccountFilter,
			CspNonceFilter cspNonceFilter,
			SecurityErrorWriter errorWriter,
			@Value("${app.security.require-https:false}") boolean requireHttps) throws Exception {
		LoginUrlAuthenticationEntryPoint pageEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
		pageEntryPoint.setFavorRelativeUris(true);
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/register", "/login", "/api/auth/register", "/api/health",
						"/css/**", "/js/**", "/images/**", "/fonts/**", "/media/**", "/error").permitAll()
				.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/login")
				.loginProcessingUrl("/login")
				.usernameParameter("email")
				.successHandler(successHandler)
				.failureHandler(failureHandler)
				.permitAll())
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessHandler((request, response, authentication) ->
					RelativeRedirects.send(response, "/login?logout"))
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID"))
			.exceptionHandling(exceptions -> exceptions
				.accessDeniedHandler((request, response, exception) -> {
					if (request.getRequestURI().startsWith("/api/")) errorWriter.write(response, request.getRequestURI(), 403, "FORBIDDEN", "You do not have permission to perform this action");
					else response.sendError(403);
				})
				.authenticationEntryPoint((request, response, exception) -> {
				if (request.getRequestURI().startsWith("/api/")) {
					errorWriter.write(response, request.getRequestURI(), 401, "AUTHENTICATION_REQUIRED", "Sign in to continue");
				} else {
					pageEntryPoint.commence(request, response, exception);
				}
			}))
			.addFilterBefore(cspNonceFilter, CsrfFilter.class)
			.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterAfter(activeAccountFilter, AnonymousAuthenticationFilter.class);
		if (requireHttps) http.redirectToHttps(Customizer.withDefaults());
		return http.build();
	}
}
