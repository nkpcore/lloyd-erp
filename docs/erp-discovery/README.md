# Lloyd ERP — Complete API & Data Surface Discovery

**Target Host:** `https://erp.lloydcollege.in`  
**Base API URL:** `https://erp.lloydcollege.in/api`  
**Repository:** `nkpcore/lloyd-erp`  
**Classification Standard:** `VERIFIED`, `INFERRED`, `UNKNOWN`  

---

## 1. Executive Summary of Discovery

This discovery report documents the authenticated API and data surface of the Lloyd College ERP (`erp.lloydcollege.in`), mapped to support the development of a real-data Android companion application (`nkpcore/lloyd-erp`).

A foundational mandate of this assessment is **Zero Mock Data** (Section 26):
The target Android application must interface strictly with **real ERP endpoints**, serialize **authentic academic models**, store data in **local offline caches**, and deliver high-performance native user interfaces.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        DATA ARCHITECTURE PIPELINE                      │
└────────────────────────────────────────────────────────────────────────┘

    REAL LLOYD ERP (HTTPS)
              │
              │ TLS 1.3 / Bearer JWT
              ▼
    REMOTE OKHTTP CLIENT (ErpApiClient)
              │
              │ Structured Deserialization (Gson)
              ▼
    UNIFIED REPOSITORY (AttendanceRepository)
              │
              ├──► LOCAL DISK CACHE (Room DB / EncryptedStore)
              │           │
              │           ▼ (Instant < 30ms paint)
              ▼           │
    DOMAIN ENGINE ────────┼──► ANDROID UI (MainActivity 4-Tab MD3)
    (Bunk & Goal Maths)   ├──► HOME SCREEN WIDGET (RemoteViews)
                          └──► HEADS-UP ALERTS (Dynamic Island)
```

---

## 2. Directory Structure of Discovery Reports

- **[`ENDPOINTS.md`](file:///C:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/ENDPOINTS.md)**: Exhaustive technical catalog of verified API endpoints, request schemas, parameters, and response structures.
- **[`CAPABILITY_MATRIX.md`](file:///C:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/CAPABILITY_MATRIX.md)**: Full audit matrix tracking 19 potential ERP modules across `Verified`, `Partially Verified`, `Unknown`, and `Not Available`.
- **[`DATA_MODEL.md`](file:///C:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/DATA_MODEL.md)**: Canonical relational data models and domain entity specifications.
- **[`ANDROID_FEATURE_MAP.md`](file:///C:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/ANDROID_FEATURE_MAP.md)**: Prioritized feature mapping for native Android integration (priority, offline capability, widget and notification integration).
- **[`SYNC_STRATEGY.md`](file:///C:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/SYNC_STRATEGY.md)**: Zero-latency offline-first synchronization architecture and WorkManager diff detection.
- **[`LIMITATIONS.md`](file:///C:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/LIMITATIONS.md)**: Explicit boundaries detailing what could not be tested and modules lacking student endpoints.
- **[`VERIFIED_FEATURES.md`](file:///C:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/VERIFIED_FEATURES.md)**: Deep dive into the four verified student feature domains with concrete wire evidence.
