# GDG CoreHQ — Team Responsibilities and Collaboration Rules

## 1. Purpose

This document defines ownership boundaries for GDG CoreHQ so both teammates can work in parallel without overwriting each other's work.

**Project:** GDG CoreHQ  
**Platform:** Android  
**Source of truth:** `PRD.md` for requirements, `design.md` for visual styling, and `PROJECT_RULES.md` for quality and security checks.

## 2. Team Roles

### Developer A — App/UI Developer (You)
**Primary ownership:** Android screens, UI implementation, navigation, and non-Firebase application presentation.

Responsibilities:
- Inspect and maintain the existing Android project structure.
- Implement and style existing screens according to `design.md`.
- Implement layouts, reusable UI components, typography, colors, spacing, icons, and responsive behavior.
- Implement navigation and role-based screen presentation using the authentication/role state exposed through the agreed interface.
- Build the existing Login, Admin Dashboard, Member Dashboard, profile, task/Kanban, agenda, post-meeting form, reimbursement, attendance scanner presentation, and strike dashboard UI specified in `PRD.md`.
- Implement client-side validation and clear loading, empty, success, and error states.
- Integrate the UI with agreed service/repository interfaces without independently changing Firebase configuration, database schema, security rules, or Cloud Functions.
- Use approved logo assets and preserve the established visual identity.
- Maintain UI tests and documentation where relevant.
- Run available builds and UI tests and report integration blockers.

**Not assigned to Developer A:** Creating or modifying Firebase projects, Firebase Authentication configuration, Firestore schema, Firestore Security Rules, Storage rules/buckets, Cloud Functions, FCM configuration, privileged service credentials, or backend strike/attendance automation.

### Developer B — Firebase and Backend Developer (Teammate)
**Primary ownership:** Firebase setup, backend data model, access control, server-side logic, and Firebase integrations.

Responsibilities:
- Configure the Firebase project and Android app registration.
- Maintain Firebase configuration and approved handling of `google-services.json`.
- Implement Firebase Authentication setup and provisioned-account workflow; no public registration unless requirements explicitly change.
- Define and document Firestore collections/documents, field names, identifiers, indexes, and data ownership.
- Implement and test Firestore Security Rules and Storage Security Rules.
- Configure Firebase Storage for receipt images and access permissions.
- Implement backend validation for PRN lookup, invalid/duplicate attendance prevention, and authorized attendance writes.
- Configure Firebase Cloud Messaging and push notifications where required by the PRD.
- Implement Cloud Functions or other approved trusted backend logic for 48-hour overdue-task evaluation, absence-related strike processing, strike deduplication, and threshold notifications as required.
- Implement backend checks for reimbursement state transitions and rejection reasons.
- Define safe app read/write interfaces and document setup, deployment steps, rules tests, indexes, and limitations.
- Keep service-account credentials and private secrets off the Android client.

**Not assigned to Developer B:** Redesigning UI screens, changing the approved visual system, adding product features, or changing app navigation without agreement. Minimal UI changes needed for integration should be coordinated with Developer A.

## 3. Shared Responsibilities

Both developers must:
- Follow `PRD.md`, `design.md`, and `PROJECT_RULES.md`.
- Keep the existing framework and architecture unless both agree to a change.
- Avoid adding unapproved screens, roles, features, or workflows.
- Use feature branches and pull requests when practical.
- Agree on API/service contracts before parallel implementation.
- Never commit passwords, private keys, service-account JSON files, tokens, or other privileged secrets.
- Communicate schema/interface changes before merging.
- Test complete workflows after integration.
- Review changes that affect both the client and Firebase backend.
- Document assumptions instead of inventing requirements.

## 4. Ownership Matrix

| Area | Developer A — You | Developer B — Teammate |
|---|---|---|
| UI design system and styling | Owner | Consult |
| Android layouts/components | Owner | Consult |
| Navigation and screen presentation | Owner | Consult |
| Login screen UI | Owner | Authentication integration |
| Firebase Authentication configuration | Integration only | Owner |
| Role state | Display/use agreed interface | Define and enforce source |
| Firestore schema and indexes | Consume agreed schema | Owner |
| Firestore Security Rules | Coordinate before any change | Owner |
| Receipt capture UI | Owner | Storage setup and permissions |
| QR scanner UI/result confirmation | Owner | PRN validation and attendance writes |
| Tasks/Kanban UI | Owner | Data model and access rules |
| Agenda UI | Owner | Data model and push notifications |
| Post-meeting form UI | Owner | Data model and authorized writes |
| Expense UI/history | Owner | Storage, schema, approval constraints |
| Strike dashboard UI | Owner | Strike automation and backend validation |
| Cloud Functions/scheduled logic | No ownership | Owner |
| Notification display/deep-link UI | Owner, using agreed payload | FCM setup and delivery |
| Client-side validation and feedback | Owner | Server-side validation |
| Integration and end-to-end testing | Shared | Shared |
| Firebase deployment and rules testing | Support only | Owner |

## 5. Integration Contract — Agree Before Parallel Work

Agree on and document these contracts before implementing integrations:

1. **Authentication result:** how the app obtains the authenticated user's stable ID and role.
2. **Member identity:** canonical member document ID, PRN format, and role values.
3. **Attendance:** meeting ID, uniqueness rule, record fields, and confirmation result.
4. **Tasks:** field names, status values (`To-Do`, `In Progress`, `Done`), deadline format, and update permissions.
5. **Agendas:** meeting ID, date/time format, location, topic representation, and notification payload.
6. **Post-meeting report:** field names, required values, meeting association, and duplicate-submission behavior if applicable.
7. **Expenses:** amount/date/category formats, receipt path, status values, and mandatory rejection-reason behavior.
8. **Strikes:** trigger identifiers, deduplication strategy, thresholds, and admin pardon/removal audit data.
9. **Errors:** agreed result/error format so the UI can show accurate feedback.
10. **Real-time updates:** which records require listeners and how listeners are released when screens close.

Do not invent undocumented fields or business rules. Raise unclear decisions for agreement before implementation.

## 6. Recommended Git Workflow

- Keep `main` buildable.
- Use separate branches, for example `feat/android-ui`, `feat/firebase-backend`, and `integration/firebase-ui`.
- Keep commits focused and descriptive.
- Avoid editing the same file simultaneously.
- If shared edits are necessary, agree who owns the file for that change.
- Use pull requests to review authentication, security rules, data schema, and navigation changes.
- Do not commit generated build output, local machine settings, or private credentials.
- Do not put privileged secrets in a `.env` file bundled into the Android app. Client-bundled configuration can be recovered from the APK; privileged secrets belong on a trusted backend.

## 7. File and Change Ownership

### Developer A normally changes
- Android screen/layout files.
- UI components and style/theme resources.
- Navigation presentation and screen-level state.
- Client-side form validation and feedback.
- UI tests and UI-related documentation.

### Developer B normally changes
- Firebase configuration and integration setup.
- Firestore schema documentation and indexes.
- Firestore/Storage Security Rules.
- Cloud Functions and scheduled backend code.
- FCM setup and server-side notification logic.
- Backend integration/service implementations and rules tests.

### Shared or coordinate first
- Gradle files and dependencies.
- Application entry points and navigation graph.
- Shared models and repositories.
- Authentication/session interfaces.
- Firebase service abstractions.
- Manifest permissions and deep-link configuration.
- Any file used by both the UI and backend integration.

These are default boundaries, not assumptions about the current repository. Inspect the actual file structure before assigning individual files.

## 8. Definition of Done

A feature is ready to merge when:
- It matches the approved PRD and design system.
- Ownership boundaries were respected.
- No privileged secrets were committed.
- UI states reflect real backend results rather than hard-coded success.
- Relevant unit, UI, integration, or rules tests pass where available.
- The Android app builds successfully.
- Required Firebase setup, indexes, rules, or deployment steps are documented by Developer B.
- Unresolved decisions and limitations are clearly reported.
- Both developers have reviewed changes that affect their shared contract.

## 9. Instructions for Antigravity

Before editing the repository:

1. Read `PRD.md`, `design.md`, `PROJECT_RULES.md`, and this file.
2. Inspect the repository to identify the actual Android framework, file layout, and current implementation.
3. Treat Developer A as the owner of UI/presentation and Developer B as the owner of Firebase/backend tasks.
4. Do not independently create or modify Firebase configuration, schema, security rules, Cloud Functions, or privileged credentials when working on Developer A's tasks.
5. For UI work, consume agreed interfaces. Temporary mocks may be used only in development/test code when backend integration is not ready; never ship fake data as real.
6. If a Firebase change is needed, describe the exact required contract or change for Developer B instead of silently implementing it.
7. Do not change product scope or add screens/features without explicit approval.
8. At the end of a task, report files changed, tests/build results, integration dependencies, and actions required from the other developer.

## 10. Scope and Conflict Resolution

- `PRD.md` governs functionality and approved workflows.
- `design.md` governs visual presentation.
- `PROJECT_RULES.md` governs security, quality, performance, and verification requirements.
- `TEAM_RESPONSIBILITIES.md` governs task ownership and collaboration.
- Security requirements cannot be waived merely to simplify UI implementation.
- If documents conflict or an implementation decision is unclear, state the conflict and request agreement rather than silently changing behavior.
