package com.smartev.assistant.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.response.FavouriteResponse;
import com.smartev.assistant.entity.Favourite;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.exception.ConflictException;
import com.smartev.assistant.exception.NotFoundException;
import com.smartev.assistant.repository.FavouriteRepository;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;

@Service
public class FavouriteService {
	private final FavouriteRepository favourites; private final StationRepository stations; private final UserRepository users;
	public FavouriteService(FavouriteRepository favourites, StationRepository stations, UserRepository users) {
		this.favourites = favourites; this.stations = stations; this.users = users;
	}
	@Transactional(readOnly = true)
	public List<FavouriteResponse> list(Long userId) {
		return favourites.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
				.filter(favourite -> favourite.getStation().getDeletedAt() == null).map(this::response).toList();
	}
	@Transactional
	public FavouriteResponse add(Long userId, Long stationId) {
		if (favourites.existsByUserIdAndStationId(userId, stationId)) throw duplicate();
		User user = users.findById(userId).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
		Station station = stations.findByIdAndDeletedAtIsNull(stationId).orElseThrow(() -> new NotFoundException("STATION_NOT_FOUND", "Station not found"));
		try { return response(favourites.saveAndFlush(new Favourite(user, station))); }
		catch (DataIntegrityViolationException exception) { throw duplicate(); }
	}
	@Transactional
	public void remove(Long userId, Long stationId) {
		Favourite favourite = favourites.findByUserIdAndStationId(userId, stationId)
				.orElseThrow(() -> new NotFoundException("FAVOURITE_NOT_FOUND", "Favourite not found"));
		favourites.delete(favourite);
	}
	private FavouriteResponse response(Favourite favourite) {
		Station station = favourite.getStation();
		return new FavouriteResponse(favourite.getId(), station.getId(), station.getName(), station.getCity(),
				station.getStatus(), station.getAvailablePorts(), station.getChargingSpeedKw(), favourite.getCreatedAt());
	}
	private ConflictException duplicate() { return new ConflictException("FAVOURITE_ALREADY_EXISTS", "Station is already a favourite"); }
}
