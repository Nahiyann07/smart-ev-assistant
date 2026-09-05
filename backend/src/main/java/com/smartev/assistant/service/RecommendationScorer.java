package com.smartev.assistant.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.smartev.assistant.dto.response.RecommendationBreakdown;

@Component
public class RecommendationScorer {
	public ScoreResult score(int availablePorts, int totalPorts, double averageRating, double speedKw, Double distanceKm) {
		double availability = clamp(availablePorts / (double) totalPorts);
		double rating = clamp(averageRating / 5.0);
		double speed = clamp(speedKw / 150.0);
		double distance = distanceKm == null ? 0 : clamp(1 - distanceKm / 25.0);
		double factor = distanceKm == null ? 1.0 / 0.9 : 1.0;
		double availabilityPoints = availability * 40 * factor;
		double ratingPoints = rating * 30 * factor;
		double speedPoints = speed * 20 * factor;
		double distancePoints = distanceKm == null ? 0 : distance * 10;
		double total = round(availabilityPoints + ratingPoints + speedPoints + distancePoints);
		RecommendationBreakdown breakdown = new RecommendationBreakdown(round(availability), round(rating), round(speed),
				distanceKm == null ? null : round(distance), round(availabilityPoints), round(ratingPoints),
				round(speedPoints), round(distancePoints));
		List<String> reasons = new ArrayList<>();
		if (availability >= 0.5) reasons.add("Good port availability");
		if (rating >= 0.8) reasons.add("Highly rated by drivers");
		if (speed >= 0.8) reasons.add("High-speed charging");
		if (distanceKm != null && distanceKm <= 5) reasons.add("Close to your location");
		if (reasons.isEmpty()) reasons.add("Best overall match among operational stations");
		return new ScoreResult(total, breakdown, List.copyOf(reasons));
	}
	private double clamp(double value) { return Math.max(0, Math.min(1, value)); }
	private double round(double value) { return Math.round(value * 100.0) / 100.0; }
	public record ScoreResult(double score, RecommendationBreakdown breakdown, List<String> reasons) {
		public ScoreResult { reasons = List.copyOf(reasons); }
	}
}
