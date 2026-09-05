package com.smartev.assistant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.smartev.assistant.config.GoogleMapsProperties;
import com.smartev.assistant.dto.request.RouteRequest;
import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.enums.ChargerType;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.exception.ServiceUnavailableException;
import com.smartev.assistant.repository.StationRepository;

class RouteServiceTests {
	@Test
	void disabledConfigurationReturnsServiceUnavailable() {
		StationRepository repository = mock(StationRepository.class);
		Station station = new Station(new StationRequest("Station", "Road", "City", new BigDecimal("8.52"),
				new BigDecimal("76.93"), ChargerType.AC, 2, 1, 0, new BigDecimal("22"), "24 hours", StationStatus.AVAILABLE, "Test"));
		when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(station));
		RouteService service = new RouteService(repository, mock(RouteProvider.class), new GoogleMapsProperties());
		assertThatThrownBy(() -> service.compute(new RouteRequest(1L, 8.52, 76.93)))
				.isInstanceOf(ServiceUnavailableException.class).hasMessageContaining("not configured");
	}
}
