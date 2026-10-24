package com.gdgcorehq.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.QrCryptoManager;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.model.AttendanceRecord;
import com.gdgcorehq.app.model.PostMeetingReport;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.StrikeRecord;
import com.gdgcorehq.app.model.TaskItem;
import com.gdgcorehq.app.model.User;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests validating core business rules, security boundaries,
 * and validation constraints strictly specified by PRD and PROJECT_RULES.md.
 */
public class CoreHQLogicTest {

    private MockDataRepository repo;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        repo = MockDataRepository.getInstance();
        sessionManager = SessionManager.getInstance();
        sessionManager.logout(); // start with clean session
    }

    // --- Authentication & Session Tests ---

    @Test
    public void testValidLoginGrantsAccess() {
        final boolean[] success = {false};
        sessionManager.authenticate("admin", "admin123", new SessionManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                success[0] = true;
                assertNotNull(user);
                assertTrue(user.isSuperAdmin());
            }

            @Override
            public void onError(String message) {
                success[0] = false;
            }
        });

        assertTrue("Valid admin credentials must authenticate successfully", success[0]);
        assertTrue("SessionManager must reflect logged-in state", sessionManager.isLoggedIn());
        assertEquals("Aditya Desai", sessionManager.getCurrentUser().getName());
    }

    @Test
    public void testInvalidLoginIsRejected() {
        // Unprovisioned account must be rejected per PRD §3.1
        final boolean[] errorTriggered = {false};
        sessionManager.authenticate("unregistered_user", "password123", new SessionManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                errorTriggered[0] = false;
            }

            @Override
            public void onError(String message) {
                errorTriggered[0] = true;
            }
        });
        assertTrue("Unprovisioned user must be rejected", errorTriggered[0]);
        assertFalse("Session must not be active for invalid user", sessionManager.isLoggedIn());

        // Incorrect password for valid user must be rejected
        errorTriggered[0] = false;
        sessionManager.authenticate("admin", "wrong_password", new SessionManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                errorTriggered[0] = false;
            }

            @Override
            public void onError(String message) {
                errorTriggered[0] = true;
            }
        });
        assertTrue("Incorrect password must be rejected", errorTriggered[0]);
        assertFalse("Session must not be active on bad password", sessionManager.isLoggedIn());
    }

    @Test
    public void testSessionClearingOnLogout() {
        sessionManager.authenticate("lead", "lead123", new SessionManager.AuthCallback() {
            @Override public void onSuccess(User user) {}
            @Override public void onError(String message) {}
        });
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.logout();
        assertFalse("isLoggedIn must be false after logout", sessionManager.isLoggedIn());
        assertNull("currentUser must be null after logout", sessionManager.getCurrentUser());
    }

    // --- Attendance Tests ---

    @Test
    public void testPrnValidation() {
        // Valid registered PRN
        User member = repo.findMemberByPrn("PRN2024001");
        assertNotNull("Registered PRN should resolve to a valid member", member);
        assertEquals("Siddhi Kulkarni", member.getName());

        // Invalid PRN must be rejected per PRD §3.2
        User invalid = repo.findMemberByPrn("INVALID999");
        assertNull("Unregistered PRN must return null / be rejected", invalid);
    }

    @Test
    public void testDuplicateAttendancePrevention() {
        String meetingId = "test_meeting_01";
        String prn = "PRN2024001";

        // First attendance record must succeed
        AttendanceRecord record1 = new AttendanceRecord(
                "att_01", meetingId, prn, "Siddhi Kulkarni", "Core Member", "10:00 AM", "PRESENT", "admin_01"
        );
        boolean firstRecord = repo.recordAttendance(record1);
        assertTrue("First attendance scan must be accepted", firstRecord);

        // Duplicate attendance scan for same meeting & PRN must be rejected per PRD §3.2 and PROJECT_RULES §5
        AttendanceRecord recordDuplicate = new AttendanceRecord(
                "att_02", meetingId, prn, "Siddhi Kulkarni", "Core Member", "10:05 AM", "PRESENT", "admin_01"
        );
        boolean duplicateRecord = repo.recordAttendance(recordDuplicate);
        assertFalse("Duplicate attendance scan for the same meeting must be rejected", duplicateRecord);
    }

    // --- Task Tests ---

    @Test
    public void testTaskStatusTransitionsAndOverdueClearing() {
        TaskItem task = new TaskItem(
                "test_task_01", "Deploy Backend Microservice", "Set up Docker container",
                "usr_member_03", "Rahul Patil", "Oct 18, 2026",
                TaskItem.Status.TODO, "Technical", true
        );
        repo.addTask(task);

        assertEquals(TaskItem.Status.TODO, task.getStatus());
        assertTrue("Task should be flagged overdue initially", task.isOverdue48h());

        // Move to IN_PROGRESS
        repo.updateTaskStatus(task.getId(), TaskItem.Status.IN_PROGRESS);
        assertEquals(TaskItem.Status.IN_PROGRESS, task.getStatus());

        // Move to DONE — completed tasks must clear active overdue strike risk
        repo.updateTaskStatus(task.getId(), TaskItem.Status.DONE);
        assertEquals(TaskItem.Status.DONE, task.getStatus());
        assertFalse("Overdue status must be cleared when task is marked Done", task.isOverdue48h());
    }

    // --- Post-Meeting Report Tests ---

    @Test
    public void testPostMeetingReportValidationAndLinking() {
        // Valid report
        PostMeetingReport validReport = new PostMeetingReport(
                "rep_01", "meet_101", "usr_member_01", "Siddhi Kulkarni",
                "Completed poster deliverables", "None", "Great session", "Oct 24, 2026 06:30 PM"
        );
        boolean submitted = repo.submitPostMeetingReport(validReport);
        assertTrue("Valid report submission must succeed", submitted);
        assertTrue("hasSubmittedReport must confirm submission", repo.hasSubmittedReport("meet_101", "usr_member_01"));

        // Report with empty progress must be rejected
        PostMeetingReport invalidProgress = new PostMeetingReport(
                "rep_02", "meet_101", "usr_member_02", "Tanmay Patil",
                "", "No blockers", "Feedback", "Oct 24, 2026"
        );
        assertFalse("Report with empty progress must be rejected", repo.submitPostMeetingReport(invalidProgress));

        // Report with empty roadblocks must be rejected
        PostMeetingReport invalidRoadblocks = new PostMeetingReport(
                "rep_03", "meet_101", "usr_member_02", "Tanmay Patil",
                "Progress text", "", "Feedback", "Oct 24, 2026"
        );
        assertFalse("Report with empty roadblocks must be rejected", repo.submitPostMeetingReport(invalidRoadblocks));
    }

    // --- Reimbursement Tests ---

    @Test
    public void testReimbursementRejectionReasonRequirement() {
        Reimbursement expense = new Reimbursement(
                "test_exp_01", "usr_member_01", "Siddhi Kulkarni", 500.0,
                "Oct 20, 2026", "Refreshments", "receipt_test.jpg",
                Reimbursement.Status.PENDING, null
        );
        repo.submitReimbursement(expense);

        assertEquals(Reimbursement.Status.PENDING, expense.getStatus());

        // Rejection without reason must fail per PRD §3.5 and PROJECT_RULES R22
        boolean emptyReject = repo.rejectReimbursement(expense.getId(), "");
        assertFalse("Rejection without reason must be rejected", emptyReject);
        assertEquals(Reimbursement.Status.PENDING, expense.getStatus());

        boolean whitespaceReject = repo.rejectReimbursement(expense.getId(), "   ");
        assertFalse("Rejection with whitespace-only reason must be rejected", whitespaceReject);
        assertEquals(Reimbursement.Status.PENDING, expense.getStatus());

        // Rejection with valid reason must succeed
        String reason = "Missing vendor GST invoice";
        boolean validReject = repo.rejectReimbursement(expense.getId(), reason);
        assertTrue("Rejection with valid reason note must succeed", validReject);
        assertEquals(Reimbursement.Status.REJECTED, expense.getStatus());
        assertEquals(reason, expense.getRejectionReason());
    }

    @Test
    public void testInvalidExpenseAmountRejected() {
        // Zero amount must fail
        Reimbursement zeroExp = new Reimbursement(
                "test_exp_zero", "usr_member_01", "Siddhi Kulkarni", 0.0,
                "Oct 20, 2026", "Refreshments", "receipt.jpg",
                Reimbursement.Status.PENDING, null
        );
        assertFalse("Expense with 0 amount must be rejected", repo.submitReimbursement(zeroExp));

        // Negative amount must fail
        Reimbursement negExp = new Reimbursement(
                "test_exp_neg", "usr_member_01", "Siddhi Kulkarni", -150.0,
                "Oct 20, 2026", "Refreshments", "receipt.jpg",
                Reimbursement.Status.PENDING, null
        );
        assertFalse("Expense with negative amount must be rejected", repo.submitReimbursement(negExp));
    }

    // --- Strike Tests ---

    @Test
    public void testStrikePardonRequiresJustification() {
        StrikeRecord strike = new StrikeRecord(
                "test_strk_01", "usr_member_04", "Ananya Shinde",
                StrikeRecord.TriggerType.ABSENCE, "Unexcused meeting absence",
                "Oct 15, 2026", false, null, null
        );
        repo.addStrike(strike);

        assertFalse("New strike should not be pardoned by default", strike.isPardoned());

        // Pardon without justification must fail
        boolean emptyPardon = repo.pardonStrike(strike.getId(), "", "Aditya Desai");
        assertFalse("Pardon without justification must fail", emptyPardon);

        // Admin pardons with justification per PRD §3.6
        boolean validPardon = repo.pardonStrike(strike.getId(), "Medical emergency verified", "Aditya Desai");
        assertTrue("Pardon with justification must succeed", validPardon);
        assertTrue("Strike record must be marked pardoned", strike.isPardoned());
        assertEquals("Medical emergency verified", strike.getPardonedReason());
        assertEquals("Aditya Desai", strike.getPardonedBy());
    }

    // --- Bottom Navigation & Role Destination Tests ---

    @Test
    public void testAdminNavigationDestinations() {
        BottomNavHelper.NavItem home = new BottomNavHelper.NavItem(null, AdminDashboardActivity.class);
        BottomNavHelper.NavItem tasks = new BottomNavHelper.NavItem(null, AdminTasksActivity.class);
        BottomNavHelper.NavItem scanner = new BottomNavHelper.NavItem(null, QrScannerActivity.class);
        BottomNavHelper.NavItem expenses = new BottomNavHelper.NavItem(null, ReimbursementReviewActivity.class);
        BottomNavHelper.NavItem strikes = new BottomNavHelper.NavItem(null, StrikeDashboardActivity.class);

        assertEquals(AdminDashboardActivity.class, home.target);
        assertEquals(AdminTasksActivity.class, tasks.target);
        assertEquals(QrScannerActivity.class, scanner.target);
        assertEquals(ReimbursementReviewActivity.class, expenses.target);
        assertEquals(StrikeDashboardActivity.class, strikes.target);

        assertEquals(0, BottomNavHelper.INDEX_HOME);
        assertEquals(1, BottomNavHelper.INDEX_TASKS);
        assertEquals(2, BottomNavHelper.INDEX_SCANNER);
        assertEquals(3, BottomNavHelper.INDEX_EXPENSES);
        assertEquals(4, BottomNavHelper.INDEX_STRIKES);
    }

    @Test
    public void testMemberNavigationDestinations() {
        BottomNavHelper.NavItem home = new BottomNavHelper.NavItem(null, MemberDashboardActivity.class);
        BottomNavHelper.NavItem tasks = new BottomNavHelper.NavItem(null, MemberTasksActivity.class);
        BottomNavHelper.NavItem agenda = new BottomNavHelper.NavItem(null, MemberAgendaActivity.class);
        BottomNavHelper.NavItem expenses = new BottomNavHelper.NavItem(null, ExpenseHistoryActivity.class);
        BottomNavHelper.NavItem profile = new BottomNavHelper.NavItem(null, ProfileActivity.class);

        assertEquals(MemberDashboardActivity.class, home.target);
        assertEquals(MemberTasksActivity.class, tasks.target);
        assertEquals(MemberAgendaActivity.class, agenda.target);
        assertEquals(ExpenseHistoryActivity.class, expenses.target);
        assertEquals(ProfileActivity.class, profile.target);

        assertEquals(0, BottomNavHelper.INDEX_HOME);
        assertEquals(1, BottomNavHelper.INDEX_TASKS);
        assertEquals(2, BottomNavHelper.INDEX_AGENDA);
        assertEquals(3, BottomNavHelper.INDEX_EXPENSES);
        assertEquals(4, BottomNavHelper.INDEX_PROFILE);
    }

    @Test
    public void testRoleAccessDistinction() {
        sessionManager.authenticate("admin", "admin123", new SessionManager.AuthCallback() {
            @Override public void onSuccess(User user) {}
            @Override public void onError(String message) {}
        });
        User admin = sessionManager.getCurrentUser();
        assertNotNull(admin);
        assertTrue("Admin user must have Super Admin authority", admin.isSuperAdmin());

        sessionManager.logout();
        sessionManager.authenticate("lead", "lead123", new SessionManager.AuthCallback() {
            @Override public void onSuccess(User user) {}
            @Override public void onError(String message) {}
        });
        User member = sessionManager.getCurrentUser();
        assertNotNull(member);
        assertFalse("Core Member must not have Super Admin authority", member.isSuperAdmin());
    }

    @Test
    public void testFindMemberByIdAndStrikeCountThreshold() {
        User memberWith3Strikes = repo.findMemberById("usr_member_03");
        assertNotNull("User usr_member_03 should exist", memberWith3Strikes);
        assertEquals("Rahul Patil", memberWith3Strikes.getName());
        assertTrue("Member should meet 3+ critical strike threshold", memberWith3Strikes.getStrikeCount() >= 3);

        User memberNormal = repo.findMemberById("usr_member_02");
        assertNotNull("User usr_member_02 should exist", memberNormal);
        assertEquals(0, memberNormal.getStrikeCount());
        assertFalse("Member should not meet 3+ critical strike threshold", memberNormal.getStrikeCount() >= 3);
    }

    // --- QR Encryption, Decryption & Replay Attack Prevention Tests ---

    @Test
    public void testQrEncryptionDecryptionSuccess() throws Exception {
        String prn = "PRN2024001";
        String userId = "usr_member_01";

        String token = QrCryptoManager.encryptPayload(prn, userId);
        assertNotNull("Generated token must not be null", token);
        assertFalse("Generated token must not be empty", token.trim().isEmpty());

        QrCryptoManager.DecryptedPayload result = QrCryptoManager.decryptPayload(token);
        assertTrue("Decryption must succeed for valid ciphertext", result.isSuccess());
        assertEquals(prn, result.getPrn());
        assertEquals(userId, result.getUserId());
        assertFalse("Fresh QR code must not be expired", result.isExpired());
        assertTrue("Timestamp must be valid", result.getTimestamp() > 0);
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testQrTamperedCiphertextRejected() throws Exception {
        String prn = "PRN2024001";
        String userId = "usr_member_01";

        String token = QrCryptoManager.encryptPayload(prn, userId);

        // Tamper with characters in the middle of ciphertext
        char[] chars = token.toCharArray();
        chars[chars.length / 2] = (chars[chars.length / 2] == 'A') ? 'B' : 'A';
        String tamperedToken = new String(chars);

        QrCryptoManager.DecryptedPayload result = QrCryptoManager.decryptPayload(tamperedToken);
        assertFalse("Tampered token must fail authenticated AES-GCM verification", result.isSuccess());
        assertNotNull("Failure must contain error description", result.getErrorMessage());
    }

    @Test
    public void testQrReplayAttackExpiredPayloadRejected() throws Exception {
        String prn = "PRN2024001";
        String userId = "usr_member_01";
        // Create a payload timestamped 20 minutes ago (exceeding 15-minute window)
        long expiredTimestamp = System.currentTimeMillis() - (20 * 60 * 1000L);

        String expiredToken = QrCryptoManager.encryptPayloadWithTime(prn, userId, expiredTimestamp);
        assertNotNull(expiredToken);

        QrCryptoManager.DecryptedPayload result = QrCryptoManager.decryptPayload(expiredToken, QrCryptoManager.DEFAULT_EXPIRATION_MS);
        assertFalse("Expired payload must not be marked success", result.isSuccess());
        assertTrue("isExpired() must be true for token older than 15 minutes", result.isExpired());
        assertEquals(prn, result.getPrn());
        assertEquals(userId, result.getUserId());
    }

    @Test
    public void testQrInvalidOrCorruptTokenRejected() {
        QrCryptoManager.DecryptedPayload nullResult = QrCryptoManager.decryptPayload(null);
        assertFalse("Null token must fail gracefully", nullResult.isSuccess());

        QrCryptoManager.DecryptedPayload emptyResult = QrCryptoManager.decryptPayload("   ");
        assertFalse("Empty token must fail gracefully", emptyResult.isSuccess());

        QrCryptoManager.DecryptedPayload garbageResult = QrCryptoManager.decryptPayload("not_a_valid_base64_payload_at_all");
        assertFalse("Garbage payload must fail gracefully", garbageResult.isSuccess());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQrEncryptThrowsOnEmptyPrn() throws Exception {
        QrCryptoManager.encryptPayload("", "usr_member_01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQrEncryptThrowsOnEmptyUserId() throws Exception {
        QrCryptoManager.encryptPayload("PRN2024001", "");
    }
}
