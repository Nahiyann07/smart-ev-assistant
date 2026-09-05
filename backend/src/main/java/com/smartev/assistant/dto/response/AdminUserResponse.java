package com.smartev.assistant.dto.response;

import java.time.Instant;
import com.smartev.assistant.enums.Role;

public record AdminUserResponse(Long id, String name, String email, Role role, boolean enabled,
		Instant createdAt, Instant updatedAt) {}
