const DEFAULT_LOCATION = { latitude: 8.5241, longitude: 76.9366, name: 'Thiruvananthapuram demo location' };
const LOAD_TIMEOUT_MS = 10000;
let mapsPromise;

export function mapsConfiguration(root) {
  return {
    enabled: root?.dataset.mapsEnabled === 'true',
    key: root?.dataset.googleMapsKey || '',
    mapId: root?.dataset.googleMapId || ''
  };
}

export function loadGoogleMaps(configuration) {
  if (!configuration.enabled || !configuration.key || !configuration.mapId) return Promise.reject(new Error('Google Maps is not configured.'));
  if (window.google?.maps?.importLibrary) return Promise.resolve(window.google.maps);
  if (mapsPromise) return mapsPromise;
  mapsPromise = new Promise((resolve, reject) => {
    const callback = `smartEvMapsReady${Date.now()}`;
    const script = document.createElement('script');
	const nonce = document.querySelector('script[nonce]')?.nonce;
	if (nonce) script.nonce = nonce;
    const previousAuthFailure = window.gm_authFailure;
    let settled = false;
    const cleanup = () => {
      clearTimeout(timeout);
      delete window[callback];
      if (previousAuthFailure) window.gm_authFailure = previousAuthFailure;
      else delete window.gm_authFailure;
    };
    const succeed = () => {
      if (settled) return;
      settled = true;
      cleanup();
      resolve(window.google.maps);
    };
    const fail = message => {
      if (settled) return;
      settled = true;
      cleanup();
      script.remove();
      mapsPromise = undefined;
      reject(new Error(message));
    };
    const timeout = window.setTimeout(() => fail('Google Maps timed out. Check the browser key, API access, billing, and Map ID.'), LOAD_TIMEOUT_MS);
    window[callback] = succeed;
    window.gm_authFailure = () => fail('Google Maps rejected the browser key. Check its referrer restriction, API access, billing, and Map ID.');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(configuration.key)}&callback=${callback}&loading=async&v=weekly`;
    script.async = true;
    script.dataset.smartEvMaps = 'true';
    script.onerror = () => fail('Google Maps could not be loaded. Check your connection and Maps configuration.');
    document.head.append(script);
  });
  return mapsPromise;
}

export function googleMapsSearchUrl(station) {
  const latitude = Number(station?.latitude);
  const longitude = Number(station?.longitude);
  const location = Number.isFinite(latitude) && Number.isFinite(longitude)
    ? { latitude, longitude }
    : DEFAULT_LOCATION;
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${location.latitude},${location.longitude}`)}`;
}

export function updateMapFallback(fallback, station, message) {
  if (!fallback) return;
  const latitude = Number(station?.latitude);
  const longitude = Number(station?.longitude);
  const hasCoordinates = Number.isFinite(latitude) && Number.isFinite(longitude);
  const location = hasCoordinates ? { latitude, longitude, name: station.name } : DEFAULT_LOCATION;
  const name = fallback.querySelector('[data-map-preview-name]');
  const coordinates = fallback.querySelector('[data-map-preview-coordinates]');
  const link = fallback.querySelector('[data-map-preview-link]');
  const status = fallback.querySelector('[data-map-message]');
  if (name) name.textContent = location.name || DEFAULT_LOCATION.name;
  if (coordinates) coordinates.textContent = `${location.latitude.toFixed(4)}, ${location.longitude.toFixed(4)}`;
  if (link) {
    link.href = googleMapsSearchUrl(location);
    link.setAttribute('aria-label', `Open ${location.name || 'demo location'} in Google Maps`);
  }
  if (status && message) status.textContent = message;
  fallback.removeAttribute('hidden');
}

export async function createStationMap({ canvas, fallback, configuration, stations = [], onSelect = () => {} }) {
  canvas?.setAttribute('aria-busy', 'true');
  let Map;
  let AdvancedMarkerElement;
  try {
    await loadGoogleMaps(configuration);
    [{ Map }, { AdvancedMarkerElement }] = await Promise.all([
      google.maps.importLibrary('maps'), google.maps.importLibrary('marker')
    ]);
  } finally {
    canvas?.setAttribute('aria-busy', 'false');
  }
  const usable = stations.filter(station => station.latitude != null && station.longitude != null);
  const center = usable.length ? { lat: Number(usable[0].latitude), lng: Number(usable[0].longitude) } : { lat: 8.5241, lng: 76.9366 };
  const map = new Map(canvas, { center, zoom: usable.length ? 12 : 10, mapId: configuration.mapId, disableDefaultUI: true, zoomControl: true, clickableIcons: false });
  fallback?.setAttribute('hidden', '');
  let markers = [];
  let selectedId = null;

  function select(id, pan = true) {
    selectedId = String(id);
    markers.forEach(({ marker, station, content }) => {
      const selected = String(station.id) === selectedId;
      content.dataset.selected = String(selected);
      marker.zIndex = selected ? 20 : 1;
      if (selected && pan) map.panTo(marker.position);
    });
  }

  function setStations(nextStations) {
    markers.forEach(({ marker }) => { marker.map = null; });
    markers = [];
    const bounds = new google.maps.LatLngBounds();
    nextStations.filter(station => station.latitude != null && station.longitude != null).forEach(station => {
      const position = { lat: Number(station.latitude), lng: Number(station.longitude) };
      const content = document.createElement('button');
      content.type = 'button';
      content.className = 'map-marker';
      content.dataset.status = station.status;
      content.setAttribute('aria-label', `Select ${station.name}`);
      content.innerHTML = `<span>${station.availablePorts}</span>`;
      const marker = new AdvancedMarkerElement({ map, position, title: station.name, content, gmpClickable: true });
      marker.addListener('click', () => { select(station.id); onSelect(station); });
      markers.push({ marker, station, content });
      bounds.extend(position);
    });
    if (markers.length > 1) map.fitBounds(bounds, 44);
    else if (markers.length === 1) { map.setCenter(markers[0].marker.position); map.setZoom(14); }
    if (selectedId) select(selectedId, false);
  }

  setStations(stations);
  return { map, select, setStations };
}

export function decodePolyline(encoded) {
  const points = []; let index = 0; let latitude = 0; let longitude = 0;
  while (index < encoded.length) {
    let result = 0; let shift = 0; let byte;
    do { byte = encoded.charCodeAt(index++) - 63; result |= (byte & 0x1f) << shift; shift += 5; } while (byte >= 0x20);
    latitude += (result & 1) ? ~(result >> 1) : result >> 1;
    result = 0; shift = 0;
    do { byte = encoded.charCodeAt(index++) - 63; result |= (byte & 0x1f) << shift; shift += 5; } while (byte >= 0x20);
    longitude += (result & 1) ? ~(result >> 1) : result >> 1;
    points.push({ lat: latitude / 1e5, lng: longitude / 1e5 });
  }
  return points;
}
