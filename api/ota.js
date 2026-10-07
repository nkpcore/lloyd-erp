/**
 * Lloyd ERP — Dynamic In-App OTA Update Resolution API (Vercel Serverless / Cloud)
 * Resolves releases dynamically from live fleet governance, server state, and GitHub Releases.
 * ZERO HARDCODED VERSIONS.
 */

const fs = require('fs');
const path = require('path');

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

function getKvCredentials() {
    let url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
    let token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
    if ((!url || !token) && (process.env.REDIS_URL || process.env.KV_URL)) {
        try {
            const raw = process.env.REDIS_URL || process.env.KV_URL;
            const parsed = new URL(raw);
            url = `https://${parsed.hostname}`;
            token = decodeURIComponent(parsed.password || parsed.username || '');
        } catch (_) {}
    }
    return { url, token };
}

async function callKv(command, ...args) {
    const { url, token } = getKvCredentials();
    if (!url || !token) return null;

    try {
        const resp = await fetch(url.replace(/\/+$/, ''), {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify([command, ...args])
        });
        if (!resp.ok) return null;
        const data = await resp.json();
        return data.result;
    } catch (e) {
        return null;
    }
}

// In-memory cache for GitHub release to stay well within rate limits
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

        const resp = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, { headers });
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
    } catch (e) {
        return null;
    }
}

async function getDynamicRelease() {
    // 1. Check KV config
    const kvData = await callKv('GET', 'lloyd_fleet_config');
    if (kvData) {
        try {
            const parsed = typeof kvData === 'string' ? JSON.parse(kvData) : kvData;
            if (parsed.latest_version_name && parsed.download_url) {
                return {
                    version: parsed.latest_version_name,
                    download_url: parsed.download_url,
                    notes: parsed.broadcast_notice || 'New Lloyd ERP update available.'
                };
            }
        } catch (e) {}
    }

    // 2. Check local disk config
    try {
        const localPath = path.join(process.cwd(), 'fleet_config.json');
        if (fs.existsSync(localPath)) {
            const raw = fs.readFileSync(localPath, 'utf8');
            const parsed = JSON.parse(raw);
            if (parsed.latest_version_name && parsed.download_url) {
                return {
                    version: parsed.latest_version_name,
                    download_url: parsed.download_url,
                    notes: parsed.broadcast_notice || 'New Lloyd ERP update available.'
                };
            }
        }
    } catch (e) {}

    return null;
}

async function getLiveFleetTelemetryVersion() {
    try {
        const kvValues = await callKv('HVALS', 'lloyd_fleet_telemetry');
        if (Array.isArray(kvValues) && kvValues.length > 0) {
            const records = kvValues.map(v => {
                try { return typeof v === 'string' ? JSON.parse(v) : v; } catch (e) { return null; }
            }).filter(Boolean);
            const sorted = records
                .map(r => ({ name: (r.app_version || '').trim(), code: parseInt(r.version_code) || 0 }))
                .filter(v => v.name || v.code > 0)
                .sort((a, b) => b.code - a.code);
            if (sorted.length > 0) return sorted[0].name || null;
        }

        const localPath = path.join(process.cwd(), 'fleet_telemetry.json');
        if (fs.existsSync(localPath)) {
            const raw = fs.readFileSync(localPath, 'utf8');
            const data = JSON.parse(raw);
            const list = Array.isArray(data) ? data : Object.values(data);
            const sorted = list
                .map(r => ({ name: (r.app_version || '').trim(), code: parseInt(r.version_code) || 0 }))
                .filter(v => v.name || v.code > 0)
                .sort((a, b) => b.code - a.code);
            if (sorted.length > 0) return sorted[0].name || null;
        }
    } catch (e) {}
    return null;
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

        // 1. If download action is requested, proxy private GitHub release asset or redirect
        if (isDownload) {
            const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
            const ghRelease = await getLiveGitHubRelease();
            if (ghRelease && ghRelease.assetUrl && token) {
                const assetResp = await fetch(ghRelease.assetUrl, {
                    headers: {
                        'User-Agent': 'LloydERP-OTA-Server',
                        'Authorization': `Bearer ${token}`,
                        'Accept': 'application/octet-stream'
                    },
                    redirect: 'manual'
                });
                const redirectLocation = assetResp.headers.get('location');
                if (redirectLocation) {
                    res.writeHead(302, { 'Location': redirectLocation });
                    res.end();
                    return;
                }
            }

            const dynamicRelease = await getDynamicRelease();
            if (dynamicRelease && dynamicRelease.download_url) {
                res.writeHead(302, { 'Location': dynamicRelease.download_url });
                res.end();
                return;
            }

            res.status(404).send('Release asset not found or not published yet.');
            return;
        }

        // 2. Query check for update
        const clientVersion = req.query.current_version || '';
        const ghRelease = await getLiveGitHubRelease();
        const dynamicRelease = await getDynamicRelease();
        const telemetryVersion = await getLiveFleetTelemetryVersion();

        // Dynamically compute latest version: GitHub Release > Admin Config > Telemetry Record
        const latestVersion = (ghRelease && ghRelease.version)
            || (dynamicRelease && dynamicRelease.version)
            || telemetryVersion
            || cleanVersion(clientVersion);

        let downloadUrl = null;
        let releaseNotes = 'You are running the latest version.';

        if (ghRelease && ghRelease.assetUrl) {
            const host = req.headers['x-forwarded-host'] || req.headers.host || '';
            const proto = req.headers['x-forwarded-proto'] || 'https';
            downloadUrl = host ? `${proto}://${host}/api/ota?download=latest` : `/api/ota?download=latest`;
            releaseNotes = ghRelease.notes;
        } else if (dynamicRelease && dynamicRelease.download_url) {
            downloadUrl = dynamicRelease.download_url;
            releaseNotes = dynamicRelease.notes;
        }

        const hasUpdate = Boolean(latestVersion && clientVersion && isNewerVersion(latestVersion, clientVersion) && downloadUrl);

        res.status(200).json({
            has_update: hasUpdate,
            latest_version: cleanVersion(latestVersion),
            current_version: cleanVersion(clientVersion),
            download_url: hasUpdate ? downloadUrl : null,
            release_notes: hasUpdate ? releaseNotes : 'You are running the latest version.',
            source: ghRelease ? 'github_release' : (dynamicRelease ? 'admin_config' : 'fleet_telemetry')
        });
    } catch (err) {
        res.status(200).json({
            has_update: false,
            latest_version: cleanVersion(req.query.current_version),
            current_version: cleanVersion(req.query.current_version),
            download_url: null,
            release_notes: 'You are running the latest version.'
        });
    }
};
