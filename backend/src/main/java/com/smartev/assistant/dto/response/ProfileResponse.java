package com.smartev.assistant.dto.response;

import java.time.Instant;

public record ProfileResponse(Long id, String name, String email, Instant createdAt,
		long favouriteCount, long reviewCount, long reportCount) {}
