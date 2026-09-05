package com.smartev.assistant.security;

import java.io.IOException;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartev.assistant.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class ActiveAccountFilter extends OncePerRequestFilter {
	private final UserRepository users;
	private final SecurityErrorWriter errors;

	public ActiveAccountFilter(UserRepository users, SecurityErrorWriter errors) {
		this.users = users;
		this.errors = errors;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.equals("/") || path.equals("/login") || path.equals("/register")
				|| path.equals("/api/auth/register") || path.equals("/api/health") || path.equals("/error")
				|| path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
				|| path.startsWith("/fonts/") || path.startsWith("/media/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			UserRepository.SecurityState state = users.findSecurityStateById(principal.id()).orElse(null);
			if (state == null || !state.getEnabled() || state.getRole() != principal.role()) {
				revoke(request);
				if (request.getRequestURI().startsWith("/api/"))
					errors.write(response, request.getRequestURI(), 401, "ACCOUNT_DISABLED", "This account is no longer active");
				else response.sendRedirect("/login?disabled");
				return;
			}
		} catch (DataAccessException exception) {
			if (request.getRequestURI().startsWith("/api/"))
				errors.write(response, request.getRequestURI(), 503, "DATABASE_UNAVAILABLE",
						"The data service is temporarily unavailable. Please try again");
			else response.sendError(503);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void revoke(HttpServletRequest request) {
		SecurityContextHolder.clearContext();
		HttpSession session = request.getSession(false);
		if (session != null) session.invalidate();
	}
}
