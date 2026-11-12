/* CyberShield shared JavaScript: theme, sidebar, CSRF-aware fetch, confirmations, global search, notifications. */
(function () {
    'use strict';

    // ---------------------------------------------------------------- CSRF helper for AJAX calls
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    window.csFetch = function (url, options = {}) {
        const opts = Object.assign({ credentials: 'same-origin', headers: {} }, options);
        opts.headers = Object.assign({ 'Accept': 'application/json' }, opts.headers);
        const method = (opts.method || 'GET').toUpperCase();
        if (method !== 'GET' && csrfToken && csrfHeader) {
            opts.headers[csrfHeader] = csrfToken;
        }
        return fetch(url, opts).then(r => {
            if (r.status === 401 || r.redirected && r.url.includes('/login')) {
                window.location.href = '/login';
                throw new Error('Session expired');
            }
            if (!r.ok) {
                throw new Error('Request failed: ' + r.status);
            }
            return r.json();
        });
    };

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }

    // ---------------------------------------------------------------- theme toggle
    function currentTheme() {
        return document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'dark' : 'light';
    }
    function applyThemeIcon() {
        document.querySelectorAll('[data-theme-icon]').forEach(i => {
            i.className = currentTheme() === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
        });
    }
    document.querySelectorAll('[data-theme-toggle]').forEach(btn => btn.addEventListener('click', () => {
        const next = currentTheme() === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-bs-theme', next);
        try { localStorage.setItem('cs-theme', next); } catch (e) { /* storage unavailable */ }
        applyThemeIcon();
        document.dispatchEvent(new CustomEvent('cs:themechange', { detail: next }));
    }));
    applyThemeIcon();

    // ---------------------------------------------------------------- sidebar
    try {
        if (localStorage.getItem('cs-sidebar') === 'collapsed' && window.innerWidth >= 992) {
            document.body.classList.add('cs-collapsed');
        }
    } catch (e) { /* ignore */ }
    document.querySelectorAll('[data-sidebar-toggle]').forEach(btn => btn.addEventListener('click', () => {
        if (window.innerWidth < 992) {
            document.body.classList.toggle('cs-mobile-open');
            return;
        }
        document.body.classList.toggle('cs-collapsed');
        try {
            localStorage.setItem('cs-sidebar', document.body.classList.contains('cs-collapsed') ? 'collapsed' : 'expanded');
        } catch (e) { /* ignore */ }
    }));

    // ---------------------------------------------------------------- confirmation modal for destructive actions
    const confirmModalEl = document.getElementById('csConfirmModal');
    let pendingForm = null;
    if (confirmModalEl && window.bootstrap) {
        const confirmModal = new bootstrap.Modal(confirmModalEl);
        document.addEventListener('submit', e => {
            const form = e.target;
            if (form.dataset.confirm && form.dataset.confirmed !== 'yes') {
                e.preventDefault();
                pendingForm = form;
                confirmModalEl.querySelector('[data-confirm-message]').textContent = form.dataset.confirm;
                confirmModal.show();
            }
        });
        confirmModalEl.querySelector('[data-confirm-ok]').addEventListener('click', () => {
            if (pendingForm) {
                pendingForm.dataset.confirmed = 'yes';
                confirmModal.hide();
                if (pendingForm.requestSubmit) { pendingForm.requestSubmit(); } else { pendingForm.submit(); }
            }
        });
    }

    // ---------------------------------------------------------------- global search (role-filtered on the server)
    const searchInput = document.getElementById('csGlobalSearch');
    const searchBox = document.getElementById('csSearchResults');
    let searchTimer = null;
    if (searchInput && searchBox) {
        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimer);
            const q = searchInput.value.trim();
            if (q.length < 2) { searchBox.classList.remove('show'); return; }
            searchTimer = setTimeout(() => {
                csFetch('/api/search?q=' + encodeURIComponent(q)).then(results => {
                    if (!results.length) {
                        searchBox.innerHTML = '<div class="p-3 cs-muted small">No matching records you can access.</div>';
                    } else {
                        searchBox.innerHTML = results.map(r =>
                            '<a href="' + escapeHtml(r.url) + '"><i class="bi ' + escapeHtml(r.icon) + ' text-primary"></i>' +
                            '<div class="flex-grow-1"><div class="fw-semibold">' + escapeHtml(r.name) + '</div>' +
                            '<div class="small cs-muted">' + escapeHtml(r.category) + ' &middot; <span class="cs-code">' +
                            escapeHtml(r.identifier) + '</span></div></div>' +
                            '<span class="badge cs-badge">' + escapeHtml(r.status) + '</span></a>').join('');
                    }
                    searchBox.classList.add('show');
                }).catch(() => searchBox.classList.remove('show'));
            }, 250);
        });
        document.addEventListener('click', e => {
            if (!searchBox.contains(e.target) && e.target !== searchInput) { searchBox.classList.remove('show'); }
        });
        searchInput.addEventListener('keydown', e => { if (e.key === 'Escape') { searchBox.classList.remove('show'); } });
    }

    // ---------------------------------------------------------------- notifications (database-backed, polled)
    const bellCount = document.getElementById('csNotifCount');
    const bellList = document.getElementById('csNotifList');

    function renderNotifications(summary) {
        if (bellCount) {
            bellCount.textContent = summary.unreadCount > 99 ? '99+' : summary.unreadCount;
            bellCount.classList.toggle('d-none', summary.unreadCount === 0);
        }
        if (!bellList) { return; }
        if (!summary.items.length) {
            bellList.innerHTML = '<div class="p-4 text-center cs-muted small"><i class="bi bi-bell-slash d-block fs-3 mb-1"></i>No notifications yet</div>';
            return;
        }
        bellList.innerHTML = summary.items.map(n =>
            '<div class="cs-notif-item' + (n.read ? '' : ' unread') + '" data-notif-id="' + n.id + '" data-link="' + escapeHtml(n.link) + '">' +
            '<i class="bi ' + escapeHtml(n.icon) + '"></i><div class="flex-grow-1"><div class="fw-semibold small">' + escapeHtml(n.title) +
            '</div><div class="small">' + escapeHtml(n.message) + '</div><div class="cs-notif-time">' + escapeHtml(n.createdAt) +
            ' &middot; ' + escapeHtml(n.category) + '</div></div></div>').join('');
    }

    function pollNotifications() {
        csFetch('/api/notifications/summary').then(renderNotifications).catch(() => { /* retry next time */ });
    }

    if (bellList) {
        bellList.addEventListener('click', e => {
            const item = e.target.closest('[data-notif-id]');
            if (!item) { return; }
            csFetch('/api/notifications/' + item.dataset.notifId + '/read', { method: 'POST' })
                .finally(() => { window.location.href = item.dataset.link || '/notifications'; });
        });
        document.getElementById('csNotifReadAll')?.addEventListener('click', e => {
            e.preventDefault();
            csFetch('/api/notifications/read-all', { method: 'POST' }).then(pollNotifications);
        });
        pollNotifications();
        setInterval(pollNotifications, 30000);
    }

    // ---------------------------------------------------------------- upload size check (server enforces it too)
    document.querySelectorAll('input[type="file"][data-max-size]').forEach(input => {
        input.addEventListener('change', () => {
            const max = parseInt(input.dataset.maxSize, 10);
            const file = input.files && input.files[0];
            input.setCustomValidity(file && file.size > max ? 'The file is larger than the 10 MB limit.' : '');
            input.reportValidity();
        });
    });

    // ---------------------------------------------------------------- misc
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => window.bootstrap && new bootstrap.Tooltip(el));
    // auto-submit filter selects
    document.querySelectorAll('form[data-auto-submit] select').forEach(s => s.addEventListener('change', () => s.form.submit()));
})();
