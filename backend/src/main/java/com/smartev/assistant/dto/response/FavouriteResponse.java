package com.smartev.assistant.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import com.smartev.assistant.enums.StationStatus;

public record FavouriteResponse(Long id, Long stationId, String stationName, String city,
		StationStatus status, int availablePorts, BigDecimal chargingSpeedKw, Instant createdAt) {
}
