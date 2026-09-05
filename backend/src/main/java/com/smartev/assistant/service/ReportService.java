package com.smartev.assistant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartev.assistant.dto.request.ReportRequest;
import com.smartev.assistant.dto.response.ReportResponse;
import com.smartev.assistant.entity.Report;
import com.smartev.assistant.entity.Station;
import com.smartev.assistant.entity.User;
import com.smartev.assistant.enums.ReportStatus;
import com.smartev.assistant.exception.BadRequestException;
import com.smartev.assistant.exception.NotFoundException;
import com.smartev.assistant.repository.ReportRepository;
import com.smartev.assistant.repository.StationRepository;
import com.smartev.assistant.repository.UserRepository;

@Service
public class ReportService {
	private final ReportRepository reports; private final StationRepository stations; private final UserRepository users;
	public ReportService(ReportRepository reports, StationRepository stations, UserRepository users) {
		this.reports = reports; this.stations = stations; this.users = users;
	}

	@Transactional
	public ReportResponse submit(Long stationId, Long userId, ReportRequest request) {
		Station station = stations.findByIdAndDeletedAtIsNull(stationId).orElseThrow(() -> notFound("STATION_NOT_FOUND", "Station not found"));
		User user = users.findById(userId).orElseThrow(() -> notFound("USER_NOT_FOUND", "User not found"));
		return response(reports.save(new Report(user, station, request.issueType(), request.description().trim())));
	}

	@Transactional(readOnly = true)
	public List<ReportResponse> userHistory(Long userId) {
		return reports.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public List<ReportResponse> adminQueue(ReportStatus status) {
		List<Report> result = status == null ? reports.findAllByOrderByCreatedAtDesc() : reports.findAllByStatusOrderByCreatedAtDesc(status);
		return result.stream().map(this::response).toList();
	}

	@Transactional
	public ReportResponse resolve(Long id, Long adminId, ReportStatus resolution) {
		if (resolution == ReportStatus.PENDING) throw new BadRequestException("INVALID_REPORT_TRANSITION", "A report can only be resolved or rejected");
		Report report = reports.findById(id).orElseThrow(() -> notFound("REPORT_NOT_FOUND", "Report not found"));
		if (report.getStatus() != ReportStatus.PENDING) throw new BadRequestException("INVALID_REPORT_TRANSITION", "Only pending reports can change status");
		User admin = users.findById(adminId).orElseThrow(() -> notFound("USER_NOT_FOUND", "User not found"));
		report.resolve(resolution, admin); return response(report);
	}

	private ReportResponse response(Report report) {
		User resolver = report.getResolvedBy();
		return new ReportResponse(report.getId(), report.getUser().getId(), report.getUser().getName(),
				report.getStation().getId(), report.getStation().getName(), report.getIssueType(), report.getDescription(),
				report.getStatus(), resolver == null ? null : resolver.getId(), resolver == null ? null : resolver.getName(),
				report.getCreatedAt(), report.getResolvedAt());
	}
	private NotFoundException notFound(String code, String message) { return new NotFoundException(code, message); }
}
