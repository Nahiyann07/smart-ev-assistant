const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
const gsap = window.gsap;
const ScrollTrigger = window.ScrollTrigger;

function makeVisible(target) { target.dataset.visible = 'true'; }

export function revealNew(root = document) {
  const targets = [...root.querySelectorAll('[data-reveal]:not([data-visible])')];
  if (reduced || !('IntersectionObserver' in window)) {
    targets.forEach(makeVisible);
    return;
  }
  const observer = new IntersectionObserver(entries => entries.forEach(entry => {
    if (!entry.isIntersecting) return;
    makeVisible(entry.target);
    observer.unobserve(entry.target);
  }), { threshold: .12, rootMargin: '0px 0px -5% 0px' });
  targets.forEach(target => observer.observe(target));
}

revealNew();

if (!reduced && gsap && ScrollTrigger) {
  gsap.registerPlugin(ScrollTrigger);
  document.querySelectorAll('[data-parallax]').forEach(element => {
    gsap.fromTo(element, { yPercent: -3 }, { yPercent: 3, ease: 'none', scrollTrigger: { trigger: element, scrub: .8, start: 'top bottom', end: 'bottom top' } });
  });
  document.querySelectorAll('[data-counter]').forEach(element => {
    const finalValue = Number(element.dataset.counter || element.textContent || 0);
    const state = { value: 0 };
    gsap.to(state, { value: finalValue, duration: .8, ease: 'power2.out', scrollTrigger: { trigger: element, start: 'top 90%', once: true }, onUpdate: () => { element.textContent = Math.round(state.value); } });
  });
}
