# Material Design 3 Expressive Design System
*(Synthesized from Google Material Design 3, hamen/material-3-skill, and UI/UX Pro Max)*

## 1. Design System Overview

The Lloyd Student user interface adheres to **Material Design 3 (M3) Expressive** standards. It rejects generic gradients, AI-generated placeholder templates, and emoji iconography in favor of crisp typography, semantic tonal surfaces, spring physics motion, and vector drawables.

---

## 2. Color System & Semantic Roles (Dark Theme Baseline)

| Token Name | Hex Code | Semantic Role & Component Usage |
| :--- | :--- | :--- |
| `surface` | `#0F172A` | Primary window background (Slate 900). |
| `surfaceContainerLowest` | `#0B0F19` | Deep recesses, background wells, search input backgrounds. |
| `surfaceContainerLow` | `#151F32` | Secondary low-emphasis containers. |
| `surfaceContainer` | `#1B243B` | Base container surface for cards and timeline period tiles. |
| `surfaceContainerHigh` | `#222E49` | Elevated surfaces, action cards, bottom navigation bar. |
| `surfaceContainerHighest` | `#2B3958` | Hover/pressed states, active navigation pills. |
| `primary` | `#818CF8` | Primary accent (Indigo 400), key interactive elements. |
| `primaryContainer` | `#312E81` | Filled pill background (Indigo 900). |
| `onPrimaryContainer` | `#E0E7FF` | High-contrast text on primary filled containers. |
| `secondary` | `#38BDF8` | Sky Blue 400 — Supplementary metrics and informational highlights. |
| `status_safe` | `#10B981` | Emerald 500 — Attendance $\ge 75.0\%$ or "Present" badge. |
| `status_danger` | `#EF4444` | Rose 500 — Attendance $< 75.0\%$ or "Absent" badge. |
| `status_warning` | `#F59E0B` | Amber 500 — Attendance within critical buffer ($75.0\% - 76.5\%$). |
| `outline` | `#475569` | Interactive element borders and active input strokes. |
| `outlineVariant` | `#334155` | 1dp subtle card border stroke (Slate 700). |

---

## 3. Typography Hierarchy (M3 Baseline & Emphasized Scale)

MD3 uses 15 baseline styles + 15 emphasized styles:

| Scale | Weight | Size (sp) | Line Height | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Display Large** | 400 / 600 | 57sp | 64sp | Key splash stats |
| **Display Medium** | 600 (Bold) | 45sp | 52sp | Hero attendance percentage (`tv_main_percentage`) |
| **Display Small** | 400 / 600 | 36sp | 44sp | Metric tile large numbers |
| **Headline Medium**| 600 | 28sp | 36sp | Section headers |
| **Headline Small** | 600 | 24sp | 32sp | Modal dialog headers, bottom sheet titles |
| **Title Medium**   | 500 (Med)  | 16sp | 24sp | Student name, subject card titles |
| **Title Small**    | 500 (Med)  | 14sp | 20sp | Period lecture headers |
| **Body Large**     | 400 (Reg)  | 16sp | 24sp | Primary instructional body copy |
| **Body Medium**    | 400 (Reg)  | 14sp | 20sp | Descriptive copy, timeline notes |
| **Body Small**     | 400 (Reg)  | 12sp | 16sp | Secondary metadata, faculty notes |
| **Label Large**    | 500 (Med)  | 14sp | 20sp | Interactive button labels, tab text |
| **Label Medium**   | 500 (Med)  | 12sp | 16sp | Filter chips, tag labels |
| **Label Small**    | 500 (Med)  | 11sp | 16sp | Status badges, timestamps, lecture numbers |

---

## 4. Shape Scale (M3 Tokens)

| Token | Radius | Applied Components |
| :--- | :--- | :--- |
| `shape.none` | 0dp | Edge-to-edge dialogs |
| `shape.extraSmall` | 4dp | Status indicator dots, subtle badges |
| `shape.small` | 8dp | Input text fields, time duration chips |
| `shape.medium` | 12dp | Timeline cards, subject list items |
| `shape.large` | 16dp | Elevated dashboard cards |
| `shape.extraLarge` | 28dp | Bottom sheets (`dialog_subject_details`), login surface |
| `shape.full` | 9999dp | Pill buttons, filter chips, navigation active indicator |

---

## 5. Spacing Rhythm (8dp Grid System)

Following Google I/O 2026 Material 3 and UI/UX Pro Max:
- **Micro spacing**: `4dp` (between icon and text inside a badge).
- **Component padding**: `8dp` or `12dp` (card internal elements).
- **Content gutters**: `16dp` horizontal margin on phones, `24dp` on tablets.
- **Section rhythm**: `16dp` / `24dp` between distinct card groups.
- **Touch target minimum**: $\ge 48\text{dp} \times 48\text{dp}$ on all interactive buttons and chips.

---

## 6. Vector Iconography (Strictly No Emojis)

Every icon is an Android Vector Drawable (`VectorDrawable` XML) with explicit scalable viewports:
- Dashboard: `ic_dashboard.xml` (Geometric 4-pane grid)
- Schedule: `ic_calendar_today.xml` (Minimal calendar frame)
- Attendance History: `ic_history.xml` (Clockwise circular history arrow)
- Simulator & Tuning: `ic_tune.xml` (Slider adjustment switches)
- Search: `ic_search.xml` (Magnifying glass)
- Share / Export: `ic_share.xml` (Branching share node)
- Close / Clear: `ic_close.xml` (Crisp cross)
- Present Check: `ic_check.xml` (Geometric checkmark)
- Shortage Alert: `ic_alert.xml` (Triangle warning outline)
- Privacy / Stealth: `ic_visibility.xml` / `ic_visibility_off.xml`
- Academic / College: `ic_school.xml` (Mortarboard graduation cap)

---

## 7. Motion & Spring Physics (M3 Expressive)

- **Emphasized Enter**: 500ms duration with `PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f)` for bottom sheets and screen entrances.
- **Emphasized Exit**: 200ms duration with `PathInterpolator(0.3f, 0.0f, 0.8f, 0.15f)` for dismissed cards.
- **Number Rolling Animation**: 600ms linear-out-slow-in interpolator for percentage counters.
- **Breathing Pulse**: 1500ms infinite loop for ongoing classroom pulse (`scale 1.0` to `1.2`, `alpha 0.4` to `1.0`).
- **Reduced Motion**: If user enables `AccessibilityManager.isTouchExplorationEnabled()` or prefers reduced motion, animations immediately resolve to their terminal state with zero duration.
