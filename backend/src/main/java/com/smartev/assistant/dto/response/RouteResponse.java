package com.smartev.assistant.dto.response;

public record RouteResponse(Long stationId, int distanceMeters, long durationSeconds, String encodedPolyline) {
}
