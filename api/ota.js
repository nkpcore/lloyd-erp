/**
 * Lloyd ERP — Dynamic In-App OTA Update Resolution API (Vercel Serverless / Cloud)
 * Resolves releases dynamically from live fleet governance, server state, and GitHub Releases.
 * Zero hardcoded fallback lock-in: checks fleet config, live releases, and direct downloads.
 */

const db = require('./lib/db');

function setCors(res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept');
}

function cleanVersion(v) {
    return (v || '').trim().replace(/^[vV]/, '').trim();
}

function isNewerVersion(remote, current) {
    const rParts = cleanVersion(remote).split('.').map(n => parseInt(n) || 0);
    const cParts = cleanVersion(current).split('.').map(n => parseInt(n) || 0);
    const maxLen = Math.max(rParts.length, cParts.length);
    for (let i = 0; i < maxLen; i++) {
        const r = rParts[i] || 0;
        const c = cParts[i] || 0;
        if (r > c) return true;
        if (r < c) return false;
    }
    return false;
}

let cachedGhRelease = null;
let lastGhCheckTime = 0;

async function getLiveGitHubRelease() {
    const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
    const repo = process.env.GITHUB_REPOSITORY || 'nkpcore/lloyd-erp';
    const now = Date.now();

    if (cachedGhRelease && (now - lastGhCheckTime < 60000)) {
        return cachedGhRelease;
    }

    try {
        const headers = {
            'User-Agent': 'LloydERP-OTA-Server',
            'Accept': 'application/vnd.github.v3+json'
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const resp = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, {
            headers,
            signal: AbortSignal.timeout(1500)
        });
        if (!resp.ok) return null;

        const release = await resp.json();
        const apkAsset = (release.assets || []).find(a => (a.name || '').toLowerCase().endsWith('.apk'));

        cachedGhRelease = {
            version: cleanVersion(release.tag_name || release.name || ''),
            notes: release.body || release.name || 'New release available from GitHub.',
            assetId: apkAsset ? apkAsset.id : null,
            assetUrl: apkAsset ? apkAsset.url : null,
            browserDownloadUrl: apkAsset ? apkAsset.browser_download_url : null
        };
        lastGhCheckTime = now;
        return cachedGhRelease;
    } catch (_) {
        return null;
    }
}

module.exports = async function handler(req, res) {
    setCors(res);

    if (req.method === 'OPTIONS') {
        res.status(204).end();
        return;
    }

    if (req.method !== 'GET') {
        res.status(405).json({ error: 'Method not allowed' });
        return;
    }

    try {
        const isDownload = req.query.download === 'latest' || req.query.download === 'true' || req.query.download === '1';
        const config = await db.getConfig();
        const ghRelease = await getLiveGitHubRelease();

        const host = req.headers['x-forwarded-host'] || req.headers.host || '';
        const proto = req.headers['x-forwarded-proto'] || 'https';
        const baseUrl = host ? `${proto}://${host}` : 'https://lloyd-erp-sand.vercel.app';

        // 1. If download action is requested, redirect to direct APK or proxy
        if (isDownload) {
            if (config.download_url && config.download_url.trim().length > 0) {
                let target = config.download_url.trim();
                if (!target.startsWith('http://') && !target.startsWith('https://')) {
                    target = `${baseUrl}/${target.replace(/^\/+/, '')}`;
                }
                res.writeHead(302, { 'Location': target });
                res.end();
                return;
            }

            if (ghRelease && ghRelease.browserDownloadUrl) {
                res.writeHead(302, { 'Location': ghRelease.browserDownloadUrl });
                res.end();
                return;
            }

            // Fallback to static release APK on Vercel CDN
            res.writeHead(302, { 'Location': `${baseUrl}/downloads/LloydAttendance-latest.apk` });
            res.end();
            return;
        }

        // 2. Query check for update
        const clientVersion = req.query.current_version || '';
        const cleanClient = cleanVersion(clientVersion);

        // Resolve latest version from: GitHub Release > Admin Config > Telemetry Record
        let latestVersion = (ghRelease && ghRelease.version)
            || (config && config.latest_version_name)
            || null;

        if (!latestVersion) {
            const telemetry = await db.getTelemetryRecords();
            if (telemetry.length > 0 && telemetry[0].app_version) {
                latestVersion = telemetry[0].app_version;
            }
        }

        if (!latestVersion) {
            latestVersion = cleanClient;
        }

        latestVersion = cleanVersion(latestVersion);

        const hasUpdate = isNewerVersion(latestVersion, cleanClient);

        let downloadUrl = null;
        let releaseNotes = config.broadcast_notice || 'You are running the latest version.';

        if (hasUpdate) {
            if (config.download_url && config.download_url.trim().length > 0) {
                let dl = config.download_url.trim();
                downloadUrl = (dl.startsWith('http://') || dl.startsWith('https://'))
                    ? dl
                    : `${baseUrl}/${dl.replace(/^\/+/, '')}`;
            } else if (ghRelease && ghRelease.browserDownloadUrl) {
                downloadUrl = ghRelease.browserDownloadUrl;
            } else {
                downloadUrl = `${baseUrl}/downloads/LloydAttendance-latest.apk`;
            }

            releaseNotes = (ghRelease && ghRelease.notes)
                || config.broadcast_notice
                || `Lloyd Attendance v${latestVersion} is now available with updated stability and features.`;
        }

        res.status(200).json({
            has_update: hasUpdate,
            latest_version: latestVersion,
            current_version: cleanClient.length > 0 ? cleanClient : latestVersion,
            download_url: downloadUrl,
            release_notes: releaseNotes,
            source: ghRelease ? 'github_releases' : 'fleet_governance'
        });
    } catch (err) {
        console.error('[API/OTA] Handler error:', err);
        res.status(200).json({
            has_update: false,
            latest_version: cleanVersion(req.query.current_version || ''),
            current_version: cleanVersion(req.query.current_version || ''),
            download_url: null,
            release_notes: 'Update check completed.',
            source: 'error_fallback'
        });
    }
};
