package com.smartev.assistant.controller.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.smartev.assistant.dto.response.RecommendationResponse;
import com.smartev.assistant.service.RecommendationService;

@RestController
public class RecommendationController {
	private final RecommendationService service;
	public RecommendationController(RecommendationService service) { this.service = service; }
	@GetMapping("/api/recommendations")
	public List<RecommendationResponse> recommendations(@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude) { return service.recommend(latitude, longitude); }
}
