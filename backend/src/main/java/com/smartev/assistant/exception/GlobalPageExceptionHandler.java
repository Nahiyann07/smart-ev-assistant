package com.smartev.assistant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = "com.smartev.assistant.controller.page")
public class GlobalPageExceptionHandler {
	@ExceptionHandler(ApiException.class)
	ModelAndView handle(ApiException exception) {
		HttpStatus status = exception.getStatus();
		ModelAndView view = new ModelAndView("error/" + status.value());
		view.setStatus(status);
		view.addObject("message", exception.getMessage());
		view.addObject("status", status.value());
		return view;
	}
}
