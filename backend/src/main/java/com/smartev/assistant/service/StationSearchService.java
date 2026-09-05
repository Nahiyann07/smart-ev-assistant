package com.smartev.assistant.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.response.PageResponse;
import com.smartev.assistant.dto.response.StationSummaryResponse;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.exception.BadRequestException;
import com.smartev.assistant.repository.ReviewRepository;
import com.smartev.assistant.repository.StationRepository;

@Service
public class StationSearchService {
	private final StationRepository stationRepository;
	private final ReviewRepository reviewRepository;

	public StationSearchService(StationRepository stationRepository, ReviewRepository reviewRepository) {
		this.stationRepository = stationRepository; this.reviewRepository = reviewRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<StationSummaryResponse> search(String query, String city, ChargerType chargerType,
			boolean availableOnly, BigDecimal minSpeedKw, Double minRating, String sort,
			Double latitude, Double longitude, int page, int size) {
		validate(latitude, longitude, minRating, page, size, sort);
		Specification<Station> spec = (root, criteria, builder) -> builder.isNull(root.get("deletedAt"));
		if (hasText(query)) {
			String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
			spec = spec.and((root, criteria, b) -> b.or(b.like(b.lower(root.get("name")), pattern),
					b.like(b.lower(root.get("address")), pattern), b.like(b.lower(root.get("city")), pattern)));
		}
		if (hasText(city)) {
			String value = city.trim().toLowerCase(Locale.ROOT);
			spec = spec.and((root, criteria, b) -> b.equal(b.lower(root.get("city")), value));
		}
		if (chargerType != null) spec = spec.and((root, criteria, b) -> b.equal(root.get("chargerType"), chargerType));
		if (availableOnly) spec = spec.and((root, criteria, b) -> b.greaterThan(root.get("availablePorts"), 0));
		if (minSpeedKw != null) spec = spec.and((root, criteria, b) -> b.greaterThanOrEqualTo(root.get("chargingSpeedKw"), minSpeedKw));

		List<Station> stations = stationRepository.findAll(spec);
		Map<Long, Rating> ratings = ratings(stations);
		List<StationSummaryResponse> results = new ArrayList<>();
		for (Station station : stations) {
			Rating rating = ratings.getOrDefault(station.getId(), new Rating(0, 0));
			if (minRating != null && rating.average < minRating) continue;
			Double distance = latitude == null ? null : haversine(latitude, longitude,
					station.getLatitude(), station.getLongitude());
			results.add(new StationSummaryResponse(station.getId(), station.getName(), station.getAddress(), station.getCity(),
					station.getLatitude(), station.getLongitude(),
					station.getChargerType(), station.getChargingSpeedKw(), station.getStatus(), station.getTotalPorts(),
					station.getAvailablePorts(), station.getOccupiedPorts(), rating.average, rating.count, distance));
		}
		results.sort(comparator(sort, latitude != null));
		long total = results.size();
		long offset = (long) page * size;
		int from = (int) Math.min(offset, results.size());
		int to = Math.min(from + size, results.size());
		int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);
		return new PageResponse<>(List.copyOf(results.subList(from, to)), page, size, total, totalPages,
				page == 0, totalPages == 0 || page >= totalPages - 1);
	}

	private Map<Long, Rating> ratings(List<Station> stations) {
		Map<Long, Rating> result = new HashMap<>();
		if (stations.isEmpty()) return result;
		List<Long> ids = stations.stream().map(Station::getId).toList();
		for (ReviewRepository.RatingSummary row : reviewRepository.summarizeRatings(ids))
			result.put(row.getStationId(), new Rating(round(row.getAverageRating()), row.getReviewCount()));
		return result;
	}

	private Comparator<StationSummaryResponse> comparator(String sort, boolean hasCoordinates) {
		Comparator<StationSummaryResponse> byId = Comparator.comparing(StationSummaryResponse::id);
		return switch (sort == null ? "name" : sort) {
			case "speed" -> Comparator.comparing(StationSummaryResponse::chargingSpeedKw).reversed().thenComparing(byId);
			case "rating" -> Comparator.comparingDouble(StationSummaryResponse::averageRating).reversed().thenComparing(byId);
			case "distance" -> {
				if (!hasCoordinates) throw new BadRequestException("COORDINATES_REQUIRED", "Distance sorting requires latitude and longitude");
				yield Comparator.comparing(StationSummaryResponse::distanceKm,
						Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(byId);
			}
			default -> Comparator.comparing(StationSummaryResponse::name, String.CASE_INSENSITIVE_ORDER).thenComparing(byId);
		};
	}

	private void validate(Double latitude, Double longitude, Double minRating, int page, int size, String sort) {
		if (page < 0 || size < 1 || size > 50) throw new BadRequestException("INVALID_PAGE", "Page must be non-negative and size must be 1 to 50");
		if ((latitude == null) != (longitude == null)) throw new BadRequestException("INVALID_COORDINATES", "Latitude and longitude must be supplied together");
		if (latitude != null && (!Double.isFinite(latitude) || !Double.isFinite(longitude)
				|| latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180))
			throw new BadRequestException("INVALID_COORDINATES", "Coordinates are outside their valid ranges");
		if (minRating != null && (!Double.isFinite(minRating) || minRating < 0 || minRating > 5))
			throw new BadRequestException("INVALID_RATING", "Minimum rating must be between 0 and 5");
		if (sort != null && !List.of("name", "speed", "rating", "distance").contains(sort))
			throw new BadRequestException("INVALID_SORT", "Sort must be name, speed, rating, or distance");
	}

	private Double haversine(double lat1, double lon1, BigDecimal lat2, BigDecimal lon2) {
		if (lat2 == null || lon2 == null) return null;
		double latDistance = Math.toRadians(lat2.doubleValue() - lat1);
		double lonDistance = Math.toRadians(lon2.doubleValue() - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2.doubleValue())) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		return round(6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
	}

	private double round(double value) { return Math.round(value * 100.0) / 100.0; }
	private boolean hasText(String value) { return value != null && !value.isBlank(); }
	private record Rating(double average, long count) {}
}
