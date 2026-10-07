/**
 * Lloyd ERP — Student Fleet & Telemetry Admin Dashboard
 * Dynamic telemetry loader, KPI computation, and real-time fleet filtering.
 */

// Default sample telemetry seeds used when no remote database endpoint is provided
// dynamically generates records without hardcoding static student data.
function generateDynamicFleetTelemetry(count = 28) {
    const versions = ['v1.0.15', 'v1.0.14', 'v1.0.13', 'v1.0.10'];
    const versionWeights = [0.65, 0.20, 0.10, 0.05];
    const models = [
        'Google Pixel 8 Pro', 'Samsung Galaxy S24 Ultra', 'OnePlus 12',
        'Xiaomi 14', 'Nothing Phone (2)', 'Realme GT 6', 'Motorola Edge 50'
    ];
    const osVersions = [
        'Android 16 (API 36)', 'Android 15 (API 35)', 'Android 14 (API 34)', 'Android 13 (API 33)'
    ];

    const records = [];
    const now = Date.now();

    for (let i = 1; i <= count; i++) {
        // Pick version based on weighted distribution
        const r = Math.random();
        let cumulative = 0;
        let selectedVer = versions[0];
        for (let j = 0; j < versions.length; j++) {
            cumulative += versionWeights[j];
            if (r <= cumulative) {
                selectedVer = versions[j];
                break;
            }
        }

        const model = models[Math.floor(Math.random() * models.length)];
        const os = osVersions[Math.floor(Math.random() * osVersions.length)];
        // Random last seen in last 72 hours
        const minutesAgo = Math.floor(Math.random() * 4320);
        const timestamp = new Date(now - minutesAgo * 60 * 1000).toISOString();
        const studentId = 20000 + i * 37;

        records.push({
            device_id: `dev-${i.toString().padStart(4, '0')}-${Math.random().toString(36).substring(2, 7)}`,
            student_id: studentId,
            student_name: `Student #${studentId}`,
            app_version: selectedVer,
            version_code: parseInt(selectedVer.replace(/[^\d]/g, '')) || 15,
            os_version: os,
            device_model: model,
            timestamp: timestamp
        });
    }

    return records.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
}

class TelemetryDashboard {
    constructor() {
        this.records = [];
        this.filteredRecords = [];
        this.endpointUrl = localStorage.getItem('telemetry_api_url') ||
            (typeof window !== 'undefined' && window.location.protocol.startsWith('http') ? window.location.origin : '');
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
            min_version_code: 15,
            download_url: 'https://github.com/nkpcore/lloyd-erp/releases/latest',
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
        this.cfgMinVersion.value = this.fleetConfig.min_version_code || 15;
        this.cfgDownloadUrl.value = this.fleetConfig.download_url || '';
        this.cfgBroadcastNotice.value = this.fleetConfig.broadcast_notice || '';
        if (this.cfgBannedDevices) {
            this.cfgBannedDevices.value = (this.fleetConfig.banned_devices || []).join(', ');
        }
        this.cfgMaintenanceMode.checked = !!this.fleetConfig.maintenance_mode;
    }

    saveConfig() {
        const minVer = parseInt(this.cfgMinVersion.value) || 1;
        const dlUrl = (this.cfgDownloadUrl.value || '').trim();
        const notice = (this.cfgBroadcastNotice.value || '').trim();
        const bannedDevStr = (this.cfgBannedDevices ? this.cfgBannedDevices.value : '').trim();
        const bannedDevList = bannedDevStr
            ? bannedDevStr.split(',').map(s => s.trim()).filter(Boolean)
            : [];
        const isMaint = this.cfgMaintenanceMode.checked;

        this.fleetConfig.min_version_code = minVer;
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
                const current = localStorage.getItem('telemetry_api_url') || this.endpointUrl;
                const entered = prompt('Enter Telemetry Backend API URL (leave blank for local simulation):', current);
                if (entered !== null) {
                    if (entered.trim()) {
                        localStorage.setItem('telemetry_api_url', entered.trim());
                        this.endpointUrl = entered.trim();
                    } else {
                        localStorage.removeItem('telemetry_api_url');
                        this.endpointUrl = (typeof window !== 'undefined' && window.location.protocol.startsWith('http') ? window.location.origin : '');
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
        if (this.endpointUrl) {
            const base = this.endpointUrl.replace(/\/+$/, '');
            try {
                this.syncStatusEl.textContent = 'Syncing remote fleet...';

                // Synchronize remote config if available
                try {
                    const cfgRes = await fetch(base + '/config');
                    if (cfgRes.ok) {
                        const remoteCfg = await cfgRes.json();
                        this.fleetConfig = { ...this.fleetConfig, ...remoteCfg };
                        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
                        this.populateGovernanceFields();
                    }
                } catch (e) {
                    console.warn('Could not fetch remote config, using local cache:', e);
                }

                // Fetch telemetry heartbeats
                const res = await fetch(base + '/telemetry');
                if (res.ok) {
                    this.records = await res.json();
                    this.syncStatusEl.textContent = 'Remote Telemetry Connected';
                } else {
                    throw new Error(`HTTP ${res.status}`);
                }
            } catch (err) {
                console.warn('Failed to load from remote endpoint, fallback to local stream:', err);
                this.syncStatusEl.textContent = 'Simulated Local Telemetry';
                this.records = generateDynamicFleetTelemetry();
            }
        } else {
            this.syncStatusEl.textContent = 'Local Telemetry Active';
            this.records = generateDynamicFleetTelemetry();
        }

        this.populateVersionFilter();
        this.computeKPIs();
        this.renderVersionBars();
        this.applyFilters();
    }

    populateVersionFilter() {
        const uniqueVersions = [...new Set(this.records.map(r => r.app_version))].sort().reverse();
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
        const activeToday = this.records.filter(r => new Date(r.timestamp).getTime() >= oneDayAgo).length;
        this.activeTodayEl.textContent = activeToday.toLocaleString();

        // Calculate latest version and adoption rate
        const versionCounts = {};
        this.records.forEach(r => {
            versionCounts[r.app_version] = (versionCounts[r.app_version] || 0) + 1;
        });

        const sortedVersions = Object.keys(versionCounts).sort().reverse();
        const latestVer = sortedVersions[0] || 'v1.0.15';
        const latestCount = versionCounts[latestVer] || 0;
        const adoptionPercent = total > 0 ? Math.round((latestCount / total) * 100) : 0;

        this.latestVersionEl.textContent = latestVer;
        this.adoptionRateEl.textContent = `${adoptionPercent}% of fleet updated`;

        // Modern OS share (API 34+)
        const modernCount = this.records.filter(r => {
            const os = r.os_version || '';
            return os.includes('API 34') || os.includes('API 35') || os.includes('API 36') ||
                   os.includes('Android 14') || os.includes('Android 15') || os.includes('Android 16');
        }).length;
        const modernPercent = total > 0 ? Math.round((modernCount / total) * 100) : 0;
        this.osDominanceEl.textContent = `${modernPercent}%`;
    }

    renderVersionBars() {
        const versionCounts = {};
        this.records.forEach(r => {
            versionCounts[r.app_version] = (versionCounts[r.app_version] || 0) + 1;
        });

        const sorted = Object.entries(versionCounts).sort((a, b) => b[1] - a[1]);
        const total = this.records.length || 1;

        this.versionBarsEl.innerHTML = '';
        sorted.forEach(([ver, count]) => {
            const pct = Math.round((count / total) * 100);
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
            const isOnline = new Date(r.timestamp).getTime() >= oneDayAgo;

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
            const lastActiveTime = new Date(r.timestamp).getTime();
            const isOnline = lastActiveTime >= oneDayAgo;
            const timeAgoStr = this.formatTimeAgo(lastActiveTime);
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
                    <td><span class="version-pill">${r.app_version}</span></td>
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
