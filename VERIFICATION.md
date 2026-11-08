# GDG CoreHQ — Comprehensive Read-Only Verification Report

**Verification Date:** November 8, 2026  
**Mode:** Read-Only Audit (Zero code modifications)  
**Scope:** Verification of completion claims, architectural integrity, security posture, and compliance with `PRD.md`, `design.md`, and `PROJECT_RULES.md`.

---

## 1. Verification of Specific Inquiries

### Question 1: Is `MockDataRepository` still used anywhere in production code paths?
* **Verdict:** **Yes**
* **Evidence:**
  * `app/src/main/java/com/gdgcorehq/app/data/MockDataRepository.java` (Class `MockDataRepository`, method `getInstance()`): Implemented as a singleton facade delegating internally to `FirebaseDataRepository.getInstance()`.
  * Every single production UI Activity directly imports and calls `MockDataRepository.getInstance()` rather than directly calling `FirebaseDataRepository`:
    * `AdminDashboardActivity.java` (`loadDashboardStats()`, line 81)
    * `AdminTasksActivity.java` (`loadTasks()`, `showAddTaskDialog()`, lines 115, 150, 171, 243, 267)
    * `AgendaManagementActivity.java` (`loadAgendas()`, `showPublishAgendaDialog()`, lines 62, 163)
    * `ExpenseHistoryActivity.java` (`loadReimbursements()`, line 73)
    * `MemberAgendaActivity.java` (`loadAgendas()`, lines 32, 61)
    * `MemberDashboardActivity.java` (`loadDashboardData()`, `setupAttendanceListener()`, lines 107, 117, 131, 146, 151, 216)
    * `MemberTasksActivity.java` (`loadTasks()`, `onTaskStatusChanged()`, lines 107, 140, 150)
    * `NewExpenseActivity.java` (`submitExpense()`, lines 232, 247)
    * `PostMeetingReportActivity.java` (`onCreate()`, `submitReport()`, lines 34, 98)
    * `QrScannerActivity.java` (`onCreate()`, `processQrBarcode()`, `recordAttendance()`, lines 86, 199, 285)
    * `ReimbursementReviewActivity.java` (`loadReimbursements()`, `approveReimbursement()`, `rejectReimbursement()`, lines 62, 96, 163)
    * `StrikeDashboardActivity.java` (`evaluateOverdueTasks()`, `loadStrikes()`, `showPardonDialog()`, lines 50, 73, 114, 157)
* **Impact:** While `MockDataRepository` forwards calls to `FirebaseDataRepository`, production activities remain coupled to a class named "Mock" that exposes test fixture methods (`seedTestData`).

---

### Question 2: Are hardcoded credentials (admin/lead/tech/prn accounts) still in the source?
* **Verdict:** **Partial**
* **Evidence:**
  * **Production Source (`app/src/main/`):** **No hardcoded dictionary of user accounts exists.** `SessionManager.java` (lines 35-39) initializes `private final Map<String, ProvisionedAccount> provisionedAccounts = new HashMap<>()` empty. However, line 210 contains method `authenticateLocal(String id, String password, AuthCallback callback)` which checks `provisionedAccounts.get(id)`. In production, this map is empty unless populated via `seedTestAccounts(...)`.
  * **Test Source (`app/src/test/`):** **Yes, hardcoded credentials exist.** `app/src/test/java/com/gdgcorehq/app/TestFixtures.java` (lines 28-135, method `getTestAccounts()`) defines hardcoded credentials:
    * `admin / admin123` (`usr_admin_01`, Aditya Desai)
    * `lead / lead123` (`usr_member_01`, Sneha Kulkarni)
    * `member2 / member123` (`usr_member_02`, Tanmay Patil)
    * `strike / strike123` (`usr_member_03`, Rahul Patil)
    * `member4 / member123` (`usr_member_04`, Ananya Shinde)

---

### Question 3: Is the QR code generated from an AES-encrypted PRN, and does the scanner decrypt it? Where is the key stored?
* **Verdict:** **Yes (with critical security finding on key storage)**
* **Evidence:**
  * **QR Generation:** **Yes.** `app/src/main/java/com/gdgcorehq/app/MemberDashboardActivity.java` (line 178, method `generateAndDisplayQr()`) invokes `QrCryptoManager.encryptPayload(user.getPrn(), user.getId())` and passes the ciphertext to `QrCryptoManager.generateQrBitmap()`.
  * **Scanner Decryption:** **Yes.** `app/src/main/java/com/gdgcorehq/app/QrScannerActivity.java` (lines 202-212, method `processQrBarcode()`) passes the scanned barcode value to `QrCryptoManager.decryptPayload(rawValue)`. It extracts the decrypted PRN, User ID, and timestamp, validating timestamp freshness (15-minute window).
  * **Key Storage:** **Hardcoded in code.** `app/src/main/java/com/gdgcorehq/app/data/QrCryptoManager.java` (lines 37-48):
    ```java
    private static final byte[] PRE_FIREBASE_AES_KEY = new byte[] {
            (byte) 0x3d, (byte) 0x8a, (byte) 0x9f, (byte) 0x12, ...
    };
    private static final SecretKey SECRET_KEY = new SecretKeySpec(PRE_FIREBASE_AES_KEY, "AES");
    ```
    The 256-bit AES symmetric key is statically compiled into the client binary. This is a direct violation of Rule R03 ("Keep secrets out of frontend and repository").

---

### Question 4: Is there a Firestore snapshot listener that reacts to attendance and opens the Post-Meeting Form?
* **Verdict:** **Yes**
* **Evidence:**
  * **Listener Registration:** `app/src/main/java/com/gdgcorehq/app/data/FirebaseDataRepository.java` (lines 941-956, method `listenForMemberAttendance(String meetingId, String prn, AttendanceListener listener)`):
    Registers `db.collection("attendance").document(docId).addSnapshotListener(...)` where `docId = meetingId + "_" + cleanPrn`.
  * **UI Reaction & Dialog:** `app/src/main/java/com/gdgcorehq/app/MemberDashboardActivity.java` (lines 146-167, method `setupAttendanceListener(User user)`):
    When the listener detects status `"PRESENT"`, it checks `hasSubmittedReport(currentMeetingId, user.getId())`. If false, it displays an AlertDialog titled `"Attendance Verified!"` with positive button `"Submit Report Now"` that starts `PostMeetingReportActivity` passing `putExtra("meetingId", currentMeetingId)`.

---

### Question 5: Is FCM implemented (client token saving + sending)? Which Cloud Functions exist in the repo?
* **Verdict:** **Partial**
* **Evidence:**
  * **Client Token Registration:** **Yes.**
    * `app/src/main/java/com/gdgcorehq/app/data/SessionManager.java` (lines 223-225, method `postAuthSuccess(User user)`): Calls `FirebaseMessaging.getInstance().subscribeToTopic("meeting_agendas")` and writes the device token to Firestore via `FirebaseDataRepository.getInstance().updateUserFcmToken(user.getId(), token)`.
    * `app/src/main/java/com/gdgcorehq/app/service/CoreHQMessagingService.java` (lines 36-42, method `onNewToken(@NonNull String token)`): Forwards token updates to `FirebaseDataRepository.getInstance().updateUserFcmToken(...)`.
    * `app/src/main/java/com/gdgcorehq/app/data/FirebaseDataRepository.java` (lines 890-896, method `updateUserFcmToken(String userId, String token)`): Writes `fcmToken` and `fcmTokenUpdatedAt` into `db.collection("users").document(userId)`.
  * **Incoming Notification Handling:** **Yes.** `CoreHQMessagingService.java` (lines 45-148, method `onMessageReceived(@NonNull RemoteMessage remoteMessage)`): Parses notification payload, builds notification channels (`gdg_channel_agendas`, `gdg_channel_strikes`, `gdg_channel_general`), and creates deep-linking PendingIntents to `MemberAgendaActivity` or `MemberDashboardActivity`.
  * **FCM Message Dispatch (Sending):** **No.** Neither the Android app nor any script in the repository contains FCM message dispatch logic (HTTP v1 API calls or Firebase Admin SDK `send()` calls).
  * **Cloud Functions:** **None exist.** There is no `functions/` directory, no Node.js/TypeScript backend, and no Cloud Functions defined anywhere in the repository.

---

### Question 6: Where are strikes created? Is it idempotent (same event cannot create two strikes)?
* **Verdict:** **Partial**
* **Evidence:**
  * **Creation Location (Task Overdue):** `app/src/main/java/com/gdgcorehq/app/data/FirebaseDataRepository.java` (lines 736-860, method `evaluateOverdueTasks(OverdueEvaluationCallback callback)`). Triggered in `StrikeDashboardActivity.java` (line 50).
  * **Task Strike Idempotency:** **Yes.**
    * The document ID is deterministically set to `strikeDocId = "strk_task_" + taskId`.
    * Firestore branch: Queries `db.collection("strikes").document(strikeDocId).get()`. Only if the document does not exist does it create the strike and increment `strikeCount`. It also updates `tasks/{taskId}` with `isOverdue48h = true`.
    * In-memory branch: Checks if `strikeRecords` contains `strikeDocId` and checks `t.isOverdue48h()`.
  * **Missing Strike Trigger (Meeting Absence):** **No strike creation logic exists for meeting absences.** `StrikeRecord.TriggerType.ABSENCE` is declared in `StrikeRecord.java` (line 13), but no method or background trigger evaluates unattended meetings to generate absence strikes.

---

### Question 7: Do firestore.rules and storage.rules exist in the repo? Were they deployed? Show the project ID.
* **Verdict:** **Partial (Rules exist; deployment cannot be verified/unperformed)**
* **Evidence:**
  * **Rules Files:**
    * `firestore.rules` exists in repository root (125 lines, defining RBAC for users, meetings, attendance, tasks, postMeetingReports, reimbursements, strikes).
    * `storage.rules` exists in repository root (22 lines, restricting `/receipts/{userId}/{fileName}` to image MIME types under 5MB).
    * `firebase.json` references both rules files.
  * **Deployment Status:** **Not deployed / Unverified.**
    * No `.firebaserc` file exists in the repository.
    * The Firebase CLI (`firebase`) is not installed in the execution environment.
    * No deployment metadata or CI/CD deployment workflows exist.
  * **Project ID:** **`gdg-corehq`** (retrieved from `app/google-services.json`, line 4). Storage bucket: `gdg-corehq.firebasestorage.app`.

---

### Question 8: Does any cleartext traffic or secret exist in the manifest/resources?
* **Verdict:** **No cleartext / No secrets in manifest/resources (Found secret in Java code)**
* **Evidence:**
  * **Cleartext Traffic:** **Explicitly Prohibited.** `app/src/main/AndroidManifest.xml` (line 23):
    `android:usesCleartextTraffic="false"`
  * **Manifest & Resources Secrets:** None found in `AndroidManifest.xml` or `app/src/main/res/values/*.xml`.
  * **API Key Context:** `app/google-services.json` (line 18) contains `"current_key": "AIzaSyD9PXKJnWxIjZQSAqRbiE1thvduWo9h7Tk"`. Per Rule R03, Google client API keys identify the Firebase project and are not secrets by themselves, provided Firestore and Storage security rules are enforced.
  * **Code Secret Finding:** As noted in Question 3, a static 256-bit AES key is hardcoded in `app/src/main/java/com/gdgcorehq/app/data/QrCryptoManager.java` (lines 37-48).

---

### Question 9: Which of the 25 tests touch Firebase logic vs mock logic? List them.
* **Verdict:** **All 25 tests run in local JVM offline mode; ZERO tests execute against a live Firebase backend.**
* **Explanation:** In the JVM unit test environment, `FirebaseApp.getInstance()` is not initialized. Consequently, `FirebaseDataRepository.isFirebaseAvailable` defaults to `false`, and `SessionManager` catches the missing Firebase runtime, redirecting all calls to in-memory collections and `authenticateLocal`.

| # | Test Method Name in `CoreHQLogicTest.java` | Logic Touched | Notes / Code Path |
|---|---|:---:|---|
| 1 | `testValidLoginGrantsAccess` | **Mock / In-Memory** | `SessionManager.authenticateLocal()` via `provisionedAccounts` |
| 2 | `testInvalidLoginIsRejected` | **Mock / In-Memory** | `SessionManager.authenticateLocal()` rejection branch |
| 3 | `testSessionClearingOnLogout` | **Mock / In-Memory** | `SessionManager.logout()` in-memory state clearing |
| 4 | `testPrnValidation` | **Mock / In-Memory** | `FirebaseDataRepository.recordAttendance()` local validation |
| 5 | `testDuplicateAttendancePrevention` | **Mock / In-Memory** | `FirebaseDataRepository.recordAttendance()` in-memory set check |
| 6 | `testTaskStatusTransitionsAndOverdueClearing` | **Mock / In-Memory** | `FirebaseDataRepository.updateTaskStatus()` in-memory list |
| 7 | `testPostMeetingReportValidationAndLinking` | **Mock / In-Memory** | `FirebaseDataRepository.submitPostMeetingReport()` in-memory validation |
| 8 | `testReimbursementRejectionReasonRequirement` | **Mock / In-Memory** | `FirebaseDataRepository.rejectReimbursement()` in-memory reason check |
| 9 | `testInvalidExpenseAmountRejected` | **Mock / In-Memory** | `FirebaseDataRepository.submitReimbursement()` amount validation |
| 10 | `testStrikePardonRequiresJustification` | **Mock / In-Memory** | `FirebaseDataRepository.pardonStrike()` in-memory check |
| 11 | `testAdminNavigationDestinations` | **Pure Unit Logic** | `BottomNavHelper` enum and menu mapping |
| 12 | `testMemberNavigationDestinations` | **Pure Unit Logic** | `BottomNavHelper` enum and menu mapping |
| 13 | `testRoleAccessDistinction` | **Mock / In-Memory** | `SessionManager` role evaluation (`isSuperAdmin`) |
| 14 | `testFindMemberByIdAndStrikeCountThreshold` | **Mock / In-Memory** | `FirebaseDataRepository.findMemberById()` in-memory lookup |
| 15 | `testQrEncryptionDecryptionSuccess` | **Pure Unit Logic** | `QrCryptoManager` AES-GCM Java cryptography |
| 16 | `testQrTamperedCiphertextRejected` | **Pure Unit Logic** | `QrCryptoManager` authentication tag failure check |
| 17 | `testQrReplayAttackExpiredPayloadRejected` | **Pure Unit Logic** | `QrCryptoManager` timestamp freshness math |
| 18 | `testQrInvalidOrCorruptTokenRejected` | **Pure Unit Logic** | `QrCryptoManager` Base64 / decrypt error handling |
| 19 | `testQrEncryptThrowsOnEmptyPrn` | **Pure Unit Logic** | `QrCryptoManager` input guard assertions |
| 20 | `testQrEncryptThrowsOnEmptyUserId` | **Pure Unit Logic** | `QrCryptoManager` input guard assertions |
| 21 | `testReceiptUploadValidation` | **Mock / In-Memory** | `FirebaseDataRepository.uploadReceiptImage()` client guard check |
| 22 | `testAutomatedStrikeTriggerForOverdueTasks` | **Mock / In-Memory** | `FirebaseDataRepository.evaluateOverdueTasks()` in-memory branch |
| 23 | `testFindMeetingById` | **Mock / In-Memory** | `FirebaseDataRepository.getMeetingById()` in-memory list search |
| 24 | `testWriteCallbacksForFirestoreOperations` | **Mock / In-Memory** | `FirebaseDataRepository` offline fallback callback invocation |
| 25 | `testFullAttendanceTimestampFormat` | **Pure Unit Logic** | `AttendanceRecord` timestamp format regex assertion |

---

## 2. PROJECT_RULES.md R01–R25 Full Compliance Table

| Rule | Rule Description | Applies to | Status | Evidence | Approval needed |
|---|---|---|:---:|---|:---:|
| **R01** | Privacy policy | Android | **Needs owner input** | No privacy policy URL configured in `AndroidManifest.xml` or settings UI. | Yes (Owner must provide official Privacy Policy URL) |
| **R02** | Terms and conditions | Android | **Needs owner input** | No Terms of Service link or text configured in app. | Yes (Owner must provide approved Terms text) |
| **R03** | Keep secrets out of frontend and repository | Android | **Needs fix** | 256-bit AES key is hardcoded in `QrCryptoManager.java` (lines 37-46). | No (Must migrate to remote key / Android Keystore) |
| **R04** | HTTPS and secure transport | Android | **Completed** | `AndroidManifest.xml` sets `android:usesCleartextTraffic="false"`. TLS used for Firebase. | No |
| **R05** | Cookie consent | Website only | **Not applicable** | Native Android application only; no website surface present. | No |
| **R06** | Spam and bot protection | Android / Backend | **Partial** | Role routing and client form guards present; Firebase App Check not configured. | Yes (Enable Firebase App Check in Console) |
| **R07** | Analytics and privacy | Android | **Completed** | No third-party tracking or analytics SDK initialized without consent; no PII leakage. | No |
| **R08** | Compress and optimize images | Android | **Completed** | `NewExpenseActivity.java` (lines 175-224) compresses receipts to max 1600px at 80% JPEG. | No |
| **R09** | Check loading performance | Android | **Completed** | Video splash preload non-blocking; snapshot listeners populate asynchronous views cleanly. | No |
| **R10** | Remove unnecessary horizontal scrolling | Android | **Completed** | All layouts use vertical constraint/linear layouts; no unwanted horizontal scrolls. | No |
| **R11** | Mobile-friendly and responsive layouts | Android | **Completed** | Tap targets meet 48dp minimum; system window insets handled in layouts. | No |
| **R12** | Color contrast and accessibility | Android | **Completed** | Text labels accompany all status icons; content descriptions added across XML layouts. | No |
| **R13** | Titles and descriptions | Website only | **Not applicable** | HTML `<title>` / meta tags apply to web only. Android screen titles properly set in strings. | No |
| **R14** | Social preview image | Website only | **Not applicable** | Open Graph preview metadata applies to web only. | No |
| **R15** | Favicon and app icon | Android | **Completed** | Adaptive icons defined in `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`. | No |
| **R16** | Sitemap and robots.txt | Website only | **Not applicable** | Applies to indexable public websites only. | No |
| **R17** | Custom 404 and missing destinations | Android | **Completed** | Null checks and graceful redirects implemented on missing session/intent records. | No |
| **R18** | Broken links and broken buttons | Android | **Completed** | Dead placeholder buttons removed; all click handlers wired to active workflows. | No |
| **R19** | Mobile menu and unused navigation | Android | **Completed** | `BottomNavHelper.java` correctly drives 5 Member and 5 Admin tabs; no stale destinations. | No |
| **R20** | Footer, logo, phone, and email links | Website only | **Not applicable** | Applies to website footers; internal Android screens have no footer clutter. | No |
| **R21** | Copyright year | Website only | **Not applicable** | No hardcoded copyright footer introduced. | No |
| **R22** | Form validation | Android | **Completed** | Validates PRN format, expense amount > 0, receipt required, rejection reason required. | No |
| **R23** | Success and error messages | Android | **Completed** | Write callbacks added across all writes; buttons disabled during async operations and re-enabled on error. | No |
| **R24** | Remove placeholders and dead UI | Android | **Completed** | Removed dummy 43-byte GIF receipt, fake user fallbacks, and placeholder dialogs. | No |
| **R25** | One clear primary call to action | Android | **Completed** | Each screen features a single clear CTA button (e.g. "Submit Expense", "Publish", "Scan"). | No |

---

## 3. Comprehensive List of Items NOT Yet Done

The claim that the app is "100% complete" was inaccurate in the context of production readiness and end-to-end cloud operations. Below is the complete list of remaining items:

### 1. Codebase Architecture & Technical Debt
1. **`MockDataRepository` Deprecation / Removal:** All 12 production activities still call `MockDataRepository.getInstance()`. While functioning as a facade to `FirebaseDataRepository`, production code should directly interact with a clean `DataRepository` interface rather than an entity bearing the name "Mock".
2. **Hardcoded AES Key Security Vulnerability (Rule R03):** The 256-bit symmetric encryption key (`PRE_FIREBASE_AES_KEY`) in `QrCryptoManager.java` is hardcoded in the client. Anyone decompiling the APK can extract this key and forge attendance QR codes. This must be replaced with Android Keystore key generation or dynamic retrieval via Firebase Remote Config / Secret Manager.
3. **Missing Automated Strike Trigger for Meeting Absences:** PRD §3.6 specifies that strikes are triggered by either "Meeting Absence or Task Overdue by 48 Hours". `FirebaseDataRepository.evaluateOverdueTasks()` handles task overdue strikes, but no mechanism evaluates absent members at meeting conclusion to record absence strikes.

### 2. Backend & Cloud Infrastructure (Repo Gaps)
4. **Missing Cloud Functions / FCM Backend Dispatch:** No Cloud Functions exist in the repository.
   - When an Admin publishes a meeting agenda in `AgendaManagementActivity`, no cloud function broadcasts an FCM push notification to the `meeting_agendas` topic.
   - When a member receives a strike, no cloud function sends a push warning to the member's FCM token.
5. **Firebase Security Rules Deployment:** `firestore.rules` and `storage.rules` are written in the repository, but have not been deployed to the live Firebase project (`gdg-corehq`).
6. **Firebase App Check (Rule R06):** Firebase App Check is not configured to protect Firestore and Storage endpoints against unauthorized API clients.

### 3. Testing & Verification
7. **Zero Live Firebase Integration Tests:** All 25 unit tests run in JVM offline mode using in-memory fallbacks. There are no integration tests validating transactions against the live Firestore rules or Firebase Storage emulator.
8. **Physical Hardware QA:** CameraX QR scanning, autofocus, and photo receipt capture have not been validated on physical Android devices across varying screen sizes and lighting conditions.

### 4. Legal & Distribution Readiness
9. **Privacy Policy and Terms of Service (Rules R01, R02):** Google Play requires a live Privacy Policy URL covering PRN identity, attendance, receipts, and photos. No policy URL exists in the project.
10. **Release Keystore Signing:** `app/build.gradle` has `minifyEnabled true`, but release signing configs are not hooked to a production `.jks` keystore.

