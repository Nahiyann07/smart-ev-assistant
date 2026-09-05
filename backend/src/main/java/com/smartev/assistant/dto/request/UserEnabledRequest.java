package com.smartev.assistant.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserEnabledRequest(@NotNull(message = "Enabled is required") Boolean enabled) {}
