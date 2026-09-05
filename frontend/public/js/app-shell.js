const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');

function setupNavigation() {
  const toggle = document.querySelector('[data-nav-toggle]');
  const links = document.querySelector('[data-nav-links]');
  const setOpen = open => {
    if (!toggle || !links) return;
    links.dataset.open = String(open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
  };
  toggle?.addEventListener('click', () => setOpen(links?.dataset.open !== 'true'));
  links?.addEventListener('click', event => { if (event.target.closest('a')) setOpen(false); });
  document.addEventListener('keydown', event => { if (event.key === 'Escape') setOpen(false); });
  document.querySelectorAll('.nav-links a').forEach(link => {
    if (link.pathname === location.pathname) link.setAttribute('aria-current', 'page');
  });
}

function setupPageTransitions() {
  if (reducedMotion.matches) return;
  const veil = document.createElement('div');
  veil.className = 'route-veil';
  veil.dataset.routeVeil = '';
  veil.setAttribute('aria-hidden', 'true');
  document.body.append(veil);

  if (sessionStorage.getItem('smartEvTransitionPending') === 'true') {
    sessionStorage.removeItem('smartEvTransitionPending');
    veil.animate(
      [{ transform: 'translateY(0)' }, { transform: 'translateY(-100%)' }],
      { duration: 220, easing: 'cubic-bezier(.23, 1, .32, 1)', fill: 'forwards' }
    );
  }

  document.addEventListener('click', event => {
    const link = event.target.closest('a[href]');
    if (!link || event.defaultPrevented || event.button !== 0 || event.detail === 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    const url = new URL(link.href, location.href);
    if (url.origin !== location.origin || url.pathname === location.pathname && url.search === location.search || link.target || link.hasAttribute('download') || url.hash) return;
    event.preventDefault();
    veil.animate(
      [{ transform: 'translateY(100%)' }, { transform: 'translateY(0)' }],
      { duration: 180, easing: 'cubic-bezier(.77, 0, .175, 1)', fill: 'forwards' }
    ).finished.then(() => {
      sessionStorage.setItem('smartEvTransitionPending', 'true');
      location.assign(url.href);
    });
  });
}

setupNavigation();
setupPageTransitions();
