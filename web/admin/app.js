/**
 * Lloyd ERP — Student Fleet & Telemetry Admin Dashboard
 * Dynamic telemetry loader, real-time KPI computation, active ban management,
 * and zero-race-condition fleet governance controls.
 * ZERO HARDCODED DATA / VERSIONS: Grounded strictly in live network/Redis responses.
 */

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function safeAttr(str) {
    if (str === null || str === undefined) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

function parseSemver(ver) {
    return String(ver || '')
        .trim()
        .replace(/^[vV]/, '')
        .split(/[.-]/)
        .map(part => {
            const n = parseInt(part, 10);
            return Number.isNaN(n) ? 0 : n;
        });
}

function compareSemverDesc(a, b) {
    const pa = parseSemver(a);
    const pb = parseSemver(b);
    const len = Math.max(pa.length, pb.length);
    for (let i = 0; i < len; i++) {
        const diff = (pb[i] || 0) - (pa[i] || 0);
        if (diff !== 0) return diff;
    }
    return String(b || '').localeCompare(String(a || ''));
}

const ICON_COPY = `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>`;
const ICON_TRASH = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>`;
const ICON_DEVICE = `<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>`;
const ICON_STUDENT = `<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`;
const ICON_EYE = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`;
const ICON_EYE_OFF = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`;

class TelemetryDashboard {
    constructor() {
        this.records = [];
        this.filteredRecords = [];
        this.isFetching = false;
        this.lastSyncAt = null;
        this.baseSyncStatusText = '';
        this.syncAgeTimer = null;
        this.pollTimer = null;
        this.sortColumn = 'timestamp';
        this.sortDirection = 'desc';
        this.viewMode = (typeof window !== 'undefined' && window.innerWidth < 768) ? 'card' : 'table';

        // Auto-connect to origin or local node server without prompting
        const isHttp = typeof window !== 'undefined' && window.location.protocol.startsWith('http');
        const defaultOrigin = isHttp ? window.location.origin : 'http://localhost:8080';
        let storedUrl = localStorage.getItem('telemetry_api_url');
        if (typeof window !== 'undefined' && window.location.protocol === 'https:' && storedUrl && storedUrl.startsWith('http://')) {
            storedUrl = null;
            localStorage.removeItem('telemetry_api_url');
        }
        this.endpointUrl = storedUrl || defaultOrigin;
        this.fleetConfig = this.loadStoredConfig();

        this.initDOMElements();
        this.initGovernanceElements();
        this.bindEvents();
        this.populateGovernanceFields();
        this.updateServerLabel();
        this.renderMaintenanceBanner();
        this.renderOtaPausedBanner();
        this.renderActiveBanChips();
        this.updateSortIcons();
        this.setViewMode(this.viewMode);

        if (this.checkAuthStatus()) {
            this.loadTelemetry();
        }
    }

    loadStoredConfig() {
        const stored = localStorage.getItem('fleet_config');
        if (stored) {
            try {
                return JSON.parse(stored);
            } catch (e) { }
        }
        return {
            min_version_code: 1,
            latest_version_name: null,
            download_url: '',
            banned_students: [],
            banned_devices: [],
            maintenance_mode: false,
            maintenance_message: 'Lloyd ERP service is currently undergoing routine maintenance.',
            broadcast_notice: '',
            pause_updates: false,
            notifications_enabled: true
        };
    }

    initDOMElements() {
        this.totalUsersEl = document.getElementById('totalUsers');
        this.activeTodayEl = document.getElementById('activeToday');
        this.latestVersionEl = document.getElementById('latestVersion');
        this.adoptionRateEl = document.getElementById('adoptionRate');
        this.osDominanceEl = document.getElementById('osDominance');
        this.restrictedCountEl = document.getElementById('restrictedCount');
        this.restrictedSubEl = document.getElementById('restrictedSub');
        this.versionBarsEl = document.getElementById('versionBars');
        this.tableBodyEl = document.getElementById('tableBody');
        this.tableCountEl = document.getElementById('tableCount');
        this.searchInput = document.getElementById('searchInput');
        this.statusFilter = document.getElementById('statusFilter');
        this.versionFilter = document.getElementById('versionFilter');
        this.refreshBtn = document.getElementById('refreshBtn');
        this.configBtn = document.getElementById('configBtn');
        this.pingBtn = document.getElementById('pingBtn');
        this.exportCsvBtn = document.getElementById('exportCsvBtn');
        this.syncStatusEl = document.getElementById('syncStatus');
        this.liveDotEl = document.getElementById('liveDot');
        this.syncIndicatorEl = document.getElementById('syncIndicator');
        this.toastContainer = document.getElementById('toastContainer');

        // Sidebar & Mobile Navigation
        this.sidebar = document.getElementById('sidebar');
        this.sidebarOverlay = document.getElementById('sidebarOverlay');
        this.closeSidebarBtn = document.getElementById('closeSidebarBtn');
        this.mobileMenuToggle = document.getElementById('mobileMenuToggle');
        this.mobileLiveDot = document.getElementById('mobileLiveDot');
        this.mobileRefreshBtn = document.getElementById('mobileRefreshBtn');
        this.mobileLockBtn = document.getElementById('mobileLockBtn');

        // Server switcher
        this.switchServerBtn = document.getElementById('switchServerBtn');
        this.serverLabel = document.getElementById('serverLabel');

        // Maintenance banner
        this.maintenanceAlertBanner = document.getElementById('maintenanceAlertBanner');
        this.maintBannerMsg = document.getElementById('maintBannerMsg');
        this.deactivateMaintBtn = document.getElementById('deactivateMaintBtn');

        // In-App OTA Updates Paused Banner & Control
        this.otaPausedAlertBanner = document.getElementById('otaPausedAlertBanner');
        this.resumeOtaBtn = document.getElementById('resumeOtaBtn');
        this.cfgPauseUpdates = document.getElementById('cfgPauseUpdates');
        this.cfgNotificationsEnabled = document.getElementById('cfgNotificationsEnabled');

        // View Mode Switcher & Card Grid
        this.tableViewBtn = document.getElementById('tableViewBtn');
        this.cardViewBtn = document.getElementById('cardViewBtn');
        this.tableContainer = document.getElementById('tableContainer');
        this.studentCardGrid = document.getElementById('studentCardGrid');

        // Quick broadcast tools
        this.quickBroadcastInput = document.getElementById('quickBroadcastInput');
        this.quickBroadcastBtn = document.getElementById('quickBroadcastBtn');
        this.clearBroadcastBtn = document.getElementById('clearBroadcastBtn');

        // Quick manual restriction
        this.quickBanType = document.getElementById('quickBanType');
        this.quickBanInput = document.getElementById('quickBanInput');
        this.quickBanBtn = document.getElementById('quickBanBtn');
        this.quickUnbanBtn = document.getElementById('quickUnbanBtn');

        // Active bans visual manager
        this.bannedDevChips = document.getElementById('bannedDevChips');
        this.bannedStudentChips = document.getElementById('bannedStudentChips');
        this.bannedDevCount = document.getElementById('bannedDevCount');
        this.bannedStudentCount = document.getElementById('bannedStudentCount');

        // Test telemetry cleanup & GitHub sync
        this.purgeTestsBtn = document.getElementById('purgeTestsBtn');
        this.fetchGhBtn = document.getElementById('fetchGhBtn');

        // Admin Security Passkey Gate Elements
        this.adminAuthOverlay = document.getElementById('adminAuthOverlay');
        this.authCard = document.getElementById('authCard');
        this.adminAuthForm = document.getElementById('adminAuthForm');
        this.adminPassInput = document.getElementById('adminPassInput');
        this.togglePassVisibility = document.getElementById('togglePassVisibility');
        this.eyeIcon = document.getElementById('eyeIcon');
        this.authErrorMsg = document.getElementById('authErrorMsg');
        this.rememberAdminPass = document.getElementById('rememberAdminPass');
        this.unlockPortalBtn = document.getElementById('unlockPortalBtn');
        this.lockPortalBtn = document.getElementById('lockPortalBtn');
    }

    getStoredPass() {
        // Key: lloyd_admin_token — stores opaque session token issued by /api/auth, NOT the raw password.
        return sessionStorage.getItem('lloyd_admin_token') || localStorage.getItem('lloyd_admin_token') || '';
    }

    setStoredPass(token, remember = true) {
        sessionStorage.setItem('lloyd_admin_token', token);
        if (remember) {
            localStorage.setItem('lloyd_admin_token', token);
        } else {
            localStorage.removeItem('lloyd_admin_token');
        }
    }

    clearStoredPass() {
        sessionStorage.removeItem('lloyd_admin_token');
        localStorage.removeItem('lloyd_admin_token');
    }

    getAuthHeaders() {
        const pass = this.getStoredPass();
        return {
            'Content-Type': 'application/json',
            'x-admin-key': pass,
            'x-admin-password': pass,
            'Authorization': `Bearer ${pass}`
        };
    }

    checkAuthStatus() {
        const pass = this.getStoredPass();
        if (pass) {
            this.hideAuthOverlay();
            return true;
        } else {
            this.showAuthOverlay();
            return false;
        }
    }

    showAuthOverlay() {
        if (!this.adminAuthOverlay) return;
        this.adminAuthOverlay.style.display = 'flex';
        if (this.authErrorMsg) this.authErrorMsg.style.display = 'none';
        if (this.adminPassInput) {
            this.adminPassInput.value = '';
            setTimeout(() => this.adminPassInput.focus(), 150);
        }
    }

    hideAuthOverlay() {
        if (!this.adminAuthOverlay) return;
        this.adminAuthOverlay.style.display = 'none';
        if (this.authErrorMsg) this.authErrorMsg.style.display = 'none';
    }

    async handleAuthSubmit() {
        if (!this.adminPassInput) return;
        const pass = (this.adminPassInput.value || '').trim();
        if (!pass) return;

        if (this.unlockPortalBtn) {
            this.unlockPortalBtn.disabled = true;
            this.unlockPortalBtn.innerHTML = '<span>Verifying passkey...</span>';
        }

        try {
            let apiUrl = this.getApiUrl('/auth');
            let res;
            const authHeaders = {
                'Content-Type': 'application/json',
                'x-admin-password': pass,
                'x-admin-key': pass
            };
            try {
                res = await fetch(apiUrl, {
                    method: 'POST',
                    headers: authHeaders,
                    body: JSON.stringify({ password: pass })
                });
            } catch (networkErr) {
                if (typeof window !== 'undefined' && window.location.origin && window.location.origin.startsWith('http') && apiUrl.indexOf(window.location.origin) === -1) {
                    apiUrl = `${window.location.origin}/api/auth`;
                    res = await fetch(apiUrl, {
                        method: 'POST',
                        headers: authHeaders,
                        body: JSON.stringify({ password: pass })
                    });
                } else {
                    throw networkErr;
                }
            }

            if (res.ok) {
                const data = await res.json();
                const token = data.token; // opaque session token, NOT the raw password
                const remember = this.rememberAdminPass ? this.rememberAdminPass.checked : true;
                this.setStoredPass(token, remember);
                this.hideAuthOverlay();
                this.showToast('Admin access verified. Welcome!', 'success');
                this.loadTelemetry();
            } else {
                this.showAuthError('Incorrect passkey. Access denied.');
            }
        } catch (err) {
            console.error('[Auth] Server unreachable:', err);
            this.showAuthError('Connection error: cannot reach server. Check network connection.');
        } finally {
            if (this.unlockPortalBtn) {
                this.unlockPortalBtn.disabled = false;
                this.unlockPortalBtn.innerHTML = `
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>
                    <span>Unlock Admin Portal</span>
                `;
            }
        }
    }

    showAuthError(msg = null) {
        if (this.authErrorMsg) {
            const span = this.authErrorMsg.querySelector('span');
            if (span && msg) span.textContent = msg;
            this.authErrorMsg.style.display = 'block';
        }
        if (this.authCard) {
            this.authCard.classList.remove('shake');
            void this.authCard.offsetWidth;
            this.authCard.classList.add('shake');
        }
        if (this.adminPassInput) {
            this.adminPassInput.select();
            this.adminPassInput.focus();
        }
    }

    lockPortal() {
        this.clearStoredPass();
        this.stopPolling();
        this.stopSyncAgeTimer();
        // Always navigate back to overview when locking so main-content is
        // visible (not feedback view) once the user re-authenticates.
        document.body.classList.remove('view-feedback');
        if (window.history && window.history.replaceState) {
            window.history.replaceState(null, '', '#overview');
        }
        this.showAuthOverlay();
        this.showToast('Admin portal locked', 'info');
    }

    initGovernanceElements() {
        this.cfgMinVersion = document.getElementById('cfgMinVersion');
        this.cfgLatestVersion = document.getElementById('cfgLatestVersion');
        this.cfgDownloadUrl = document.getElementById('cfgDownloadUrl');
        this.cfgBroadcastNotice = document.getElementById('cfgBroadcastNotice');
        this.cfgMaintenanceMessage = document.getElementById('cfgMaintenanceMessage');
        this.cfgMaintenanceMode = document.getElementById('cfgMaintenanceMode');
        this.saveConfigBtn = document.getElementById('saveConfigBtn');
        this.downloadConfigBtn = document.getElementById('downloadConfigBtn');
        this.copyConfigBtn = document.getElementById('copyConfigBtn');
    }

    getApiUrl(route) {
        const clean = (this.endpointUrl || 'http://localhost:8080').replace(/\/+$/, '');
        if (clean.endsWith('/api')) {
            return `${clean}${route}`;
        }
        return `${clean}/api${route}`;
    }

    async _extractErrorText(res) {
        try {
            const text = await res.text();
            if (!text) return `HTTP ${res.status}`;
            try {
                const parsed = JSON.parse(text);
                if (parsed && parsed.error) return String(parsed.error);
            } catch (_) {}
            return text;
        } catch (_) {
            return `HTTP ${res.status}`;
        }
    }

    _createSnapshot() {
        return {
            fleetConfig: JSON.parse(JSON.stringify(this.fleetConfig)),
            records: JSON.parse(JSON.stringify(this.records))
        };
    }

    _restoreSnapshot(snapshot) {
        if (!snapshot) return;
        this.fleetConfig = snapshot.fleetConfig;
        this.records = snapshot.records;
        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
        this.populateGovernanceFields();
        this.renderMaintenanceBanner();
        this.renderOtaPausedBanner();
        this.renderActiveBanChips();
        this.populateVersionFilter();
        this.computeKPIs();
        this.renderVersionBars();
        this.applyFilters();
    }

    populateGovernanceFields() {
        if (!this.cfgMinVersion) return;
        this.cfgMinVersion.value = this.fleetConfig.min_version_code || '';
        if (this.cfgLatestVersion) {
            this.cfgLatestVersion.value = this.fleetConfig.latest_version_name || '';
        }
        if (this.cfgDownloadUrl) {
            this.cfgDownloadUrl.value = this.fleetConfig.download_url || '';
        }
        if (this.cfgBroadcastNotice) {
            this.cfgBroadcastNotice.value = this.fleetConfig.broadcast_notice || '';
        }
        if (this.quickBroadcastInput) {
            this.quickBroadcastInput.value = this.fleetConfig.broadcast_notice || '';
        }
        if (this.cfgMaintenanceMessage) {
            this.cfgMaintenanceMessage.value = this.fleetConfig.maintenance_message || '';
        }
        if (this.cfgMaintenanceMode) {
            this.cfgMaintenanceMode.checked = !!this.fleetConfig.maintenance_mode;
        }
        if (this.cfgPauseUpdates) {
            this.cfgPauseUpdates.checked = !!this.fleetConfig.pause_updates;
        }
        if (this.cfgNotificationsEnabled) {
            this.cfgNotificationsEnabled.checked = this.fleetConfig.notifications_enabled !== false;
        }
    }

    renderMaintenanceBanner() {
        if (!this.maintenanceAlertBanner) return;
        const isMaint = !!this.fleetConfig.maintenance_mode;
        if (isMaint) {
            this.maintenanceAlertBanner.style.display = 'flex';
            if (this.maintBannerMsg) {
                this.maintBannerMsg.textContent = this.fleetConfig.maintenance_message || 'Routine maintenance in progress.';
            }
        } else {
            this.maintenanceAlertBanner.style.display = 'none';
        }
    }

    renderOtaPausedBanner() {
        if (!this.otaPausedAlertBanner) return;
        const isPaused = !!this.fleetConfig.pause_updates;
        this.otaPausedAlertBanner.style.display = isPaused ? 'flex' : 'none';
        if (this.cfgPauseUpdates) {
            this.cfgPauseUpdates.checked = isPaused;
        }
    }

    async togglePauseUpdates(explicitState = null) {
        const prevState = !!this.fleetConfig.pause_updates;
        const nextState = explicitState !== null ? !!explicitState : !prevState;

        try {
            const res = await fetch(this.getApiUrl('/config'), {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ pause_updates: nextState })
            });
            if (res.status === 401) {
                if (this.cfgPauseUpdates) this.cfgPauseUpdates.checked = prevState;
                this.showToast('Unauthorized: Please unlock admin portal.', 'error');
                this.lockPortal();
                return;
            }
            if (!res.ok && res.status !== 401) {
                if (this.cfgPauseUpdates) this.cfgPauseUpdates.checked = prevState;
                const errText = await this._extractErrorText(res);
                this.showToast(`Failed to update OTA pause state: ${errText}`, 'error');
                return;
            }
            this.fleetConfig.pause_updates = nextState;
            if (this.cfgPauseUpdates) this.cfgPauseUpdates.checked = nextState;
            this.renderOtaPausedBanner();
            localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
            if (nextState) {
                this.showToast('In-app OTA updates PAUSED fleet-wide.', 'info');
            } else {
                this.showToast('In-app OTA updates RESUMED.', 'success');
            }
        } catch (e) {
            if (this.cfgPauseUpdates) this.cfgPauseUpdates.checked = prevState;
            this.showToast(`Failed to update OTA pause state: ${e.message}`, 'error');
        }
    }

    openSidebar() {
        if (this.sidebar) this.sidebar.classList.add('open');
        if (this.sidebarOverlay) this.sidebarOverlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    closeSidebar() {
        if (this.sidebar) this.sidebar.classList.remove('open');
        if (this.sidebarOverlay) this.sidebarOverlay.classList.remove('active');
        document.body.style.overflow = '';
    }

    syncMobileLiveDot() {
        if (!this.mobileLiveDot) return;
        if (this.liveDotEl) {
            this.mobileLiveDot.style.backgroundColor = this.liveDotEl.style.backgroundColor;
            this.mobileLiveDot.style.boxShadow = this.liveDotEl.style.boxShadow;
        }
    }

    setViewMode(mode) {
        this.viewMode = mode;
        if (this.tableViewBtn && this.cardViewBtn) {
            if (mode === 'table') {
                this.tableViewBtn.classList.add('active');
                this.cardViewBtn.classList.remove('active');
                if (this.tableContainer) this.tableContainer.style.display = 'block';
                if (this.studentCardGrid) this.studentCardGrid.style.display = 'none';
            } else {
                this.cardViewBtn.classList.add('active');
                this.tableViewBtn.classList.remove('active');
                if (this.tableContainer) this.tableContainer.style.display = 'none';
                if (this.studentCardGrid) this.studentCardGrid.style.display = 'grid';
            }
        }
        this.renderCurrentView();
    }

    renderCurrentView() {
        this.renderTable();
        this.renderCards();
    }

    renderActiveBanChips() {
        // Render Banned Devices
        if (this.bannedDevChips) {
            const devList = this.fleetConfig.banned_devices || [];
            if (this.bannedDevCount) this.bannedDevCount.textContent = devList.length.toString();
            if (devList.length === 0) {
                this.bannedDevChips.innerHTML = '<span class="no-chips">No devices currently revoked.</span>';
            } else {
                this.bannedDevChips.innerHTML = devList.map(rawId => {
                    const id = String(rawId || '').trim();
                    const short = id.length > 22 ? id.substring(0, 10) + '...' + id.slice(-6) : id;
                    return `
                        <div class="ban-chip" title="Device UUID: ${escapeHtml(id)}">
                            <span>${ICON_DEVICE} ${escapeHtml(short)}</span>
                            <button class="btn-remove-chip" title="Restore device access" aria-label="Restore device" onclick="window.telemetryDashboard.toggleDeviceBan('${safeAttr(id)}', 'unban')">×</button>
                        </div>
                    `;
                }).join('');
            }
        }

        // Render Banned Student Accounts
        if (this.bannedStudentChips) {
            const studentList = this.fleetConfig.banned_students || [];
            if (this.bannedStudentCount) this.bannedStudentCount.textContent = studentList.length.toString();
            if (studentList.length === 0) {
                this.bannedStudentChips.innerHTML = '<span class="no-chips">No student accounts currently banned.</span>';
            } else {
                this.bannedStudentChips.innerHTML = studentList.map(rawId => {
                    const id = parseInt(rawId);
                    return `
                        <div class="ban-chip ban-chip-student" title="Student ID: ${id}">
                            <span>${ICON_STUDENT} #${id}</span>
                            <button class="btn-remove-chip" title="Unban student account" aria-label="Unban student account" onclick="window.telemetryDashboard.toggleStudentBan(${id}, 'unban')">×</button>
                        </div>
                    `;
                }).join('');
            }
        }
    }

    updateServerLabel() {
        if (!this.serverLabel) return;
        const current = (this.endpointUrl || '').replace(/\/+$/, '');
        if (current.includes('localhost') || current.includes('127.0.0.1')) {
            this.serverLabel.textContent = 'Server: Local';
        } else if (current.includes('vercel.app')) {
            this.serverLabel.textContent = 'Server: Cloud';
        } else {
            this.serverLabel.textContent = 'Server: Custom';
        }
    }

    switchServer() {
        const current = (this.endpointUrl || '').replace(/\/+$/, '');
        let nextUrl = '';
        if (current.includes('localhost') || current.includes('127.0.0.1')) {
            nextUrl = 'https://lloyd-erp-sand.vercel.app';
        } else {
            nextUrl = 'http://localhost:8080';
        }
        this.endpointUrl = nextUrl;
        localStorage.setItem('telemetry_api_url', nextUrl);
        this.updateServerLabel();
        this.showToast(`Switched API server to ${nextUrl}`, 'info');
        this.loadTelemetry();
    }

    showToast(message, type = 'info') {
        if (!this.toastContainer) return;
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        // Use textContent to avoid XSS from any user-controlled message content
        const span = document.createElement('span');
        span.textContent = String(message || '');
        toast.appendChild(span);
        this.toastContainer.appendChild(toast);

        requestAnimationFrame(() => toast.classList.add('show'));
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    copyToClipboard(text, label = 'Text') {
        if (!text) return;
        navigator.clipboard.writeText(String(text).trim()).then(() => {
            this.showToast(`Copied ${label} to clipboard!`, 'success');
        }).catch(() => {
            this.showToast(`Failed to copy to clipboard`, 'error');
        });
    }

    async saveConfig() {
        const minVerRaw = this.cfgMinVersion ? (this.cfgMinVersion.value || '').trim() : '';
        const latestVer = this.cfgLatestVersion ? (this.cfgLatestVersion.value || '').trim() : '';
        const dlUrl = (this.cfgDownloadUrl ? this.cfgDownloadUrl.value : '').trim();
        const notice = (this.cfgBroadcastNotice ? this.cfgBroadcastNotice.value : '').trim();
        const maintMsg = (this.cfgMaintenanceMessage ? this.cfgMaintenanceMessage.value : '').trim();
        const isMaint = this.cfgMaintenanceMode ? this.cfgMaintenanceMode.checked : false;
        const pauseUpdates = this.cfgPauseUpdates ? this.cfgPauseUpdates.checked : false;
        const notifEnabled = this.cfgNotificationsEnabled ? this.cfgNotificationsEnabled.checked : true;

        // Build patch object only from filled fields; omit blank optional fields like download_url
        const patch = {
            maintenance_mode: isMaint,
            pause_updates: pauseUpdates,
            notifications_enabled: notifEnabled
        };
        if (minVerRaw !== '') {
            const minVer = parseInt(minVerRaw, 10);
            if (!Number.isInteger(minVer) || minVer < 0) {
                this.showToast('Minimum Allowed Version Code must be a non-negative integer', 'error');
                return;
            }
            patch.min_version_code = minVer;
        }
        if (latestVer !== '') {
            patch.latest_version_name = latestVer;
        }
        if (dlUrl !== '') {
            patch.download_url = dlUrl;
        }
        if (notice !== '') {
            patch.broadcast_notice = notice;
        } else if (this.fleetConfig.broadcast_notice) {
            patch.broadcast_notice = null;
        }
        if (maintMsg !== '') {
            patch.maintenance_message = maintMsg;
        }

        if (!this.endpointUrl) {
            this.showToast('No API endpoint configured', 'error');
            return;
        }

        try {
            const res = await fetch(this.getApiUrl('/config'), {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(patch)
            });
            if (res.status === 401) {
                this.showToast('Unauthorized: Please unlock admin portal with passkey.', 'error');
                this.lockPortal();
                return;
            }
            if (!res.ok) {
                const errText = await this._extractErrorText(res);
                this.showToast(`Failed to save config: ${errText}`, 'error');
                return;
            }

            const data = await res.json().catch(() => ({}));
            const savedConfig = (data && data.config) ? data.config : patch;
            this.fleetConfig = { ...this.fleetConfig, ...savedConfig };
            localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));

            this.showToast('Config saved and deployed to fleet!', 'success');
            if (this.saveConfigBtn) {
                const originalText = this.saveConfigBtn.innerHTML;
                this.saveConfigBtn.innerHTML = `✓ Saved & Deployed`;
                this.saveConfigBtn.style.background = '#84A59D';
                setTimeout(() => {
                    this.saveConfigBtn.innerHTML = originalText;
                    this.saveConfigBtn.style.background = '';
                }, 2000);
            }

            this.populateGovernanceFields();
            this.renderMaintenanceBanner();
            this.renderOtaPausedBanner();
            this.renderActiveBanChips();
            this.computeKPIs();
            this.renderCurrentView();
        } catch (err) {
            console.warn('Could not sync remote config:', err);
            this.showToast(`Failed to save config: ${err.message}`, 'error');
        }
    }

    async deactivateMaintenance() {
        try {
            const res = await fetch(this.getApiUrl('/config'), {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ maintenance_mode: false })
            });
            if (res.status === 401) {
                this.showToast('Unauthorized: Please unlock admin portal.', 'error');
                this.lockPortal();
                return;
            }
            if (!res.ok && res.status !== 401) {
                const errText = await this._extractErrorText(res);
                this.showToast(`Failed to deactivate maintenance mode: ${errText}`, 'error');
                return;
            }
            this.fleetConfig.maintenance_mode = false;
            if (this.cfgMaintenanceMode) this.cfgMaintenanceMode.checked = false;
            localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
            this.renderMaintenanceBanner();
            this.computeKPIs();
            this.renderCurrentView();
            this.showToast('Global maintenance mode deactivated! All students restored.', 'success');
        } catch (e) {
            this.showToast(`Failed to deactivate maintenance mode: ${e.message}`, 'error');
        }
    }

    async pushBroadcast(noticeText = null) {
        const text = (noticeText !== null ? noticeText : (this.quickBroadcastInput ? this.quickBroadcastInput.value : '')).trim();

        try {
            const res = await fetch(this.getApiUrl('/config'), {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ broadcast_notice: text || null })
            });
            if (res.status === 401) {
                this.showToast('Unauthorized: Please unlock admin portal.', 'error');
                this.lockPortal();
                return;
            }
            if (!res.ok && res.status !== 401) {
                const errText = await this._extractErrorText(res);
                this.showToast(`Failed to push broadcast: ${errText}`, 'error');
                return;
            }
            this.fleetConfig.broadcast_notice = text || null;
            if (this.cfgBroadcastNotice) this.cfgBroadcastNotice.value = text;
            if (this.quickBroadcastInput) this.quickBroadcastInput.value = text;
            localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
            this.showToast(text ? 'Broadcast notice pushed to student dashboards!' : 'Broadcast notice cleared.', 'success');
        } catch (e) {
            this.showToast(`Failed to push broadcast: ${e.message}`, 'error');
        }
    }

    async clearBroadcast() {
        if (this.quickBroadcastInput) this.quickBroadcastInput.value = '';
        await this.pushBroadcast('');
    }

    downloadConfig() {
        const json = JSON.stringify(this.fleetConfig, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'fleet_config.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showToast('Exported fleet_config.json', 'success');
    }

    copyConfig() {
        const json = JSON.stringify(this.fleetConfig, null, 2);
        navigator.clipboard.writeText(json).then(() => {
            if (this.copyConfigBtn) {
                const originalText = this.copyConfigBtn.innerHTML;
                this.copyConfigBtn.innerHTML = `✓ Copied!`;
                setTimeout(() => {
                    this.copyConfigBtn.innerHTML = originalText;
                }, 1800);
            }
            this.showToast('Configuration JSON copied to clipboard', 'success');
        });
    }

    // Direct atomic device ban/unban (Zero race condition)
    async toggleDeviceBan(deviceId, explicitAction = null) {
        if (!deviceId) return;
        const id = String(deviceId).trim();
        const currentBans = new Set((this.fleetConfig.banned_devices || []).map(d => String(d).trim()));
        const isBanned = currentBans.has(id);
        const action = explicitAction || (isBanned ? 'unban' : 'ban');
        const snapshot = this._createSnapshot();

        // Optimistic UI update
        if (action === 'ban') {
            currentBans.add(id);
        } else {
            currentBans.delete(id);
        }
        this.fleetConfig.banned_devices = Array.from(currentBans);
        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));

        this.populateGovernanceFields();
        this.renderActiveBanChips();
        this.computeKPIs();
        this.renderCurrentView();

        try {
            const res = await fetch(this.getApiUrl('/ban'), {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ device_id: id, action })
            });
            if (res.status === 401) {
                this._restoreSnapshot(snapshot);
                this.showToast('Unauthorized: Please unlock admin portal.', 'error');
                this.lockPortal();
                return;
            }
            if (res.ok) {
                const data = await res.json();
                if (Array.isArray(data.banned_devices)) {
                    this.fleetConfig.banned_devices = data.banned_devices;
                    localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
                    this.populateGovernanceFields();
                    this.renderActiveBanChips();
                    this.computeKPIs();
                    this.renderCurrentView();
                }
                this.showToast(action === 'ban' ? `Device ${id} revoked successfully` : `Device ${id} restored successfully`, action === 'ban' ? 'error' : 'success');
            } else {
                this._restoreSnapshot(snapshot);
                this.showToast('Action failed — restored previous state', 'error');
            }
        } catch (err) {
            console.warn('Network call to /ban failed:', err);
            this._restoreSnapshot(snapshot);
            this.showToast('Action failed — restored previous state', 'error');
        }
    }

    // Direct atomic student account ban/unban (Zero race condition)
    async toggleStudentBan(studentId, explicitAction = null) {
        if (!studentId) return;
        const id = parseInt(studentId);
        if (isNaN(id) || id <= 0) return;

        const currentBans = new Set((this.fleetConfig.banned_students || []).map(s => parseInt(s)).filter(Boolean));
        const isBanned = currentBans.has(id);
        const action = explicitAction || (isBanned ? 'unban' : 'ban');
        const snapshot = this._createSnapshot();

        // Optimistic UI update
        if (action === 'ban') {
            currentBans.add(id);
        } else {
            currentBans.delete(id);
        }
        this.fleetConfig.banned_students = Array.from(currentBans);
        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));

        this.populateGovernanceFields();
        this.renderActiveBanChips();
        this.computeKPIs();
        this.renderCurrentView();

        try {
            const res = await fetch(this.getApiUrl('/ban'), {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ student_id: id, action })
            });
            if (res.status === 401) {
                this._restoreSnapshot(snapshot);
                this.showToast('Unauthorized: Please unlock admin portal.', 'error');
                this.lockPortal();
                return;
            }
            if (res.ok) {
                const data = await res.json();
                if (Array.isArray(data.banned_students)) {
                    this.fleetConfig.banned_students = data.banned_students;
                    localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
                    this.populateGovernanceFields();
                    this.renderActiveBanChips();
                    this.computeKPIs();
                    this.renderCurrentView();
                }
                this.showToast(action === 'ban' ? `Student #${id} account banned` : `Student #${id} account restored`, action === 'ban' ? 'error' : 'success');
            } else {
                this._restoreSnapshot(snapshot);
                this.showToast('Action failed — restored previous state', 'error');
            }
        } catch (err) {
            console.warn('Network call to /ban failed:', err);
            this._restoreSnapshot(snapshot);
            this.showToast('Action failed — restored previous state', 'error');
        }
    }

    async executeQuickRestriction(action) {
        if (!this.quickBanInput) return;
        const val = (this.quickBanInput.value || '').trim();
        if (!val) {
            this.showToast('Please enter a Student ID or Device UUID', 'error');
            return;
        }
        const type = this.quickBanType ? this.quickBanType.value : 'DEVICE';
        if (type === 'STUDENT') {
            const parsed = parseInt(val);
            if (isNaN(parsed) || parsed <= 0) {
                this.showToast('Invalid Student ID (must be a positive number)', 'error');
                return;
            }
            await this.toggleStudentBan(parsed, action);
        } else {
            await this.toggleDeviceBan(val, action);
        }
        this.quickBanInput.value = '';
    }

    async sendTestPing() {
        const sampleId = 'dev-test-' + Math.random().toString(36).substring(2, 8);
        const sampleStudentId = 28000 + Math.floor(Math.random() * 900);

        const samplePayload = {
            device_id: sampleId,
            student_id: sampleStudentId,
            student_name: 'Fleet Verified Student',
            app_version: this.fleetConfig.latest_version_name || '1.0.15',
            version_code: this.fleetConfig.min_version_code || 15,
            os_version: 'Android 16 (API 36)',
            device_model: 'Google Pixel 9 Pro',
            is_test: true,
            timestamp: new Date().toISOString()
        };

        this.showToast('Dispatching test telemetry ping...', 'info');
        try {
            const res = await fetch(this.getApiUrl('/telemetry'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(samplePayload)
            });
            if (res.ok) {
                this.showToast(`Test device heartbeat verified: ${sampleId}`, 'success');
                await this.loadTelemetry();
            } else {
                this.showToast(`Server returned HTTP ${res.status}`, 'error');
            }
        } catch (err) {
            this.showToast(`Test ping failed: ${err.message}`, 'error');
        }
    }

    exportCsv() {
        if (this.filteredRecords.length === 0) {
            this.showToast('No records to export', 'info');
            return;
        }
        const headers = ['Student Name', 'Student ID', 'App Version', 'Version Code', 'Device Model', 'OS Version', 'Device UUID', 'Last Active Timestamp', 'Device Status', 'Account Status'];
        const bannedDevs = new Set(this.fleetConfig.banned_devices || []);
        const bannedStudents = new Set((this.fleetConfig.banned_students || []).map(s => String(s)));

        const rows = this.filteredRecords.map(r => {
            const devStatus = r.device_id && bannedDevs.has(r.device_id) ? 'REVOKED' : 'AUTHORIZED';
            const accStatus = r.student_id && bannedStudents.has(String(r.student_id)) ? 'BANNED' : 'AUTHORIZED';
            return [
                `"${(r.student_name || '').replace(/"/g, '""')}"`,
                r.student_id ?? '',
                `"${r.app_version || ''}"`,
                r.version_code ?? '',
                `"${(r.device_model || '').replace(/"/g, '""')}"`,
                `"${(r.os_version || '').replace(/"/g, '""')}"`,
                `"${r.device_id || ''}"`,
                `"${r.timestamp || r.server_received_at || ''}"`,
                devStatus,
                accStatus
            ].join(',');
        });

        const csvContent = [headers.join(','), ...rows].join('\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `lloyd_fleet_telemetry_${new Date().toISOString().slice(0, 10)}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showToast(`Exported ${this.filteredRecords.length} records to CSV`, 'success');
    }

    async fetchFromGitHub() {
        if (this.fetchGhBtn) {
            this.fetchGhBtn.textContent = 'Fetching...';
            this.fetchGhBtn.disabled = true;
        }
        try {
            const res = await fetch('https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest', {
                signal: AbortSignal.timeout(5000)
            });
            if (!res.ok) throw new Error(`GitHub returned HTTP ${res.status}`);
            const data = await res.json();
            const tag = (data.tag_name || data.name || '').replace(/^[vV]/, '').trim();
            const apkAsset = (data.assets || []).find(a => (a.name || '').toLowerCase().endsWith('.apk'));

            if (tag && this.cfgLatestVersion) {
                this.cfgLatestVersion.value = tag;
                this.fleetConfig.latest_version_name = tag;
            }
            if (apkAsset && this.cfgDownloadUrl) {
                this.cfgDownloadUrl.value = apkAsset.browser_download_url;
                this.fleetConfig.download_url = apkAsset.browser_download_url;
            }
            this.showToast(`Synced latest GitHub release: v${tag}`, 'success');
        } catch (err) {
            this.showToast(`GitHub sync failed: ${err.message}`, 'error');
        } finally {
            if (this.fetchGhBtn) {
                this.fetchGhBtn.innerHTML = `
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>
                    <span>Sync GitHub</span>
                `;
                this.fetchGhBtn.disabled = false;
            }
        }
    }

    async deleteTelemetry(deviceId) {
        if (!deviceId) return;
        if (!confirm(`Are you sure you want to remove device "${deviceId}" from telemetry?`)) return;

        const snapshot = this._createSnapshot();
        this.records = this.records.filter(r => r.device_id !== deviceId);
        this.computeKPIs();
        this.renderVersionBars();
        this.applyFilters();

        try {
            const res = await fetch(this.getApiUrl(`/telemetry?device_id=${encodeURIComponent(deviceId)}`), {
                method: 'DELETE',
                headers: this.getAuthHeaders(),
                signal: AbortSignal.timeout(5000)
            });
            if (res.status === 401) {
                this._restoreSnapshot(snapshot);
                this.showToast('Unauthorized: Please unlock admin portal.', 'error');
                this.lockPortal();
                return;
            }
            if (res.ok) {
                this.showToast(`Removed device record`, 'success');
            } else {
                this._restoreSnapshot(snapshot);
                this.showToast('Action failed — restored previous state', 'error');
            }
        } catch (e) {
            this._restoreSnapshot(snapshot);
            this.showToast('Action failed — restored previous state', 'error');
        }
    }

    async purgeTestRecords() {
        if (!confirm('Purge all simulated test device records from telemetry?')) return;

        const snapshot = this._createSnapshot();
        this.records = this.records.filter(r => !(
            r.is_test === true ||
            (r.device_id && (r.device_id.startsWith('dev-test-') || r.device_id.startsWith('dev-unit-test-') || r.device_id.includes('test')))
        ));
        this.computeKPIs();
        this.renderVersionBars();
        this.applyFilters();

        try {
            const res = await fetch(this.getApiUrl('/telemetry?purge=test'), {
                method: 'DELETE',
                headers: this.getAuthHeaders(),
                signal: AbortSignal.timeout(5000)
            });
            if (res.status === 401) {
                this._restoreSnapshot(snapshot);
                this.showToast('Unauthorized: Please unlock admin portal.', 'error');
                this.lockPortal();
                return;
            }
            if (res.ok) {
                const data = await res.json();
                this.showToast(`Purged ${data.purged_count || 0} test records`, 'success');
            } else {
                this._restoreSnapshot(snapshot);
                this.showToast('Action failed — restored previous state', 'error');
            }
        } catch (e) {
            this._restoreSnapshot(snapshot);
            this.showToast('Action failed — restored previous state', 'error');
        }
    }

    setSort(column) {
        if (this.sortColumn === column) {
            this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortColumn = column;
            this.sortDirection = (column === 'timestamp') ? 'desc' : 'asc';
        }
        this.updateSortIcons();
        this.applyFilters();
    }

    updateSortIcons() {
        ['student_name', 'student_id', 'app_version', 'timestamp'].forEach(col => {
            const iconEl = document.getElementById(`sort-${col}`);
            if (iconEl) {
                if (this.sortColumn === col) {
                    iconEl.textContent = this.sortDirection === 'asc' ? '▲' : '▼';
                    iconEl.style.color = 'var(--brand-honey-bronze)';
                } else {
                    iconEl.textContent = '↕';
                    iconEl.style.color = '';
                }
            }
        });
    }

    bindEvents() {
        if (this.lockPortalBtn) {
            this.lockPortalBtn.addEventListener('click', () => this.lockPortal());
        }

        if (this.adminAuthForm) {
            this.adminAuthForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleAuthSubmit();
            });
        }

        if (this.togglePassVisibility && this.adminPassInput) {
            this.togglePassVisibility.addEventListener('click', () => {
                const isPassword = this.adminPassInput.type === 'password';
                this.adminPassInput.type = isPassword ? 'text' : 'password';
                if (this.eyeIcon) {
                    this.eyeIcon.innerHTML = isPassword ? ICON_EYE_OFF : ICON_EYE;
                }
                this.togglePassVisibility.setAttribute('aria-label', isPassword ? 'Hide passkey' : 'Show passkey');
            });
        }

        // Mobile drawer navigation listeners
        if (this.mobileMenuToggle) {
            this.mobileMenuToggle.addEventListener('click', () => this.openSidebar());
        }
        if (this.closeSidebarBtn) {
            this.closeSidebarBtn.addEventListener('click', () => this.closeSidebar());
        }
        if (this.sidebarOverlay) {
            this.sidebarOverlay.addEventListener('click', () => this.closeSidebar());
        }
        if (this.mobileRefreshBtn) {
            this.mobileRefreshBtn.addEventListener('click', () => {
                const icon = this.mobileRefreshBtn.querySelector('.refresh-icon');
                if (icon) icon.classList.add('spinning');
                this.loadTelemetry().finally(() => {
                    setTimeout(() => {
                        if (icon) icon.classList.remove('spinning');
                    }, 600);
                });
            });
        }
        if (this.mobileLockBtn) {
            this.mobileLockBtn.addEventListener('click', () => this.lockPortal());
        }

        // OTA pause controls
        if (this.resumeOtaBtn) {
            this.resumeOtaBtn.addEventListener('click', () => this.togglePauseUpdates(false));
        }
        if (this.cfgPauseUpdates) {
            this.cfgPauseUpdates.addEventListener('change', () => this.togglePauseUpdates(this.cfgPauseUpdates.checked));
        }

        // View Mode Switcher (Table vs Mobile Cards)
        if (this.tableViewBtn) {
            this.tableViewBtn.addEventListener('click', () => this.setViewMode('table'));
        }
        if (this.cardViewBtn) {
            this.cardViewBtn.addEventListener('click', () => this.setViewMode('card'));
        }

        if (this.switchServerBtn) {
            this.switchServerBtn.addEventListener('click', () => this.switchServer());
        }

        if (this.deactivateMaintBtn) {
            this.deactivateMaintBtn.addEventListener('click', () => this.deactivateMaintenance());
        }

        if (this.quickBroadcastBtn) {
            this.quickBroadcastBtn.addEventListener('click', () => this.pushBroadcast());
        }
        if (this.clearBroadcastBtn) {
            this.clearBroadcastBtn.addEventListener('click', () => this.clearBroadcast());
        }
        if (this.quickBroadcastInput) {
            this.quickBroadcastInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') this.pushBroadcast();
            });
        }

        if (this.configBtn) {
            this.configBtn.addEventListener('click', () => {
                const current = this.endpointUrl || 'http://localhost:8080';
                const entered = prompt('Telemetry Server URL (Default: https://lloyd-erp-sand.vercel.app or http://localhost:8080):', current);
                if (entered !== null) {
                    const cleaned = entered.trim();
                    if (cleaned) {
                        localStorage.setItem('telemetry_api_url', cleaned);
                        this.endpointUrl = cleaned;
                    } else {
                        localStorage.removeItem('telemetry_api_url');
                        const isHttp = typeof window !== 'undefined' && window.location.protocol.startsWith('http');
                        this.endpointUrl = isHttp ? window.location.origin : 'http://localhost:8080';
                    }
                    this.updateServerLabel();
                    this.loadTelemetry();
                }
            });
        }

        if (this.saveConfigBtn) {
            this.saveConfigBtn.addEventListener('click', () => this.saveConfig());
        }
        if (this.downloadConfigBtn) {
            this.downloadConfigBtn.addEventListener('click', () => this.downloadConfig());
        }
        if (this.copyConfigBtn) {
            this.copyConfigBtn.addEventListener('click', () => this.copyConfig());
        }
        if (this.pingBtn) {
            this.pingBtn.addEventListener('click', () => this.sendTestPing());
        }
        if (this.exportCsvBtn) {
            this.exportCsvBtn.addEventListener('click', () => this.exportCsv());
        }
        if (this.purgeTestsBtn) {
            this.purgeTestsBtn.addEventListener('click', () => this.purgeTestRecords());
        }
        if (this.fetchGhBtn) {
            this.fetchGhBtn.addEventListener('click', () => this.fetchFromGitHub());
        }

        // Table column sorting listeners
        document.querySelectorAll('.sortable-th').forEach(th => {
            th.addEventListener('click', () => {
                const col = th.getAttribute('data-sort');
                if (col) this.setSort(col);
            });
        });

        // Dual quick restriction actions
        if (this.quickBanBtn) {
            this.quickBanBtn.addEventListener('click', () => this.executeQuickRestriction('ban'));
        }
        if (this.quickUnbanBtn) {
            this.quickUnbanBtn.addEventListener('click', () => this.executeQuickRestriction('unban'));
        }
        if (this.quickBanInput) {
            this.quickBanInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') this.executeQuickRestriction('ban');
            });
        }

        if (this.refreshBtn) {
            this.refreshBtn.addEventListener('click', () => {
                const icon = this.refreshBtn.querySelector('.refresh-icon');
                if (icon) icon.classList.add('spinning');
                this.loadTelemetry().finally(() => {
                    setTimeout(() => {
                        if (icon) icon.classList.remove('spinning');
                    }, 600);
                });
            });
        }

        // Debounced search for smooth 60fps typing without layout jitter
        if (this.searchInput) {
            this.searchInput.addEventListener('input', () => {
                clearTimeout(this.searchDebounceTimer);
                this.searchDebounceTimer = setTimeout(() => this.applyFilters(), 150);
            });
        }
        if (this.statusFilter) {
            this.statusFilter.addEventListener('change', () => this.applyFilters());
        }
        if (this.versionFilter) {
            this.versionFilter.addEventListener('change', () => this.applyFilters());
        }

        window.addEventListener('resize', () => {
            if (window.innerWidth >= 1024) {
                this.closeSidebar();
            }
        });

        if (typeof document !== 'undefined') {
            document.addEventListener('visibilitychange', () => {
                if (document.visibilityState === 'hidden') {
                    this.stopPolling();
                    this.stopSyncAgeTimer();
                } else if (this.getStoredPass()) {
                    this.loadTelemetry();
                    this.startPolling();
                    this.startSyncAgeTimer();
                }
            });
        }
    }

    startPolling() {
        if (this.pollTimer) return;
        if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return;
        this.pollTimer = setInterval(() => this.loadTelemetry(), 12000);
    }

    stopPolling() {
        if (this.pollTimer) {
            clearInterval(this.pollTimer);
            this.pollTimer = null;
        }
    }

    startSyncAgeTimer() {
        if (this.syncAgeTimer) return;
        if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return;
        this.syncAgeTimer = setInterval(() => this.updateSyncStatusText(), 1000);
    }

    stopSyncAgeTimer() {
        if (this.syncAgeTimer) {
            clearInterval(this.syncAgeTimer);
            this.syncAgeTimer = null;
        }
    }

    updateSyncStatusText() {
        if (!this.syncStatusEl || !this.baseSyncStatusText) return;
        if (this.lastSyncAt) {
            const secs = Math.max(0, Math.floor((Date.now() - this.lastSyncAt) / 1000));
            this.syncStatusEl.textContent = `${this.baseSyncStatusText} · Updated ${secs} seconds ago`;
        } else {
            this.syncStatusEl.textContent = this.baseSyncStatusText;
        }
    }

    async loadTelemetry() {
        if (this.isFetching) return;
        this.isFetching = true;

        const base = (this.endpointUrl || 'http://localhost:8080').replace(/\/+$/, '');

        // Candidate URLs to auto-connect (supports direct file:// browsing and web deployments)
        const candidates = [];
        if (typeof window !== 'undefined' && window.location.origin && window.location.origin.startsWith('http')) {
            candidates.push(window.location.origin);
        }
        if (base && !candidates.includes(base)) {
            candidates.push(base);
        }
        if (!candidates.includes('https://lloyd-erp-sand.vercel.app')) {
            candidates.push('https://lloyd-erp-sand.vercel.app');
        }
        if (!candidates.includes('http://localhost:8080')) {
            candidates.push('http://localhost:8080');
        }

        let connectedUrl = null;
        let configOk = false;
        let telemetryOk = false;
        let isAuthError = false;

        for (const candidate of candidates) {
            let candConfigOk = false;
            let candTelemetryOk = false;

            // Synchronize remote admin config (includes banned_devices / banned_students)
            try {
                const cfgUrl = candidate + (candidate.includes('/api') ? '/config/admin' : '/api/config/admin');
                const cfgRes = await fetch(cfgUrl, {
                    headers: this.getAuthHeaders(),
                    signal: AbortSignal.timeout(6000)
                });
                if (cfgRes.status === 401) {
                    connectedUrl = candidate;
                    isAuthError = true;
                    break;
                }
                if (cfgRes.ok) {
                    const remoteCfg = await cfgRes.json();
                    this.fleetConfig = { ...this.fleetConfig, ...remoteCfg };
                    localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
                    this.populateGovernanceFields();
                    this.renderMaintenanceBanner();
                    this.renderOtaPausedBanner();
                    this.renderActiveBanChips();
                    candConfigOk = true;
                }
            } catch (e) { }

            // Fetch telemetry heartbeats
            try {
                const telUrl = candidate + (candidate.includes('/api') ? '/telemetry' : '/api/telemetry');
                const res = await fetch(telUrl, {
                    headers: this.getAuthHeaders(),
                    signal: AbortSignal.timeout(6000)
                });
                if (res.status === 401) {
                    connectedUrl = candidate;
                    isAuthError = true;
                    break;
                }
                if (res.ok) {
                    this.records = await res.json();
                    candTelemetryOk = true;
                }
            } catch (err) { }

            if (candConfigOk || candTelemetryOk) {
                connectedUrl = candidate;
                configOk = candConfigOk;
                telemetryOk = candTelemetryOk;
                break;
            }
        }

        this.isFetching = false;

        if (isAuthError) {
            this.clearStoredPass();
            this.stopPolling();
            this.stopSyncAgeTimer();
            this.baseSyncStatusText = '';
            this.lastSyncAt = null;
            this.showAuthOverlay();
            this.showAuthError('Session expired. Please re-enter your admin passkey.');
            if (this.syncStatusEl) {
                this.syncStatusEl.textContent = 'Auth Required';
                this.syncStatusEl.style.color = '#F6BD60';
            }
            if (this.liveDotEl) {
                this.liveDotEl.style.backgroundColor = '#F6BD60';
                this.liveDotEl.style.boxShadow = '0 0 8px #F6BD60';
            }
            this.syncMobileLiveDot();
            return;
        }

        if (connectedUrl && configOk && telemetryOk) {
            this.endpointUrl = connectedUrl;
            this.updateServerLabel();
            this.lastSyncAt = Date.now();
            this.baseSyncStatusText = `Connected (${connectedUrl.replace(/^https?:\/\//, '')})`;
            this.updateSyncStatusText();
            this.startSyncAgeTimer();
            if (this.syncStatusEl) {
                this.syncStatusEl.style.color = '#84A59D';
            }
            if (this.liveDotEl) {
                this.liveDotEl.style.backgroundColor = '#84A59D';
                this.liveDotEl.style.boxShadow = '0 0 8px #84A59D';
            }
        } else if (connectedUrl && configOk && !telemetryOk) {
            this.endpointUrl = connectedUrl;
            this.updateServerLabel();
            this.baseSyncStatusText = 'Config OK · Telemetry unavailable';
            this.updateSyncStatusText();
            if (this.syncStatusEl) {
                this.syncStatusEl.style.color = '#F6BD60';
            }
            if (this.liveDotEl) {
                this.liveDotEl.style.backgroundColor = '#F6BD60';
                this.liveDotEl.style.boxShadow = '0 0 8px #F6BD60';
            }
        } else if (connectedUrl && !configOk && telemetryOk) {
            this.endpointUrl = connectedUrl;
            this.updateServerLabel();
            this.lastSyncAt = Date.now();
            this.baseSyncStatusText = 'Telemetry OK · Config unavailable';
            this.updateSyncStatusText();
            this.startSyncAgeTimer();
            if (this.syncStatusEl) {
                this.syncStatusEl.style.color = '#F6BD60';
            }
            if (this.liveDotEl) {
                this.liveDotEl.style.backgroundColor = '#F6BD60';
                this.liveDotEl.style.boxShadow = '0 0 8px #F6BD60';
            }
        } else {
            this.baseSyncStatusText = '';
            this.stopSyncAgeTimer();
            if (this.syncStatusEl) {
                const isCloud = typeof window !== 'undefined' && window.location.protocol === 'https:';
                this.syncStatusEl.textContent = isCloud ? 'Connecting to Fleet Server...' : 'Server Offline (Start server.js)';
                this.syncStatusEl.style.color = '#F28482';
            }
            if (this.liveDotEl) {
                this.liveDotEl.style.backgroundColor = '#F28482';
                this.liveDotEl.style.boxShadow = '0 0 8px #F28482';
            }
        }
        this.syncMobileLiveDot();

        this.populateVersionFilter();
        this.computeKPIs();
        this.renderVersionBars();
        this.renderMaintenanceBanner();
        this.renderOtaPausedBanner();
        this.renderActiveBanChips();
        this.applyFilters();

        // Setup background polling if visible and not already started
        this.startPolling();
    }

    populateVersionFilter() {
        if (!this.versionFilter) return;
        const uniqueVersions = [...new Set(this.records.map(r => r.app_version).filter(Boolean))].sort(compareSemverDesc);
        const currentVal = this.versionFilter.value;
        this.versionFilter.innerHTML = '<option value="ALL">All Versions</option>';
        uniqueVersions.forEach(ver => {
            const opt = document.createElement('option');
            opt.value = ver;
            opt.textContent = ver;
            this.versionFilter.appendChild(opt);
        });
        if (uniqueVersions.includes(currentVal)) {
            this.versionFilter.value = currentVal;
        }
    }

    computeKPIs() {
        const prodRecords = this.records.filter(r => r && r.is_test !== true);
        const total = prodRecords.length;
        if (this.totalUsersEl) this.totalUsersEl.textContent = total.toLocaleString();

        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        const activeToday = prodRecords.filter(r => {
            const t = r.timestamp || r.server_received_at;
            return t && new Date(t).getTime() >= oneDayAgo;
        }).length;
        if (this.activeTodayEl) this.activeTodayEl.textContent = activeToday.toLocaleString();

        // Calculate latest version and adoption rate purely from live production telemetry
        const versionCounts = {};
        prodRecords.forEach(r => {
            if (r.app_version) {
                versionCounts[r.app_version] = (versionCounts[r.app_version] || 0) + 1;
            }
        });

        const sortedVersions = Object.keys(versionCounts).sort(compareSemverDesc);
        const latestVer = sortedVersions[0] || this.fleetConfig.latest_version_name || '--';
        const latestCount = versionCounts[latestVer] || 0;
        const adoptionPercent = total > 0 && latestVer !== '--' ? Math.round((latestCount / total) * 100) : 0;

        if (this.latestVersionEl) this.latestVersionEl.textContent = latestVer;
        if (this.adoptionRateEl) {
            this.adoptionRateEl.textContent = total > 0 ? `${adoptionPercent}% of fleet running ${latestVer}` : 'No active fleet telemetry yet';
        }

        // Modern OS share (API 34+)
        const modernCount = prodRecords.filter(r => {
            const os = r.os_version || '';
            return os.includes('API 34') || os.includes('API 35') || os.includes('API 36') ||
                os.includes('Android 14') || os.includes('Android 15') || os.includes('Android 16');
        }).length;
        const modernPercent = total > 0 ? Math.round((modernCount / total) * 100) : 0;
        if (this.osDominanceEl) this.osDominanceEl.textContent = total > 0 ? `${modernPercent}%` : '--';

        // Restricted count
        const bannedDevCount = (this.fleetConfig.banned_devices || []).length;
        const bannedStudentCount = (this.fleetConfig.banned_students || []).length;
        const totalRestrictions = bannedDevCount + bannedStudentCount;
        if (this.restrictedCountEl) {
            this.restrictedCountEl.textContent = totalRestrictions.toString();
        }
        if (this.restrictedSubEl) {
            this.restrictedSubEl.textContent = `${bannedDevCount} devices • ${bannedStudentCount} accounts`;
        }
    }

    renderVersionBars() {
        if (!this.versionBarsEl) return;
        const prodRecords = this.records.filter(r => r && r.is_test !== true);
        const versionCounts = {};
        prodRecords.forEach(r => {
            if (r.app_version) {
                versionCounts[r.app_version] = (versionCounts[r.app_version] || 0) + 1;
            }
        });

        const sortedKeys = Object.keys(versionCounts).sort(compareSemverDesc);
        const sorted = sortedKeys.map(ver => [ver, versionCounts[ver]]);
        const total = prodRecords.length || 0;

        this.versionBarsEl.innerHTML = '';
        if (sorted.length === 0) {
            this.versionBarsEl.innerHTML = `
                <div style="color: var(--text-secondary); font-size: 13px; text-align: center; padding: 24px 0;">
                    No version adoption telemetry records yet.
                </div>
            `;
            return;
        }

        sorted.forEach(([ver, count]) => {
            const pct = total > 0 ? Math.round((count / total) * 100) : 0;
            const item = document.createElement('div');
            item.className = 'version-bar-item';
            item.innerHTML = `
                <div class="version-bar-labels">
                    <strong>${escapeHtml(ver)}</strong>
                    <span>${count} devices (${pct}%)</span>
                </div>
                <div class="bar-track">
                    <div class="bar-fill" style="width: ${pct}%"></div>
                </div>
            `;
            this.versionBarsEl.appendChild(item);
        });
    }

    applyFilters() {
        const query = (this.searchInput.value || '').trim().toLowerCase();
        const verFilter = this.versionFilter ? this.versionFilter.value : 'ALL';
        const statFilter = this.statusFilter ? this.statusFilter.value : 'ALL';
        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        const bannedDevSet = new Set((this.fleetConfig.banned_devices || []).map(d => String(d).trim()));
        const bannedStudentSet = new Set((this.fleetConfig.banned_students || []).map(s => String(s).trim()));
        const minVerCode = parseInt(this.fleetConfig.min_version_code) || 1;

        this.filteredRecords = this.records.filter(r => {
            const matchesVer = verFilter === 'ALL' || r.app_version === verFilter;
            const matchesQuery = !query ||
                (r.student_name && r.student_name.toLowerCase().includes(query)) ||
                (r.student_id && r.student_id.toString().includes(query)) ||
                (r.device_id && r.device_id.toLowerCase().includes(query)) ||
                (r.device_model && r.device_model.toLowerCase().includes(query)) ||
                (r.app_version && r.app_version.toLowerCase().includes(query));

            const isDevBanned = r.device_id && bannedDevSet.has(String(r.device_id).trim());
            const isStudentBanned = r.student_id && bannedStudentSet.has(String(r.student_id).trim());
            const isOnline = (r.timestamp || r.server_received_at) && (new Date(r.timestamp || r.server_received_at).getTime() >= oneDayAgo);
            const isOutdated = r.version_code && (parseInt(r.version_code) < minVerCode);

            let matchesStatus = true;
            if (statFilter === 'ACTIVE') matchesStatus = isOnline && !isDevBanned && !isStudentBanned;
            else if (statFilter === 'IDLE') matchesStatus = !isOnline && !isDevBanned && !isStudentBanned;
            else if (statFilter === 'REVOKED_DEVICE') matchesStatus = isDevBanned;
            else if (statFilter === 'BANNED_STUDENT') matchesStatus = isStudentBanned;
            else if (statFilter === 'OUTDATED') matchesStatus = isOutdated;

            return matchesVer && matchesQuery && matchesStatus;
        });

        // Apply interactive column sorting
        this.filteredRecords.sort((a, b) => {
            if (this.sortColumn === 'student_name') {
                const valA = (a.student_name || '').toLowerCase();
                const valB = (b.student_name || '').toLowerCase();
                return this.sortDirection === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA);
            } else if (this.sortColumn === 'student_id') {
                const valA = parseInt(a.student_id) || 0;
                const valB = parseInt(b.student_id) || 0;
                return this.sortDirection === 'asc' ? valA - valB : valB - valA;
            } else if (this.sortColumn === 'app_version') {
                const cmp = compareSemverDesc(a.app_version, b.app_version);
                return this.sortDirection === 'asc' ? -cmp : cmp;
            } else { // timestamp
                const valA = (a.timestamp || a.server_received_at) ? new Date(a.timestamp || a.server_received_at).getTime() : 0;
                const valB = (b.timestamp || b.server_received_at) ? new Date(b.timestamp || b.server_received_at).getTime() : 0;
                return this.sortDirection === 'asc' ? valA - valB : valB - valA;
            }
        });

        this.renderCurrentView();
    }

    renderTable() {
        if (!this.tableBodyEl) return;
        if (this.tableCountEl) {
            this.tableCountEl.textContent = `(${this.filteredRecords.length} records)`;
        }

        if (this.records.length === 0) {
            this.tableBodyEl.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 48px 16px; color: var(--text-secondary);">
                        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--brand-honey-bronze)" stroke-width="1.8" style="margin-bottom: 8px;"><path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"/><path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"/><circle cx="12" cy="12" r="2"/><path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"/><path d="M19.1 4.9C23 8.8 23 15.2 19.1 19.1"/></svg>
                        <strong style="display: block; font-size: 15px; color: var(--text-primary); margin-bottom: 6px;">No Active Fleet Devices Registered Yet</strong>
                        <span style="font-size: 13px;">Open the Lloyd ERP Android app or click "Test Ping" above to dispatch telemetry into the live fleet stream.</span>
                    </td>
                </tr>
            `;
            return;
        }

        if (this.filteredRecords.length === 0) {
            this.tableBodyEl.innerHTML = `
                <tr>
                    <td colspan="8" class="loading-cell">No student records match the search filter.</td>
                </tr>
            `;
            return;
        }

        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        const bannedDevSet = new Set((this.fleetConfig.banned_devices || []).map(d => String(d).trim()));
        const bannedStudentSet = new Set((this.fleetConfig.banned_students || []).map(s => String(s).trim()));
        const minVerCode = parseInt(this.fleetConfig.min_version_code) || 1;

        this.tableBodyEl.innerHTML = this.filteredRecords.map(r => {
            const lastActiveTime = (r.timestamp || r.server_received_at) ? new Date(r.timestamp || r.server_received_at).getTime() : 0;
            const isOnline = lastActiveTime >= oneDayAgo;
            const timeAgoStr = lastActiveTime > 0 ? this.formatTimeAgo(lastActiveTime) : 'Never';
            const fullDateStr = lastActiveTime > 0 ? new Date(lastActiveTime).toLocaleString() : 'N/A';

            const isDevBanned = r.device_id && bannedDevSet.has(String(r.device_id).trim());
            const isStudentBanned = r.student_id && bannedStudentSet.has(String(r.student_id).trim());
            const isOutdated = r.version_code && (parseInt(r.version_code) < minVerCode);

            const studentName = escapeHtml(r.student_name || 'Student');
            const initials = r.student_name
                ? r.student_name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
                : 'ST';

            const deviceId = String(r.device_id || '').trim();
            const shortId = deviceId.length > 20 ? deviceId.substring(0, 16) + '...' : deviceId;
            const studentId = r.student_id ? parseInt(r.student_id) : null;

            return `
                <tr>
                    <td>
                        <div class="student-col">
                            <div class="avatar-circle">${escapeHtml(initials)}</div>
                            <div>
                                <strong>${studentName}</strong>
                                <small style="display:block; color:var(--text-secondary); font-size:11px;">
                                    UUID: <span style="font-family:'JetBrains Mono',monospace;">${escapeHtml(shortId || '--')}</span>
                                    ${deviceId ? `<button class="copy-inline" title="Copy Full UUID" aria-label="Copy Full UUID" onclick="window.telemetryDashboard.copyToClipboard('${safeAttr(deviceId)}', 'Device UUID')">${ICON_COPY}</button>` : ''}
                                </small>
                            </div>
                        </div>
                    </td>
                    <td>
                        <code>${studentId !== null ? studentId : '--'}</code>
                        ${studentId !== null ? `<button class="copy-inline" title="Copy Student ID" aria-label="Copy Student ID" onclick="window.telemetryDashboard.copyToClipboard('${studentId}', 'Student ID')">${ICON_COPY}</button>` : ''}
                    </td>
                    <td>
                        <span class="version-pill">${escapeHtml(r.app_version || '--')}</span>
                        ${isOutdated ? '<span class="status-badge status-outdated" style="margin-left:4px;" title="Below minimum allowed version">Outdated</span>' : ''}
                    </td>
                    <td>${escapeHtml(r.device_model || 'Unknown')}</td>
                    <td><small style="color: var(--text-secondary)">${escapeHtml(r.os_version || 'Android')}</small></td>
                    <td title="${escapeHtml(fullDateStr)}">${escapeHtml(timeAgoStr)}</td>
                    <td>
                        <div class="badges-wrap">
                            ${isDevBanned
                    ? '<span class="status-badge status-revoked">● Device Revoked</span>'
                    : `<span class="status-badge ${isOnline ? 'status-online' : 'status-offline'}">● ${isOnline ? 'Active' : 'Idle'}</span>`
                }
                            ${isStudentBanned ? '<span class="status-badge status-banned">● Account Banned</span>' : ''}
                        </div>
                    </td>
                    <td>
                        <div class="actions-cluster">
                            ${deviceId ? (
                    isDevBanned
                        ? `<button class="btn-action btn-restore" onclick="window.telemetryDashboard.toggleDeviceBan('${safeAttr(deviceId)}', 'unban')">Restore Device</button>`
                        : `<button class="btn-action btn-revoke" onclick="window.telemetryDashboard.toggleDeviceBan('${safeAttr(deviceId)}', 'ban')">Revoke Device</button>`
                ) : ''}
                            ${studentId ? (
                    isStudentBanned
                        ? `<button class="btn-action btn-unban-student" onclick="window.telemetryDashboard.toggleStudentBan(${studentId}, 'unban')">Unban Account</button>`
                        : `<button class="btn-action btn-ban-student" onclick="window.telemetryDashboard.toggleStudentBan(${studentId}, 'ban')">Ban Account</button>`
                ) : ''}
                            ${deviceId ? `<button class="btn-action btn-delete-row" title="Delete record from telemetry" aria-label="Delete record" onclick="window.telemetryDashboard.deleteTelemetry('${safeAttr(deviceId)}')">${ICON_TRASH}</button>` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    renderCards() {
        if (!this.studentCardGrid) return;

        if (this.records.length === 0) {
            this.studentCardGrid.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 48px 16px; color: var(--text-secondary); background: var(--bg-surface-elevated); border-radius: var(--radius-lg); border: 1px dashed var(--border-color);">
                    <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--brand-honey-bronze)" stroke-width="1.8" style="margin-bottom: 8px;"><path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"/><path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"/><circle cx="12" cy="12" r="2"/><path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"/><path d="M19.1 4.9C23 8.8 23 15.2 19.1 19.1"/></svg>
                    <strong style="display: block; font-size: 15px; color: var(--text-primary); margin-bottom: 6px;">No Active Fleet Devices Registered Yet</strong>
                    <span style="font-size: 13px;">Open the Lloyd ERP Android app or click "Test Ping" above to dispatch telemetry into the live fleet stream.</span>
                </div>
            `;
            return;
        }

        if (this.filteredRecords.length === 0) {
            this.studentCardGrid.innerHTML = `
                <div style="grid-column: 1 / -1; text-align: center; padding: 36px 16px; color: var(--text-secondary); background: var(--bg-surface-elevated); border-radius: var(--radius-lg);">
                    No student records match the search filter.
                </div>
            `;
            return;
        }

        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        const bannedDevSet = new Set((this.fleetConfig.banned_devices || []).map(d => String(d).trim()));
        const bannedStudentSet = new Set((this.fleetConfig.banned_students || []).map(s => String(s).trim()));
        const minVerCode = parseInt(this.fleetConfig.min_version_code) || 1;

        this.studentCardGrid.innerHTML = this.filteredRecords.map(r => {
            const lastActiveTime = (r.timestamp || r.server_received_at) ? new Date(r.timestamp || r.server_received_at).getTime() : 0;
            const isOnline = lastActiveTime >= oneDayAgo;
            const timeAgoStr = lastActiveTime > 0 ? this.formatTimeAgo(lastActiveTime) : 'Never';
            const fullDateStr = lastActiveTime > 0 ? new Date(lastActiveTime).toLocaleString() : 'N/A';

            const isDevBanned = r.device_id && bannedDevSet.has(String(r.device_id).trim());
            const isStudentBanned = r.student_id && bannedStudentSet.has(String(r.student_id).trim());
            const isOutdated = r.version_code && (parseInt(r.version_code) < minVerCode);

            const studentName = escapeHtml(r.student_name || 'Student');
            const initials = r.student_name
                ? r.student_name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
                : 'ST';

            const deviceId = String(r.device_id || '').trim();
            const shortId = deviceId.length > 22 ? deviceId.substring(0, 14) + '...' + deviceId.slice(-6) : deviceId;
            const studentId = r.student_id ? parseInt(r.student_id) : null;

            return `
                <div class="student-card">
                    <div class="student-card-header">
                        <div class="student-card-profile">
                            <div class="avatar-circle">${escapeHtml(initials)}</div>
                            <div class="student-card-name-wrap">
                                <div class="student-card-name" title="${studentName}">${studentName}</div>
                                <div class="student-card-id">
                                    <span>#${studentId !== null ? studentId : '--'}</span>
                                    ${studentId !== null ? `<button class="copy-inline" title="Copy Student ID" aria-label="Copy Student ID" onclick="window.telemetryDashboard.copyToClipboard('${studentId}', 'Student ID')">${ICON_COPY}</button>` : ''}
                                </div>
                            </div>
                        </div>
                        <div class="student-card-badges">
                            ${isDevBanned
                    ? '<span class="status-badge status-revoked">● Device Revoked</span>'
                    : `<span class="status-badge ${isOnline ? 'status-online' : 'status-offline'}">● ${isOnline ? 'Active' : 'Idle'}</span>`
                }
                            ${isStudentBanned ? '<span class="status-badge status-banned">● Account Banned</span>' : ''}
                        </div>
                    </div>

                    <div class="student-card-meta">
                        <div class="meta-item">
                            <span class="meta-label">App Version</span>
                            <span class="meta-val">
                                <span class="version-pill">${escapeHtml(r.app_version || '--')}</span>
                                ${isOutdated ? '<span class="status-badge status-outdated" style="margin-left:4px;" title="Below minimum allowed version">Outdated</span>' : ''}
                            </span>
                        </div>
                        <div class="meta-item">
                            <span class="meta-label">Last Active</span>
                            <span class="meta-val" title="${escapeHtml(fullDateStr)}">${escapeHtml(timeAgoStr)}</span>
                        </div>
                        <div class="meta-item">
                            <span class="meta-label">Device</span>
                            <span class="meta-val">${escapeHtml(r.device_model || 'Unknown')}</span>
                        </div>
                        <div class="meta-item">
                            <span class="meta-label">OS Version</span>
                            <span class="meta-val">${escapeHtml(r.os_version || 'Android')}</span>
                        </div>
                        <div class="meta-item meta-uuid">
                            <div style="min-width:0; overflow:hidden; text-overflow:ellipsis;">
                                <span class="meta-label" style="display:block;">Device UUID</span>
                                <span style="color:var(--text-secondary);">${escapeHtml(shortId || '--')}</span>
                            </div>
                            ${deviceId ? `<button class="copy-inline" title="Copy Full UUID" aria-label="Copy Full UUID" onclick="window.telemetryDashboard.copyToClipboard('${safeAttr(deviceId)}', 'Device UUID')">${ICON_COPY}</button>` : ''}
                        </div>
                    </div>

                    <div class="student-card-actions">
                        ${deviceId ? (
                    isDevBanned
                        ? `<button class="btn-action btn-restore" onclick="window.telemetryDashboard.toggleDeviceBan('${safeAttr(deviceId)}', 'unban')">Restore Device</button>`
                        : `<button class="btn-action btn-revoke" onclick="window.telemetryDashboard.toggleDeviceBan('${safeAttr(deviceId)}', 'ban')">Revoke Device</button>`
                ) : ''}
                        ${studentId ? (
                    isStudentBanned
                        ? `<button class="btn-action btn-unban-student" onclick="window.telemetryDashboard.toggleStudentBan(${studentId}, 'unban')">Unban Account</button>`
                        : `<button class="btn-action btn-ban-student" onclick="window.telemetryDashboard.toggleStudentBan(${studentId}, 'ban')">Ban Account</button>`
                ) : ''}
                        ${deviceId ? `<button class="btn-action btn-delete-row" title="Delete record from telemetry" aria-label="Delete record" onclick="window.telemetryDashboard.deleteTelemetry('${safeAttr(deviceId)}')">${ICON_TRASH}</button>` : ''}
                    </div>
                </div>
            `;
        }).join('');
    }

    formatTimeAgo(timestamp) {
        const diffMs = Date.now() - timestamp;
        const diffMins = Math.floor(diffMs / (60 * 1000));
        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins}m ago`;
        const diffHours = Math.floor(diffMins / 60);
        if (diffHours < 24) return `${diffHours}h ago`;
        const diffDays = Math.floor(diffHours / 24);
        return `${diffDays}d ago`;
    }
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    window.telemetryDashboard = new TelemetryDashboard();
    window.feedbackPanel = new FeedbackPanel(window.telemetryDashboard);
    initSectionNav();
});

// ─── Section Navigation ───────────────────────────────────────────────────────

function initSectionNav() {
    const navLinks = document.querySelectorAll('.nav-menu .nav-item');

    function setActiveLink(targetId) {
        navLinks.forEach(link => {
            const href = (link.getAttribute('href') || '').replace('#', '');
            link.classList.toggle('active', href === targetId);
        });
    }

    function navigateTo(targetId, updateHistory = true) {
        if (!targetId || targetId === 'overview') {
            document.body.classList.remove('view-feedback');
            setActiveLink('overview');
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } else if (targetId === 'feedback') {
            document.body.classList.add('view-feedback');
            setActiveLink('feedback');
            window.scrollTo({ top: 0, behavior: 'smooth' });
            if (window.feedbackPanel) {
                window.feedbackPanel.loadIfNeeded();
            }
        } else if (['users', 'governance', 'releases'].includes(targetId)) {
            document.body.classList.remove('view-feedback');
            setActiveLink(targetId);
            const targetEl = document.getElementById(targetId);
            if (targetEl) {
                targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }

        if (updateHistory && window.history && window.history.pushState) {
            window.history.pushState(null, '', `#${targetId}`);
        }
    }

    navLinks.forEach(link => {
        link.addEventListener('click', e => {
            const href = (link.getAttribute('href') || '').replace('#', '');
            if (href) {
                e.preventDefault();
                navigateTo(href);
                // Close sidebar on mobile
                const sidebar = document.getElementById('sidebar');
                if (sidebar) sidebar.classList.remove('open');
                const overlay = document.getElementById('sidebarOverlay');
                if (overlay) overlay.style.display = 'none';
            }
        });
    });

    // Update active nav link on scroll when on dashboard view
    const observerSections = ['releases', 'governance', 'users'];
    if (typeof IntersectionObserver !== 'undefined') {
        const scrollObserver = new IntersectionObserver((entries) => {
            if (document.body.classList.contains('view-feedback')) return;
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    setActiveLink(entry.target.id);
                }
            });
        }, { rootMargin: '-20% 0px -70% 0px' });

        observerSections.forEach(id => {
            const el = document.getElementById(id);
            if (el) scrollObserver.observe(el);
        });
    }

    // Handle initial hash on load — always start on overview/main view
    // (avoids blank screen if hash=#feedback was left from a previous session)
    const hash = window.location.hash.replace('#', '');
    if (hash && ['users', 'governance', 'releases'].includes(hash)) {
        navigateTo(hash, false);
    } else {
        // Default: ensure main-content is always visible on fresh load
        document.body.classList.remove('view-feedback');
        setActiveLink('overview');
    }

    // Support browser back/forward buttons and direct hash changes safely
    window.addEventListener('hashchange', () => {
        const h = (window.location.hash || '').replace('#', '');
        navigateTo(h || 'overview', false);
    });
    window.addEventListener('popstate', () => {
        const h = (window.location.hash || '').replace('#', '');
        navigateTo(h || 'overview', false);
    });
}

// ─── FeedbackPanel ────────────────────────────────────────────────────────────

class FeedbackPanel {
    constructor(dashboard) {
        this.dashboard = dashboard; // reference for auth headers & toast
        this.items = [];
        this.loaded = false;
        this.currentItem = null;

        this.cardsEl = document.getElementById('fbCards');
        this.emptyEl = document.getElementById('fbEmpty');
        this.totalEl = document.getElementById('fbTotal');
        this.newCountEl = document.getElementById('fbNewCount');
        this.actEl = document.getElementById('fbActionedCount');
        this.closedEl = document.getElementById('fbClosedCount');
        this.badgeEl = document.getElementById('feedbackBadge');
        this.modalEl = document.getElementById('fbModal');
        this.modalBody = document.getElementById('fbModalContent');
        this.statusSel = document.getElementById('fbStatusFilter');
        this.categorySel = document.getElementById('fbCategoryFilter');
        this.refreshBtn = document.getElementById('fbRefreshBtn');

        if (this.refreshBtn) this.refreshBtn.addEventListener('click', () => this.load());
        if (this.statusSel) this.statusSel.addEventListener('change', () => this.render());
        if (this.categorySel) this.categorySel.addEventListener('change', () => this.render());
        if (this.modalEl) {
            this.modalEl.addEventListener('click', e => {
                if (e.target === this.modalEl) this.closeModal();
            });
        }
    }

    loadIfNeeded() {
        if (!this.loaded) this.load();
    }

    async load() {
        if (!this.dashboard) return;
        const headers = this.dashboard.getAuthHeaders();
        const url = this.dashboard.getApiUrl('/feedback') + '?limit=200';
        try {
            if (this.refreshBtn) {
                this.refreshBtn.disabled = true;
                this.refreshBtn.querySelector('svg').classList.add('spinning');
            }
            const res = await fetch(url, { headers });
            if (res.status === 401) {
                this.dashboard.lockPortal();
                return;
            }
            if (!res.ok && res.status !== 401) {
                const errText = await this.dashboard._extractErrorText(res);
                this.dashboard.showToast(`Failed to load feedback: ${errText}`, 'error');
                return;
            }
            const data = await res.json();
            if (!data || !Array.isArray(data.feedback)) {
                this.dashboard.showToast('Invalid feedback payload received from server', 'error');
                return;
            }
            this.items = data.feedback;
            this.loaded = true;
            this.render();
        } catch (err) {
            console.error('[FeedbackPanel] load error:', err);
            this.dashboard.showToast('Failed to load feedback', 'error');
        } finally {
            if (this.refreshBtn) {
                this.refreshBtn.disabled = false;
                this.refreshBtn.querySelector('svg').classList.remove('spinning');
            }
        }
    }

    render() {
        let filtered = [...this.items];
        const statusF = this.statusSel ? this.statusSel.value : '';
        const catF = this.categorySel ? this.categorySel.value : '';
        if (statusF) filtered = filtered.filter(f => f.status === statusF);
        if (catF) filtered = filtered.filter(f => f.category === catF);

        // Stats (always from full list)
        const newC = this.items.filter(f => f.status === 'new').length;
        const actC = this.items.filter(f => f.status === 'actioned').length;
        const closedC = this.items.filter(f => f.status === 'closed').length;
        if (this.totalEl) this.totalEl.textContent = this.items.length;
        if (this.newCountEl) this.newCountEl.textContent = newC;
        if (this.actEl) this.actEl.textContent = actC;
        if (this.closedEl) this.closedEl.textContent = closedC;

        // Badge in nav
        if (this.badgeEl) {
            if (newC > 0) { this.badgeEl.textContent = newC; this.badgeEl.style.display = 'inline-block'; }
            else { this.badgeEl.style.display = 'none'; }
        }

        if (!this.cardsEl) return;
        if (filtered.length === 0) {
            this.cardsEl.innerHTML = '';
            if (this.emptyEl) this.emptyEl.style.display = 'block';
            return;
        }
        if (this.emptyEl) this.emptyEl.style.display = 'none';

        this.cardsEl.innerHTML = filtered.map(f => this._cardHtml(f)).join('');
    }

    _statusBadge(status) {
        const map = {
            new: 'background:#ff6b35;color:#fff',
            read: 'background:#6c757d;color:#fff',
            actioned: 'background:#0d6efd;color:#fff',
            closed: 'background:#198754;color:#fff'
        };
        return `<span style="font-size:11px;font-weight:600;padding:2px 8px;border-radius:20px;${map[status] || ''}">${escapeHtml(status)}</span>`;
    }

    _catBadge(cat) {
        return `<span style="font-size:11px;padding:2px 7px;border-radius:20px;background:rgba(255,255,255,.08);color:var(--text-muted)">${escapeHtml(cat)}</span>`;
    }

    _cardHtml(f) {
        const date = f.submitted_at ? new Date(f.submitted_at).toLocaleString() : '—';
        const sid = f.student_id ? `<span style="font-size:11px;color:var(--text-muted)">Student: ${escapeHtml(f.student_id)}</span>` : '';
        const ver = f.app_version ? `<span style="font-size:11px;color:var(--text-muted)">v${escapeHtml(f.app_version)}</span>` : '';
        const msg = escapeHtml(f.message || '').slice(0, 180) + (f.message && f.message.length > 180 ? '…' : '');
        return `
<div class="fb-card" style="padding:16px 20px;border-bottom:1px solid rgba(255,255,255,.06);cursor:pointer;"
     onclick="window.feedbackPanel.openModal('${escapeHtml(f.id)}')">
    <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;flex-wrap:wrap;">
        ${this._statusBadge(f.status)}
        ${this._catBadge(f.category)}
        ${sid}${ver}
        <span style="margin-left:auto;font-size:11px;color:var(--text-muted)">${date}</span>
    </div>
    <div style="font-size:14px;line-height:1.5;color:var(--text)">${msg}</div>
    ${f.admin_notes ? `<div style="margin-top:6px;font-size:12px;color:var(--text-muted);font-style:italic">Note: ${escapeHtml(f.admin_notes)}</div>` : ''}
</div>`;
    }

    openModal(id) {
        this.currentItem = this.items.find(f => f.id === id);
        if (!this.currentItem || !this.modalEl || !this.modalBody) return;
        this._renderModal();
        this.modalEl.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    closeModal() {
        if (this.modalEl) this.modalEl.style.display = 'none';
        document.body.style.overflow = '';
        this.currentItem = null;
    }

    _renderModal() {
        const f = this.currentItem;
        const date = f.submitted_at ? new Date(f.submitted_at).toLocaleString() : '—';
        const resolvedAt = f.resolved_at ? new Date(f.resolved_at).toLocaleString() : null;
        this.modalBody.innerHTML = `
<h3 style="margin:0 0 14px;font-size:17px;">Feedback Detail</h3>
<div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px;">
    ${this._statusBadge(f.status)} ${this._catBadge(f.category)}
</div>
<div style="margin-bottom:12px;">
    <div style="font-size:12px;color:var(--text-muted);margin-bottom:4px;">Submitted: ${date}</div>
    ${f.student_id ? `<div style="font-size:12px;color:var(--text-muted)">Student ID: ${escapeHtml(f.student_id)}</div>` : ''}
    ${f.app_version ? `<div style="font-size:12px;color:var(--text-muted)">Version: ${escapeHtml(f.app_version)}</div>` : ''}
    ${f.device_model ? `<div style="font-size:12px;color:var(--text-muted)">Device: ${escapeHtml(f.device_model)}</div>` : ''}
    ${resolvedAt ? `<div style="font-size:12px;color:var(--text-muted)">Resolved: ${resolvedAt}</div>` : ''}
</div>
<div style="background:rgba(255,255,255,.04);border-radius:8px;padding:14px;margin-bottom:16px;font-size:14px;line-height:1.6;white-space:pre-wrap;">
${escapeHtml(f.message)}
</div>

<label style="display:block;font-size:12px;color:var(--text-muted);margin-bottom:6px;">Update Status</label>
<div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:16px;">
    ${['new', 'read', 'actioned', 'closed'].map(s =>
            `<button class="btn btn-secondary" style="font-size:12px;padding:4px 12px;${f.status === s ? 'outline:2px solid var(--accent);' : ''}"
            onclick="window.feedbackPanel.updateStatus('${f.id}','${s}')">${s}</button>`
        ).join('')}
</div>

<label style="display:block;font-size:12px;color:var(--text-muted);margin-bottom:6px;">Admin Notes</label>
<textarea id="fbModalNotes" rows="3" style="width:100%;background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.1);border-radius:8px;padding:10px;color:var(--text);font-size:13px;resize:vertical;box-sizing:border-box;"
>${escapeHtml(f.admin_notes || '')}</textarea>

<div style="display:flex;gap:8px;margin-top:14px;">
    <button class="btn btn-primary" style="flex:1" onclick="window.feedbackPanel.saveNotes('${f.id}')">Save Notes</button>
    <button class="btn btn-secondary" style="color:#e74c3c;border-color:#e74c3c" onclick="window.feedbackPanel.deleteFeedback('${f.id}')">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        Delete
    </button>
</div>`;
    }

    async updateStatus(id, status) {
        await this._patch(id, { status });
    }

    async saveNotes(id) {
        const textarea = document.getElementById('fbModalNotes');
        if (!textarea) return;
        await this._patch(id, { admin_notes: textarea.value });
    }

    async _patch(id, body) {
        const url = this.dashboard.getApiUrl(`/feedback?id=${encodeURIComponent(id)}`);
        try {
            const res = await fetch(url, {
                method: 'PATCH',
                headers: this.dashboard.getAuthHeaders(),
                body: JSON.stringify(body)
            });
            if (res.status === 401) { this.dashboard.lockPortal(); return; }
            if (!res.ok) {
                const errText = await this.dashboard._extractErrorText(res);
                this.dashboard.showToast(`Update failed: ${errText}`, 'error');
                return;
            }
            const data = await res.json();
            if (data && data.success) {
                const idx = this.items.findIndex(f => f.id === id);
                if (idx !== -1) this.items[idx] = data.feedback;
                this.currentItem = data.feedback;
                this._renderModal();
                this.render();
                this.dashboard.showToast('Feedback updated', 'success');
            } else {
                this.dashboard.showToast(data?.error || 'Update failed', 'error');
            }
        } catch (err) {
            this.dashboard.showToast('Update failed', 'error');
        }
    }

    async deleteFeedback(id) {
        if (!confirm('Delete this feedback entry? This cannot be undone.')) return;
        const url = this.dashboard.getApiUrl(`/feedback?id=${encodeURIComponent(id)}`);
        try {
            const res = await fetch(url, { method: 'DELETE', headers: this.dashboard.getAuthHeaders() });
            if (res.status === 401) { this.dashboard.lockPortal(); return; }
            if (!res.ok) {
                const errText = await this.dashboard._extractErrorText(res);
                this.dashboard.showToast(`Delete failed: ${errText}`, 'error');
                return;
            }
            const data = await res.json();
            if (data && data.success) {
                this.items = this.items.filter(f => f.id !== id);
                this.closeModal();
                this.render();
                this.dashboard.showToast('Feedback deleted', 'success');
            } else {
                this.dashboard.showToast(data?.error || 'Delete failed', 'error');
            }
        } catch (err) {
            this.dashboard.showToast('Delete failed', 'error');
        }
    }
}

