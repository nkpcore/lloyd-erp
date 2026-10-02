// Variables used by Scriptable.
// These must be at the very top of the file. Do not edit.
// icon-color: indigo; icon-glyph: graduation-cap;

/**
 * Lloyd College Attendance iOS Widget (for Scriptable)
 * Instructions:
 * 1. Download the free "Scriptable" app from the App Store on iOS.
 * 2. Create a new script, paste this entire file, and fill in your USERNAME and PASSWORD below.
 * 3. Go to your iOS Home Screen, long press, tap "+", search "Scriptable", add a Medium or Small widget, and select this script!
 */

const USERNAME = "YOUR_ADMISSION_NO"; // e.g. "2023LLOYD1234"
const PASSWORD = "YOUR_PASSWORD";

const API_BASE = "https://erp.lloydcollege.in/api";

async function createWidget() {
  const widget = new ListWidget();
  widget.backgroundColor = new Color("#0F172A");

  const header = widget.addStack();
  header.centerAlignContent();

  const title = header.addText("🎓 LLOYD ERP");
  title.font = Font.boldSystemFont(11);
  title.textColor = new Color("#94A3B8");

  header.addSpacer();

  const timeText = header.addDate(new Date());
  timeText.applyTimeStyle();
  timeText.font = Font.systemFont(10);
  timeText.textColor = new Color("#64748B");

  widget.addSpacer(8);

  try {
    const data = await fetchAttendance();
    const pct = data.overallPercentage;
    const isSafe = pct >= 75.0;

    const mainStack = widget.addStack();
    mainStack.centerAlignContent();

    const pctText = mainStack.addText(`${pct.toFixed(1)}%`);
    pctText.font = Font.boldSystemFont(32);
    pctText.textColor = isSafe ? new Color("#10B981") : new Color("#EF4444");

    mainStack.addSpacer(12);

    const infoStack = mainStack.addStack();
    infoStack.layoutVertically();

    const badge = infoStack.addText(isSafe ? "SAFE (≥75%)" : "SHORTAGE (<75%)");
    badge.font = Font.boldSystemFont(10);
    badge.textColor = isSafe ? new Color("#34D399") : new Color("#F87171");

    const classesText = infoStack.addText(`${data.present} / ${data.total} attended`);
    classesText.font = Font.systemFont(11);
    classesText.textColor = new Color("#94A3B8");

    widget.addSpacer(8);

    const bunkStack = widget.addStack();
    bunkStack.backgroundColor = new Color("#1E293B");
    bunkStack.cornerRadius = 8;
    bunkStack.setPadding(6, 10, 6, 10);
    bunkStack.centerAlignContent();

    let bunkAdvice = "";
    if (isSafe) {
      const canBunk = Math.floor((data.present - 0.75 * data.total) / 0.75);
      bunkAdvice = canBunk > 0 ? `Can safely miss ${canBunk} class${canBunk > 1 ? "es" : ""}` : "Safe! Attend next class";
    } else {
      const needed = Math.ceil((0.75 * data.total - data.present) / 0.25);
      bunkAdvice = `Must attend ${needed} class${needed > 1 ? "es" : ""} in a row`;
    }

    const adviceText = bunkStack.addText(bunkAdvice);
    adviceText.font = Font.boldSystemFont(11);
    adviceText.textColor = new Color("#F1F5F9");

  } catch (err) {
    const errText = widget.addText("Tap to check credentials in Scriptable");
    errText.font = Font.systemFont(12);
    errText.textColor = new Color("#F87171");
  }

  return widget;
}

async function fetchAttendance() {
  if (USERNAME === "YOUR_ADMISSION_NO" || PASSWORD === "YOUR_PASSWORD") {
    throw new Error("Please enter your actual Lloyd ERP credentials in this script.");
  }

  // 1. Login
  const loginReq = new Request(`${API_BASE}/auth/login`);
  loginReq.method = "POST";
  loginReq.headers = { "Content-Type": "application/json" };
  loginReq.body = JSON.stringify({
    username: USERNAME,
    password: PASSWORD,
    device_id: "ios-scriptable-widget",
    app_version: "2.0.0",
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

  // 2. Fetch monthly attendance
  const attReq = new Request(`${API_BASE}/student/me/monthly-attendance`);
  attReq.headers = { "Authorization": `Bearer ${token}` };
  const attRes = await attReq.loadJSON();

  let present = 0;
  let total = 0;
  const months = attRes.data?.months || [];
  for (const m of months) {
    present += (m.present || 0);
    total += (m.total || 0);
  }

  const pct = total > 0 ? (present / total) * 100 : 100;
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
