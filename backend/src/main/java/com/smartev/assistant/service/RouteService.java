package com.smartev.assistant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.config.GoogleMapsProperties;
import com.smartev.assistant.dto.request.RouteRequest;
import com.smartev.assistant.dto.response.RouteResponse;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.exception.BadRequestException;
import com.smartev.assistant.exception.NotFoundException;
import com.smartev.assistant.exception.ServiceUnavailableException;
import com.smartev.assistant.repository.StationRepository;

@Service
public class RouteService {
	private final StationRepository stationRepository;
	private final RouteProvider routeProvider;
	private final GoogleMapsProperties properties;

	public RouteService(StationRepository stationRepository, RouteProvider routeProvider, GoogleMapsProperties properties) {
		this.stationRepository = stationRepository;
		this.routeProvider = routeProvider;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public RouteResponse compute(RouteRequest request) {
		if (!Double.isFinite(request.originLatitude()) || !Double.isFinite(request.originLongitude()))
			throw new BadRequestException("INVALID_COORDINATES", "Coordinates must be finite numbers");
		Station station = stationRepository.findByIdAndDeletedAtIsNull(request.stationId())
				.orElseThrow(() -> new NotFoundException("STATION_NOT_FOUND", "Station not found"));
		if (!properties.routesEnabled())
			throw new ServiceUnavailableException("ROUTES_NOT_CONFIGURED", "Route guidance is not configured yet");
		if (station.getLatitude() == null || station.getLongitude() == null)
			throw new ServiceUnavailableException("STATION_COORDINATES_UNAVAILABLE", "This station does not have route coordinates");
		RouteProvider.RouteResult result = routeProvider.compute(request.originLatitude(), request.originLongitude(),
				station.getLatitude().doubleValue(), station.getLongitude().doubleValue(), properties.getRoutesServerKey());
		return new RouteResponse(station.getId(), result.distanceMeters(), result.durationSeconds(), result.encodedPolyline());
	}
}
