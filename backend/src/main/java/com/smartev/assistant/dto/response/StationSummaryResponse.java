package com.smartev.assistant.dto.response;

import java.math.BigDecimal;

import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.StationStatus;

public record StationSummaryResponse(Long id, String name, String address, String city,
		BigDecimal latitude, BigDecimal longitude,
		ChargerType chargerType, BigDecimal chargingSpeedKw, StationStatus status,
		int totalPorts, int availablePorts, int occupiedPorts, double averageRating,
		long reviewCount, Double distanceKm) {
}
