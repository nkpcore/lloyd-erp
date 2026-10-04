// Variables used by Scriptable.
// These must be at the very top of the file. Do not edit.
// icon-color: indigo; icon-glyph: graduation-cap;

/**
 * Lloyd College ERP Attendance iOS Widget (Material Design 3 Expressive)
 *
 * Secure Token Storage:
 * Uses iOS Keychain via Scriptable's Keychain API to avoid storing plaintext passwords.
 * If credentials are not stored in Keychain, configure USERNAME and PASSWORD below for initial run,
 * and the script will securely save your session token to iOS Keychain.
 */

const USERNAME = "YOUR_ADMISSION_NO"; // e.g. "2023LLOYD1234"
const PASSWORD = "YOUR_PASSWORD";

const API_BASE = "https://erp.lloydcollege.in/api";
const KEYCHAIN_TOKEN_KEY = "lloyd_erp_access_token";

// Material Design 3 Color Tokens (Dark Theme Palette)
const MD3 = {
  surface: new Color("#0B0F19"),
  surfaceContainer: new Color("#1E293B"),
  surfaceContainerHigh: new Color("#334155"),
  primary: new Color("#93C5FD"),
  onPrimary: new Color("#1E3A8A"),
  secondary: new Color("#5EEAD4"),
  onSurface: new Color("#F1F5F9"),
  onSurfaceVariant: new Color("#94A3B8"),
  outlineVariant: new Color("#334155"),
  healthyText: new Color("#34D399"),
  healthyContainer: new Color("#064E3B"),
  borderlineText: new Color("#FBBF24"),
  borderlineContainer: new Color("#78350F"),
  criticalText: new Color("#F87171"),
  criticalContainer: new Color("#7F1D1D"),
  unrecordedText: new Color("#94A3B8"),
  unrecordedContainer: new Color("#1E293B")
};

async function createWidget() {
  const widget = new ListWidget();
  widget.backgroundColor = MD3.surface;

  // Header Stack
  const header = widget.addStack();
  header.centerAlignContent();

  const title = header.addText("🎓 LLOYD ERP");
  title.font = Font.boldSystemFont(11);
  title.textColor = MD3.primary;

  header.addSpacer();

  const timeText = header.addDate(new Date());
  timeText.applyTimeStyle();
  timeText.font = Font.systemFont(10);
  timeText.textColor = MD3.onSurfaceVariant;

  widget.addSpacer(8);

  try {
    const data = await fetchAttendance();
    const hasData = data.total > 0;
    const pct = data.overallPercentage;

    let healthText = MD3.unrecordedText;
    let healthContainer = MD3.unrecordedContainer;
    let badgeTitle = "NO RECORDS";
    let pctDisplay = "--.-%";
    let bunkAdvice = "No classes recorded yet";

    if (hasData) {
      pctDisplay = `${pct.toFixed(1)}%`;
      if (pct >= 75.0) {
        healthText = MD3.healthyText;
        healthContainer = MD3.healthyContainer;
        badgeTitle = "SAFE (≥75%)";
        const canBunk = Math.max(0, Math.floor((100 * data.present - 75 * data.total) / 75));
        bunkAdvice = canBunk > 0
          ? `Can safely miss ${canBunk} class${canBunk > 1 ? "es" : ""}`
          : "Safe at 75%! Attend next class";
      } else if (pct >= 65.0) {
        healthText = MD3.borderlineText;
        healthContainer = MD3.borderlineContainer;
        badgeTitle = "BORDERLINE (65-74%)";
        const needed = Math.max(0, Math.ceil((75 * data.total - 100 * data.present) / 25));
        bunkAdvice = `Must attend next ${needed} class${needed > 1 ? "es" : ""}`;
      } else {
        healthText = MD3.criticalText;
        healthContainer = MD3.criticalContainer;
        badgeTitle = "CRITICAL (<65%)";
        const needed = Math.max(0, Math.ceil((75 * data.total - 100 * data.present) / 25));
        bunkAdvice = `Shortage! Attend next ${needed} class${needed > 1 ? "es" : ""}`;
      }
    }

    const mainStack = widget.addStack();
    mainStack.centerAlignContent();

    const pctText = mainStack.addText(pctDisplay);
    pctText.font = Font.boldSystemFont(32);
    pctText.textColor = hasData ? healthText : MD3.onSurfaceVariant;

    mainStack.addSpacer(12);

    const infoStack = mainStack.addStack();
    infoStack.layoutVertically();

    const badge = infoStack.addText(badgeTitle);
    badge.font = Font.boldSystemFont(10);
    badge.textColor = healthText;

    const classesText = infoStack.addText(
      hasData ? `${data.present} / ${data.total} attended` : "0 / 0 recorded"
    );
    classesText.font = Font.systemFont(11);
    classesText.textColor = MD3.onSurfaceVariant;

    widget.addSpacer(8);

    const bunkStack = widget.addStack();
    bunkStack.backgroundColor = MD3.surfaceContainer;
    bunkStack.cornerRadius = 8;
    bunkStack.setPadding(6, 10, 6, 10);
    bunkStack.centerAlignContent();

    const adviceText = bunkStack.addText(bunkAdvice);
    adviceText.font = Font.boldSystemFont(11);
    adviceText.textColor = MD3.onSurface;

  } catch (err) {
    const errStack = widget.addStack();
    errStack.layoutVertically();

    const errTitle = errStack.addText("Authentication Required");
    errTitle.font = Font.boldSystemFont(12);
    errTitle.textColor = MD3.criticalText;

    const errDesc = errStack.addText("Set credentials in Scriptable");
    errDesc.font = Font.systemFont(10);
    errDesc.textColor = MD3.onSurfaceVariant;
  }

  return widget;
}

async function getOrRenewToken() {
  if (Keychain.contains(KEYCHAIN_TOKEN_KEY)) {
    const cachedToken = Keychain.get(KEYCHAIN_TOKEN_KEY);
    if (cachedToken && cachedToken.length > 10) {
      return cachedToken;
    }
  }

  if (USERNAME === "YOUR_ADMISSION_NO" || PASSWORD === "YOUR_PASSWORD") {
    throw new Error("Credentials missing. Update USERNAME and PASSWORD.");
  }

  const loginReq = new Request(`${API_BASE}/auth/login`);
  loginReq.method = "POST";
  loginReq.headers = { "Content-Type": "application/json" };
  loginReq.body = JSON.stringify({
    username: USERNAME,
    password: PASSWORD,
    device_id: "ios-scriptable-md3",
    app_version: "2.1.0",
    timezone: "Asia/Kolkata",
    browser_name: "Scriptable",
    browser_version: "1.0",
    os_name: "iOS",
    device_type: "mobile"
  });

  const loginRes = await loginReq.loadJSON();
  if (!loginRes.status || !loginRes.data || !loginRes.data.access_token) {
    throw new Error(loginRes.message || "Login failed");
  }

  const token = loginRes.data.access_token;
  Keychain.set(KEYCHAIN_TOKEN_KEY, token);
  return token;
}

async function fetchAttendance() {
  let token = await getOrRenewToken();

  let attReq = new Request(`${API_BASE}/student/me/monthly-attendance`);
  attReq.headers = { "Authorization": `Bearer ${token}` };
  let attRes = await attReq.loadJSON();

  // If token expired, clear from keychain and retry once
  if (attRes.status === 401 || (attRes.message && attRes.message.includes("Unauthenticated"))) {
    if (Keychain.contains(KEYCHAIN_TOKEN_KEY)) {
      Keychain.remove(KEYCHAIN_TOKEN_KEY);
    }
    token = await getOrRenewToken();
    attReq = new Request(`${API_BASE}/student/me/monthly-attendance`);
    attReq.headers = { "Authorization": `Bearer ${token}` };
    attRes = await attReq.loadJSON();
  }

  let present = 0;
  let total = 0;
  const months = attRes.data?.months || [];
  for (const m of months) {
    present += (m.present || 0);
    total += (m.total || 0);
  }

  const pct = total > 0 ? (present / total) * 100 : 0;
  return { overallPercentage: pct, present, total };
}

if (config.runsInWidget) {
  const widget = await createWidget();
  Scriptable.setWidget(widget);
} else {
  const widget = await createWidget();
  widget.presentMedium();
}
Scriptable.complete();
