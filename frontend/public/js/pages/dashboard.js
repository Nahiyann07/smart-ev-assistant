import { api, emptyState, escapeHtml, formatDate, humanize, stationCard } from '../api.js';
import '../motion.js';

const greeting = document.querySelector('[data-greeting]');
if (greeting) greeting.textContent = new Date().getHours() < 12 ? 'Good morning' : new Date().getHours() < 18 ? 'Good afternoon' : 'Good evening';
const recommendation = document.querySelector('[data-dashboard-recommendation]');
const favourites = document.querySelector('[data-dashboard-favourites]');
const stats = document.querySelector('[data-dashboard-stats]');
const activity = document.querySelector('[data-dashboard-activity]');

Promise.allSettled([api('/api/recommendations'), api('/api/favourites'), api('/api/stations?size=50'), api('/api/users/me/reports')]).then(([recResult, favResult, stationResult, reportResult]) => {
  if (recResult.status === 'fulfilled') {
    const rec = recResult.value[0];
    recommendation.innerHTML = rec ? `<p class="eyebrow">Strongest match</p><span class="recommendation-score">${Math.round(rec.score)}</span><span class="muted"> / 100</span><h2>${escapeHtml(rec.stationName)}</h2><p>${rec.availablePorts} ports available · ${rec.chargingSpeedKw} kW</p><p class="muted">${rec.reasons.map(escapeHtml).join(' · ')}</p><a href="/stations/${rec.stationId}">Inspect station →</a>` : emptyState('No match yet', 'Active stations will appear here.');
  } else recommendation.innerHTML = emptyState('Recommendation unavailable', recResult.reason.message);

  if (favResult.status === 'fulfilled') favourites.innerHTML = favResult.value.length ? favResult.value.slice(0, 3).map(item => stationCard(item)).join('') : emptyState('No favourites yet', 'Save a station to build your quick-access list.', '<a class="button" href="/stations">Explore stations</a>');
  else favourites.innerHTML = emptyState('Favourites unavailable', favResult.reason.message);

  if (stationResult.status === 'fulfilled') {
    const stations = stationResult.value.content;
    const available = stations.filter(item => item.status === 'AVAILABLE').length;
    const ports = stations.reduce((sum, item) => sum + item.availablePorts, 0);
    const fast = stations.filter(item => item.chargerType === 'DC_FAST').length;
    stats.innerHTML = `<div class="stat"><strong class="stat-value">${ports}</strong><span class="stat-label">Ports ready</span></div><div class="stat"><strong class="stat-value">${available}</strong><span class="stat-label">Available stations</span></div><div class="stat"><strong class="stat-value">${fast}</strong><span class="stat-label">DC fast profiles</span></div><div class="stat"><strong class="stat-value">${stationResult.value.totalElements}</strong><span class="stat-label">Managed stations</span></div>`;
  } else stats.innerHTML = emptyState('Network pulse unavailable', stationResult.reason.message);

  if (reportResult.status === 'fulfilled') {
    activity.innerHTML = reportResult.value.length ? reportResult.value.slice(0, 3).map(report => `<article class="process-step"><span class="status status--${report.status.toLowerCase()}">${humanize(report.status)}</span><div><h3>${escapeHtml(report.stationName)}</h3><p class="muted">${humanize(report.issueType)} · ${formatDate(report.createdAt)}</p></div></article>`).join('') : emptyState('No reports yet', 'Your submitted station issues will appear here.');
  } else activity.innerHTML = emptyState('Activity unavailable', reportResult.reason.message);
});
