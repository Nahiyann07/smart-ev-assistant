package com.smartev.assistant.controller.page;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import com.smartev.assistant.config.GoogleMapsProperties;

@ControllerAdvice(basePackages = "com.smartev.assistant.controller.page")
public class MapsModelAdvice {
	private final GoogleMapsProperties properties;
	public MapsModelAdvice(GoogleMapsProperties properties) { this.properties = properties; }

	@ModelAttribute
	void mapsConfiguration(Model model) {
		model.addAttribute("mapsEnabled", properties.browserEnabled());
		model.addAttribute("googleMapsBrowserKey", properties.getBrowserKey());
		model.addAttribute("googleMapId", properties.getMapId());
	}
}
