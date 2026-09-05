package com.smartev.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.Test;

import com.smartev.assistant.exception.ServiceUnavailableException;

import tools.jackson.databind.json.JsonMapper;

class GoogleRoutesClientTests {
	@Test
	void parsesTheMinimalGoogleResponse() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		@SuppressWarnings("unchecked") HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenReturn("{\"routes\":[{\"duration\":\"720s\",\"distanceMeters\":4200,\"polyline\":{\"encodedPolyline\":\"route\"}}]}");
		when(httpClient.send(any(HttpRequest.class), anyStringBodyHandler())).thenReturn(response);
		RouteProvider.RouteResult result = new GoogleRoutesClient(httpClient, JsonMapper.builder().build())
				.compute(8.52, 76.93, 8.60, 76.98, "secret");
		assertThat(result).isEqualTo(new RouteProvider.RouteResult(4200, 720, "route"));
	}

	@Test
	void malformedProviderDataBecomesServiceUnavailable() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		@SuppressWarnings("unchecked") HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(200); when(response.body()).thenReturn("{\"routes\":[]}");
		when(httpClient.send(any(HttpRequest.class), anyStringBodyHandler())).thenReturn(response);
		assertThatThrownBy(() -> new GoogleRoutesClient(httpClient, JsonMapper.builder().build())
				.compute(8.52, 76.93, 8.60, 76.98, "secret")).isInstanceOf(ServiceUnavailableException.class);
	}

	@Test
	void providerTimeoutBecomesServiceUnavailable() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		when(httpClient.send(any(HttpRequest.class), anyStringBodyHandler())).thenThrow(new HttpTimeoutException("timeout"));
		assertThatThrownBy(() -> new GoogleRoutesClient(httpClient, JsonMapper.builder().build())
				.compute(8.52, 76.93, 8.60, 76.98, "secret")).isInstanceOf(ServiceUnavailableException.class);
	}

	@SuppressWarnings("unchecked")
	private static HttpResponse.BodyHandler<String> anyStringBodyHandler() {
		return any(HttpResponse.BodyHandler.class);
	}
}
