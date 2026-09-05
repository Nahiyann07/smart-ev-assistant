package com.smartev.assistant.dto.response;

import java.time.Instant;

import com.smartev.assistant.enums.Role;

public record RegistrationResponse(
		Long id,
		String name,
		String email,
		Role role,
		Instant createdAt) {
}
