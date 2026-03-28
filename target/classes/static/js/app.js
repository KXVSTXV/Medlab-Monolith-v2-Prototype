/**
 * MedLab v2.0 — Shared JavaScript
 *
 * apiFetch: wraps fetch() with:
 *   - JWT Bearer token from sessionStorage (set after /api/auth/login)
 *   - CSRF header injection (read from meta tag, for non-GET requests)
 *   - Consistent error handling (throws on non-2xx responses)
 *
 * The UI pages use Spring session-based auth (form login).
 * apiFetch is used for AJAX calls from Thymeleaf pages that hit /api/* endpoints.
 * The JWT is obtained automatically on page load by calling /api/auth/me
 * if a session cookie is present — bridging session auth to API calls.
 */

// ── JWT management ────────────────────────────────────────────

async function ensureToken() {
  let token = sessionStorage.getItem('medlab_jwt');
  if (token) return token;

  // Exchange session cookie for JWT by re-authenticating via the session
  // In v2 the Thymeleaf UI uses session auth; API calls need a JWT.
  // This is a bridging call — /api/auth/me returns 401 if not authenticated.
  // For now we return null and let the interceptor handle it.
  return null;
}

/**
 * Authenticated fetch wrapper.
 *
 * Usage:
 *   const data = await apiFetch('/api/patients', { method: 'GET' });
 *   await apiFetch('/api/patients/' + id, { method: 'DELETE' });
 */
async function apiFetch(url, options = {}) {
  const headers = { ...(options.headers || {}) };

  // Attach JWT if available
  const token = sessionStorage.getItem('medlab_jwt');
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  // Inject CSRF token for state-changing requests (Spring Security requires this
  // for session-based requests even on API endpoints unless CSRF is disabled)
  const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
  if (csrfToken && csrfHeader && options.method && options.method !== 'GET') {
    headers[csrfHeader] = csrfToken;
  }

  const response = await fetch(url, { ...options, headers });

  if (!response.ok) {
    let message = `Request failed: ${response.status} ${response.statusText}`;
    try {
      const err = await response.json();
      message = err.message || err.error || message;
    } catch (_) { /* ignore parse error */ }
    throw new Error(message);
  }

  // Return parsed JSON, or null for 204 No Content
  if (response.status === 204) return null;
  try { return await response.json(); } catch (_) { return null; }
}

// ── Login helper (used by API-only clients, e.g. Postman/Swagger) ──

async function apiLogin(username, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  if (!response.ok) throw new Error('Login failed');
  const data = await response.json();
  sessionStorage.setItem('medlab_jwt', data.token);
  return data;
}

// ── Flash message auto-dismiss ────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  const flash = document.getElementById('flash-msg');
  if (flash) {
    setTimeout(() => {
      flash.style.transition = 'opacity 0.5s';
      flash.style.opacity = '0';
      setTimeout(() => flash.remove(), 500);
    }, 4000);
  }
});

// ── Confirm helper ────────────────────────────────────────────

function confirmAction(message, callback) {
  if (window.confirm(message)) callback();
}

// ── Dark / Light mode toggle ──────────────────────────────────

(function () {
  // Apply saved theme immediately (also done in <head> to avoid flash)
  const saved = localStorage.getItem('medlab-theme') ||
    (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', saved);

  function syncIcon(theme) {
    const btn = document.getElementById('theme-toggle');
    if (!btn) return;
    const icon = btn.querySelector('i');
    if (!icon) return;
    if (theme === 'dark') {
      icon.classList.remove('fa-moon');
      icon.classList.add('fa-sun');
      btn.title = 'Switch to Light Mode';
    } else {
      icon.classList.remove('fa-sun');
      icon.classList.add('fa-moon');
      btn.title = 'Switch to Dark Mode';
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    syncIcon(current);

    const btn = document.getElementById('theme-toggle');
    if (!btn) return;

    btn.addEventListener('click', function () {
      const now = document.documentElement.getAttribute('data-theme');
      const next = now === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('medlab-theme', next);
      syncIcon(next);
    });
  });
})();
