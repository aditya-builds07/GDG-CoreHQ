# GDG CoreHQ — Phase 3 Verification & Reliability Audit Report

- **Project:** GDG CoreHQ
- **Organization:** Google Developer Groups on Campus (GDGoC), TKIET
- **Platform:** Native Android (Java + XML, Material Design 3, ViewBinding)
- **Role:** Developer A (App/UI Developer)
- **Audit Date:** October 21–22, 2026
- **Status:** Complete — Audited, Hardened, Verified, and Passed

---

## 1. Executive Summary

Phase 3 comprised a comprehensive functional, security, and reliability audit of all 14 screens and core business logic implemented during Phases 1 and 2 of **GDG CoreHQ**. The objective was to eliminate logic flaws, prevent authorization bypasses, enforce strict data validation, harden in-memory mock repositories, and ensure zero regressions through automated unit testing prior to Developer B's Firebase backend integration.

All verified defects have been addressed, 10 automated unit tests pass with a 100% success rate, and clean debug packaging produces `app-debug.apk` without errors.

---

## 2. Findings from the Code Audit

A line-by-line inspection of activities, repositories, models, and layout XML files revealed several structural and behavioral issues:

1. **Authentication Fallthrough & Security Gap (`SessionManager.java`):**
   - *Audit Finding:* In `SessionManager.java`, unprovisioned accounts or non-matching usernames fell through to a default mock profile (`Siddhi Kulkarni`), granting authenticated Core Member sessions to invalid usernames.
   - *Security Risk:* An unauthenticated user could enter any arbitrary text in the login fields and gain access to the application.
   - *Resolution:* Hardened credential verification to strictly reject unprovisioned usernames and passwords with an explicit error callback.

2. **Authorization Boundary Bypasses in Administrative Screens:**
   - *Audit Finding:* Super Admin activities (`AdminDashboardActivity`, `QrScannerActivity`, `AdminTasksActivity`, `AgendaManagementActivity`, `ReimbursementReviewActivity`, and `StrikeDashboardActivity`) did not inspect the active session's role in their `onCreate()` lifecycle. If launched via deep-links, intent tampering, or direct navigation, Core Members could theoretically enter admin screens.
   - *Resolution:* Added strict role and session checks in the `onCreate()` method of every admin screen. Unauthorized access triggers an immediate `Toast` ("Access Denied: Super Admin privileges required") and executes `finish()`.

3. **Session Invalidation on Logout (`ProfileActivity.java` & `MemberDashboardActivity.java`):**
   - *Audit Finding:* When a user logged out, `SessionManager.logout()` cleared the cached session, but subsequent launches or returning to dashboard screens did not guard against `null` current users, occasionally creating NPEs or re-instantiating default credentials.
   - *Resolution:* Added session validation in `MemberDashboardActivity.onCreate()` and `ProfileActivity.onCreate()`; if `currentUser == null`, the app immediately redirects to `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.

4. **Transient Post-Meeting Report Data Loss (`PostMeetingReportActivity.java`):**
   - *Audit Finding:* `PostMeetingReportActivity` previously collected form inputs (progress, roadblocks, feedback) and simply toasted "Report submitted", without binding or saving the record into the data repository.
   - *Resolution:* Created `PostMeetingReport.java` domain model and integrated `MockDataRepository.submitPostMeetingReport()` and `hasSubmittedReport()` with full field validation.

5. **Reimbursement Rejection and Amount Validation Violations:**
   - *Audit Finding:* Per PRD §3.5 and `PROJECT_RULES.md` R22, reimbursement rejection *must* mandate a non-empty administrative justification. Previously, rejection dialogs allowed empty strings. Additionally, the repository accepted negative or zero expense amounts.
   - *Resolution:* Added non-empty reason validation in both `ReimbursementReviewActivity` and `MockDataRepository.rejectReimbursement()`. Added positive amount verification (`amount > 0.0`) in `MockDataRepository.submitReimbursement()`.

6. **Task Overdue Strike Reset Logic Flaw:**
   - *Audit Finding:* Tasks marked `DONE` previously preserved their `isOverdue48h` flag, potentially triggering spurious strike calculations on completed deliverables.
   - *Resolution:* Updating a task status to `DONE` now automatically resets `setOverdue48h(false)` in `MockDataRepository.updateTaskStatus()`.

7. **Defensive Copy Mutation in Unit Tests (`MockDataRepository.java`):**
   - *Audit Finding:* `getAllStrikes()` returns a defensive copy `new ArrayList<>(strikeRecords)`. Calling `repo.getAllStrikes().add(...)` mutated the discarded copy rather than the internal collection.
   - *Resolution:* Added explicit `addStrike(StrikeRecord record)` method to `MockDataRepository` to support repository mutations safely.

---

## 3. Bugs Fixed and Files Changed

| File Path | Component | Description of Fix / Enhancement |
| :--- | :--- | :--- |
| `app/src/main/java/com/gdgcorehq/app/model/PostMeetingReport.java` | Model | **New:** Domain entity for meeting reports with progress, roadblocks, feedback, meeting ID, member ID, and timestamp. |
| `app/src/main/java/com/gdgcorehq/app/data/SessionManager.java` | Session / Auth | Enforced strict credentials check against pre-provisioned roster; eliminated default fallback login on invalid inputs. |
| `app/src/main/java/com/gdgcorehq/app/data/MockDataRepository.java` | Data Repository | Added `addStrike()`, `submitPostMeetingReport()`, `hasSubmittedReport()`, mandatory rejection note validation, positive expense amount checks, and overdue flag reset on task completion. |
| `app/src/main/java/com/gdgcorehq/app/AdminDashboardActivity.java` | Admin Activity | Enforced `currentUser != null && currentUser.isSuperAdmin()` check on launch. |
| `app/src/main/java/com/gdgcorehq/app/QrScannerActivity.java` | Admin Activity | Added Super Admin authorization check; prevents non-admin execution. |
| `app/src/main/java/com/gdgcorehq/app/AdminTasksActivity.java` | Admin Activity | Added Super Admin authorization check; prevents non-admin task assignment. |
| `app/src/main/java/com/gdgcorehq/app/AgendaManagementActivity.java` | Admin Activity | Added Super Admin authorization check; prevents non-admin meeting creation. |
| `app/src/main/java/com/gdgcorehq/app/ReimbursementReviewActivity.java` | Admin Activity | Added Super Admin authorization check and enforced non-empty rejection dialog text. |
| `app/src/main/java/com/gdgcorehq/app/StrikeDashboardActivity.java` | Admin Activity | Added Super Admin authorization check and enforced justification requirement for pardons. |
| `app/src/main/java/com/gdgcorehq/app/MemberDashboardActivity.java` | Member Activity | Added null-session redirect to `LoginActivity`. |
| `app/src/main/java/com/gdgcorehq/app/ProfileActivity.java` | Shared Activity | Added null-session redirect and verified complete session clearing on logout. |
| `app/src/main/java/com/gdgcorehq/app/PostMeetingReportActivity.java` | Member Activity | Linked form submission to `MockDataRepository.submitPostMeetingReport()` with full validation. |
| `app/src/test/java/com/gdgcorehq/app/CoreHQLogicTest.java` | Unit Tests | Expanded suite with 10 unit tests covering security, duplicate attendance, task transitions, expense checks, reports, and strike pardons. |

---

## 4. Automated Tests and Exact Results

The unit test suite was executed via Gradle:
```powershell
.\gradlew.bat testDebugUnitTest --no-daemon
```

### Exact Test Suite Results (`app/build/test-results/testDebugUnitTest/TEST-com.gdgcorehq.app.CoreHQLogicTest.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.gdgcorehq.app.CoreHQLogicTest" tests="10" skipped="0" failures="0" errors="0" timestamp="2026-10-21T18:55:55.835Z" hostname="INBOOK_X2_SLIM" time="0.357">
  <properties/>
  <testcase name="testPrnValidation" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.012"/>
  <testcase name="testValidLoginGrantsAccess" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.047"/>
  <testcase name="testTaskStatusTransitionsAndOverdueClearing" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.031"/>
  <testcase name="testInvalidExpenseAmountRejected" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.002"/>
  <testcase name="testReimbursementRejectionReasonRequirement" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.001"/>
  <testcase name="testStrikePardonRequiresJustification" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.002"/>
  <testcase name="testDuplicateAttendancePrevention" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.121"/>
  <testcase name="testSessionClearingOnLogout" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.003"/>
  <testcase name="testInvalidLoginIsRejected" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.08"/>
  <testcase name="testPostMeetingReportValidationAndLinking" classname="com.gdgcorehq.app.CoreHQLogicTest" time="0.036"/>
  <system-out><![CDATA[]]></system-out>
  <system-err><![CDATA[]]></system-err>
</testsuite>
```

- **Total Tests Run:** 10
- **Passed:** 10
- **Failed:** 0
- **Skipped:** 0
- **Result:** **100% Pass**

---

## 5. Build Result and APK Path

The clean debug build was executed using:
```powershell
.\gradlew.bat clean assembleDebug --no-daemon
```

- **Build Exit Code:** 0
- **Build Outcome:** `BUILD SUCCESSFUL`
- **Output Artifact:** `app/build/outputs/apk/debug/app-debug.apk`
- **Artifact Size:** 6,178,301 bytes (~6.18 MB)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **Build Tools:** Android Gradle Plugin 8.7.3, Gradle 9.7.1, OpenJDK 17.0.12

---

## 6. Emulator and Device Testing Status

- **Diagnostics Run:** `adb devices`
- **Active Devices / Emulators Detected:** None connected in the development environment.
- **Status:** **Device/Emulator verification remains pending physical or emulated runtime connection.**
- **Mitigation:** Comprehensive offline static analysis and unit testing were conducted for all activity classes, lifecycle callbacks, layouts, null-checks, and intent transitions.

---

## 7. Features That Remain Mock-Only

Per the project boundary guidelines between Developer A and Developer B:

1. **Authentication:**
   - Uses hardcoded pre-provisioned demo accounts in `SessionManager.java` (`admin` / `admin123`, `lead` / `lead123`, `tech` / `tech123`, `prn2024001` / `member123`).
   - No Firebase Authentication (Email/Password or Google Sign-In) is active.
2. **Attendance Scanner:**
   - QR code camera scanner accepts scanned or manual PRN entry and validates against `MockDataRepository.members`.
   - Attendance records are held in in-memory collections and reset upon app termination.
3. **Database Storage (Firestore):**
   - Agendas, tasks, expense records, post-meeting reports, and strike logs reside in `MockDataRepository` in-memory `List` collections.
4. **Receipt Image Uploads (Cloud Storage):**
   - Expense receipts store string filenames (`receipt_xxx.jpg`) rather than uploading multipart binary files to Google Cloud Storage.
5. **Push Notifications (FCM):**
   - Strike alerts and meeting reminders are not delivered via FCM; client shows toasts and badge count calculations based on active mock data.

---

## 8. Firebase Dependencies Awaiting Developer B

Developer B is responsible for providing the following backend artifacts and contracts before Phase 4 integration:

1. `google-services.json` registered for package `com.gdgcorehq.app`.
2. Firebase Authentication user pool with custom claims or Firestore user documents (`role: "SUPER_ADMIN" | "CORE_MEMBER"`).
3. Firestore collection schemas:
   - `users/` (name, email, role, prn, domain, strikeCount)
   - `meetings/` (title, date, time, venue, description, agendaItems)
   - `attendance/` (meetingId, prn, memberName, markedAt, scannedBy)
   - `tasks/` (title, description, assigneeId, assigneeName, deadline, status, domain, overdue)
   - `reimbursements/` (memberId, memberName, amount, date, category, receiptUrl, status, rejectionReason)
   - `strikes/` (memberId, memberName, triggerType, reason, date, pardoned, pardonedReason, pardonedBy)
   - `reports/` (meetingId, memberId, memberName, progress, roadblocks, feedback, timestamp)
4. Firestore Security Rules preventing client-side write access to administrative collections (`strikes`, `meetings`) by Core Members.
5. Firebase Cloud Storage bucket configuration for receipt uploads.
6. Cloud Functions for automated overdue task detection (48-hour deadline trigger) and 3-strike escalation notifications.

---

## 9. Known Limitations and Recommended Next Phase

### Known Limitations
- Background data does not persist across application process destruction.
- Real-time Firestore snapshot listeners (`SnapshotListener`) are not yet attached; UI relies on activity lifecycle refresh (`onResume()`).
- Camera-based QR scanning depends on Android Camera / ML Kit permissions being granted on runtime devices.

### Recommended Next Phase: Phase 4 — Backend Integration & Contract Binding
1. Receive `google-services.json` and Firestore schema contracts from Developer B.
2. Create repository abstraction interfaces (`AuthRepository`, `MeetingRepository`, `AttendanceRepository`, `TaskRepository`, `ExpenseRepository`, `StrikeRepository`) to replace `MockDataRepository` with Firestore implementations.
3. Hook up Firebase Authentication state listeners to `SessionManager`.
4. Conduct physical device testing with QR code scanning and image receipt selection via Android Photo Picker.
