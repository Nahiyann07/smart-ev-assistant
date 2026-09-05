package com.smartev.assistant.dto.response;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		Map<String, String> fieldErrors,
		String path) {
	public ApiErrorResponse {
		fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
	}
}
