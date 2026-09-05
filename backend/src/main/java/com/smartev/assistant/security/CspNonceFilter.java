package com.smartev.assistant.security;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CspNonceFilter extends OncePerRequestFilter {
	private static final SecureRandom RANDOM = new SecureRandom();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		byte[] nonceBytes = new byte[16];
		RANDOM.nextBytes(nonceBytes);
		String nonce = Base64.getEncoder().withoutPadding().encodeToString(nonceBytes);
		request.setAttribute("cspNonce", nonce);
		response.setHeader("Content-Security-Policy", policy(nonce));
		response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
		response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");
		filterChain.doFilter(request, response);
	}

	private String policy(String nonce) {
		return "default-src 'self'; "
				+ "script-src 'self' 'nonce-" + nonce + "' 'strict-dynamic' https: 'unsafe-eval' blob:; "
				+ "script-src-attr 'none'; "
				+ "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
				+ "img-src 'self' data: blob: https://*.googleapis.com https://*.gstatic.com https://*.google.com https://*.googleusercontent.com; "
				+ "connect-src 'self' data: blob: https://*.googleapis.com https://*.gstatic.com https://*.google.com; "
				+ "font-src 'self' https://fonts.gstatic.com; frame-src https://*.google.com; worker-src blob:; "
				+ "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";
	}
}
