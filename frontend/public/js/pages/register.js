import { api, setBusy } from '../api.js';
const form = document.querySelector('#registration-form'); const message = document.querySelector('#registration-message');
form?.addEventListener('submit', async event => {
  event.preventDefault(); message.textContent = ''; message.dataset.kind = '';
  if (!form.reportValidity()) return;
  const button = form.querySelector('button[type="submit"]'); setBusy(button, true, 'Creating account…');
  try { await api('/api/auth/register', { method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(form))) }); message.dataset.kind = 'success'; message.textContent = 'Account created. Taking you to sign in…'; setTimeout(() => window.location.assign('/login?registered'), 650); }
  catch (error) { message.dataset.kind = 'error'; message.textContent = Object.values(error.fieldErrors || {})[0] || error.message; message.focus(); const first = Object.keys(error.fieldErrors || {})[0]; if (first) form.elements[first]?.focus(); setBusy(button, false); }
});
