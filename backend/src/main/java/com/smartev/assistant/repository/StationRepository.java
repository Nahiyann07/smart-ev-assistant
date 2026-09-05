package com.smartev.assistant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.smartev.assistant.entity.Station;
import com.smartev.assistant.enums.StationStatus;

public interface StationRepository extends JpaRepository<Station, Long>, JpaSpecificationExecutor<Station> {
	Optional<Station> findByIdAndDeletedAtIsNull(Long id);
	List<Station> findAllByDeletedAtIsNullOrderByNameAsc();
	long countByDeletedAtIsNull();
	long countByStatusAndDeletedAtIsNull(StationStatus status);
}
