package com.smartev.assistant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.smartev.assistant.entity.Favourite;

public interface FavouriteRepository extends JpaRepository<Favourite, Long> {
	boolean existsByUserIdAndStationId(Long userId, Long stationId);
	Optional<Favourite> findByUserIdAndStationId(Long userId, Long stationId);
	@EntityGraph(attributePaths = "station")
	List<Favourite> findAllByUserIdOrderByCreatedAtDesc(Long userId);
	long countByUserId(Long userId);
}
