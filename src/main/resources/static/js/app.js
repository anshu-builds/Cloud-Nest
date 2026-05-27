/**
 * ============================================================================
 * CloudNest Enterprise — Core Application JavaScript
 * Distributed Infrastructure Platform
 * ============================================================================
 * Modules:
 *   1. AppShell       — Sidebar collapse, active nav, mobile drawer
 *   2. CommandPalette — Ctrl+K, fuzzy search, keyboard navigation
 *   3. ToastSystem    — Floating notifications
 *   4. AnimatedCounters — Number tween on viewport entry
 *   5. ScrollReveal   — IntersectionObserver reveal animations
 *   6. UploadZone     — Drag & drop upload with progress
 *   7. MagneticButtons — Mouse-tracking button physics
 *   8. FileExplorer   — Grid/List toggle, context menu
 *   9. AvatarInitials — Auto-set avatar letter from username
 *  10. AlertSystem    — Auto-dismiss alerts
 * ============================================================================
 */

'use strict';

/* ============================================================================
   1. APP SHELL — Sidebar, Mobile Drawer, Active Navigation
   ============================================================================ */
const AppShell = (() => {
  let sidebar, mainContent, toggleBtn, toggleIcon, overlay, mobileMenuBtn;
  let isCollapsed = false;

  function init() {
    sidebar      = document.getElementById('cnSidebar');
    mainContent  = document.getElementById('cnMainContent');
    toggleBtn    = document.getElementById('cnSidebarToggle');
    toggleIcon   = document.getElementById('cnToggleIcon');
    overlay      = document.getElementById('cnSidebarOverlay');
    mobileMenuBtn = document.getElementById('cnMobileMenuBtn');

    if (!sidebar) return;

    // Restore collapsed state from localStorage
    isCollapsed = localStorage.getItem('cn_sidebar_collapsed') === 'true';
    if (isCollapsed) applyCollapsed(false); // no animation on load

    // Sidebar toggle button
    toggleBtn?.addEventListener('click', toggleCollapse);

    // Mobile open button
    mobileMenuBtn?.addEventListener('click', openMobile);

    // Overlay closes mobile sidebar
    overlay?.addEventListener('click', closeMobile);

    // Escape key closes mobile sidebar / command palette
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeMobile();
    });

    // Mark active nav item based on current URL
    markActiveNav();

    // Set page title in topbar
    setTopbarTitle();

    // Set user avatar initials
    setAvatarInitials();

    // Animate sidebar items in on load
    animateSidebarEntry();
  }

  function applyCollapsed(animate = true) {
    if (!sidebar || !mainContent) return;
    if (animate) {
      sidebar.style.transition = 'width 0.3s cubic-bezier(0.165, 0.84, 0.44, 1)';
      mainContent.style.transition = 'margin-left 0.3s cubic-bezier(0.165, 0.84, 0.44, 1)';
    }
    sidebar.classList.toggle('collapsed', isCollapsed);
    mainContent.classList.toggle('sidebar-collapsed', isCollapsed);

    // Rotate toggle icon
    if (toggleIcon) {
      toggleIcon.style.transform = isCollapsed ? 'rotate(180deg)' : 'rotate(0deg)';
    }
  }

  function toggleCollapse() {
    isCollapsed = !isCollapsed;
    localStorage.setItem('cn_sidebar_collapsed', isCollapsed);
    applyCollapsed(true);
  }

  function openMobile() {
    sidebar?.classList.add('mobile-open');
    overlay?.style.setProperty('display', 'block');
    document.body.style.overflow = 'hidden';
    setTimeout(() => overlay?.style.setProperty('opacity', '1'), 10);
  }

  function closeMobile() {
    sidebar?.classList.remove('mobile-open');
    document.body.style.overflow = '';
    if (overlay) {
      overlay.style.opacity = '0';
      setTimeout(() => overlay.style.display = 'none', 300);
    }
  }

  function markActiveNav() {
    const path = window.location.pathname;
    const navMap = {
      '/dashboard':     'nav-dashboard',
      '/files':         'nav-files',
      '/trash':         'nav-trash',
      '/nodes':         'nav-nodes',
      '/replication':   'nav-replication',
      '/deduplication': 'nav-dedup',
      '/network':       'nav-network',
      '/analytics':     'nav-analytics',
      '/monitoring':    'nav-monitoring',
    };

    // Find best match (longest prefix)
    let bestMatch = '';
    let bestId = '';
    for (const [route, id] of Object.entries(navMap)) {
      if (path.startsWith(route) && route.length > bestMatch.length) {
        bestMatch = route;
        bestId = id;
      }
    }

    if (bestId) {
      const el = document.getElementById(bestId);
      if (el) {
        el.classList.add('active');
        // Scroll into view if sidebar is scrollable
        el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      }
    }
  }

  function setTopbarTitle() {
    const titleEl = document.getElementById('cnPageTitle');
    if (!titleEl) return;

    const titleMap = {
      '/dashboard':     'Command Center',
      '/files':         'File Explorer',
      '/trash':         'Recycle Bin',
      '/nodes':         'Storage Nodes',
      '/replication':   'Replication',
      '/deduplication': 'Deduplication Center',
      '/network':       'Network Activity',
      '/analytics':     'Analytics',
      '/monitoring':    'Monitoring',
    };

    const path = window.location.pathname;
    for (const [route, title] of Object.entries(titleMap)) {
      if (path.startsWith(route)) {
        titleEl.textContent = title;
        document.title = `CloudNest — ${title}`;
        return;
      }
    }
  }

  function setAvatarInitials() {
    const avatarEl = document.getElementById('userAvatarInitial');
    if (!avatarEl) return;
    const name = avatarEl.textContent.trim();
    if (name && name !== 'U') {
      avatarEl.textContent = name.charAt(0).toUpperCase();
    }
  }

  function animateSidebarEntry() {
    const navItems = document.querySelectorAll('.cn-nav-item');
    navItems.forEach((item, i) => {
      item.style.opacity = '0';
      item.style.transform = 'translateX(-10px)';
      setTimeout(() => {
        item.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        item.style.opacity = '1';
        item.style.transform = 'translateX(0)';
      }, 80 + i * 40);
    });
  }

  return { init, openMobile, closeMobile };
})();

/* ============================================================================
   2. COMMAND PALETTE — Ctrl+K fuzzy search, keyboard navigation
   ============================================================================ */
const CommandPalette = (() => {
  let palette, input, list, items, overlay, selectedIndex = -1;

  function init() {
    palette  = document.getElementById('cnCommandPalette');
    input    = document.getElementById('cnCommandInput');
    list     = document.getElementById('cnCommandList');
    overlay  = palette;

    if (!palette) return;

    // Open triggers
    document.getElementById('cnCommandTrigger')?.addEventListener('click', open);

    // Keyboard shortcut Ctrl+K or Cmd+K
    document.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        toggle();
      }
      if (palette.classList.contains('active')) {
        handleKeyNav(e);
      }
    });

    // Close on overlay click (outside box)
    palette.addEventListener('click', (e) => {
      if (e.target === palette) close();
    });

    // Filter on input
    input?.addEventListener('input', filterItems);

    // Cache all command items
    refreshItems();
  }

  function refreshItems() {
    items = [...(list?.querySelectorAll('.cn-command-item') || [])];
  }

  function open() {
    palette?.classList.add('active');
    setTimeout(() => input?.focus(), 60);
    selectedIndex = -1;
    filterItems();
  }

  function close() {
    palette?.classList.remove('active');
    if (input) input.value = '';
    filterItems();
    selectedIndex = -1;
  }

  function toggle() {
    palette?.classList.contains('active') ? close() : open();
  }

  function filterItems() {
    const query = input?.value.toLowerCase().trim() || '';
    refreshItems();
    items.forEach(item => {
      const text  = item.textContent.toLowerCase();
      const search = item.dataset.search?.toLowerCase() || '';
      const match = !query || text.includes(query) || search.includes(query);
      item.style.display = match ? '' : 'none';
    });
    // Reset selection
    selectedIndex = -1;
    updateSelection();
  }

  function handleKeyNav(e) {
    const visible = items.filter(i => i.style.display !== 'none');
    if (!visible.length) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      selectedIndex = Math.min(selectedIndex + 1, visible.length - 1);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      selectedIndex = Math.max(selectedIndex - 1, 0);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (selectedIndex >= 0 && visible[selectedIndex]) {
        const item = visible[selectedIndex];
        if (item.href) window.location.href = item.href;
        else item.click();
        close();
      }
    } else if (e.key === 'Escape') {
      close();
    }
    updateSelection(visible);
  }

  function updateSelection(visible) {
    const all = items.filter(i => i.style.display !== 'none');
    all.forEach((item, i) => {
      item.classList.toggle('selected', i === selectedIndex);
    });
    if (selectedIndex >= 0 && all[selectedIndex]) {
      all[selectedIndex].scrollIntoView({ block: 'nearest' });
    }
  }

  return { init, open, close };
})();

/* ============================================================================
   3. TOAST NOTIFICATION SYSTEM
   ============================================================================ */
const Toast = (() => {
  const ICONS = {
    success: `<svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`,
    error:   `<svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`,
    warning: `<svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`,
    info:    `<svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`,
  };

  function show(type, title, message, duration = 4000) {
    const container = document.getElementById('cnToastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `cn-toast ${type}`;
    toast.innerHTML = `
      <div class="cn-toast-icon">${ICONS[type] || ICONS.info}</div>
      <div class="cn-toast-content">
        <div class="cn-toast-title">${escapeHtml(title)}</div>
        ${message ? `<div class="cn-toast-message">${escapeHtml(message)}</div>` : ''}
      </div>
      <button class="cn-toast-close" aria-label="Dismiss notification">✕</button>
    `;

    // Dismiss on close button
    toast.querySelector('.cn-toast-close').addEventListener('click', () => dismiss(toast));

    container.appendChild(toast);

    // Auto dismiss
    if (duration > 0) {
      setTimeout(() => dismiss(toast), duration);
    }

    return toast;
  }

  function dismiss(toast) {
    toast.classList.add('leaving');
    setTimeout(() => toast.remove(), 350);
  }

  function success(title, message, duration) { return show('success', title, message, duration); }
  function error(title, message, duration)   { return show('error',   title, message, duration); }
  function warning(title, message, duration) { return show('warning', title, message, duration); }
  function info(title, message, duration)    { return show('info',    title, message, duration); }

  return { show, success, error, warning, info, dismiss };
})();

/* ============================================================================
   4. ANIMATED COUNTERS — Number tween on viewport entry
   ============================================================================ */
const AnimatedCounters = (() => {
  function init() {
    const counters = document.querySelectorAll('[data-counter]');
    if (!counters.length) return;

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          animateCounter(entry.target);
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.3 });

    counters.forEach(el => observer.observe(el));
  }

  function animateCounter(el) {
    const target  = parseFloat(el.dataset.counter);
    const suffix  = el.dataset.counterSuffix || '';
    const prefix  = el.dataset.counterPrefix || '';
    const decimals = el.dataset.counterDecimals ? parseInt(el.dataset.counterDecimals) : 0;
    const duration = 1500;
    const start    = Date.now();

    function easeOutQuart(t) {
      return 1 - Math.pow(1 - t, 4);
    }

    function tick() {
      const elapsed  = Date.now() - start;
      const progress = Math.min(elapsed / duration, 1);
      const value    = easeOutQuart(progress) * target;

      el.textContent = prefix + value.toFixed(decimals) + suffix;

      if (progress < 1) {
        requestAnimationFrame(tick);
      } else {
        el.textContent = prefix + target.toFixed(decimals) + suffix;
      }
    }

    requestAnimationFrame(tick);
  }

  return { init };
})();

/* ============================================================================
   5. SCROLL REVEAL — IntersectionObserver
   ============================================================================ */
const ScrollReveal = (() => {
  function init() {
    const elements = document.querySelectorAll('.cn-reveal, .cn-reveal-left, .cn-reveal-right');
    if (!elements.length) return;

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          // Stagger based on data-delay attribute
          const delay = entry.target.dataset.revealDelay || 0;
          setTimeout(() => {
            entry.target.classList.add('revealed');
          }, parseInt(delay));
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.1, rootMargin: '0px 0px -50px 0px' });

    elements.forEach(el => observer.observe(el));
  }

  return { init };
})();

/* ============================================================================
   6. UPLOAD ZONE — Drag & drop with visual feedback
   ============================================================================ */
const UploadZone = (() => {
  function init() {
    const zones = document.querySelectorAll('.cn-dropzone, #uploadZone');
    zones.forEach(initZone);

    // Also handle the hidden file input from legacy code
    const legacyInput = document.getElementById('fileInput');
    if (legacyInput) {
      legacyInput.addEventListener('change', (e) => updateFileDisplay(e.target));
    }
  }

  function initZone(zone) {
    const inputId = zone.dataset.inputId || 'fileInput';
    const input   = document.getElementById(inputId) || zone.querySelector('input[type="file"]');
    if (!input) return;

    // Click to browse
    zone.addEventListener('click', (e) => {
      if (e.target === zone || e.target.closest('.cn-dropzone-icon, .cn-dropzone-title, .cn-dropzone-subtitle')) {
        input.click();
      }
    });

    // Drag events
    ['dragenter', 'dragover'].forEach(evt => {
      zone.addEventListener(evt, (e) => {
        e.preventDefault();
        e.stopPropagation();
        zone.classList.add('drag-over');
      });
    });

    ['dragleave', 'drop'].forEach(evt => {
      zone.addEventListener(evt, (e) => {
        e.preventDefault();
        e.stopPropagation();
        zone.classList.remove('drag-over');
      });
    });

    zone.addEventListener('drop', (e) => {
      const files = e.dataTransfer.files;
      if (files.length) {
        input.files = files;
        updateFileDisplay(input);
        Toast.info('Files Selected', `${files.length} file${files.length > 1 ? 's' : ''} ready to upload`);
      }
    });

    // File input change
    input.addEventListener('change', () => updateFileDisplay(input));
  }

  function updateFileDisplay(input) {
    const nameEl = document.getElementById('selectedFileName') || document.getElementById('cnSelectedFileName');
    if (!nameEl) return;

    if (input.files.length === 1) {
      nameEl.textContent = input.files[0].name;
      nameEl.style.color = 'var(--blue-400)';
    } else if (input.files.length > 1) {
      nameEl.textContent = `${input.files.length} files selected`;
      nameEl.style.color = 'var(--blue-400)';
    }
  }

  return { init };
})();

/* ============================================================================
   7. MAGNETIC BUTTONS — Premium button physics
   ============================================================================ */
const MagneticButtons = (() => {
  function init() {
    const magnetics = document.querySelectorAll('.cn-magnetic, .cn-btn-primary');
    magnetics.forEach(initMagnetic);
  }

  function initMagnetic(btn) {
    btn.addEventListener('mousemove', (e) => {
      const rect = btn.getBoundingClientRect();
      const cx   = rect.left + rect.width / 2;
      const cy   = rect.top  + rect.height / 2;
      const dx   = (e.clientX - cx) * 0.25;
      const dy   = (e.clientY - cy) * 0.25;
      btn.style.transform = `translate(${dx}px, ${dy}px)`;
    });

    btn.addEventListener('mouseleave', () => {
      btn.style.transform = '';
      btn.style.transition = 'transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
    });

    btn.addEventListener('mouseenter', () => {
      btn.style.transition = 'transform 0.1s ease';
    });
  }

  return { init };
})();

/* ============================================================================
   8. FILE EXPLORER — Grid/List view toggle
   ============================================================================ */
const FileExplorer = (() => {
  function init() {
    const gridBtn  = document.getElementById('viewGridBtn');
    const listBtn  = document.getElementById('viewListBtn');
    const gridView = document.getElementById('filesGridView');
    const listView = document.getElementById('filesListView');

    if (!gridBtn && !listBtn) return;

    // Restore saved view preference
    const savedView = localStorage.getItem('cn_file_view') || 'list';
    setView(savedView, { gridBtn, listBtn, gridView, listView });

    gridBtn?.addEventListener('click', () => {
      setView('grid', { gridBtn, listBtn, gridView, listView });
      localStorage.setItem('cn_file_view', 'grid');
    });

    listBtn?.addEventListener('click', () => {
      setView('list', { gridBtn, listBtn, gridView, listView });
      localStorage.setItem('cn_file_view', 'list');
    });
  }

  function setView(view, { gridBtn, listBtn, gridView, listView }) {
    const isGrid = view === 'grid';

    gridBtn?.classList.toggle('active', isGrid);
    listBtn?.classList.toggle('active', !isGrid);

    if (gridView) gridView.style.display = isGrid ? '' : 'none';
    if (listView) listView.style.display = isGrid ? 'none' : '';
  }

  return { init };
})();

/* ============================================================================
   9. ALERT AUTO-DISMISS
   ============================================================================ */
const AlertSystem = (() => {
  function init() {
    // Auto-convert old Bootstrap alerts to new cn-alert style
    document.querySelectorAll('.alert-dismissible, .cn-alert').forEach(alert => {
      // Auto-dismiss after 5s
      setTimeout(() => {
        alert.style.transition = 'opacity 0.4s, max-height 0.4s, margin 0.4s, padding 0.4s';
        alert.style.opacity = '0';
        alert.style.maxHeight = '0';
        alert.style.padding = '0';
        alert.style.margin = '0';
        alert.style.overflow = 'hidden';
        setTimeout(() => alert.remove(), 450);
      }, 5000);

      // Manual close button
      const closeBtn = alert.querySelector('.cn-alert-close, .btn-close');
      closeBtn?.addEventListener('click', () => {
        alert.style.opacity = '0';
        setTimeout(() => alert.remove(), 300);
      });
    });

    // Convert Thymeleaf success/error alerts to toasts if present
    const successAlert = document.querySelector('[data-toast-success]');
    const errorAlert   = document.querySelector('[data-toast-error]');

    if (successAlert) {
      Toast.success('Success', successAlert.dataset.toastSuccess);
      successAlert.remove();
    }
    if (errorAlert) {
      Toast.error('Error', errorAlert.dataset.toastError);
      errorAlert.remove();
    }
  }

  return { init };
})();

/* ============================================================================
   10. SHARE LINK — Copy with animation
   ============================================================================ */
function copyShareLink() {
  const input = document.getElementById('shareLinkInput');
  if (!input) return;

  const fullUrl = window.location.origin + input.value;
  navigator.clipboard.writeText(fullUrl).then(() => {
    Toast.success('Link Copied!', 'Share link copied to clipboard');
    const btn = input.nextElementSibling;
    if (btn) {
      const orig = btn.innerHTML;
      btn.innerHTML = '✓ Copied';
      btn.style.background = 'var(--success-bg)';
      btn.style.color = 'var(--success)';
      setTimeout(() => {
        btn.innerHTML = orig;
        btn.style.background = '';
        btn.style.color = '';
      }, 2000);
    }
  }).catch(() => {
    // Fallback
    input.select();
    document.execCommand('copy');
    Toast.info('Copied', 'Link copied to clipboard');
  });
}

/* ============================================================================
   LEGACY COMPATIBILITY
   Keep legacy function names working for templates not yet updated
   ============================================================================ */
function togglePassword(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  const icon = btn.querySelector('i, svg');
  if (input.type === 'password') {
    input.type = 'text';
    if (icon) icon.style.opacity = '0.5';
  } else {
    input.type = 'password';
    if (icon) icon.style.opacity = '1';
  }
}

function updateFileName(input) {
  UploadZone.init(); // Re-init is safe; also call update directly
  const nameEl = document.getElementById('selectedFileName');
  if (!nameEl || !input.files.length) return;
  nameEl.textContent = input.files.length === 1
    ? input.files[0].name
    : `${input.files.length} files selected`;
}

function openMoveModal(type, id) {
  const form = document.getElementById('moveForm');
  if (!form) return;
  form.action = type === 'file' ? `/files/move/${id}` : `/folders/move/${id}`;

  const modal = document.getElementById('moveModalOverlay');
  if (modal) {
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
  }
}

/* ============================================================================
   MODAL HELPERS (new custom modals)
   ============================================================================ */
function openModal(id) {
  document.getElementById(id)?.classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeModal(id) {
  document.getElementById(id)?.classList.remove('active');
  document.body.style.overflow = '';
}

// Close modal on overlay click
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('cn-modal-overlay')) {
    e.target.classList.remove('active');
    document.body.style.overflow = '';
  }
});

/* ============================================================================
   PROGRESS BAR ANIMATION
   Called after page load to animate all progress bars to their final width
   ============================================================================ */
function animateProgressBars() {
  document.querySelectorAll('.cn-progress-bar[data-width]').forEach(bar => {
    const target = bar.dataset.width;
    // Start at 0, animate to target
    bar.style.width = '0%';
    requestAnimationFrame(() => {
      setTimeout(() => {
        bar.style.width = target + '%';
      }, 100);
    });
  });

  // Also animate bars with inline style width (set on page load)
  document.querySelectorAll('.cn-progress-bar').forEach(bar => {
    const currentWidth = bar.style.width;
    if (currentWidth && currentWidth !== '0%') {
      bar.style.width = '0%';
      setTimeout(() => {
        bar.style.transition = 'width 1.2s cubic-bezier(0.165, 0.84, 0.44, 1)';
        bar.style.width = currentWidth;
      }, 200);
    }
  });
}

/* ============================================================================
   QUOTA BAR ANIMATION (sidebar)
   ============================================================================ */
function animateSidebarQuota() {
  const fill = document.getElementById('sidebarQuotaFill');
  if (!fill) return;
  const target = fill.style.width;
  fill.style.width = '0%';
  setTimeout(() => {
    fill.style.transition = 'width 1.5s cubic-bezier(0.165, 0.84, 0.44, 1)';
    fill.style.width = target;
  }, 500);
}

/* ============================================================================
   UTILITY FUNCTIONS
   ============================================================================ */
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatNumber(num) {
  return new Intl.NumberFormat().format(num);
}

/* ============================================================================
   LUCIDE ICON RE-INIT
   Needed after dynamic DOM changes
   ============================================================================ */
function initIcons() {
  if (typeof lucide !== 'undefined') {
    lucide.createIcons();
  }
}

/* ============================================================================
   MAIN INITIALIZATION
   ============================================================================ */
document.addEventListener('DOMContentLoaded', () => {
  // Core shell
  AppShell.init();
  CommandPalette.init();

  // UI enhancements
  AlertSystem.init();
  AnimatedCounters.init();
  ScrollReveal.init();
  UploadZone.init();
  FileExplorer.init();

  // Premium interactions (desktop only for performance)
  if (window.innerWidth >= 768) {
    MagneticButtons.init();
  }

  // Animate progress bars
  animateProgressBars();
  animateSidebarQuota();

  // Re-init Lucide icons (in case any were added dynamically)
  initIcons();

  // Topbar entry animation
  const topbar = document.querySelector('.cn-topbar');
  if (topbar) {
    topbar.style.opacity = '0';
    topbar.style.transform = 'translateY(-8px)';
    setTimeout(() => {
      topbar.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
      topbar.style.opacity = '1';
      topbar.style.transform = 'translateY(0)';
    }, 100);
  }

  // Workspace entry animation
  const workspace = document.querySelector('.cn-workspace');
  if (workspace) {
    workspace.style.opacity = '0';
    setTimeout(() => {
      workspace.style.transition = 'opacity 0.5s ease';
      workspace.style.opacity = '1';
    }, 150);
  }

  // Show system status
  setTimeout(() => {
    const pill = document.getElementById('systemStatusPill');
    if (pill) {
      pill.style.opacity = '0';
      setTimeout(() => {
        pill.style.transition = 'opacity 0.4s';
        pill.style.opacity = '1';
      }, 50);
    }
  }, 600);

  console.log('%c CloudNest Enterprise v2.0 ', 'background: #3B82F6; color: #fff; font-weight: bold; border-radius: 4px; padding: 4px 8px;');
  console.log('%c Distributed Infrastructure Platform ', 'color: #8B5CF6; font-style: italic;');
});
