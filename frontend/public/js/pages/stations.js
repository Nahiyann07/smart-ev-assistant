import { api, emptyState, stationCard, toast } from '../api.js';
import { revealNew } from '../motion.js';
import { createStationMap, mapsConfiguration, updateMapFallback } from '../maps.js';

const form = document.querySelector('[data-station-filters]');
const results = document.querySelector('[data-station-results]');
const count = document.querySelector('[data-results-count]');
const pager = document.querySelector('[data-pagination]');
const workspace = document.querySelector('[data-discovery-workspace]');
const configRoot = document.querySelector('[data-map-config]');
let page = 0;
let coordinates = null;
let mapController = null;
let stationCache = [];

function selectStation(station, scroll = false) {
  station = stationCache.find(item => String(item.id) === String(station.id)) || station;
  const card = results.querySelector(`[data-station-card="${station.id}"]`);
  results.querySelectorAll('[data-station-card]').forEach(item => item.dataset.selected = String(item === card));
  mapController?.select(station.id, !scroll);
  updateMapFallback(document.querySelector('[data-map-fallback]'), station);
  if (scroll && card) card.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'nearest' });
}

async function syncMap(stations) {
  const configuration = mapsConfiguration(configRoot);
  const fallback = document.querySelector('[data-map-fallback]');
  const fallbackMessage = configuration.enabled
    ? 'The interactive map could not be loaded. Open this database station directly in Google Maps.'
    : 'Interactive markers need restricted Google Maps credentials. This location link works without a key.';
  updateMapFallback(fallback, stations[0], fallbackMessage);
  if (!configuration.enabled) return;
  try {
    if (!mapController) mapController = await createStationMap({ canvas: document.querySelector('[data-map-canvas]'), fallback, configuration, stations, onSelect: station => selectStation(station, true) });
    else mapController.setStations(stations);
  } catch (error) {
    updateMapFallback(fallback, stations[0], error.message);
  }
}

async function load() {
  results.ariaBusy = 'true';
  results.innerHTML = '<div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div>';
  const params = new URLSearchParams();
  new FormData(form).forEach((value, key) => { if (value) params.set(key, value); });
  params.set('page', page); params.set('size', 12);
  if (coordinates) { params.set('latitude', coordinates.latitude); params.set('longitude', coordinates.longitude); }
  try {
    const data = await api(`/api/stations?${params}`);
    stationCache = data.content;
    count.textContent = `${data.totalElements} station${data.totalElements === 1 ? '' : 's'} found`;
    results.innerHTML = data.content.length ? data.content.map(item => stationCard(item)).join('') : emptyState('No stations match', 'Clear a filter or search another place.');
    pager.innerHTML = data.totalPages > 1 ? `<button class="button quiet" type="button" data-page="${page - 1}" ${data.first ? 'disabled' : ''}>Previous</button><span>Page ${page + 1} of ${data.totalPages}</span><button class="button quiet" type="button" data-page="${page + 1}" ${data.last ? 'disabled' : ''}>Next</button>` : '';
    revealNew(results);
    syncMap(data.content);
  } catch (error) {
    count.textContent = 'Could not load stations';
    results.innerHTML = emptyState('Network unavailable', error.message, '<button class="button" type="button" data-retry>Try again</button>');
  } finally { results.ariaBusy = 'false'; }
}

form?.addEventListener('submit', event => { event.preventDefault(); page = 0; load(); });
document.querySelector('[data-clear-filters]')?.addEventListener('click', () => { form.reset(); page = 0; load(); });
pager?.addEventListener('click', event => { const target = event.target.closest('[data-page]'); if (target) { page = Number(target.dataset.page); load(); results.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' }); } });
results?.addEventListener('click', event => {
  if (event.target.closest('[data-retry]')) load();
  const card = event.target.closest('[data-station-card]');
  if (card && !event.target.closest('a,button')) selectStation({ id: card.dataset.stationCard });
});
document.querySelectorAll('.view-switcher [data-view]').forEach(button => button.addEventListener('click', () => {
  workspace.dataset.view = button.dataset.view;
  document.querySelectorAll('.view-switcher [data-view]').forEach(item => item.setAttribute('aria-pressed', String(item === button)));
  if (button.dataset.view === 'map') requestAnimationFrame(() => mapController?.map && google.maps.event.trigger(mapController.map, 'resize'));
}));
document.querySelector('[data-location]')?.addEventListener('click', () => {
  if (!navigator.geolocation) { toast('Location is not available in this browser.', 'error'); return; }
  navigator.geolocation.getCurrentPosition(position => {
    coordinates = position.coords;
    document.querySelector('[data-distance-sort]').disabled = false;
    toast('Location added. Distance sorting is now available.', 'success');
    load();
  }, () => toast('Location was not shared. All other filters still work.', 'error'), { timeout: 8000 });
});

load();
