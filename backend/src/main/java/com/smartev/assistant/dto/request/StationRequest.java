package com.smartev.assistant.dto.request;

import java.math.BigDecimal;

import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.StationStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StationRequest(
		@NotBlank @Size(max = 150) String name,
		@NotBlank @Size(max = 255) String address,
		@NotBlank @Size(max = 100) String city,
		@DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitude,
		@NotNull ChargerType chargerType,
		@Min(1) int totalPorts,
		@Min(0) int availablePorts,
		@Min(0) int outOfServicePorts,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal chargingSpeedKw,
		@NotBlank @Size(max = 100) String operatingHours,
		@NotNull StationStatus status,
		@NotBlank @Size(max = 5000) String description) {
}
