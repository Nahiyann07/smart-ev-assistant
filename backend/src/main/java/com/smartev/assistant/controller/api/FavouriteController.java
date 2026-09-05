package com.smartev.assistant.controller.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartev.assistant.dto.response.FavouriteResponse;
import com.smartev.assistant.security.AppUserPrincipal;
import com.smartev.assistant.service.FavouriteService;

@RestController @RequestMapping("/api/favourites")
public class FavouriteController {
	private final FavouriteService service;
	public FavouriteController(FavouriteService service) { this.service = service; }
	@GetMapping public List<FavouriteResponse> list(@AuthenticationPrincipal AppUserPrincipal user) { return service.list(user.id()); }
	@PostMapping("/{stationId}") public ResponseEntity<FavouriteResponse> add(@PathVariable Long stationId,
			@AuthenticationPrincipal AppUserPrincipal user) {
		FavouriteResponse response = service.add(user.id(), stationId);
		return ResponseEntity.created(URI.create("/api/favourites/" + stationId)).body(response);
	}
	@DeleteMapping("/{stationId}") public ResponseEntity<Void> remove(@PathVariable Long stationId,
			@AuthenticationPrincipal AppUserPrincipal user) { service.remove(user.id(), stationId); return ResponseEntity.noContent().build(); }
}
