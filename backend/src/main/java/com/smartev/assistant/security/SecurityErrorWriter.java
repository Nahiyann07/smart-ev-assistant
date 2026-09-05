package com.smartev.assistant.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.smartev.assistant.dto.response.ApiErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorWriter {
	private final ObjectMapper objectMapper;

	public SecurityErrorWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper.rebuild().build();
	}

	public void write(HttpServletResponse response, String path, int status, String code, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(),
				new ApiErrorResponse(Instant.now(), status, code, message, Map.of(), path));
	}
}
