import { api, emptyState, escapeHtml, formatDate, setBusy, toast } from '../api.js';
import { revealNew } from '../motion.js';
import { createStationMap, decodePolyline, mapsConfiguration, updateMapFallback } from '../maps.js';

const main = document.querySelector('[data-station-id]');
const stationId = main?.dataset.stationId;
const favourite = document.querySelector('[data-favourite]');
const reviewsRoot = document.querySelector('[data-reviews]');
const reportDialog = document.querySelector('[data-report-panel]');
let saved = false;
let currentUserId = null;
let reviewCache = [];
let mapController = null;
let routeLine = null;

api('/api/auth/me').then(user => { currentUserId = user.id; loadReviews(); });

async function syncFavourite() {
  try {
    const items = await api('/api/favourites');
    saved = items.some(item => String(item.stationId) === stationId);
    favourite.setAttribute('aria-pressed', String(saved));
    favourite.querySelector('span').textContent = saved ? 'Saved' : 'Save station';
  } catch { favourite.hidden = true; }
}

async function loadReviews() {
  try {
    const reviews = await api(`/api/stations/${stationId}/reviews`);
    reviewCache = reviews;
    reviewsRoot.innerHTML = reviews.length ? reviews.map(review => `<article class="review" data-reveal><header><strong>${escapeHtml(review.userName)}</strong><span class="rating" aria-label="${review.rating} out of 5 stars">${'★'.repeat(review.rating)}${'☆'.repeat(5 - review.rating)}</span></header><p>${escapeHtml(review.comment)}</p><div class="station-card__footer"><small class="muted">${formatDate(review.updatedAt)}</small>${Number(review.userId) === Number(currentUserId) ? `<span class="action-row"><button class="button quiet" type="button" data-edit-review="${review.id}">Edit</button><button class="button danger" type="button" data-delete-review="${review.id}">Delete</button></span>` : ''}</div></article>`).join('') : emptyState('No reviews yet', 'Be the first driver to share a useful note.');
    revealNew(reviewsRoot);
  } catch (error) { reviewsRoot.innerHTML = emptyState('Could not load reviews', error.message); }
}

favourite?.addEventListener('click', async () => {
  favourite.disabled = true; favourite.setAttribute('aria-busy', 'true');
  try {
    if (saved) await api(`/api/favourites/${stationId}`, { method: 'DELETE' }); else await api(`/api/favourites/${stationId}`, { method: 'POST' });
    saved = !saved; favourite.setAttribute('aria-pressed', String(saved)); favourite.querySelector('span').textContent = saved ? 'Saved' : 'Save station';
    favourite.animate([{ transform: 'scale(.94)' }, { transform: 'scale(1)' }], { duration: 180, easing: 'ease-out' });
    toast(saved ? 'Station saved.' : 'Station removed.', 'success');
  } catch (error) { toast(error.message, 'error'); }
  finally { favourite.disabled = false; favourite.removeAttribute('aria-busy'); }
});

document.querySelector('[data-review-form]')?.addEventListener('submit', async event => {
  event.preventDefault(); const form = event.currentTarget; const message = form.querySelector('[data-review-message]'); const button = form.querySelector('button');
  if (!form.reportValidity()) return;
  const reviewId = form.dataset.reviewId; setBusy(button, true, reviewId ? 'Updating…' : 'Publishing…');
  try {
    const values = Object.fromEntries(new FormData(form)); values.rating = Number(values.rating);
    await api(reviewId ? `/api/reviews/${reviewId}` : `/api/stations/${stationId}/reviews`, { method: reviewId ? 'PUT' : 'POST', body: JSON.stringify(values) });
    form.reset(); delete form.dataset.reviewId; button.dataset.label = 'Publish review'; message.dataset.kind = 'success'; message.textContent = reviewId ? 'Review updated.' : 'Review published.'; loadReviews();
  } catch (error) { message.dataset.kind = 'error'; message.textContent = error.status === 409 ? 'You already reviewed this station.' : error.message; }
  finally { setBusy(button, false); }
});

reviewsRoot?.addEventListener('click', async event => {
  const edit = event.target.closest('[data-edit-review]'); const remove = event.target.closest('[data-delete-review]'); const form = document.querySelector('[data-review-form]');
  if (edit) { const review = reviewCache.find(item => String(item.id) === edit.dataset.editReview); form.elements.rating.value = String(review.rating); form.elements.comment.value = review.comment; form.dataset.reviewId = review.id; form.querySelector('button[type="submit"]').textContent = 'Update review'; form.scrollIntoView({ behavior: 'smooth' }); form.elements.comment.focus(); }
  if (remove && confirm('Delete your review? This cannot be undone.')) {
    try { await api(`/api/reviews/${remove.dataset.deleteReview}`, { method: 'DELETE' }); toast('Review deleted.', 'success'); form.reset(); delete form.dataset.reviewId; form.querySelector('button[type="submit"]').textContent = 'Publish review'; loadReviews(); }
    catch (error) { toast(error.message, 'error'); }
  }
});

document.querySelector('[data-open-report]')?.addEventListener('click', () => reportDialog.showModal());
document.querySelector('[data-close-report]')?.addEventListener('click', () => reportDialog.close());
reportDialog?.addEventListener('click', event => { if (event.target === reportDialog) reportDialog.close(); });
document.querySelector('[data-report-form]')?.addEventListener('submit', async event => {
  event.preventDefault(); const form = event.currentTarget; const button = form.querySelector('button'); const message = form.querySelector('[data-report-message]');
  if (!form.reportValidity()) return; setBusy(button, true, 'Sending…');
  try { await api(`/api/stations/${stationId}/reports`, { method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(form))) }); form.reset(); message.dataset.kind = 'success'; message.textContent = 'Report submitted for administrator review.'; setTimeout(() => reportDialog.close(), 700); }
  catch (error) { message.dataset.kind = 'error'; message.textContent = error.message; }
  finally { setBusy(button, false); }
});

function updateExternalRoute(origin) {
  const destination = `${main.dataset.stationLatitude},${main.dataset.stationLongitude}`;
  const originValue = origin ? `&origin=${origin.latitude},${origin.longitude}` : '';
  document.querySelectorAll('[data-open-google]').forEach(link => { link.href = `https://www.google.com/maps/dir/?api=1${originValue}&destination=${destination}&travelmode=driving`; });
}

async function buildRoute(button) {
  if (!navigator.geolocation) { toast('Location is not available in this browser.', 'error'); return; }
  setBusy(button, true, 'Locating…');
  navigator.geolocation.getCurrentPosition(async position => {
    const origin = { latitude: position.coords.latitude, longitude: position.coords.longitude };
    updateExternalRoute(origin); setBusy(button, true, 'Building route…');
    try {
      const route = await api('/api/routes', { method: 'POST', body: JSON.stringify({ stationId: Number(stationId), originLatitude: origin.latitude, originLongitude: origin.longitude }) });
      document.querySelector('[data-route-distance]').textContent = route.distanceMeters >= 1000 ? `${(route.distanceMeters / 1000).toFixed(1)} km` : `${route.distanceMeters} m`;
      document.querySelector('[data-route-duration]').textContent = `${Math.max(1, Math.round(route.durationSeconds / 60))} min`;
      document.querySelector('[data-route-summary]').hidden = false;
      if (mapController) {
        routeLine?.setMap(null);
        const path = decodePolyline(route.encodedPolyline);
        routeLine = new google.maps.Polyline({ path, map: mapController.map, strokeColor: '#FF4D1F', strokeOpacity: .95, strokeWeight: 5 });
        const bounds = new google.maps.LatLngBounds(); path.forEach(point => bounds.extend(point)); mapController.map.fitBounds(bounds, 50);
      }
    } catch (error) { toast(error.message, 'error'); }
    finally { setBusy(button, false); }
  }, () => { toast('Location was not shared. Open the destination directly in Google Maps instead.', 'error'); setBusy(button, false); }, { timeout: 8000, enableHighAccuracy: true });
}

document.addEventListener('click', event => { const button = event.target.closest('[data-build-route]'); if (button) buildRoute(button); });
document.querySelector('[data-refresh-status]')?.addEventListener('click', async event => { setBusy(event.currentTarget, true, 'Refreshing…'); try { await api(`/api/stations/${stationId}`); location.reload(); } catch (error) { toast(error.message, 'error'); setBusy(event.currentTarget, false); } });

async function initMap() {
  updateExternalRoute();
  const fallback = document.querySelector('[data-map-fallback]');
  const station = { id: Number(stationId), name: main.dataset.stationName, latitude: main.dataset.stationLatitude, longitude: main.dataset.stationLongitude, availablePorts: Number(document.querySelector('.availability-big')?.textContent || 0), status: 'AVAILABLE' };
  const configuration = mapsConfiguration(main);
  updateMapFallback(fallback, station, configuration.enabled
    ? 'The interactive route map could not be loaded. The destination link remains available.'
    : 'Interactive route drawing needs restricted Google Maps credentials. Open this station in Google Maps instead.');
  if (!main.dataset.stationLatitude || !configuration.enabled) return;
  try {
    mapController = await createStationMap({ canvas: document.querySelector('[data-map-canvas]'), fallback, configuration, stations: [station] });
    mapController.select(stationId);
    document.querySelector('[data-route-summary]').hidden = false;
  } catch (error) { updateMapFallback(fallback, station, error.message); }
}

syncFavourite();
initMap();
