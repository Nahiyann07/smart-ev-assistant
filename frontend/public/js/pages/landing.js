const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
const finePointer = matchMedia('(hover: hover) and (pointer: fine)').matches;
const loader = document.querySelector('[data-intro-loader]');
const video = document.querySelector('[data-hero-video]');

function dismissLoader() {
  if (!loader || loader.dataset.done === 'true') return;
  loader.dataset.done = 'true';
  loader.dataset.ready = 'true';
  loader.setAttribute('aria-hidden', 'true');
  sessionStorage.setItem('smartEvIntroSeen', 'true');
  setTimeout(() => loader.remove(), 360);
}

if (loader) {
  if (sessionStorage.getItem('smartEvIntroSeen') === 'true') loader.remove();
  else {
    const deadline = setTimeout(dismissLoader, 1200);
    const ready = () => { clearTimeout(deadline); dismissLoader(); };
    if (!video || video.readyState >= 2) ready();
    else {
      video.addEventListener('loadeddata', ready, { once: true });
      video.addEventListener('error', ready, { once: true });
    }
  }
}

if (video) {
  if (reduced) {
    video.pause();
    video.removeAttribute('autoplay');
  } else {
    video.play().catch(() => document.body.dataset.videoUnavailable = 'true');
  }
}

if (!reduced && window.gsap) {
  const timeline = window.gsap.timeline({ defaults: { ease: 'power3.out' }, delay: .08 });
  timeline.fromTo('[data-hero-kicker]', { y: 18, opacity: 0 }, { y: 0, opacity: 1, duration: .5 })
    .fromTo('[data-hero-line]', { yPercent: 110 }, { yPercent: 0, duration: .75, stagger: .09 }, '-=.28')
    .fromTo('[data-hero-copy]', { y: 24, opacity: 0 }, { y: 0, opacity: 1, duration: .55 }, '-=.35')
    .fromTo('[data-telemetry]', { y: 18, opacity: 0 }, { y: 0, opacity: 1, duration: .5, stagger: .06 }, '-=.3');
}

if (!reduced && finePointer) {
  document.querySelectorAll('[data-magnetic]').forEach(button => {
    button.addEventListener('pointermove', event => {
      const rect = button.getBoundingClientRect();
      const x = (event.clientX - rect.left - rect.width / 2) * .12;
      const y = (event.clientY - rect.top - rect.height / 2) * .12;
      button.animate({ transform: `translate(${x}px, ${y}px)` }, { duration: 160, fill: 'forwards', easing: 'ease-out' });
    });
    button.addEventListener('pointerleave', () => button.animate({ transform: 'translate(0, 0)' }, { duration: 180, fill: 'forwards', easing: 'ease-out' }));
  });
}
