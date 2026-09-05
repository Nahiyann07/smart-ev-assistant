package com.smartev.assistant.dto.response;

public record DashboardStatisticsResponse(long totalUsers, long enabledUsers, long activeStations,
		long availableStations, long maintenanceStations, long outOfServiceStations,
		long pendingReports, long totalReviews, long totalFavourites) {}
