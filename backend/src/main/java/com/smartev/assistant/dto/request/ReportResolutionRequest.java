package com.smartev.assistant.dto.request;

import com.smartev.assistant.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportResolutionRequest(@NotNull ReportStatus status) {
}
