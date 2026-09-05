package com.smartev.assistant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.request.StationRequest;
import com.smartev.assistant.dto.request.StationStatusRequest;
import com.smartev.assistant.dto.response.StationResponse;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.enums.StationStatus;
import com.smartev.assistant.exception.BadRequestException;
import com.smartev.assistant.exception.NotFoundException;
import com.smartev.assistant.mapper.StationMapper;
import com.smartev.assistant.repository.StationRepository;

@Service
public class StationService {
	private final StationRepository repository;
	private final StationMapper mapper;

	public StationService(StationRepository repository, StationMapper mapper) {
		this.repository = repository; this.mapper = mapper;
	}

	@Transactional
	public StationResponse create(StationRequest request) {
		validatePortsAndStatus(request);
		return mapper.toResponse(repository.save(new Station(request)));
	}

	@Transactional
	public StationResponse update(Long id, StationRequest request) {
		validatePortsAndStatus(request);
		Station station = active(id);
		station.apply(request);
		return mapper.toResponse(repository.save(station));
	}

	@Transactional
	public void delete(Long id) { active(id).softDelete(); }

	@Transactional
	public StationResponse updateStatus(Long id, StationStatusRequest request) {
		Station station = active(id);
		validatePortsAndStatus(station.getTotalPorts(), request.availablePorts(), request.outOfServicePorts(), request.status());
		station.updateAvailability(request.status(), request.availablePorts(), request.outOfServicePorts());
		return mapper.toResponse(station);
	}

	@Transactional(readOnly = true)
	public StationResponse get(Long id) { return mapper.toResponse(active(id)); }

	@Transactional(readOnly = true)
	public List<StationResponse> list() {
		return repository.findAllByDeletedAtIsNullOrderByNameAsc().stream().map(mapper::toResponse).toList();
	}

	private Station active(Long id) {
		return repository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new NotFoundException("STATION_NOT_FOUND", "Station not found"));
	}

	private void validatePortsAndStatus(StationRequest r) {
		validatePortsAndStatus(r.totalPorts(), r.availablePorts(), r.outOfServicePorts(), r.status());
	}

	private void validatePortsAndStatus(int totalPorts, int availablePorts, int outOfServicePorts, StationStatus status) {
		if ((long) availablePorts + outOfServicePorts > totalPorts)
			throw invalid("Available and out-of-service ports cannot exceed total ports");
		boolean valid = switch (status) {
			case AVAILABLE -> availablePorts > 0;
			case OCCUPIED -> availablePorts == 0 && totalPorts > outOfServicePorts;
			case OUT_OF_SERVICE -> availablePorts == 0 && outOfServicePorts == totalPorts;
			case UNDER_MAINTENANCE -> availablePorts == 0 && outOfServicePorts > 0;
		};
		if (!valid) throw invalid("Port counts do not match station status " + status);
	}

	private BadRequestException invalid(String message) {
		return new BadRequestException("INVALID_STATION_STATUS", message);
	}
}
