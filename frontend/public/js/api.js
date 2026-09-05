const csrfToken = document.querySelector('meta[name="csrf-token"],meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="csrf-header"],meta[name="_csrf_header"]')?.content;

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  if (csrfToken && csrfHeader && !['GET', 'HEAD'].includes((options.method || 'GET').toUpperCase())) headers.set(csrfHeader, csrfToken);
  const response = await fetch(path, { ...options, headers });
  if (response.status === 401) {
    window.location.assign(`/login?continue=${encodeURIComponent(location.pathname)}`);
    throw new ApiFailure('Your session has expired.', 401, {});
  }
  const type = response.headers.get('content-type') || '';
  const data = response.status === 204 ? null : type.includes('json') ? await response.json() : await response.text();
  if (!response.ok) throw new ApiFailure(data?.message || (typeof data === 'string' && data) || `Request failed (${response.status}).`, response.status, data?.fieldErrors || {});
  return data;
}

export class ApiFailure extends Error { constructor(message, status, fieldErrors) { super(message); this.status = status; this.fieldErrors = fieldErrors; } }
export function escapeHtml(value = '') { return String(value).replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character]); }
export function statusSlug(value = '') { return String(value).toLowerCase().replaceAll('_', '-'); }
export function humanize(value = '') { return String(value).toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, letter => letter.toUpperCase()); }
export function formatDate(value) { return value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'; }
export function setBusy(button, busy, busyLabel = 'Working…') { if (!button) return; if (busy) { button.dataset.label = button.textContent; button.textContent = busyLabel; button.disabled = true; button.setAttribute('aria-busy', 'true'); } else { button.textContent = button.dataset.label || button.textContent; button.disabled = false; button.removeAttribute('aria-busy'); } }

let toastTimer;
export function toast(message, kind = 'info') { const element = document.querySelector('[data-toast]'); if (!element) return; element.textContent = message; element.dataset.kind = kind; element.dataset.show = 'true'; clearTimeout(toastTimer); toastTimer = setTimeout(() => { element.dataset.show = 'false'; }, 3600); }
export function emptyState(title, copy, action = '') { return `<div class="empty-state"><div><h3>${escapeHtml(title)}</h3><p>${escapeHtml(copy)}</p>${action}</div></div>`; }

export function stationCard(station, { removable = false } = {}) {
  const distance = station.distanceKm == null ? '' : `<span>${Number(station.distanceKm).toFixed(1)} km</span>`;
  const rating = station.averageRating == null ? '' : `<span class="rating">★ ${Number(station.averageRating).toFixed(1)}</span>`;
  const remove = removable ? `<button class="icon-button" type="button" data-remove-favourite="${station.stationId || station.id}" aria-label="Remove ${escapeHtml(station.stationName || station.name)} from favourites">×</button>` : '';
  const id = station.stationId || station.id;
  const total = Math.max(Number(station.totalPorts || 0), 1);
  const available = Number(station.availablePorts || 0);
  const progress = Math.max(0, Math.min(100, (available / total) * 100));
  const coordinates = station.latitude == null || station.longitude == null ? '' : ` data-latitude="${station.latitude}" data-longitude="${station.longitude}"`;
  return `<article class="station-card" style="--status-color:var(--${statusSlug(station.status || 'available')})" data-station-card="${id}"${coordinates} data-reveal><div class="station-card__top"><span class="status status--${statusSlug(station.status)}">${humanize(station.status)}</span>${remove}</div><h3>${escapeHtml(station.stationName || station.name)}</h3><p class="muted">${escapeHtml(station.address || station.city || '')}${station.address && station.city ? ` · ${escapeHtml(station.city)}` : ''}</p><div class="port-track" aria-label="${available} of ${total} ports available"><span style="--progress:${progress}%"></span></div><div class="station-card__data"><div class="station-card__metric"><strong>${available}</strong>available</div><div class="station-card__metric"><strong>${station.chargingSpeedKw} kW</strong>speed</div>${station.totalPorts == null ? '' : `<div class="station-card__metric"><strong>${station.totalPorts}</strong>ports</div>`}</div><div class="station-card__footer">${rating}${distance}<a href="/stations/${id}">View station <span aria-hidden="true">→</span></a></div></article>`;
}
