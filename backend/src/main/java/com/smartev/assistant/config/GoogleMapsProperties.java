package com.smartev.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.google-maps")
public class GoogleMapsProperties {
	private String browserKey = "";
	private String mapId = "";
	private String routesServerKey = "";

	public String getBrowserKey() { return browserKey; }
	public void setBrowserKey(String browserKey) { this.browserKey = clean(browserKey); }
	public String getMapId() { return mapId; }
	public void setMapId(String mapId) { this.mapId = clean(mapId); }
	public String getRoutesServerKey() { return routesServerKey; }
	public void setRoutesServerKey(String routesServerKey) { this.routesServerKey = clean(routesServerKey); }
	public boolean browserEnabled() { return !browserKey.isBlank() && !mapId.isBlank(); }
	public boolean routesEnabled() { return !routesServerKey.isBlank(); }
	private String clean(String value) { return value == null ? "" : value.trim(); }
}
