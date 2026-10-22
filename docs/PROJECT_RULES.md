# GDG CoreHQ — Project Rules for Antigravity

**Applies to:** GDG CoreHQ Android app and any existing companion website, if present.  
**Priority:** `PRD.md` defines features and screens; `design.md` defines visual appearance; this document defines implementation quality and release checks.

## 0. Non-negotiable scope and execution rules

1. Inspect the repository before editing. Identify whether each rule applies to the native Android app, an existing web frontend, backend infrastructure, or not at all.
2. **Do not add new product screens, features, roles, navigation destinations, or business workflows.** This includes legal pages and website infrastructure unless a web surface already exists and the owner authorizes the addition.
3. Fix bugs in existing functionality; do not silently replace it with mocks or placeholders.
4. Preserve existing Java/XML or other actual project stack. Do not migrate frameworks without approval.
5. Never fabricate legal text, contact details, URLs, analytics IDs, credentials, company information, or privacy claims.
6. Treat each rule as `Applicable`, `Not applicable`, `Needs owner input`, or `Completed`. Explain every non-applicable item.
7. Never mark an item complete without checking it. Report modified files and tests.

## 1. Security, privacy, and compliance

### R01 — Privacy policy
- **Website:** If a public website already exists, verify that it provides a working, accessible privacy-policy link. If missing, request approved policy content and authorization before adding a page.
- **Android:** Verify a privacy-policy URL or disclosure is available where required by distribution policies. Request approved policy text and URL; do not invent them.
- Cover actual data practices: member identity/PRN, attendance, meeting reports, tasks, receipt images, and strikes, subject to owner/legal review.

### R02 — Terms and conditions
- Verify existing terms links and text where applicable.
- Ask the owner for approved terms before creating legal content or adding a new page.
- Do not present generated legal language as legally approved.

### R03 — Keep secrets out of frontend and repository
- Do not embed private keys, service-account credentials, tokens, or privileged server secrets in Android resources, web JavaScript, source code, or commits.
- Use server-side secrets/configuration and least-privilege credentials.
- Firebase client configuration is not by itself a server secret; enforce authorization with Firebase Auth, Firestore/Storage rules, and backend checks.
- Inspect `.gitignore`, configuration files, and commit exposure. Do not print secrets in reports or logs.

### R04 — HTTPS and secure transport
- For websites and APIs, use HTTPS, redirect HTTP to HTTPS where deployment supports it, and avoid mixed content.
- For Android, prohibit cleartext traffic unless a documented, necessary exception exists.
- Verify TLS for external requests; do not bypass certificate validation.

### R05 — Cookie consent
- **Website only, conditional:** Audit whether cookies or tracking technologies require consent under applicable law. If required, use an approved consent solution and ensure nonessential tracking respects consent.
- **Native Android:** Do not add a cookie banner merely because this checklist mentions one.

### R06 — Spam and bot protection
- Protect existing public-facing web forms or sensitive endpoints with appropriate rate limits and abuse controls.
- For the private Android app, prioritize Firebase App Check where appropriate, authorization rules, input validation, and rate limiting on privileged backend operations.
- Do not add CAPTCHA to every screen without an identified abuse risk.

### R07 — Analytics and privacy
- Only enable analytics after owner approval, platform configuration, and any necessary privacy/consent review.
- Minimize event data; never log passwords, PRNs, sensitive member records, receipts, or reimbursement details in analytics.
- Do not invent tracking IDs or claim analytics are enabled without verification.

## 2. Performance and assets

### R08 — Compress and optimize images
- Compress existing raster images and choose appropriate dimensions/formats without noticeable quality loss.
- Prefer vector drawables for icons; optimize receipt previews while preserving the original receipt quality needed for review.
- Avoid unnecessary large assets, duplicated images, and oversized downloads.
- Record before/after asset sizes where feasible.

### R09 — Check loading performance
- **Website:** Audit page loading, asset sizes, render-blocking resources, and Core Web Vitals where measurable.
- **Android:** Check cold start, screen transitions, Firestore loading, image rendering, and scanner responsiveness on representative devices.
- Do not claim a performance score without a measured test.

### R10 — Remove unnecessary horizontal scrolling
- Check every existing screen at small device widths and larger font settings.
- Eliminate unintended overflow, clipped cards, off-screen buttons, and bottom-navigation overlap.
- Preserve deliberate horizontal interactions already required by the PRD (such as Kanban columns), while keeping them usable.

### R11 — Mobile-friendly and responsive layouts
- Prioritize common Android screen sizes, system insets, keyboard appearance, display cutouts, and font scaling.
- Keep tap targets at least 48dp where practical; ensure content remains scrollable and accessible.
- **Website, if present:** Verify responsive breakpoints, touch controls, viewport configuration, and mobile navigation.

### R12 — Color contrast and accessibility
- Check text, icons, status chips, buttons, and form errors against backgrounds.
- Aim for WCAG AA contrast for web content where applicable; provide equivalent readable contrast in Android UI.
- Do not communicate status by color alone. Include visible labels for attendance, tasks, expenses, and strikes.
- Add meaningful content descriptions for non-decorative images/icons and maintain logical focus order.

## 3. Navigation, metadata, and links

### R13 — Titles and descriptions
- **Website only:** Ensure every existing page has a meaningful unique `<title>` and meta description.
- **Android:** Ensure screen titles, app label, and accessibility descriptions are accurate; HTML meta tags do not apply.

### R14 — Social preview image
- **Website only:** Verify existing Open Graph/Twitter preview metadata and approved preview image. Request a branding asset if missing; do not invent one.
- Not required for a native Android-only application.

### R15 — Favicon and app icon
- **Website:** Verify favicon and relevant icon declarations.
- **Android:** Verify launcher icon and adaptive icon assets, including visibility on light/dark backgrounds.
- Do not confuse the Android launcher icon with a browser favicon.

### R16 — Sitemap and robots.txt
- **Website only:** If an indexable public website exists, validate sitemap and `robots.txt` according to its actual routes and indexing policy.
- Do not generate these for a native Android app or expose private internal routes.

### R17 — Custom 404 and missing destinations
- **Website only:** Validate that unknown URLs show a useful 404 state, using existing routing conventions. Obtain approval before adding a new standalone page if outside scope.
- **Android:** Handle invalid navigation targets or missing backend records gracefully within existing screens; do not add a 404 screen.

### R18 — Broken links and broken buttons
- Test all existing navigation destinations, deep links, buttons, QR flow actions, receipt actions, and external links.
- Fix dead controls, invalid routes, missing handlers, incorrect permissions, and nonfunctional back navigation.
- Do not leave clickable-looking controls without behavior.

### R19 — Mobile menu and unused navigation
- **Website:** Verify an existing mobile menu opens, closes, and navigates correctly; add no unnecessary menu destinations.
- **Android:** Verify existing app bars and bottom navigation; do not introduce a hamburger menu solely because of this checklist.
- Remove stale/unused navigation items only after checking whether they are referenced by the PRD.

### R20 — Footer, logo, phone, and email links
- **Website only, if present:** Fix footer links, ensure the existing logo returns to the intended home route, and make real phone/email contacts actionable using `tel:`/`mailto:` where appropriate.
- Never invent a phone number or email address; do not add a footer or contact section to the Android app without PRD approval.

### R21 — Copyright year
- **Website or existing legal/about surface only:** Correct any displayed copyright year according to the project's actual ownership and policy.
- Do not introduce a copyright footer to screens that do not have one.

## 4. Forms, validation, and user feedback

### R22 — Form validation
- Validate required fields, formats, lengths, and allowable values in existing login, post-meeting, task, agenda, and reimbursement workflows.
- Validate on the backend as well as the client. Do not rely on UI-only restrictions.
- Reimbursement rejection must require a reason.
- QR attendance must reject invalid PRNs and duplicate scans for the relevant meeting.
- Disable duplicate submission while operations are pending.

### R23 — Success and error messages
- Every existing write operation must provide an accurate loading state and a clear success or failure result.
- Never display success before Firebase/backend confirmation.
- Preserve entered form data after recoverable errors.
- Show actionable, nontechnical messages without leaking backend secrets or sensitive information.

### R24 — Remove placeholders and dead UI
- Remove `Lorem ipsum`, fake names, fabricated metrics, unused demo cards, dummy links, and placeholder buttons from production views.
- Use real Firestore data or clear loading/empty/error states.
- Keep development fixtures out of production builds.

### R25 — One clear primary call to action
- Each existing screen should emphasize the primary action appropriate to its workflow, e.g., Sign In, Confirm Attendance, Submit Report, Submit Expense, Approve/Reject, or Publish Agenda.
- Avoid competing primary buttons and unclear labels.
- Do not add a marketing CTA to the internal Android app.

## 5. GDG CoreHQ-specific regression checks

- [ ] No public registration route exists.
- [ ] Admin/Member role routing is enforced.
- [ ] Core Members cannot access privileged Admin operations, including scanner attendance writes.
- [ ] Invalid and duplicate PRN scans are rejected.
- [ ] Attendance writes contain the required member, meeting, timestamp, status, and recorder data.
- [ ] Member attendance updates are reflected in real time and the existing Post-Meeting Form flow works.
- [ ] Task statuses remain To-Do, In Progress, and Done.
- [ ] Agenda notifications use actual configured services.
- [ ] Reimbursement requires receipt/details and Admin rejection requires a reason.
- [ ] Strike triggers match absence or a task overdue by 48 hours; processing is idempotent.
- [ ] Warning notifications at 1–2 strikes and Admin red highlighting at 3+ work.
- [ ] Admin pardon/removal of a strike is authorized and reflected in data.
- [ ] No new screens, roles, or features were introduced.

## 6. Antigravity audit and fix workflow

1. Read `PRD.md`, `design.md`, and this file in full.
2. Inventory actual app screens, navigation, backend rules, assets, and any existing web surface.
3. Build an audit table: **Rule ID | Applies to | Status | Evidence | Proposed fix | Approval needed**.
4. Prioritize security, data integrity, crashes, broken workflows, accessibility, and performance.
5. Fix issues incrementally without altering PRD scope or approved design language.
6. Run available lint/build/tests and manually inspect key screens at multiple device widths/font scales.
7. Re-run affected regression checks after each change.
8. Report modified files, test commands/results, measured improvements, remaining blockers, and `Not applicable` rules.
9. Ask before creating legal pages, adding analytics/cookie tools, enabling external services, adding new screens, or changing navigation architecture.

## 7. Completion criteria

Do not declare the audit complete until:
- All applicable high-priority issues are fixed or explicitly documented as blockers.
- The Android app builds using the repository's actual framework.
- Core workflows and authorization checks are verified.
- Existing screens are usable at small widths without unintended horizontal overflow.
- Images, buttons, navigation, forms, success/error states, and contrast have been reviewed.
- Website-only items are marked `Not applicable` if no website exists.
- No invented legal, contact, analytics, or production configuration data has been introduced.

**Final rule:** Preserve the product scope in `PRD.md`, follow the visual system in `design.md`, and use this checklist to improve quality rather than expand the application.
