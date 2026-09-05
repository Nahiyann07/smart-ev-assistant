package com.smartev.assistant.security;

import jakarta.servlet.http.HttpServletResponse;

/** Keeps browser navigation on the public proxy host instead of exposing the origin host. */
public final class RelativeRedirects {
	private RelativeRedirects() {}

	public static void send(HttpServletResponse response, String location) {
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.setHeader("Location", location);
	}
}
