package com.smartev.assistant.dto.response;

import com.smartev.assistant.enums.Role;

public record CurrentUserResponse(Long id, String name, String email, Role role) {
}
