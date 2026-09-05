package com.smartev.assistant.mapper;

import org.springframework.stereotype.Component;

import com.smartev.assistant.dto.response.StationResponse;
import com.smartev.assistant.entity.Station;

@Component
public class StationMapper {
	public StationResponse toResponse(Station station) {
		return new StationResponse(station.getId(), station.getName(), station.getAddress(), station.getCity(),
				station.getLatitude(), station.getLongitude(), station.getChargerType(), station.getTotalPorts(),
				station.getAvailablePorts(), station.getOccupiedPorts(), station.getOutOfServicePorts(),
				station.getChargingSpeedKw(), station.getOperatingHours(), station.getStatus(), station.getDescription(),
				"SIMULATED_DATABASE_STATE",
				station.getCreatedAt(), station.getUpdatedAt());
	}
}
