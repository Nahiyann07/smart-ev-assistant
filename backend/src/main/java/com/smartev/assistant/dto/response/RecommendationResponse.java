package com.smartev.assistant.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(Long stationId, String stationName, String city, int availablePorts,
		BigDecimal chargingSpeedKw, double averageRating, Double distanceKm, double score,
		RecommendationBreakdown breakdown, List<String> reasons) {
	public RecommendationResponse {
		reasons = List.copyOf(reasons);
	}
}
