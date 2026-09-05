package com.smartev.assistant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.smartev.assistant.entity.Report;
import com.smartev.assistant.enums.ReportStatus;

public interface ReportRepository extends JpaRepository<Report, Long> {
	@EntityGraph(attributePaths = {"user", "station", "resolvedBy"})
	List<Report> findAllByUserIdOrderByCreatedAtDesc(Long userId);
	@EntityGraph(attributePaths = {"user", "station", "resolvedBy"})
	List<Report> findAllByOrderByCreatedAtDesc();
	@EntityGraph(attributePaths = {"user", "station", "resolvedBy"})
	List<Report> findAllByStatusOrderByCreatedAtDesc(ReportStatus status);
	@EntityGraph(attributePaths = {"user", "station", "resolvedBy"})
	@Override
	Optional<Report> findById(Long id);
	long countByStatus(ReportStatus status);
	long countByUserId(Long userId);
}
