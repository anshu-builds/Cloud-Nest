/**
 * CloudNest Enterprise — GSAP Animation Engine
 * =============================================
 * Phase 4: Advanced animation orchestration using GSAP 3 + ScrollTrigger.
 *
 * Modules:
 *  1. CloudNestGSAP.init()          — Master boot sequence
 *  2. CloudNestGSAP.pageEntrance()  — Per-page entrance animation
 *  3. CloudNestGSAP.scrollReveal()  — ScrollTrigger stagger reveals
 *  4. CloudNestGSAP.counters()      — Animated number tweens
 *  5. CloudNestGSAP.sidebarAnim()   — Sidebar nav-item stagger on load
 *  6. CloudNestGSAP.topbarAnim()    — Topbar fade-in + pulse indicators
 *  7. CloudNestGSAP.metricCards()   — Metric card pop-in cascade
 *  8. CloudNestGSAP.chartReveal()   — Chart card wipe-in
 *  9. CloudNestGSAP.nodeCards()     — Node card cascade + wave bar pulse
 * 10. CloudNestGSAP.magneticBtns()  — Magnetic button mouse tracking
 * 11. CloudNestGSAP.progressBars()  — Animated progress bar fills
 * 12. CloudNestGSAP.quotaBar()      — Sidebar quota bar fill on load
 * 13. CloudNestGSAP.pageTransition()— SPA-style route link intercept
 */

const CloudNestGSAP = (() => {
    'use strict';

    // ── Guards ─────────────────────────────────────────────────────────────
    const HAS_GSAP = typeof gsap !== 'undefined';
    const HAS_ST   = HAS_GSAP && typeof ScrollTrigger !== 'undefined';

    if (!HAS_GSAP) {
        console.warn('[CloudNestGSAP] GSAP not loaded — animations disabled.');
        return { init: () => {} };
    }

    if (HAS_ST) {
        gsap.registerPlugin(ScrollTrigger);
    }

    // ── Shared Eases ───────────────────────────────────────────────────────
    const EASE_OUT     = 'power3.out';
    const EASE_ELASTIC = 'elastic.out(1, 0.6)';
    const EASE_BACK    = 'back.out(1.4)';

    // ── Utility ───────────────────────────────────────────────────────────
    function qs(sel, ctx = document)  { return ctx.querySelector(sel); }
    function qsa(sel, ctx = document) { return [...ctx.querySelectorAll(sel)]; }

    // ─────────────────────────────────────────────────────────────────────
    // 1. PAGE ENTRANCE
    //    Runs once per page load. Animates the workspace in with a stagger
    //    of the header + content sections.
    // ─────────────────────────────────────────────────────────────────────
    function pageEntrance() {
        const tl = gsap.timeline({ defaults: { ease: EASE_OUT } });

        // Main content area slides in from the right with a subtle clip-path
        const workspace = qs('.cn-workspace');
        if (workspace) {
            gsap.set(workspace, { opacity: 0, x: 18 });
            tl.to(workspace, { opacity: 1, x: 0, duration: 0.5, delay: 0.05 });
        }

        // Page h1 headline — slide up and fade
        const h1 = qs('.cn-workspace h1');
        if (h1) {
            gsap.set(h1, { opacity: 0, y: 12 });
            tl.to(h1, { opacity: 1, y: 0, duration: 0.45 }, '<0.1');
        }

        // Page subtitle/description
        const subtitle = qs('.cn-workspace p');
        if (subtitle) {
            gsap.set(subtitle, { opacity: 0, y: 8 });
            tl.to(subtitle, { opacity: 1, y: 0, duration: 0.4 }, '<0.08');
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. SIDEBAR ANIMATION
    //    Nav items cascade in from the left on load.
    // ─────────────────────────────────────────────────────────────────────
    function sidebarAnim() {
        const navItems = qsa('.cn-nav-item');
        if (!navItems.length) return;

        gsap.set(navItems, { opacity: 0, x: -14 });
        gsap.to(navItems, {
            opacity: 1,
            x: 0,
            duration: 0.4,
            stagger: 0.055,
            ease: EASE_OUT,
            delay: 0.1,
        });

        // Section labels fade up
        const labels = qsa('.cn-nav-section-label');
        gsap.set(labels, { opacity: 0, y: 6 });
        gsap.to(labels, {
            opacity: 1,
            y: 0,
            duration: 0.35,
            stagger: 0.08,
            ease: EASE_OUT,
            delay: 0.05,
        });

        // Logo mark — subtle scale bounce
        const logo = qs('.cn-logo-mark');
        if (logo) {
            gsap.set(logo, { scale: 0.85, opacity: 0 });
            gsap.to(logo, { scale: 1, opacity: 1, duration: 0.6, ease: EASE_ELASTIC, delay: 0.0 });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. TOPBAR ANIMATION
    // ─────────────────────────────────────────────────────────────────────
    function topbarAnim() {
        const topbar = qs('.cn-topbar');
        if (!topbar) return;

        gsap.set(topbar, { opacity: 0, y: -10 });
        gsap.to(topbar, { opacity: 1, y: 0, duration: 0.4, ease: EASE_OUT, delay: 0.15 });

        // Status pill — pulse in
        const pill = qs('.cn-status-pill');
        if (pill) {
            gsap.set(pill, { opacity: 0, scale: 0.9 });
            gsap.to(pill, { opacity: 1, scale: 1, duration: 0.5, ease: EASE_BACK, delay: 0.5 });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4. METRIC CARDS — pop-in cascade with 3D tilt feel
    // ─────────────────────────────────────────────────────────────────────
    function metricCards() {
        const cards = qsa('.cn-metric-card');
        if (!cards.length) return;

        gsap.set(cards, { opacity: 0, y: 24, scale: 0.96 });
        gsap.to(cards, {
            opacity: 1,
            y: 0,
            scale: 1,
            duration: 0.55,
            stagger: 0.07,
            ease: EASE_BACK,
            delay: 0.25,
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 5. CHART CARDS — wipe in from bottom with ScrollTrigger
    // ─────────────────────────────────────────────────────────────────────
    function chartReveal() {
        if (!HAS_ST) return;

        const chartCards = qsa('.cn-card, .cn-chart-card, .cn-analytics-chart-card, .cn-topology-canvas-wrap, .cn-node-detail, .cn-replication-svg-wrap');
        chartCards.forEach((card, i) => {
            gsap.set(card, { opacity: 0, y: 20 });
            ScrollTrigger.create({
                trigger: card,
                start: 'top 92%',
                once: true,
                onEnter: () => {
                    gsap.to(card, {
                        opacity: 1,
                        y: 0,
                        duration: 0.55,
                        ease: EASE_OUT,
                        delay: (i % 4) * 0.06,
                    });
                },
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 6. ANIMATED NUMBER COUNTERS
    //    Finds elements with [data-counter] and tweens their textContent.
    // ─────────────────────────────────────────────────────────────────────
    function counters() {
        const counterEls = qsa('[data-counter]');
        if (!counterEls.length) return;

        counterEls.forEach(el => {
            const target  = parseFloat(el.getAttribute('data-counter') || el.textContent) || 0;
            const suffix  = el.getAttribute('data-counter-suffix') || '';
            const decimals = (String(target).split('.')[1] || '').length;
            const obj     = { value: 0 };

            const doTween = () => {
                gsap.to(obj, {
                    value: target,
                    duration: 1.6,
                    ease: 'power2.out',
                    onUpdate() {
                        el.textContent = decimals > 0
                            ? obj.value.toFixed(decimals) + suffix
                            : Math.round(obj.value) + suffix;
                    },
                });
            };

            if (HAS_ST) {
                ScrollTrigger.create({
                    trigger: el,
                    start: 'top 90%',
                    once: true,
                    onEnter: doTween,
                });
            } else {
                // No ScrollTrigger — run immediately after a short delay
                setTimeout(doTween, 400);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 7. PROGRESS BAR FILL ANIMATION
    //    Reads the inline 'width' style and animates from 0 → target.
    // ─────────────────────────────────────────────────────────────────────
    function progressBars() {
        const bars = qsa('.cn-progress-bar, .cn-quota-fill');
        bars.forEach(bar => {
            const targetWidth = bar.style.width || '0%';
            gsap.set(bar, { width: '0%' });

            const doFill = () => {
                gsap.to(bar, {
                    width: targetWidth,
                    duration: 1.2,
                    ease: 'power2.out',
                    delay: 0.2,
                });
            };

            if (HAS_ST) {
                ScrollTrigger.create({
                    trigger: bar,
                    start: 'top 95%',
                    once: true,
                    onEnter: doFill,
                });
            } else {
                setTimeout(doFill, 500);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 8. SIDEBAR QUOTA BAR
    //    Runs immediately on page load so the sidebar feels live.
    // ─────────────────────────────────────────────────────────────────────
    function quotaBar() {
        const fill = qs('.cn-quota-fill');
        if (!fill) return;
        const target = fill.style.width || '0%';
        gsap.set(fill, { width: '0%' });
        gsap.to(fill, {
            width: target,
            duration: 1.4,
            ease: 'power3.out',
            delay: 0.6,
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 9. NODE CARDS
    //    The cn-node-card grid pops in with a stagger.
    // ─────────────────────────────────────────────────────────────────────
    function nodeCards() {
        const cards = qsa('.cn-node-card');
        if (!cards.length) return;

        gsap.set(cards, { opacity: 0, y: 20, scale: 0.97 });
        const doAnim = () => {
            gsap.to(cards, {
                opacity: 1,
                y: 0,
                scale: 1,
                duration: 0.5,
                stagger: 0.1,
                ease: EASE_BACK,
            });
        };

        if (HAS_ST) {
            ScrollTrigger.create({
                trigger: cards[0],
                start: 'top 90%',
                once: true,
                onEnter: doAnim,
            });
        } else {
            setTimeout(doAnim, 500);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 10. SCROLL REVEAL — generic .cn-reveal elements
    //     If app.js has already handled these via IntersectionObserver,
    //     GSAP provides a smoother version that overrides only if GSAP
    //     loads after app.js and the elements still have opacity: 0.
    // ─────────────────────────────────────────────────────────────────────
    function scrollReveal() {
        if (!HAS_ST) return;

        // Only grab elements that are still invisible (app.js may have
        // already revealed them via IntersectionObserver).
        const revealEls = qsa('.cn-reveal').filter(el => {
            const computed = window.getComputedStyle(el);
            return parseFloat(computed.opacity) < 0.1;
        });

        if (!revealEls.length) return;

        gsap.set(revealEls, { opacity: 0, y: 18 });
        revealEls.forEach((el, i) => {
            ScrollTrigger.create({
                trigger: el,
                start: 'top 92%',
                once: true,
                onEnter: () => {
                    gsap.to(el, {
                        opacity: 1,
                        y: 0,
                        duration: 0.5,
                        ease: EASE_OUT,
                        delay: (i % 5) * 0.05,
                    });
                },
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 11. MAGNETIC BUTTONS — GSAP-powered mouse tracking
    // ─────────────────────────────────────────────────────────────────────
    function magneticBtns() {
        const magnets = qsa('.cn-magnetic');
        magnets.forEach(btn => {
            btn.addEventListener('mousemove', e => {
                const rect = btn.getBoundingClientRect();
                const dx   = e.clientX - (rect.left + rect.width  / 2);
                const dy   = e.clientY - (rect.top  + rect.height / 2);
                gsap.to(btn, { x: dx * 0.22, y: dy * 0.22, duration: 0.3, ease: 'power2.out' });
            });
            btn.addEventListener('mouseleave', () => {
                gsap.to(btn, { x: 0, y: 0, duration: 0.5, ease: EASE_ELASTIC });
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 12. PAGE TRANSITION — SPA-feel link intercepts
    //     When a sidebar nav link is clicked, fade out the workspace
    //     before navigating so it feels like a real SPA transition.
    // ─────────────────────────────────────────────────────────────────────
    function pageTransition() {
        // Only intercept same-origin links
        const links = qsa('.cn-nav-item[href], .cn-command-item[href]');
        links.forEach(link => {
            link.addEventListener('click', e => {
                const href = link.getAttribute('href');
                if (!href || href.startsWith('#') || href.startsWith('javascript')) return;
                if (link.hostname && link.hostname !== window.location.hostname) return;

                e.preventDefault();
                const workspace = qs('.cn-workspace') || qs('.cn-main-content');
                if (!workspace) { window.location.href = href; return; }

                gsap.to(workspace, {
                    opacity: 0,
                    y: -10,
                    duration: 0.22,
                    ease: 'power2.in',
                    onComplete: () => { window.location.href = href; },
                });
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 13. ACTIVE NAV HIGHLIGHT — animate the active sidebar pill
    // ─────────────────────────────────────────────────────────────────────
    function activeNavPill() {
        const activeItem = qs('.cn-nav-item.active');
        if (!activeItem) return;

        // Scale pop on the active item's icon
        const icon = qs('.cn-nav-icon', activeItem);
        if (icon) {
            gsap.set(icon, { scale: 0.8 });
            gsap.to(icon, { scale: 1, duration: 0.6, ease: EASE_ELASTIC, delay: 0.3 });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 14. TOPBAR SEARCH — subtle pop on focus
    // ─────────────────────────────────────────────────────────────────────
    function topbarSearchAnim() {
        const input = qs('.cn-topbar-search input');
        if (!input) return;

        input.addEventListener('focus', () => {
            gsap.to(input.parentElement, { scaleX: 1.02, duration: 0.25, ease: EASE_OUT, transformOrigin: 'left center' });
        });
        input.addEventListener('blur', () => {
            gsap.to(input.parentElement, { scaleX: 1, duration: 0.3, ease: EASE_OUT });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 15. STATUS DOT PULSE — extra GSAP pulse on status indicators
    // ─────────────────────────────────────────────────────────────────────
    function statusDotPulse() {
        const dots = qsa('.cn-status-dot');
        if (!dots.length) return;

        dots.forEach((dot, i) => {
            gsap.to(dot, {
                scale: 1.5,
                opacity: 0.3,
                duration: 1.0,
                repeat: -1,
                yoyo: true,
                ease: 'power1.inOut',
                delay: i * 0.3,
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // 16. COMMAND PALETTE — smooth open/close
    // ─────────────────────────────────────────────────────────────────────
    function commandPaletteAnim() {
        const palette = qs('#cnCommandPalette');
        const box     = qs('.cn-command-box');
        if (!palette || !box) return;

        // Override/enhance existing show/hide with GSAP
        const origShow = window._cnPaletteShow;
        const origHide = window._cnPaletteHide;

        window._cnGSAPShowPalette = () => {
            gsap.set(palette, { display: 'flex', opacity: 0 });
            gsap.set(box, { opacity: 0, y: -18, scale: 0.96 });
            gsap.to(palette, { opacity: 1, duration: 0.2, ease: 'power2.out' });
            gsap.to(box, { opacity: 1, y: 0, scale: 1, duration: 0.3, ease: EASE_BACK });
        };
        window._cnGSAPHidePalette = () => {
            gsap.to(box, {
                opacity: 0, y: -12, scale: 0.96, duration: 0.2, ease: 'power2.in',
                onComplete: () => gsap.set(palette, { display: 'none' }),
            });
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // MASTER INIT
    // ─────────────────────────────────────────────────────────────────────
    function init() {
        // Run in sequence — some need the DOM fully painted
        pageEntrance();
        sidebarAnim();
        topbarAnim();
        metricCards();
        nodeCards();
        quotaBar();
        progressBars();
        magneticBtns();
        topbarSearchAnim();
        statusDotPulse();
        activeNavPill();
        commandPaletteAnim();
        pageTransition();

        // ScrollTrigger-dependent — run last
        if (HAS_ST) {
            chartReveal();
            scrollReveal();
            counters();
            // Refresh ScrollTrigger after layout settles
            setTimeout(() => ScrollTrigger.refresh(), 300);
        } else {
            counters(); // Fallback without ST
        }

        console.log('[CloudNestGSAP] Animation engine initialized ✓');
    }

    // Public API
    return {
        init,
        pageEntrance,
        sidebarAnim,
        topbarAnim,
        metricCards,
        counters,
        progressBars,
        magneticBtns,
        nodeCards,
        chartReveal,
        scrollReveal,
        pageTransition,
        quotaBar,
    };

})();

// ── Auto-boot ───────────────────────────────────────────────────────────────
// Fire on DOMContentLoaded so all elements exist before we query them.
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => CloudNestGSAP.init());
} else {
    // DOM already ready (script loaded late)
    CloudNestGSAP.init();
}
