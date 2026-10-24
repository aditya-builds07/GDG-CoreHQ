# GDG CoreHQ — Product Requirements Document

## 1. Product Overview

**Product name:** GDG CoreHQ  
**Organization:** Google Developer Groups on Campus (GDGoC), TKIET  
**Product type:** Private Android application for internal chapter operations

### Objective
GDG CoreHQ centralizes internal chapter operations, core-team accountability, meeting attendance, task tracking, meeting agendas, and reimbursement requests in one role-based application.

### Target users
- **Super Admin:** Organizer and Community Manager
- **Core Member:** Tech Leads, Design Leads, PR members, and other authorized core-team members

The application is a closed system. There is no public sign-up flow.

## 2. User Roles and Permissions

### 2.1 Super Admin
The Super Admin has full access to existing management features:
- Open the QR scanner and record attendance.
- Create, assign, and monitor tasks.
- Publish meeting agendas.
- Review and approve or reject reimbursement requests.
- View the Strike dashboard and manage team accountability.
- Manually pardon or remove a strike when justified.
- Create and manage member accounts through the configured backend/authentication process.

### 2.2 Core Member
Core Members can:
- Sign in using pre-provisioned credentials.
- View their profile, role, current tasks, attendance, and strike status.
- View assigned tasks and a global view of tasks.
- Update their own task status: To-Do, In Progress, or Done.
- View upcoming meeting agendas.
- Submit the post-meeting progress report, roadblocks, and meeting feedback.
- Submit expense reimbursement requests with receipt images.
- View their expense history and approval status.

## 3. Functional Requirements

### 3.1 Authentication and Onboarding
- Do not provide public account registration.
- Accounts are provisioned by the administrator.
- Users sign in with their assigned username/ID and configured password.
- Users can reset their password using the supported authentication flow.
- After successful login, route users to the appropriate Admin or Member dashboard based on their role.
- The profile displays the member's name, core role, current tasks, and strike count.

### 3.2 QR Attendance
#### Member workflow
1. The Core Member logs into the GDG CoreHQ app.
2. The Member Dashboard generates and displays a digital QR code containing their securely encrypted PRN (AES encryption).
3. The member presents their screen to the Super Admin for scanning during the meeting check-in.
4. The member's app receives the attendance update through a real-time database listener.
5. After attendance is recorded, direct the member to the existing Post-Meeting Form.
6. The member submits weekly progress, roadblocks, and meeting feedback before returning to the dashboard.

#### Admin workflow
1. The authorized Admin opens the in-app QR scanner powered by Google ML Kit.
2. The scanner reads the encrypted digital QR code from the Core Member's screen.
3. The Admin app decrypts the PRN and validates the decrypted PRN against backend/registered member records.
4. If decryption fails, the PRN is invalid, or attendance has already been recorded for the meeting, reject the scan with clear feedback.
5. If valid, display the member's name and role for confirmation.
6. After Admin confirmation, record attendance with the member/PRN reference, meeting ID, status, date/timestamp, and recorder ID.

**Authorization:** Only Super Admin users can open the scanner and record attendance. Core Members generate/display their personal encrypted QR code.

### 3.3 Task Delegation and Kanban Board
- Admins can create tasks, set deadlines, assign tasks to specific members, and view the global task board.
- Members see their own tasks in a Kanban board with **To-Do**, **In Progress**, and **Done** statuses.
- Members can drag or tap to update task status.
- Members can access a Global View of other departments' tasks.
- Admins can monitor task status across members.

### 3.4 Meeting Agendas
- Admins can publish an upcoming meeting agenda with topics, time, and location.
- Members receive a push notification when a new core meeting is scheduled.
- Members can view the agenda.
- Agenda information is linked to the existing Post-Meeting Form so members can contextualize their reports.

### 3.5 Reimbursements and Receipt Scanner
- Members select **New Expense** and capture a photo of the physical receipt.
- Members enter the amount, date, and category.
- Submissions appear as **Pending** for Admin review.
- Admins review the receipt and either:
  - **Approve:** mark the request as reimbursed.
  - **Reject:** require a rejection-reason note.
- Members can view a digital history of submitted and approved/rejected expenses.

### 3.6 Strike and Accountability System
The backend assigns strikes for the specified triggers:
1. A member is marked absent for a core meeting because no valid attendance scan was recorded.
2. A task assigned to a member is overdue by 48 hours.

Strike responses:
- At 1 or 2 strikes, send automated warning notifications.
- At 3 strikes, highlight the member's profile in red on the Admin dashboard and prompt formal review.
- Admins can manually pardon/remove a strike when a member has a valid emergency.

The implementation must avoid repeatedly assigning the same strike for the same triggering event.

## 4. Screen Inventory — Existing PRD Scope Only

This inventory describes screens already implied by the requirements. It does not authorize additional product features.

### Shared
- Login
- Profile

### Super Admin
- Admin Dashboard
- QR Attendance Scanner
- Task Management / Global Kanban Board
- Meeting Agenda Management
- Reimbursement Review
- Strike Dashboard

### Core Member
- Member Dashboard
- Personal Kanban Board and Global View
- Meeting Agenda View
- Post-Meeting Form
- New Expense / Receipt Capture
- Expense History
- Attendance and Strike Status

Use dialogs, confirmation states, and validation messages within these existing flows where needed; do not invent additional standalone screens.

## 5. Proposed Technical Architecture

Use the following stack from the source PRD, subject to confirmation against the actual repository:

- **Platform:** Android
- **Language:** Java
- **UI:** XML layouts with Material Design 3 components
- **Authentication:** Firebase Authentication
- **Database:** Cloud Firestore
- **Receipt images:** Firebase Cloud Storage
- **QR scanning:** Google ML Kit Barcode Scanning
- **QR generation:** Java-compatible QR generation library for member PRN codes
- **Automated accountability:** Firebase Cloud Functions or another appropriate backend scheduler, if required

**Important:** The source PRD contains conflicting mentions of Kotlin/Jetpack Compose and Java/XML. This document follows the Java/XML direction used in the proposed project description. Before implementation, inspect the existing repository and follow its actual configured language and UI framework; do not migrate frameworks without approval.

## 6. Data Requirements

The application will need records corresponding to the existing features:
- **Users:** authentication identity, PRN, name, role, strike count, and profile data.
- **Tasks:** title/details, assignee, deadline, status, and relevant timestamps.
- **Meetings/agendas:** meeting identifier, topics, time, location, and agenda details.
- **Attendance:** member reference/PRN, meeting ID, present/absent status, timestamp, and recorder ID.
- **Post-meeting reports:** member, related meeting/agenda, progress, roadblocks, feedback, and submission time.
- **Expenses:** member, amount, date, category, receipt image reference, status, and rejection reason where applicable.
- **Strike records:** member, triggering event, reason, timestamp, and any Admin pardon/removal information.

Use stable document identifiers and validate role permissions on the backend, not only by hiding UI controls.

## 7. Security and Validation Requirements

- Restrict the app to provisioned users.
- Enforce role-based authorization for privileged operations.
- Only authorized Admins may write attendance records through the scanner workflow.
- Validate PRNs and reject invalid or duplicate attendance submissions.
- Validate required expense fields and require a reason for rejection.
- Store receipt images in Firebase Storage and reference them from expense records.
- Use Firestore Security Rules and appropriate backend checks to prevent members from changing another member's tasks, attendance, expenses, or strike records.
- Do not hard-code credentials, API secrets, or private configuration values in source code.
- Handle authentication failures, missing data, permission errors, and network failures with clear in-context feedback.

## 8. Core Workflows

### 8.1 Login
Start → Login → Firebase Authentication → credentials valid?
- No: show an error and remain on Login.
- Yes: check role → Admin Dashboard or Member Dashboard.

### 8.2 Attendance
Admin opens Scanner → scan PRN QR → validate member and duplicate status → show name/role → Admin confirms → write attendance → real-time member update → Post-Meeting Form → submit report → return to dashboard.

### 8.3 Tasks
Admin creates task and assigns deadline/member → member updates task status → Admin monitors global board → overdue task reaches 48 hours → accountability logic evaluates the strike trigger.

### 8.4 Agenda and Report
Admin publishes agenda → members receive notification and view agenda → attendance is recorded → member submits the linked post-meeting report.

### 8.5 Reimbursement
Member captures receipt and enters details → submits request → Admin reviews → approve or reject → rejection requires a reason → member can view updated expense history.

### 8.6 Strikes
Backend evaluates absence and overdue-task triggers → record applicable strike → send warning for strike 1 or 2 → highlight member for formal review at 3 strikes → Admin may pardon/remove a strike when justified.

## 9. Non-Functional Requirements

- Keep the interface clear, consistent, and usable on supported Android devices.
- Reflect attendance and other Firestore-backed updates in real time where required.
- Protect member information and expense receipts through authenticated access and backend authorization.
- Keep attendance and financial records traceable to the relevant member and action.
- Provide loading, empty, success, and error states within existing screens.
- Handle intermittent connectivity gracefully without showing an operation as successful before the backend confirms it.

## 10. Development Phases

### Phase 1 — Foundation (Weeks 1–2)
- Configure Firebase Authentication and Firestore.
- Implement Login and role-based dashboard routing.

### Phase 2 — Core Loop (Weeks 3–4)
- Integrate ML Kit for the Admin scanner.
- Implement attendance recording and real-time listeners.
- Build the Post-Meeting Form and database write logic.

### Phase 3 — Management Tools (Weeks 5–6)
- Implement task creation, assignment, and status updates.
- Implement the Kanban board and agenda publishing.

### Phase 4 — Finance and Accountability (Weeks 7–8)
- Implement receipt capture and Firebase Storage.
- Implement reimbursement review.
- Implement strike triggers and the Admin review dashboard.

## 11. Acceptance Criteria

- Unprovisioned users cannot register or access the app.
- Valid users are routed to the dashboard permitted by their role.
- Only Admins can access the attendance scanner and confirm attendance.
- Invalid PRNs and duplicate attendance scans are rejected.
- Confirmed attendance is stored with the required identifiers and timestamp.
- The member app receives the attendance update and opens the Post-Meeting Form.
- Members can update their own task statuses; Admins can view all tasks.
- Agenda publication triggers the configured member notification.
- Expense submissions include required details and a receipt image.
- Admin rejection requires a reason; members can see the resulting status.
- The specified absence and 48-hour overdue triggers create appropriate strikes without duplicate processing.
- Warning notifications and the three-strike Admin flag follow the specified thresholds.
- Admins can pardon/remove a strike with the action reflected in the records.

## 12. Instructions for Antigravity

Before writing or changing code:

1. Inspect the existing repository structure, build files, dependencies, and current implementation.
2. Treat this PRD as the functional scope. Do not add new screens, roles, features, or flows.
3. Reuse existing files and components where practical. Do not rewrite working code or migrate the framework without explicit approval.
4. Confirm the actual project stack from repository files. Resolve the Java/XML versus Kotlin/Compose ambiguity by following the existing implementation unless the user explicitly directs otherwise.
5. Implement incrementally in the stated phases, keeping the app buildable.
6. Configure Firebase using project-provided configuration; never invent credentials or commit secrets.
7. Apply backend security rules and role checks, not UI-only restrictions.
8. Test the acceptance criteria, including invalid credentials, role restrictions, invalid/duplicate PRNs, reimbursement rejection without a reason, real-time updates, and strike thresholds.
9. If a required Firebase project setting, credential, or product decision is missing, explain the blocker instead of fabricating it.
10. Report completed work, changed files, tests run, known issues, and any remaining setup required.

**Scope rule:** Do not add any feature or screen that is not specified in this PRD. If a potentially useful addition is discovered, ask for approval before implementing it.
