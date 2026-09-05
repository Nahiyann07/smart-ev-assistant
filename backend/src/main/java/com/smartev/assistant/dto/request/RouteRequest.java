package com.smartev.assistant.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RouteRequest(
		@NotNull @Positive Long stationId,
		@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double originLatitude,
		@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double originLongitude) {
}
