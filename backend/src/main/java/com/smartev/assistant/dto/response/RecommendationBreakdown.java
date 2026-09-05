package com.smartev.assistant.dto.response;

public record RecommendationBreakdown(double availabilityScore, double ratingScore, double speedScore,
		Double distanceScore, double availabilityPoints, double ratingPoints, double speedPoints,
		double distancePoints) {
}
