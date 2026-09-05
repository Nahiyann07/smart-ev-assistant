package com.smartev.assistant.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.smartev.assistant.service.StationService;

@Controller
public class StationPageController {
	private final StationService service;
	public StationPageController(StationService service) { this.service = service; }

	@GetMapping("/stations/{id}")
	String details(@PathVariable Long id, Model model) {
		model.addAttribute("station", service.get(id));
		return "stations/details";
	}
}
