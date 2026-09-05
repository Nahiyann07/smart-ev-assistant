package com.smartev.assistant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import com.smartev.assistant.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	@EntityGraph(attributePaths = {"user", "station"})
	List<Review> findAllByStationIdOrderByCreatedAtDesc(Long stationId);
	@EntityGraph(attributePaths = {"user", "station"})
	@Override
	Optional<Review> findById(Long id);
	Optional<Review> findByUserIdAndStationId(Long userId, Long stationId);
	boolean existsByUserIdAndStationId(Long userId, Long stationId);
	long countByUserId(Long userId);
	@Query("select r.station.id as stationId, avg(r.rating) as averageRating, count(r.id) as reviewCount " +
			"from Review r where r.station.id in :stationIds group by r.station.id")
	List<RatingSummary> summarizeRatings(List<Long> stationIds);

	interface RatingSummary {
		Long getStationId();
		Double getAverageRating();
		Long getReviewCount();
	}
}
