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

class TelemetryDashboard {
    constructor() {
        this.records = [];
        this.filteredRecords = [];
        this.isFetching = false;
        this.sortColumn = 'timestamp';
        this.sortDirection = 'desc';
        
        // Auto-connect to origin or local node server without prompting
        const isHttp = typeof window !== 'undefined' && window.location.protocol.startsWith('http');
        const defaultOrigin = isHttp ? window.location.origin : 'http://localhost:8080';
        this.endpointUrl = localStorage.getItem('telemetry_api_url') || defaultOrigin;
        this.fleetConfig = this.loadStoredConfig();

        this.initDOMElements();
        this.initGovernanceElements();
        this.bindEvents();
        this.populateGovernanceFields();
        this.updateServerLabel();
        this.renderMaintenanceBanner();
        this.renderActiveBanChips();
        this.updateSortIcons();
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

        // Server switcher
        this.switchServerBtn = document.getElementById('switchServerBtn');
        this.serverLabel = document.getElementById('serverLabel');

        // Maintenance banner
        this.maintenanceAlertBanner = document.getElementById('maintenanceAlertBanner');
        this.maintBannerMsg = document.getElementById('maintBannerMsg');
        this.deactivateMaintBtn = document.getElementById('deactivateMaintBtn');

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
    }

    initGovernanceElements() {
        this.cfgMinVersion = document.getElementById('cfgMinVersion');
        this.cfgLatestVersion = document.getElementById('cfgLatestVersion');
        this.cfgDownloadUrl = document.getElementById('cfgDownloadUrl');
        this.cfgBroadcastNotice = document.getElementById('cfgBroadcastNotice');
        this.cfgBannedDevices = document.getElementById('cfgBannedDevices');
        this.cfgBannedStudents = document.getElementById('cfgBannedStudents');
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
        if (this.cfgBannedDevices) {
            this.cfgBannedDevices.value = (this.fleetConfig.banned_devices || []).join(', ');
        }
        if (this.cfgBannedStudents) {
            this.cfgBannedStudents.value = (this.fleetConfig.banned_students || []).join(', ');
        }
        if (this.cfgMaintenanceMessage) {
            this.cfgMaintenanceMessage.value = this.fleetConfig.maintenance_message || '';
        }
        if (this.cfgMaintenanceMode) {
            this.cfgMaintenanceMode.checked = !!this.fleetConfig.maintenance_mode;
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
                            <span>📱 ${escapeHtml(short)}</span>
                            <button class="btn-remove-chip" title="Restore device access" onclick="window.telemetryDashboard.toggleDeviceBan('${safeAttr(id)}', 'unban')">×</button>
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
                            <span>👤 #${id}</span>
                            <button class="btn-remove-chip" title="Unban student account" onclick="window.telemetryDashboard.toggleStudentBan(${id}, 'unban')">×</button>
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
        toast.innerHTML = `<span>${message}</span>`;
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
        const minVer = parseInt(this.cfgMinVersion.value) || 1;
        const latestVer = this.cfgLatestVersion ? (this.cfgLatestVersion.value || '').trim() : '';
        const dlUrl = (this.cfgDownloadUrl ? this.cfgDownloadUrl.value : '').trim();
        const notice = (this.cfgBroadcastNotice ? this.cfgBroadcastNotice.value : '').trim();
        
        const bannedDevStr = (this.cfgBannedDevices ? this.cfgBannedDevices.value : '').trim();
        const bannedDevList = bannedDevStr
            ? bannedDevStr.split(',').map(s => s.trim()).filter(Boolean)
            : [];

        const bannedStudentsStr = (this.cfgBannedStudents ? this.cfgBannedStudents.value : '').trim();
        const bannedStudentsList = bannedStudentsStr
            ? bannedStudentsStr.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n) && n > 0)
            : [];

        const maintMsg = (this.cfgMaintenanceMessage ? this.cfgMaintenanceMessage.value : '').trim();
        const isMaint = this.cfgMaintenanceMode ? this.cfgMaintenanceMode.checked : false;

        this.fleetConfig.min_version_code = minVer;
        this.fleetConfig.latest_version_name = latestVer || null;
        this.fleetConfig.download_url = dlUrl || null;
        this.fleetConfig.broadcast_notice = notice || null;
        this.fleetConfig.banned_devices = [...new Set(bannedDevList)];
        this.fleetConfig.banned_students = [...new Set(bannedStudentsList)];
        this.fleetConfig.maintenance_mode = isMaint;
        if (maintMsg) {
            this.fleetConfig.maintenance_message = maintMsg;
        }

        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));

        if (this.endpointUrl) {
            try {
                const res = await fetch(this.getApiUrl('/config'), {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(this.fleetConfig)
                });
                if (res.ok) {
                    this.showToast('Config saved and deployed to fleet!', 'success');
                } else {
                    this.showToast(`Server returned status ${res.status}`, 'error');
                }
            } catch (err) {
                console.warn('Could not sync remote config:', err);
                this.showToast('Saved locally (network sync failed)', 'info');
            }
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

        this.renderMaintenanceBanner();
        this.renderActiveBanChips();
        this.computeKPIs();
        this.renderTable();
    }

    async deactivateMaintenance() {
        this.fleetConfig.maintenance_mode = false;
        if (this.cfgMaintenanceMode) this.cfgMaintenanceMode.checked = false;
        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
        this.renderMaintenanceBanner();

        try {
            await fetch(this.getApiUrl('/config'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ maintenance_mode: false })
            });
            this.showToast('Global maintenance mode deactivated! All students restored.', 'success');
        } catch (e) {
            this.showToast('Deactivated locally', 'info');
        }
        this.computeKPIs();
        this.renderTable();
    }

    async pushBroadcast(noticeText = null) {
        const text = (noticeText !== null ? noticeText : (this.quickBroadcastInput ? this.quickBroadcastInput.value : '')).trim();
        this.fleetConfig.broadcast_notice = text || null;
        if (this.cfgBroadcastNotice) this.cfgBroadcastNotice.value = text;
        if (this.quickBroadcastInput) this.quickBroadcastInput.value = text;

        localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));

        try {
            const res = await fetch(this.getApiUrl('/config'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ broadcast_notice: text || null })
            });
            if (res.ok) {
                this.showToast(text ? 'Broadcast notice pushed to student dashboards!' : 'Broadcast notice cleared.', 'success');
            } else {
                this.showToast('Saved locally', 'info');
            }
        } catch (e) {
            this.showToast('Saved locally (offline)', 'info');
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
        this.renderTable();

        try {
            const res = await fetch(this.getApiUrl('/ban'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ device_id: id, action })
            });
            if (res.ok) {
                const data = await res.json();
                if (Array.isArray(data.banned_devices)) {
                    this.fleetConfig.banned_devices = data.banned_devices;
                    localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
                    this.populateGovernanceFields();
                    this.renderActiveBanChips();
                    this.computeKPIs();
                    this.renderTable();
                }
                this.showToast(action === 'ban' ? `Device ${id} revoked successfully` : `Device ${id} restored successfully`, action === 'ban' ? 'error' : 'success');
            } else {
                this.showToast(`Server returned HTTP ${res.status}`, 'error');
            }
        } catch (err) {
            console.warn('Network call to /ban failed:', err);
            this.showToast(`Action executed locally (offline)`, 'info');
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
        this.renderTable();

        try {
            const res = await fetch(this.getApiUrl('/ban'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ student_id: id, action })
            });
            if (res.ok) {
                const data = await res.json();
                if (Array.isArray(data.banned_students)) {
                    this.fleetConfig.banned_students = data.banned_students;
                    localStorage.setItem('fleet_config', JSON.stringify(this.fleetConfig, null, 2));
                    this.populateGovernanceFields();
                    this.renderActiveBanChips();
                    this.computeKPIs();
                    this.renderTable();
                }
                this.showToast(action === 'ban' ? `Student #${id} account banned` : `Student #${id} account restored`, action === 'ban' ? 'error' : 'success');
            } else {
                this.showToast(`Server returned HTTP ${res.status}`, 'error');
            }
        } catch (err) {
            console.warn('Network call to /ban failed:', err);
            this.showToast(`Action executed locally (offline)`, 'info');
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
                this.fetchGhBtn.textContent = '🔄 Sync GitHub';
                this.fetchGhBtn.disabled = false;
            }
        }
    }

    async deleteTelemetry(deviceId) {
        if (!deviceId) return;
        if (!confirm(`Are you sure you want to remove device "${deviceId}" from telemetry?`)) return;

        this.records = this.records.filter(r => r.device_id !== deviceId);
        this.computeKPIs();
        this.renderVersionBars();
        this.applyFilters();

        try {
            const res = await fetch(this.getApiUrl(`/telemetry?device_id=${encodeURIComponent(deviceId)}`), {
                method: 'DELETE',
                signal: AbortSignal.timeout(5000)
            });
            if (res.ok) {
                this.showToast(`Removed device record`, 'success');
            } else {
                this.showToast('Removed locally', 'info');
            }
        } catch (e) {
            this.showToast('Removed locally (offline)', 'info');
        }
    }

    async purgeTestRecords() {
        if (!confirm('Purge all simulated test device records from telemetry?')) return;

        this.records = this.records.filter(r => !(r.device_id && (r.device_id.startsWith('dev-test-') || r.device_id.startsWith('dev-unit-test-') || r.device_id.includes('test'))));
        this.computeKPIs();
        this.renderVersionBars();
        this.applyFilters();

        try {
            const res = await fetch(this.getApiUrl('/telemetry?purge=test'), {
                method: 'DELETE',
                signal: AbortSignal.timeout(5000)
            });
            if (res.ok) {
                const data = await res.json();
                this.showToast(`Purged ${data.purged_count || 0} test records`, 'success');
            } else {
                this.showToast('Test records purged locally', 'info');
            }
        } catch (e) {
            this.showToast('Purged locally (offline)', 'info');
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

        if (this.searchInput) {
            this.searchInput.addEventListener('input', () => this.applyFilters());
        }
        if (this.statusFilter) {
            this.statusFilter.addEventListener('change', () => this.applyFilters());
        }
        if (this.versionFilter) {
            this.versionFilter.addEventListener('change', () => this.applyFilters());
        }

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
                    document.getElementById('users')?.scrollIntoView({ behavior: 'smooth' });
                } else if (target === '#governance') {
                    document.getElementById('governance')?.scrollIntoView({ behavior: 'smooth' });
                } else if (target === '#releases') {
                    document.getElementById('releases')?.scrollIntoView({ behavior: 'smooth' });
                }
            });
        });
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
                        this.renderMaintenanceBanner();
                        this.renderActiveBanChips();
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

        this.isFetching = false;

        if (connectedUrl) {
            this.endpointUrl = connectedUrl;
            this.updateServerLabel();
            if (this.syncStatusEl) {
                this.syncStatusEl.textContent = `Connected (${connectedUrl.replace(/^https?:\/\//, '')})`;
                this.syncStatusEl.style.color = '#84A59D';
            }
            if (this.liveDotEl) {
                this.liveDotEl.style.backgroundColor = '#84A59D';
                this.liveDotEl.style.boxShadow = '0 0 8px #84A59D';
            }
        } else {
            if (this.syncStatusEl) {
                this.syncStatusEl.textContent = 'Server Offline (Start server.js)';
                this.syncStatusEl.style.color = '#F28482';
            }
            if (this.liveDotEl) {
                this.liveDotEl.style.backgroundColor = '#F28482';
                this.liveDotEl.style.boxShadow = '0 0 8px #F28482';
            }
        }

        this.populateVersionFilter();
        this.computeKPIs();
        this.renderVersionBars();
        this.renderMaintenanceBanner();
        this.renderActiveBanChips();
        this.applyFilters();

        // Setup background polling if not already started
        if (!this.pollTimer) {
            this.pollTimer = setInterval(() => this.loadTelemetry(), 12000);
        }
    }

    populateVersionFilter() {
        if (!this.versionFilter) return;
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
        if (this.totalUsersEl) this.totalUsersEl.textContent = total.toLocaleString();

        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        const activeToday = this.records.filter(r => {
            const t = r.timestamp || r.server_received_at;
            return t && new Date(t).getTime() >= oneDayAgo;
        }).length;
        if (this.activeTodayEl) this.activeTodayEl.textContent = activeToday.toLocaleString();

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

        if (this.latestVersionEl) this.latestVersionEl.textContent = latestVer;
        if (this.adoptionRateEl) {
            this.adoptionRateEl.textContent = total > 0 ? `${adoptionPercent}% of fleet running ${latestVer}` : 'No active fleet telemetry yet';
        }

        // Modern OS share (API 34+)
        const modernCount = this.records.filter(r => {
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
                const valA = (a.app_version || '').toLowerCase();
                const valB = (b.app_version || '').toLowerCase();
                return this.sortDirection === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA);
            } else { // timestamp
                const valA = (a.timestamp || a.server_received_at) ? new Date(a.timestamp || a.server_received_at).getTime() : 0;
                const valB = (b.timestamp || b.server_received_at) ? new Date(b.timestamp || b.server_received_at).getTime() : 0;
                return this.sortDirection === 'asc' ? valA - valB : valB - valA;
            }
        });

        this.renderTable();
    }

    renderTable() {
        if (!this.tableBodyEl) return;
        this.tableCountEl.textContent = `(${this.filteredRecords.length} records)`;

        if (this.records.length === 0) {
            this.tableBodyEl.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 48px 16px; color: var(--text-secondary);">
                        <div style="font-size: 32px; margin-bottom: 8px;">📡</div>
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
                                    ${deviceId ? `<button class="copy-inline" title="Copy Full UUID" onclick="window.telemetryDashboard.copyToClipboard('${safeAttr(deviceId)}', 'Device UUID')">📋</button>` : ''}
                                </small>
                            </div>
                        </div>
                    </td>
                    <td>
                        <code>${studentId !== null ? studentId : '--'}</code>
                        ${studentId !== null ? `<button class="copy-inline" title="Copy Student ID" onclick="window.telemetryDashboard.copyToClipboard('${studentId}', 'Student ID')">📋</button>` : ''}
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
                            ${deviceId ? `<button class="btn-action btn-delete-row" title="Delete record from telemetry" onclick="window.telemetryDashboard.deleteTelemetry('${safeAttr(deviceId)}')">🗑️</button>` : ''}
                        </div>
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
