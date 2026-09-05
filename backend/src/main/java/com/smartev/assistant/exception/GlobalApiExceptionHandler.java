package com.smartev.assistant.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartev.assistant.dto.response.ApiErrorResponse;

@RestControllerAdvice(basePackages = "com.smartev.assistant.controller.api")
public class GlobalApiExceptionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

	@ExceptionHandler(TooManyRequestsException.class)
	ResponseEntity<ApiErrorResponse> handleRateLimit(TooManyRequestsException exception, ServletWebRequest request) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()));
		return build(exception.getStatus(), exception.getCode(), exception.getMessage(), Map.of(), request, headers);
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, ServletWebRequest request) {
		return build(exception.getStatus(), exception.getCode(), exception.getMessage(), Map.of(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, ServletWebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields", errors, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception, ServletWebRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request body is missing or invalid", Map.of(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiErrorResponse> handleIntegrityViolation(DataIntegrityViolationException exception, ServletWebRequest request) {
		return build(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data", Map.of(), request);
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
			ConstraintViolationException.class })
	ResponseEntity<ApiErrorResponse> handleInvalidParameter(Exception exception, ServletWebRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "One or more request parameters are invalid", Map.of(), request);
	}

	@ExceptionHandler(DataAccessException.class)
	ResponseEntity<ApiErrorResponse> handleDatabaseFailure(DataAccessException exception, ServletWebRequest request) {
		LOGGER.warn("Database request failed for {} {} ({})", request.getRequest().getMethod(),
				request.getRequest().getRequestURI(), exception.getClass().getSimpleName());
		return build(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
				"The data service is temporarily unavailable. Please try again", Map.of(), request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, ServletWebRequest request) {
		LOGGER.error("Unexpected request failure for {} {} ({})", request.getRequest().getMethod(),
				request.getRequest().getRequestURI(), exception.getClass().getSimpleName());
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
				"An unexpected error occurred. Please try again", Map.of(), request);
	}

	private ResponseEntity<ApiErrorResponse> build(
			HttpStatus status,
			String code,
			String message,
			Map<String, String> fieldErrors,
			ServletWebRequest request) {
		return build(status, code, message, fieldErrors, request, new HttpHeaders());
	}

	private ResponseEntity<ApiErrorResponse> build(
			HttpStatus status,
			String code,
			String message,
			Map<String, String> fieldErrors,
			ServletWebRequest request,
			HttpHeaders headers) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(), status.value(), code, message, fieldErrors, request.getRequest().getRequestURI());
		return ResponseEntity.status(status).headers(headers).body(response);
	}
}
