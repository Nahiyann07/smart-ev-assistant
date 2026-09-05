package com.smartev.assistant.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.response.RecommendationResponse;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.exception.BadRequestException;
import com.smartev.assistant.repository.ReviewRepository;
import com.smartev.assistant.repository.StationRepository;

@Service
public class RecommendationService {
	private final StationRepository stations; private final ReviewRepository reviews; private final RecommendationScorer scorer;
	public RecommendationService(StationRepository stations, ReviewRepository reviews, RecommendationScorer scorer) {
		this.stations = stations; this.reviews = reviews; this.scorer = scorer;
	}

	@Transactional(readOnly = true)
	public List<RecommendationResponse> recommend(Double latitude, Double longitude) {
		validateCoordinates(latitude, longitude);
		List<Station> candidates = stations.findAllByDeletedAtIsNullOrderByNameAsc().stream()
				.filter(s -> s.getStatus() != StationStatus.UNDER_MAINTENANCE && s.getStatus() != StationStatus.OUT_OF_SERVICE).toList();
		Map<Long, Double> ratings = ratings(candidates);
		List<RecommendationResponse> result = new ArrayList<>();
		for (Station station : candidates) {
			double rating = ratings.getOrDefault(station.getId(), 0.0);
			Double distance = latitude == null ? null : distance(latitude, longitude, station.getLatitude(), station.getLongitude());
			RecommendationScorer.ScoreResult scored = scorer.score(station.getAvailablePorts(), station.getTotalPorts(), rating,
					station.getChargingSpeedKw().doubleValue(), distance);
			result.add(new RecommendationResponse(station.getId(), station.getName(), station.getCity(), station.getAvailablePorts(),
					station.getChargingSpeedKw(), rating, distance, scored.score(), scored.breakdown(), scored.reasons()));
		}
		result.sort(Comparator.comparingDouble(RecommendationResponse::score).reversed()
				.thenComparing(Comparator.comparingDouble(RecommendationResponse::averageRating).reversed())
				.thenComparing(Comparator.comparingInt(RecommendationResponse::availablePorts).reversed())
				.thenComparing(RecommendationResponse::stationId));
		return List.copyOf(result);
	}

	private Map<Long, Double> ratings(List<Station> candidates) {
		Map<Long, Double> values = new HashMap<>(); if (candidates.isEmpty()) return values;
		for (ReviewRepository.RatingSummary row : reviews.summarizeRatings(candidates.stream().map(Station::getId).toList()))
			values.put(row.getStationId(), round(row.getAverageRating()));
		return values;
	}
	private void validateCoordinates(Double lat, Double lon) {
		if ((lat == null) != (lon == null) || lat != null && (!Double.isFinite(lat) || !Double.isFinite(lon)
				|| lat < -90 || lat > 90 || lon < -180 || lon > 180))
			throw new BadRequestException("INVALID_COORDINATES", "Valid latitude and longitude must be supplied together");
	}
	private Double distance(double lat1, double lon1, BigDecimal lat2, BigDecimal lon2) {
		if (lat2 == null || lon2 == null) return null;
		double dLat = Math.toRadians(lat2.doubleValue() - lat1), dLon = Math.toRadians(lon2.doubleValue() - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2.doubleValue()))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return round(6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
	}
	private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
