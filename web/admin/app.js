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
        this.endpointUrl = localStorage.getItem('telemetry_api_url') || '';

        this.initDOMElements();
        this.bindEvents();
        this.loadTelemetry();
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
        this.versionFilter = document.getElementById('versionFilter');
        this.refreshBtn = document.getElementById('refreshBtn');
        this.syncStatusEl = document.getElementById('syncStatus');
    }

    bindEvents() {
        this.refreshBtn.addEventListener('click', () => {
            this.refreshBtn.classList.add('loading');
            this.loadTelemetry().finally(() => {
                this.refreshBtn.classList.remove('loading');
            });
        });

        this.searchInput.addEventListener('input', () => this.applyFilters());
        this.versionFilter.addEventListener('change', () => this.applyFilters());
    }

    async loadTelemetry() {
        if (this.endpointUrl) {
            try {
                this.syncStatusEl.textContent = 'Fetching remote telemetry...';
                const res = await fetch(this.endpointUrl);
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

        this.filteredRecords = this.records.filter(r => {
            const matchesVer = verFilter === 'ALL' || r.app_version === verFilter;
            const matchesQuery = !query ||
                (r.student_name && r.student_name.toLowerCase().includes(query)) ||
                (r.student_id && r.student_id.toString().includes(query)) ||
                (r.device_model && r.device_model.toLowerCase().includes(query)) ||
                (r.app_version && r.app_version.toLowerCase().includes(query));

            return matchesVer && matchesQuery;
        });

        this.renderTable();
    }

    renderTable() {
        this.tableCountEl.textContent = `(${this.filteredRecords.length} records)`;

        if (this.filteredRecords.length === 0) {
            this.tableBodyEl.innerHTML = `
                <tr>
                    <td colspan="7" class="loading-cell">No student records match the search filter.</td>
                </tr>
            `;
            return;
        }

        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);

        this.tableBodyEl.innerHTML = this.filteredRecords.map(r => {
            const lastActiveTime = new Date(r.timestamp).getTime();
            const isOnline = lastActiveTime >= oneDayAgo;
            const timeAgoStr = this.formatTimeAgo(lastActiveTime);

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
                            </div>
                        </div>
                    </td>
                    <td><code>${r.student_id ?? '--'}</code></td>
                    <td><span class="version-pill">${r.app_version}</span></td>
                    <td>${r.device_model || 'Unknown'}</td>
                    <td><small style="color: var(--text-secondary)">${r.os_version || 'Android'}</small></td>
                    <td>${timeAgoStr}</td>
                    <td>
                        <span class="status-badge ${isOnline ? 'status-online' : 'status-offline'}">
                            ● ${isOnline ? 'Active' : 'Idle'}
                        </span>
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
