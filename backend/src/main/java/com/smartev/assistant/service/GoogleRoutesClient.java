package com.smartev.assistant.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.smartev.assistant.exception.ServiceUnavailableException;

@Component
public class GoogleRoutesClient implements RouteProvider {
	private static final URI ROUTES_URI = URI.create("https://routes.googleapis.com/directions/v2:computeRoutes");
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	@Autowired
	public GoogleRoutesClient(ObjectMapper objectMapper) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build(), objectMapper);
	}

	GoogleRoutesClient(HttpClient httpClient, ObjectMapper objectMapper) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public RouteResult compute(double originLatitude, double originLongitude, double destinationLatitude,
			double destinationLongitude, String apiKey) {
		try {
			String body = objectMapper.createObjectNode()
					.set("origin", waypoint(originLatitude, originLongitude))
					.set("destination", waypoint(destinationLatitude, destinationLongitude))
					.put("travelMode", "DRIVE")
					.put("routingPreference", "TRAFFIC_AWARE")
					.put("units", "METRIC").toString();
			HttpRequest request = HttpRequest.newBuilder(ROUTES_URI)
					.timeout(Duration.ofSeconds(8))
					.header("Content-Type", "application/json")
					.header("X-Goog-Api-Key", apiKey)
					.header("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline")
					.POST(HttpRequest.BodyPublishers.ofString(body)).build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) throw unavailable();
			JsonNode route = objectMapper.readTree(response.body()).path("routes").path(0);
			int distance = route.path("distanceMeters").asInt(-1);
			long duration = parseDuration(route.path("duration").asText());
			String polyline = route.path("polyline").path("encodedPolyline").asText();
			if (distance < 0 || duration < 0 || polyline.isBlank()) throw unavailable();
			return new RouteResult(distance, duration, polyline);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw unavailable();
		} catch (IOException | RuntimeException exception) {
			if (exception instanceof ServiceUnavailableException serviceUnavailable) throw serviceUnavailable;
			throw unavailable();
		}
	}

	private JsonNode waypoint(double latitude, double longitude) {
		return objectMapper.createObjectNode().set("location", objectMapper.createObjectNode().set("latLng",
				objectMapper.createObjectNode().put("latitude", latitude).put("longitude", longitude)));
	}

	private long parseDuration(String duration) {
		if (duration == null || !duration.endsWith("s")) return -1;
		return Math.round(Double.parseDouble(duration.substring(0, duration.length() - 1)));
	}

	private ServiceUnavailableException unavailable() {
		return new ServiceUnavailableException("ROUTES_UNAVAILABLE", "Route guidance is temporarily unavailable. You can still open the station in Google Maps");
	}
}
