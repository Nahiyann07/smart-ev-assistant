package com.smartev.assistant.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends ApiException {
	public ServiceUnavailableException(String code, String message) { super(HttpStatus.SERVICE_UNAVAILABLE, code, message); }
}
