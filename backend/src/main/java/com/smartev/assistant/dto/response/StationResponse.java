package com.smartev.assistant.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.StationStatus;

public record StationResponse(Long id, String name, String address, String city,
		BigDecimal latitude, BigDecimal longitude, ChargerType chargerType,
		int totalPorts, int availablePorts, int occupiedPorts, int outOfServicePorts,
		BigDecimal chargingSpeedKw, String operatingHours, StationStatus status,
		String description, String availabilitySource, Instant createdAt, Instant updatedAt) {
}
