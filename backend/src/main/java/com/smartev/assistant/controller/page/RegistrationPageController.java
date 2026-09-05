package com.smartev.assistant.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RegistrationPageController {

	@GetMapping("/register")
	String registrationPage() {
		return "auth/register";
	}
}
