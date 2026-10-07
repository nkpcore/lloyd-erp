/**
 * Lloyd ERP — Student Fleet & Telemetry Admin Dashboard
 * Dynamic telemetry loader, KPI computation, and real-time fleet filtering.
 * ZERO HARDCODED DATA / VERSIONS: Every metric is strictly computed from live client telemetry.
 */

class TelemetryDashboard {
    constructor() {
        this.records = [];
        this.filteredRecords = [];
        
        // Auto-connect to origin or local node server without prompting
        const isHttp = typeof window !== 'undefined' && window.location.protocol.startsWith('http');
        const defaultOrigin = isHttp ? window.location.origin : 'http://localhost:8080';
        this.endpointUrl = localStorage.getItem('telemetry_api_url') || defaultOrigin;
        this.fleetConfig = this.loadStoredConfig();

        this.initDOMElements();
        this.initGovernanceElements();
        this.bindEvents();
        this.populateGovernanceFields();
        this.loadTelemetry();
    }

    loadStoredConfig() {
        const stored = localStorage.getItem('fleet_config');
        if (stored) {
            try {
                return JSON.parse(stored);
            } catch (e) {}
        }
        return {
            min_version_code: 1,
            latest_version_name: null,
            download_url: '',
            banned_students: [],
            banned_devices: [],
            maintenance_mode: false,
            maintenance_message: 'Lloyd ERP service is currently undergoing routine maintenance.',
            broadcast_notice: ''
        };
    }

    initDOMElements() {
        this.totalUsersEl = document.getElementById('totalUsers');
        this.activeTodayEl = document.getElementById('activeToday');
        this.latestVersionEl = document.getElementById('latestVersion');
        this.adoptionRateEl = document.getElementById('adoptionRate');
        this.osDominanceEl = document.getElementById('osDominance');
        this.versionBarsEl = document.getElementById('versionBars');
        this.tableBodyEl = document.getElementById('tableBody');
        this.tableCountEl = document.getElementById('tableCount');
        this.searchInput = document.getElementById('searchInput');
        this.statusFilter = document.getElementById('statusFilter');
        this.versionFilter = document.getElementById('versionFilter');
        this.refreshBtn = document.getElementById('refreshBtn');
        this.configBtn = document.getElementById('configBtn');
        this.syncStatusEl = document.getElementById('syncStatus');
    }

    initGovernanceElements() {
        this.cfgMinVersion = document.getElementById('cfgMinVersion');
        this.cfgLatestVersion = document.getElementById('cfgLatestVersion');
        this.cfgDownloadUrl = document.getElementById('cfgDownloadUrl');
        this.cfgBroadcastNotice = document.getElementById('cfgBroadcastNotice');
        this.cfgBannedDevices = document.getElementById('cfgBannedDevices');
        this.cfgMaintenanceMode = document.getElementById('cfgMaintenanceMode');
        this.saveConfigBtn = document.getElementById('saveConfigBtn');
        this.downloadConfigBtn = document.getElementById('downloadConfigBtn');
        this.copyConfigBtn = document.getElementById('copyConfigBtn');
    }

    populateGovernanceFields() {
        if (!this.cfgMinVersion) return;
        this.cfgMinVersion.value = this.fleetConfig.min_version_code || '';
        if (this.cfgLatestVersion) {
            this.cfgLatestVersion.value = this.fleetConfig.latest_version_name || '';
        }
        this.cfgDownloadUrl.value = this.fleetConfig.download_url || '';
        this.cfgBroadcastNotice.value = this.fleetConfig.broadcast_notice || '';
        if (this.cfgBannedDevices) {
            this.cfgBannedDevices.value = (this.fleetConfig.banned_devices || []).join(', ');
        }
        this.cfgMaintenanceMode.checked = !!this.fleetConfig.maintenance_mode;
    }

    saveConfig() {
        const minVer = parseInt(this.cfgMinVersion.value) || 1;
        const latestVer = this.cfgLatestVersion ? (this.cfgLatestVersion.value || '').trim() : '';
        const dlUrl = (this.cfgDownloadUrl.value || '').trim();
        const notice = (this.cfgBroadcastNotice.value || '').trim();
        const bannedDevStr = (this.cfgBannedDevices ? this.cfgBannedDevices.value : '').trim();
        const bannedDevList = bannedDevStr
            ? bannedDevStr.split(',').map(s => s.trim()).filter(Boolean)
            : [];
        const isMaint = this.cfgMaintenanceMode.checked;

        this.fleetConfig.min_version_code = minVer;
        this.fleetConfig.latest_version_name = latestVer || null;
        this.fleetConfig.download_url = dlUrl;
        this.fleetConfig.broadcast_notice = notice || null;
        this.fleetConfig.banned_devices = [...new Set(bannedDevList)];
        this.fleetConfig.maintenance_mode = isMaint;

        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));

        if (this.endpointUrl) {
            const configUrl = this.endpointUrl.replace(/\/+$/, '') + '/config';
            fetch(configUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(this.fleetConfig)
            }).catch(err => console.warn('Could not sync remote config:', err));
        }

        if (this.saveConfigBtn) {
            const originalText = this.saveConfigBtn.innerHTML;
            this.saveConfigBtn.innerHTML = `✓ Saved & Deployed`;
            this.saveConfigBtn.style.background = '#84A59D';
            setTimeout(() => {
                this.saveConfigBtn.innerHTML = originalText;
                this.saveConfigBtn.style.background = '';
            }, 2000);
        }

        this.renderTable();
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
        });
    }

    toggleBan(deviceId) {
        if (!deviceId) return;
        const id = String(deviceId).trim();
        const bans = new Set(this.fleetConfig.banned_devices || []);
        if (bans.has(id)) {
            bans.delete(id);
        } else {
            bans.add(id);
        }
        this.fleetConfig.banned_devices = Array.from(bans);
        this.populateGovernanceFields();
        this.saveConfig();
    }

    bindEvents() {
        if (this.configBtn) {
            this.configBtn.addEventListener('click', () => {
                const current = this.endpointUrl || 'http://localhost:8080';
                const entered = prompt('Telemetry Server URL (Default: http://localhost:8080):', current);
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

        this.refreshBtn.addEventListener('click', () => {
            this.refreshBtn.classList.add('loading');
            this.loadTelemetry().finally(() => {
                this.refreshBtn.classList.remove('loading');
            });
        });

        this.searchInput.addEventListener('input', () => this.applyFilters());
        if (this.statusFilter) {
            this.statusFilter.addEventListener('change', () => this.applyFilters());
        }
        this.versionFilter.addEventListener('change', () => this.applyFilters());

        // Sidebar Navigation links
        document.querySelectorAll('.nav-menu .nav-item').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                document.querySelectorAll('.nav-menu .nav-item').forEach(item => item.classList.remove('active'));
                link.classList.add('active');
                const target = link.getAttribute('href');
                if (target === '#overview') {
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                } else if (target === '#users') {
                    document.getElementById('usersSection')?.scrollIntoView({ behavior: 'smooth' });
                } else if (target === '#releases') {
                    document.getElementById('governanceSection')?.scrollIntoView({ behavior: 'smooth' });
                }
            });
        });
    }

    async loadTelemetry() {
        const base = (this.endpointUrl || 'http://localhost:8080').replace(/\/+$/, '');

        // Candidate URLs to auto-connect (supports direct file:// browsing and web deployments)
        const candidates = [];
        if (typeof window !== 'undefined' && window.location.origin && window.location.origin.startsWith('http')) {
            candidates.push(window.location.origin);
        }
        if (base && !candidates.includes(base)) {
            candidates.push(base);
        }
        if (!candidates.includes('http://localhost:8080')) {
            candidates.push('http://localhost:8080');
        }

        let connectedUrl = null;

        for (const candidate of candidates) {
            try {
                // Synchronize remote config if available
                try {
                    const cfgUrl = candidate + (candidate.includes('/api') ? '/config' : '/api/config');
                    const cfgRes = await fetch(cfgUrl, { signal: AbortSignal.timeout(2500) });
                    if (cfgRes.ok) {
                        const remoteCfg = await cfgRes.json();
                        this.fleetConfig = { ...this.fleetConfig, ...remoteCfg };
                        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
                        this.populateGovernanceFields();
                    }
                } catch (e) {}

                // Fetch telemetry heartbeats
                const telUrl = candidate + (candidate.includes('/api') ? '/telemetry' : '/api/telemetry');
                const res = await fetch(telUrl, { signal: AbortSignal.timeout(2500) });
                if (res.ok) {
                    this.records = await res.json();
                    connectedUrl = candidate;
                    break;
                }
            } catch (err) {}
        }

        if (connectedUrl) {
            this.endpointUrl = connectedUrl;
            this.syncStatusEl.textContent = `Live Telemetry Connected (${connectedUrl.replace(/^https?:\/\//, '')})`;
            this.syncStatusEl.style.color = '#84A59D';
        } else {
            this.syncStatusEl.textContent = 'Server Offline (Run node server.js)';
            this.syncStatusEl.style.color = '#F28482';
            this.records = [];
        }

        this.populateVersionFilter();
        this.computeKPIs();
        this.renderVersionBars();
        this.applyFilters();

        // Setup background polling if not already started
        if (!this.pollTimer) {
            this.pollTimer = setInterval(() => this.loadTelemetry(), 10000);
        }
    }

    populateVersionFilter() {
        const uniqueVersions = [...new Set(this.records.map(r => r.app_version).filter(Boolean))].sort().reverse();
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
        const total = this.records.length;
        this.totalUsersEl.textContent = total.toLocaleString();

        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        const activeToday = this.records.filter(r => r.timestamp && new Date(r.timestamp).getTime() >= oneDayAgo).length;
        this.activeTodayEl.textContent = activeToday.toLocaleString();

        // Calculate latest version and adoption rate purely from live data
        const versionCounts = {};
        this.records.forEach(r => {
            if (r.app_version) {
                versionCounts[r.app_version] = (versionCounts[r.app_version] || 0) + 1;
            }
        });

        const sortedVersions = Object.keys(versionCounts).sort().reverse();
        const latestVer = sortedVersions[0] || this.fleetConfig.latest_version_name || '--';
        const latestCount = versionCounts[latestVer] || 0;
        const adoptionPercent = total > 0 && latestVer !== '--' ? Math.round((latestCount / total) * 100) : 0;

        this.latestVersionEl.textContent = latestVer;
        this.adoptionRateEl.textContent = total > 0 ? `${adoptionPercent}% of fleet updated` : 'No active fleet telemetry yet';

        // Modern OS share (API 34+)
        const modernCount = this.records.filter(r => {
            const os = r.os_version || '';
            return os.includes('API 34') || os.includes('API 35') || os.includes('API 36') ||
                   os.includes('Android 14') || os.includes('Android 15') || os.includes('Android 16');
        }).length;
        const modernPercent = total > 0 ? Math.round((modernCount / total) * 100) : 0;
        this.osDominanceEl.textContent = total > 0 ? `${modernPercent}%` : '--';
    }

    renderVersionBars() {
        const versionCounts = {};
        this.records.forEach(r => {
            if (r.app_version) {
                versionCounts[r.app_version] = (versionCounts[r.app_version] || 0) + 1;
            }
        });

        const sorted = Object.entries(versionCounts).sort((a, b) => b[1] - a[1]);
        const total = this.records.length || 0;

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
                    <strong>${ver}</strong>
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
        const verFilter = this.versionFilter.value;
        const statFilter = this.statusFilter ? this.statusFilter.value : 'ALL';
        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        const bannedSet = new Set(this.fleetConfig.banned_devices || []);

        this.filteredRecords = this.records.filter(r => {
            const matchesVer = verFilter === 'ALL' || r.app_version === verFilter;
            const matchesQuery = !query ||
                (r.student_name && r.student_name.toLowerCase().includes(query)) ||
                (r.student_id && r.student_id.toString().includes(query)) ||
                (r.device_id && r.device_id.toLowerCase().includes(query)) ||
                (r.device_model && r.device_model.toLowerCase().includes(query)) ||
                (r.app_version && r.app_version.toLowerCase().includes(query));

            const isBanned = r.device_id && bannedSet.has(r.device_id);
            const isOnline = r.timestamp && (new Date(r.timestamp).getTime() >= oneDayAgo);

            let matchesStatus = true;
            if (statFilter === 'ACTIVE') matchesStatus = isOnline && !isBanned;
            else if (statFilter === 'IDLE') matchesStatus = !isOnline && !isBanned;
            else if (statFilter === 'REVOKED') matchesStatus = isBanned;

            return matchesVer && matchesQuery && matchesStatus;
        });

        this.renderTable();
    }

    renderTable() {
        this.tableCountEl.textContent = `(${this.filteredRecords.length} records)`;

        if (this.records.length === 0) {
            this.tableBodyEl.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 48px 16px; color: var(--text-secondary);">
                        <div style="font-size: 28px; margin-bottom: 8px;">📡</div>
                        <strong style="display: block; font-size: 15px; color: var(--text-primary); margin-bottom: 6px;">No Active Devices Registered Yet</strong>
                        <span>Open the Lloyd ERP Android app or dispatch telemetry to view live fleet records.</span>
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
        const bannedSet = new Set(this.fleetConfig.banned_devices || []);

        this.tableBodyEl.innerHTML = this.filteredRecords.map(r => {
            const lastActiveTime = r.timestamp ? new Date(r.timestamp).getTime() : 0;
            const isOnline = lastActiveTime >= oneDayAgo;
            const timeAgoStr = lastActiveTime > 0 ? this.formatTimeAgo(lastActiveTime) : 'Never';
            const isBanned = r.device_id && bannedSet.has(r.device_id);

            const initials = r.student_name
                ? r.student_name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
                : 'ST';

            return `
                <tr>
                    <td>
                        <div class="student-col">
                            <div class="avatar-circle">${initials}</div>
                            <div>
                                <strong>${r.student_name || 'Student'}</strong>
                                <small style="display:block; color:var(--text-secondary); font-size:11px;">UUID: ${r.device_id || '--'}</small>
                            </div>
                        </div>
                    </td>
                    <td><code>${r.student_id ?? '--'}</code></td>
                    <td><span class="version-pill">${r.app_version || '--'}</span></td>
                    <td>${r.device_model || 'Unknown'}</td>
                    <td><small style="color: var(--text-secondary)">${r.os_version || 'Android'}</small></td>
                    <td>${timeAgoStr}</td>
                    <td>
                        ${isBanned
                            ? '<span class="status-badge status-revoked">● Revoked</span>'
                            : `<span class="status-badge ${isOnline ? 'status-online' : 'status-offline'}">● ${isOnline ? 'Active' : 'Idle'}</span>`
                        }
                    </td>
                    <td>
                        ${isBanned
                            ? `<button class="btn-action btn-restore" onclick="window.telemetryDashboard.toggleBan('${r.device_id}')">Restore Device</button>`
                            : `<button class="btn-action btn-revoke" onclick="window.telemetryDashboard.toggleBan('${r.device_id}')">Revoke Device</button>`
                        }
                    </td>
                </tr>
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
});
