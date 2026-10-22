# GDG CoreHQ — UI/UX Design System

> **Purpose:** Visual and interaction specification for Antigravity to implement the existing GDG CoreHQ Android application.
>
> **Reference:** User-provided three-screen health dashboard mockup. Reuse its visual language—soft pastel cards, near-white surfaces, rounded geometry, restrained shadows, bold readable typography, floating pill navigation, and clean chart treatments—but adapt every screen to GDG CoreHQ's existing PRD.
>
> **Scope lock:** This document changes presentation only. Do not introduce new screens, roles, features, or workflows. Implement only the existing GDG CoreHQ PRD features.

---

## 1. Product and implementation rules

- Product name: **GDG CoreHQ**
- Product context: private internal operations app for GDGoC TKIET.
- Roles: **Super Admin** (Organizer and Community Manager) and **Core Member**.
- Required existing capabilities: login and role routing; member profile; QR attendance; tasks/Kanban; meeting agendas; post-meeting report; reimbursements/receipt capture; attendance and strike status; Admin strike dashboard.
- Follow the actual project framework found in the repository. The original PRD mentions both Java/XML and Kotlin/Jetpack Compose. **Do not migrate frameworks** merely to match this document.
- Before implementation, inspect current files, dependencies, navigation, Firebase configuration, and existing screens. Reuse working code and components.
- Do not add public sign-up, chat, calendar integrations, analytics modules, social feeds, new dashboards, or any other unlisted feature.
- Do not use emojis as UI icons. Use vector icons already in the project or a consistent Material icon library.
- Do not copy health-specific labels, metrics, charts, or content from the reference image. Only its visual style is a reference.

## 2. Design direction

**Keywords:** calm, modern, pastel, structured, friendly, operational, polished Android.

The reference design uses:
- A light grey/off-white background.
- White content surfaces with large corner radii.
- Pastel pink, lavender, mint, peach, and coral cards.
- High-contrast near-black text.
- A clear hierarchy with large key values and compact secondary labels.
- Thin, low-contrast dividers and borders.
- Small, consistent outline icons.
- Floating rounded navigation with a selected-item background.
- Generous spacing and uncluttered layouts.

For GDG CoreHQ, keep the same warmth while making admin actions, task states, attendance confirmation, expense approvals, and strike warnings easy to distinguish. Pastels should support the information hierarchy, not reduce contrast.

## 3. Color tokens

Use these as the initial design tokens. If the repository already has a theme system, map these tokens into it rather than creating a competing theme.

### 3.1 Core palette

| Token | Hex | Use |
|---|---|---|
| `color.background` | `#F5F6FA` | App background |
| `color.surface` | `#FFFFFF` | Main cards, sheets, input surfaces |
| `color.surface.subtle` | `#F0F1F6` | Secondary containers |
| `color.text.primary` | `#171923` | Headings and primary content |
| `color.text.secondary` | `#666A78` | Descriptions and metadata |
| `color.text.muted` | `#8B8FA0` | Timestamps and low-priority metadata |
| `color.border` | `#E5E7EF` | Outlines and dividers |
| `color.brand.blue` | `#C8D8FF` | Soft blue emphasis |
| `color.brand.lavender` | `#D8D4FF` | Highlight cards |
| `color.brand.pink` | `#F0C5C8` | Soft pink cards |
| `color.brand.peach` | `#FFE1C9` | Secondary highlights |
| `color.brand.mint` | `#BFE9E3` | Positive/complete states |
| `color.brand.coral` | `#F5A0A5` | High-attention state backgrounds |
| `color.brand.navy` | `#18233F` | Strong headers and dark emphasis |

### 3.2 Semantic status colors

Status must be communicated through text/icon plus color, never color alone.

| Token | Hex | Use |
|---|---|---|
| `color.success` | `#247A55` | Confirmed attendance, approved expenses, completed tasks |
| `color.success.container` | `#DDF4E8` | Soft success background |
| `color.warning` | `#8A5A00` | Pending review and approaching deadlines |
| `color.warning.container` | `#FFF0C7` | Soft warning background |
| `color.error` | `#B42332` | Invalid scans, failed actions, strike warnings |
| `color.error.container` | `#FCE1E3` | Error background and 3+ strike highlight |
| `color.info` | `#315DA8` | Informational labels |
| `color.info.container` | `#E4ECFF` | Information background |

### 3.3 Color rules

- Use the neutral background for the page and white surfaces for most content.
- Pastel cards may distinguish existing feature groups, but avoid making every small component a different color.
- Use coral/red primarily for errors and the PRD-defined 3+ strike Admin flag.
- Do not use the same color as the sole signal for task status; include a label.
- Maintain readable text contrast. Use dark text on pastel surfaces.
- Do not use gradients unless already established in the project.

## 4. Typography

The reference uses a clean modern sans-serif with bold headings and readable numeric emphasis.

### Preferred font
1. Use **Roboto** if the project is a native Android application and has no established typeface.
2. If the repository already uses a coherent font family, preserve it unless the user approves a change.
3. Do not bundle a new font file without a clear need.

### Type scale

| Style token | Size | Weight | Use |
|---|---:|---|---|
| `type.display` | 28 sp | Bold | One key value or page-level emphasis only |
| `type.headline` | 24 sp | Bold | Screen title |
| `type.title.large` | 20 sp | SemiBold/Bold | Main section heading |
| `type.title.medium` | 16 sp | SemiBold | Card title |
| `type.body.large` | 16 sp | Regular | Main content |
| `type.body` | 14 sp | Regular | Standard body and input text |
| `type.label` | 12 sp | Medium | Metadata and compact labels |
| `type.caption` | 11 sp | Regular/Medium | Timestamps and helper text |

- Use `sp` for text sizing and respect Android font scaling.
- Keep labels concise and sentence case.
- Use bold text sparingly for hierarchy, not for every label.
- Never truncate important values such as member names, PRNs, amounts, task status, or strike count without a way to inspect them.
- Avoid all-caps labels except short, conventional status chips if already used consistently.

## 5. Layout, spacing, and shape

### Spacing scale
Use an 8 dp base grid, with 4 dp for fine adjustments.

| Token | Value |
|---|---:|
| `space.1` | 4 dp |
| `space.2` | 8 dp |
| `space.3` | 12 dp |
| `space.4` | 16 dp |
| `space.5` | 20 dp |
| `space.6` | 24 dp |
| `space.8` | 32 dp |

### Corner radii
- Small controls and chips: 10–12 dp.
- Standard cards: 18–22 dp.
- Feature/highlight cards: 24 dp.
- Bottom navigation container: fully rounded pill where supported by the current navigation structure.
- Buttons: 14–18 dp, or pill-shaped for primary actions if it fits the established UI.

### Elevation and borders
- Prefer subtle borders and soft elevation over heavy shadows.
- Standard card: 1 dp border using `color.border`, with low elevation only if needed.
- Avoid thick outlines, hard black shadows, and excessive glassmorphism.
- Separate sections with spacing rather than many divider lines.

### Screen padding
- Standard horizontal page padding: 16 dp.
- Large-screen content may use 20–24 dp.
- Card internal padding: 16 dp.
- Keep content scrollable on smaller screens and avoid fixed-height cards that clip content.

## 6. Icons and illustration

- Use a single icon family and consistent stroke weight.
- Prefer existing Material Symbols/Material Icons or the icon library already installed.
- Typical icon size: 20–24 dp; compact metadata icons: 16 dp.
- Use icons only when they reinforce an action or category.
- Never use emoji glyphs for navigation, attendance, tasks, expenses, strikes, or notifications.
- Avoid decorative illustrations that consume space without supporting an existing workflow.

## 7. Component specification

### 7.1 App bars
- White or background-colored app bar with no heavy shadow.
- Show a clear screen title and only the actions already required on that screen.
- Back navigation appears on secondary screens according to existing navigation.
- Keep action icons in 40–44 dp touch targets.
- Do not invent global search, profile shortcuts, or settings actions unless they already exist in the code/PRD.

### 7.2 Cards
- White cards for dense operational data.
- Pastel cards for key summaries or grouped existing features.
- Standard radius: 20 dp.
- Internal layout: icon/title row, main content, then optional action/status row.
- Use consistent padding and align card titles and values across a grid.
- Avoid excessive card nesting.

### 7.3 Buttons
- Primary: dark navy or a strong accessible brand color with high-contrast text.
- Secondary: white/surface with a subtle border.
- Destructive: semantic error color and explicit action wording.
- Minimum touch target: 48 × 48 dp.
- Use clear verbs: “Scan QR”, “Confirm Attendance”, “Submit Expense”, “Approve”, “Reject”, “Publish Agenda”.
- Show a loading state while a backend operation is pending; only show success after confirmation.

### 7.4 Inputs and forms
- White surface, subtle border, 12–14 dp corner radius.
- Label above the field; helper/error text below when required.
- Keep labels visible when the user types.
- Show validation beside the relevant field.
- Do not erase entered form data after a transient failure.
- Required fields must be visibly marked.

### 7.5 Status chips
Use compact rounded chips for existing states:
- Task: To-Do, In Progress, Done.
- Expense: Pending, Approved/Reimbursed, Rejected.
- Attendance: Present, Absent.
- Strike: 0, 1, 2, 3+ as applicable.

Chip backgrounds use pale semantic colors; text remains dark and readable. Always show the state text.

### 7.6 Empty, loading, and error states
Within existing screens only:
- Loading: small progress indicator and concise contextual label.
- Empty: simple icon, one-line explanation, and an existing action if appropriate.
- Error: clear explanation and retry action when retry is safe.
- Never display fake sample records in production.
- Distinguish no data from a failed network request.

### 7.7 Floating bottom navigation
The reference uses a floating rounded navigation bar. Reuse this style only if it fits the current app navigation and does not require adding destinations.
- White pill container with subtle shadow/border.
- Selected destination gets a pale lavender or blue rounded background.
- Icons use the same family and consistent dimensions.
- Navigation destinations must map only to existing screens.
- Keep the bar above the system gesture area and ensure it does not cover scrollable content.
- If the app already uses a different working navigation architecture, restyle it without adding tabs.

## 8. Existing screen design guidance

These are styling instructions for PRD screens, not permission to add screens.

### 8.1 Login
- Calm neutral background, white rounded form surface.
- Display the GDG CoreHQ name using the existing project branding.
- Username/ID and password fields with clear labels.
- Primary sign-in button.
- Password reset path only if implemented by the configured authentication flow.
- No public “Sign Up” link.
- Show authentication errors in context.

### 8.2 Admin Dashboard
- Use a concise welcome/title area and compact cards linking to existing Admin capabilities.
- Prioritize QR Attendance, Tasks, Meeting Agendas, Reimbursements, and Strike Dashboard.
- Use the pastel palette to distinguish these existing feature groups without adding new metrics or modules.
- Any counts shown must come from real backend data; do not hard-code sample numbers.
- The dashboard must not duplicate full workflows that belong on their existing screens.

### 8.3 Member Dashboard
- Clear welcome/title area and existing profile/role context.
- Use cards or grouped rows for existing member tasks, upcoming agendas, attendance/post-meeting action, expenses, and strike status.
- Highlight an outstanding required post-meeting report only when the backend indicates it is due.
- Do not add health-style progress rings, calorie metrics, activity summaries, or other unrelated widgets from the reference image.

### 8.4 Profile and strike status
- Present name, core role, current tasks, attendance status/history available in the existing implementation, and strike count.
- Keep strike count legible.
- Use a neutral/soft card for 0 strikes, warning styling for 1–2, and the PRD-defined red highlight for 3+ in the Admin view.
- Only Admins can access strike-management actions.

### 8.5 QR Attendance Scanner
- Make the scanner area clear and uncluttered.
- Display the scan result and member name/role in a white rounded confirmation panel.
- Use a distinct invalid/duplicate state with a concise reason.
- Provide an explicit confirmation action before writing attendance.
- Disable repeated submission while a write is pending.
- Do not expose a scanner entry point to Core Members.
- Follow actual ML Kit integration and camera permission handling in the existing app.

### 8.6 Tasks and Kanban
- Use clearly separated To-Do, In Progress, and Done columns/sections as supported by the current screen and device width.
- On narrow devices, allow horizontal scrolling or a compact status-group layout; do not cause card overflow.
- Each task card shows existing task information, deadline, assignee where permitted, and status.
- Make status changes easy to understand and confirm backend success.
- Admin sees the global board; members see their own tasks plus the existing Global View.

### 8.7 Meeting Agendas
- Show meeting topic/title, date/time, location, and agenda details already supported by the data model.
- Use a calm white card with a small lavender/blue accent.
- Make the agenda easy to scan; avoid decorative charts or unrelated statistics.
- Notifications should deep-link only to the existing agenda view if such navigation is supported.

### 8.8 Post-Meeting Form
- Simple, focused form with fields for weekly progress, roadblocks, and meeting feedback.
- Link the relevant meeting/agenda data as required by the PRD.
- Use visible labels, adequate multiline input space, and a clear submit action.
- Show confirmation only after the report is successfully stored.
- Return to the existing dashboard after successful submission as specified in the PRD.

### 8.9 Expenses and reimbursement
- Use a clear receipt preview area and a prominent capture action.
- Amount, date, and category use consistent form fields.
- Expense cards show amount, date, category, and current review status.
- Admin review displays the receipt and relevant submitted details.
- Approve and Reject actions must be visually distinct.
- The rejection flow requires a reason; do not permit submission without it.
- Use the pastel palette for grouping, not to obscure financial values or status.

### 8.10 Strike Dashboard
- Use a clear list of members and their strike counts, based on real data.
- Highlight 3+ strike members in red as specified in the PRD.
- Show the existing reason/event context needed for Admin review.
- Keep pardon/remove-strike actions restricted to authorized Admins.
- Any removal should update the backend and visible count only after success.

## 9. Responsive behavior and accessibility

- Design for common Android phone widths first.
- Respect system status and navigation bars, camera cutouts, and keyboard insets.
- Prevent horizontal overflow, clipped text, and controls hidden behind navigation.
- Use a minimum 48 dp touch target for interactive controls.
- Maintain sufficient contrast between text and pastel backgrounds.
- Do not communicate state by color alone.
- Support Android font scaling; test larger text sizes.
- Provide meaningful content descriptions for icons and controls.
- Keep focus order logical and inputs usable with accessibility services.
- Test portrait layouts first; support landscape only if the existing app requires it.

## 10. Motion and feedback

- Keep transitions subtle and brief.
- Use small state transitions for card selection, status changes, and loading feedback only where appropriate.
- Do not add decorative animations that slow operational tasks.
- Prevent double taps from submitting duplicate attendance or expense actions.
- Display success/failure feedback based on actual backend results.
- Respect reduced-motion/system animation preferences where applicable.

## 11. Data integrity and backend-aware UI

- Firebase remains the backend specified by the PRD unless the repository proves a different existing configuration.
- Enforce authorization in backend rules as well as in the UI.
- Never rely solely on hidden buttons to secure Admin actions.
- Invalid PRNs and duplicate attendance must be rejected.
- Expense rejection requires a reason.
- Strike triggers must not be processed repeatedly for the same event.
- Display real data and explicit loading/error states.
- Do not invent Firebase configuration, credentials, or backend fields. If configuration is missing, document the blocker.

## 12. Design tokens and implementation organization

Where compatible with the existing project, centralize:
- Colors and semantic status colors.
- Typography styles.
- Spacing and corner radii.
- Common card, button, input, and status-chip styles.
- Icon sizing and touch-target dimensions.

Do not duplicate design constants across every screen. Follow the repository's existing resource/theme conventions (for example, Android XML resources or Compose theme files) instead of imposing a new architecture.

## 13. Antigravity implementation workflow

1. Inspect the repository and report the actual language, UI framework, navigation pattern, design system, and Firebase setup.
2. Map each existing screen to this design specification and the functional PRD.
3. Identify reusable components and existing design tokens before changing code.
4. Implement the palette, typography, spacing, shapes, and components centrally.
5. Restyle the existing screens without changing the approved product scope.
6. Preserve authentication, role-based permissions, Firestore reads/writes, scanner validation, and existing workflows.
7. Build and test after each logical group of changes.
8. Check every screen for overflow, clipped labels, inaccessible touch targets, contrast problems, and navigation overlap.
9. Do not replace real functionality with static mockups or hard-coded values.
10. Report modified files, build/test results, screenshots if available, known issues, and remaining setup requirements.

## 14. Acceptance checklist

- [ ] The UI uses the soft pastel visual direction of the supplied reference without copying its health-specific content.
- [ ] Core palette, typography, spacing, corner radii, and component styles are consistent.
- [ ] The actual existing project framework is preserved.
- [ ] All PRD-required screens and workflows remain present.
- [ ] No unapproved screens, roles, features, or navigation destinations are introduced.
- [ ] No emoji icons are used.
- [ ] Text remains readable on pastel surfaces and at larger font scales.
- [ ] No card or navigation overflow occurs on supported phone sizes.
- [ ] Admin-only actions are protected by backend authorization.
- [ ] Loading, empty, success, and error states reflect actual backend results.
- [ ] QR duplicate/invalid handling, reimbursement rejection validation, and strike thresholds remain correct.
- [ ] The app builds and existing workflows pass available tests.

---

## Final instruction

**Treat the uploaded reference image as visual inspiration only. Treat the GDG CoreHQ PRD as the source of truth for product scope.** Apply the visual system to the existing app and do not add any screen, role, feature, or workflow that is not already specified.
