package com.smartev.assistant.service;

public interface RouteProvider {
	RouteResult compute(double originLatitude, double originLongitude, double destinationLatitude,
			double destinationLongitude, String apiKey);

	record RouteResult(int distanceMeters, long durationSeconds, String encodedPolyline) {}
}
