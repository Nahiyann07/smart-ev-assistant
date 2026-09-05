package com.smartev.assistant.dto.response;

import java.time.Instant;

public record ReviewResponse(Long id, Long userId, String userName, Long stationId,
		int rating, String comment, Instant createdAt, Instant updatedAt) {
}
