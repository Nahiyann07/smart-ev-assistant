package com.smartev.assistant.dto.request;

import com.smartev.assistant.enums.StationStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StationStatusRequest(@NotNull StationStatus status,
		@Min(0) int availablePorts, @Min(0) int outOfServicePorts) {
}
