package com.smartev.assistant.dto.request;

import com.smartev.assistant.enums.IssueType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(@NotNull IssueType issueType,
		@NotBlank(message = "Description is required") @Size(max = 1500) String description) {
}
