package com.smartev.assistant.dto.response;

import java.time.Instant;
import com.smartev.assistant.enums.IssueType;
import com.smartev.assistant.enums.ReportStatus;

public record ReportResponse(Long id, Long userId, String userName, Long stationId, String stationName,
		IssueType issueType, String description, ReportStatus status, Long resolvedByUserId,
		String resolvedByName, Instant createdAt, Instant resolvedAt) {
}
