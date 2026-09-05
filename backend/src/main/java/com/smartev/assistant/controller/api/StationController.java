package com.smartev.assistant.controller.api;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.response.PageResponse;
import com.smartev.assistant.dto.response.StationResponse;
import com.smartev.assistant.dto.response.StationSummaryResponse;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.service.StationSearchService;
import com.smartev.assistant.service.StationService;

@RestController
@RequestMapping("/api/stations")
public class StationController {
	private final StationSearchService searchService;
	private final StationService stationService;
	public StationController(StationSearchService searchService, StationService stationService) {
		this.searchService = searchService; this.stationService = stationService;
	}

	@GetMapping
	public PageResponse<StationSummaryResponse> search(
			@RequestParam(required = false) String query, @RequestParam(required = false) String city,
			@RequestParam(required = false) ChargerType chargerType,
			@RequestParam(defaultValue = "false") boolean availableOnly,
			@RequestParam(required = false) BigDecimal minSpeedKw,
			@RequestParam(required = false) Double minRating,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
		return searchService.search(query, city, chargerType, availableOnly, minSpeedKw, minRating,
				sort, latitude, longitude, page, size);
	}

	@GetMapping("/{id}") public StationResponse get(@PathVariable Long id) { return stationService.get(id); }
}
