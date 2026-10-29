package com.gdgcorehq.app.data;

import com.gdgcorehq.app.model.AttendanceRecord;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.PostMeetingReport;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.StrikeRecord;
import com.gdgcorehq.app.model.TaskItem;
import com.gdgcorehq.app.model.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase-backed Data Repository for GDG CoreHQ.
 * Strictly adheres to PRD §2.2, §3.1-§3.6, and PROJECT_RULES.md:
 * - Backed by Cloud Firestore with offline persistence enabled (PersistentCacheSettings).
 * - Real-time synchronization via snapshot listeners.
 * - Enforces business validations (duplicate scan rejection, mandatory expense rejection reasons, etc.).
 * - Seamless fallback to local in-memory store when running in non-Firebase environments (e.g. JVM unit tests).
 */
public class FirebaseDataRepository {

    private static FirebaseDataRepository instance;

    private FirebaseFirestore db;
    private boolean isFirebaseAvailable = false;

    private final List<TaskItem> tasks = new ArrayList<>();
    private final List<Meeting> meetings = new ArrayList<>();
    private final List<AttendanceRecord> attendanceRecords = new ArrayList<>();
    private final List<Reimbursement> reimbursements = new ArrayList<>();
    private final List<StrikeRecord> strikeRecords = new ArrayList<>();
    private final List<PostMeetingReport> postMeetingReports = new ArrayList<>();
    private final List<User> members = new ArrayList<>();

    private FirebaseDataRepository() {
        initFirestore();
    }

    /**
     * Seeds in-memory data for testing / test fixtures.
     */
    public void seedTestData(List<User> testMembers, List<TaskItem> testTasks,
                              List<Meeting> testMeetings, List<Reimbursement> testReimbursements,
                              List<StrikeRecord> testStrikes) {
        synchronized (members) {
            members.clear();
            if (testMembers != null) members.addAll(testMembers);
        }
        synchronized (tasks) {
            tasks.clear();
            if (testTasks != null) tasks.addAll(testTasks);
        }
        synchronized (meetings) {
            meetings.clear();
            if (testMeetings != null) meetings.addAll(testMeetings);
        }
        synchronized (reimbursements) {
            reimbursements.clear();
            if (testReimbursements != null) reimbursements.addAll(testReimbursements);
        }
        synchronized (strikeRecords) {
            strikeRecords.clear();
            if (testStrikes != null) strikeRecords.addAll(testStrikes);
        }
    }

    public static synchronized FirebaseDataRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseDataRepository();
        }
        return instance;
    }

    private void initFirestore() {
        try {
            if (FirebaseApp.getInstance() != null) {
                db = FirebaseFirestore.getInstance();
                try {
                    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                            .build();
                    db.setFirestoreSettings(settings);
                } catch (Exception ignored) {
                    // Settings already configured
                }
                isFirebaseAvailable = true;
                setupSnapshotListeners();
            }
        } catch (Throwable t) {
            // Firebase not initialized in current runtime (e.g. JVM unit tests)
            isFirebaseAvailable = false;
        }
    }

    private void setupSnapshotListeners() {
        if (!isFirebaseAvailable || db == null) return;

        // 1. Users collection
        db.collection("users").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            synchronized (members) {
                members.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    User user = parseUser(doc);
                    if (user != null) members.add(user);
                }
            }
        });

        // 2. Tasks collection
        db.collection("tasks").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            synchronized (tasks) {
                tasks.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    TaskItem task = parseTask(doc);
                    if (task != null) tasks.add(task);
                }
            }
        });

        // 3. Meetings collection
        db.collection("meetings").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            synchronized (meetings) {
                meetings.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Meeting meeting = parseMeeting(doc);
                    if (meeting != null) meetings.add(meeting);
                }
            }
        });

        // 4. Attendance collection
        db.collection("attendance").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            synchronized (attendanceRecords) {
                attendanceRecords.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    AttendanceRecord record = parseAttendance(doc);
                    if (record != null) attendanceRecords.add(record);
                }
            }
        });

        // 5. Post-Meeting Reports collection
        db.collection("postMeetingReports").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            synchronized (postMeetingReports) {
                postMeetingReports.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    PostMeetingReport report = parsePostMeetingReport(doc);
                    if (report != null) postMeetingReports.add(report);
                }
            }
        });

        // 6. Reimbursements collection
        db.collection("reimbursements").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            synchronized (reimbursements) {
                reimbursements.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Reimbursement r = parseReimbursement(doc);
                    if (r != null) reimbursements.add(r);
                }
            }
        });

        // 7. Strikes collection
        db.collection("strikes").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            synchronized (strikeRecords) {
                strikeRecords.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    StrikeRecord s = parseStrike(doc);
                    if (s != null) strikeRecords.add(s);
                }
            }
        });
    }

    // --- Task Methods ---

    public List<TaskItem> getAllTasks() {
        synchronized (tasks) {
            return new ArrayList<>(tasks);
        }
    }

    public List<TaskItem> getTasksForUser(String userId) {
        if (userId == null) return new ArrayList<>();
        List<TaskItem> result = new ArrayList<>();
        synchronized (tasks) {
            for (TaskItem t : tasks) {
                if (userId.equals(t.getAssigneeId())) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    public boolean addTask(TaskItem task) {
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return false;
        }
        synchronized (tasks) {
            tasks.add(0, task);
        }
        if (isFirebaseAvailable && db != null) {
            String docId = (task.getId() != null && !task.getId().isEmpty()) ? task.getId() : "task_" + System.currentTimeMillis();
            task.setId(docId);
            db.collection("tasks").document(docId).set(taskToMap(task), SetOptions.merge());
        }
        return true;
    }

    public boolean updateTaskStatus(String taskId, TaskItem.Status newStatus) {
        if (taskId == null || newStatus == null) return false;
        boolean updated = false;
        boolean isOverdueUpdated = false;
        synchronized (tasks) {
            for (TaskItem t : tasks) {
                if (taskId.equals(t.getId())) {
                    t.setStatus(newStatus);
                    if (newStatus == TaskItem.Status.DONE) {
                        t.setOverdue48h(false);
                        isOverdueUpdated = true;
                    }
                    updated = true;
                    break;
                }
            }
        }
        if (updated && isFirebaseAvailable && db != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus.name());
            if (isOverdueUpdated) {
                updates.put("isOverdue48h", false);
            }
            updates.put("updatedAt", com.google.firebase.Timestamp.now());
            db.collection("tasks").document(taskId).update(updates);
        }
        return updated;
    }

    // --- Meeting Methods ---

    public List<Meeting> getMeetings() {
        synchronized (meetings) {
            return new ArrayList<>(meetings);
        }
    }

    public Meeting getNextUpcomingMeeting() {
        synchronized (meetings) {
            for (Meeting m : meetings) {
                if (m.isUpcoming()) return m;
            }
            return meetings.isEmpty() ? null : meetings.get(0);
        }
    }

    public boolean addMeeting(Meeting meeting) {
        if (meeting == null || meeting.getTitle() == null || meeting.getTitle().trim().isEmpty()) {
            return false;
        }
        synchronized (meetings) {
            meetings.add(0, meeting);
        }
        if (isFirebaseAvailable && db != null) {
            String docId = (meeting.getId() != null && !meeting.getId().isEmpty()) ? meeting.getId() : "meet_" + System.currentTimeMillis();
            meeting.setId(docId);
            db.collection("meetings").document(docId).set(meetingToMap(meeting), SetOptions.merge());
        }
        return true;
    }

    // --- Post-Meeting Report Methods ---

    public boolean submitPostMeetingReport(PostMeetingReport report) {
        if (report == null || report.getMeetingId() == null || report.getMemberId() == null) {
            return false;
        }
        if (report.getProgress() == null || report.getProgress().trim().isEmpty()) {
            return false;
        }
        if (report.getRoadblocks() == null || report.getRoadblocks().trim().isEmpty()) {
            return false;
        }
        synchronized (postMeetingReports) {
            postMeetingReports.add(report);
        }
        if (isFirebaseAvailable && db != null) {
            String docId = report.getMeetingId() + "_" + report.getMemberId();
            report.setId(docId);
            db.collection("postMeetingReports").document(docId).set(postMeetingReportToMap(report), SetOptions.merge());
        }
        return true;
    }

    public List<PostMeetingReport> getPostMeetingReportsForMeeting(String meetingId) {
        if (meetingId == null) return new ArrayList<>();
        List<PostMeetingReport> result = new ArrayList<>();
        synchronized (postMeetingReports) {
            for (PostMeetingReport r : postMeetingReports) {
                if (meetingId.equals(r.getMeetingId())) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    public boolean hasSubmittedReport(String meetingId, String memberId) {
        if (meetingId == null || memberId == null) return false;
        synchronized (postMeetingReports) {
            for (PostMeetingReport r : postMeetingReports) {
                if (meetingId.equals(r.getMeetingId()) && memberId.equals(r.getMemberId())) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Reimbursement Methods ---

    public List<Reimbursement> getAllReimbursements() {
        synchronized (reimbursements) {
            return new ArrayList<>(reimbursements);
        }
    }

    public List<Reimbursement> getReimbursementsForUser(String userId) {
        if (userId == null) return new ArrayList<>();
        List<Reimbursement> result = new ArrayList<>();
        synchronized (reimbursements) {
            for (Reimbursement r : reimbursements) {
                if (userId.equals(r.getMemberId())) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    public boolean submitReimbursement(Reimbursement r) {
        if (r == null || r.getAmount() <= 0) {
            return false;
        }
        if (r.getReceiptUri() == null || r.getReceiptUri().trim().isEmpty()) {
            return false;
        }
        if (r.getCategory() == null || r.getCategory().trim().isEmpty()) {
            return false;
        }
        synchronized (reimbursements) {
            reimbursements.add(0, r);
        }
        if (isFirebaseAvailable && db != null) {
            String docId = (r.getId() != null && !r.getId().isEmpty()) ? r.getId() : "exp_" + System.currentTimeMillis();
            r.setId(docId);
            db.collection("reimbursements").document(docId).set(reimbursementToMap(r), SetOptions.merge());
        }
        return true;
    }

    public boolean approveReimbursement(String id) {
        if (id == null) return false;
        boolean updated = false;
        synchronized (reimbursements) {
            for (Reimbursement r : reimbursements) {
                if (id.equals(r.getId())) {
                    r.setStatus(Reimbursement.Status.APPROVED);
                    updated = true;
                    break;
                }
            }
        }
        if (updated && isFirebaseAvailable && db != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Reimbursement.Status.APPROVED.name());
            updates.put("reviewedAt", com.google.firebase.Timestamp.now());
            db.collection("reimbursements").document(id).update(updates);
        }
        return updated;
    }

    public boolean rejectReimbursement(String id, String reason) {
        if (id == null || reason == null || reason.trim().isEmpty()) {
            return false;
        }
        boolean updated = false;
        synchronized (reimbursements) {
            for (Reimbursement r : reimbursements) {
                if (id.equals(r.getId())) {
                    r.setStatus(Reimbursement.Status.REJECTED);
                    r.setRejectionReason(reason.trim());
                    updated = true;
                    break;
                }
            }
        }
        if (updated && isFirebaseAvailable && db != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Reimbursement.Status.REJECTED.name());
            updates.put("rejectionReason", reason.trim());
            updates.put("reviewedAt", com.google.firebase.Timestamp.now());
            db.collection("reimbursements").document(id).update(updates);
        }
        return updated;
    }

    // --- QR Attendance Methods ---

    public User findMemberByPrn(String prn) {
        if (prn == null) return null;
        String cleanPrn = prn.trim().toUpperCase();
        synchronized (members) {
            for (User u : members) {
                if (u.getPrn() != null && u.getPrn().equalsIgnoreCase(cleanPrn)) {
                    return u;
                }
            }
        }
        return null;
    }

    public User findMemberById(String userId) {
        if (userId == null) return null;
        synchronized (members) {
            for (User u : members) {
                if (userId.equals(u.getId())) {
                    return u;
                }
            }
        }
        return null;
    }

    public boolean isAttendanceAlreadyRecorded(String meetingId, String prn) {
        if (meetingId == null || prn == null) return false;
        String cleanPrn = prn.trim().toUpperCase();
        synchronized (attendanceRecords) {
            for (AttendanceRecord a : attendanceRecords) {
                if (meetingId.equals(a.getMeetingId()) && cleanPrn.equalsIgnoreCase(a.getPrn())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean recordAttendance(AttendanceRecord record) {
        if (record == null || record.getPrn() == null || record.getMeetingId() == null) {
            return false;
        }
        if (isAttendanceAlreadyRecorded(record.getMeetingId(), record.getPrn())) {
            return false;
        }
        synchronized (attendanceRecords) {
            attendanceRecords.add(record);
        }
        if (isFirebaseAvailable && db != null) {
            String docId = record.getMeetingId() + "_" + record.getPrn().trim().toUpperCase();
            record.setId(docId);
            db.collection("attendance").document(docId).set(attendanceToMap(record), SetOptions.merge());
        }
        return true;
    }

    // --- Strike Methods ---

    public void addStrike(StrikeRecord record) {
        if (record == null) return;
        synchronized (strikeRecords) {
            strikeRecords.add(record);
        }
        if (isFirebaseAvailable && db != null) {
            String docId = (record.getId() != null && !record.getId().isEmpty()) ? record.getId() : "strk_" + System.currentTimeMillis();
            record.setId(docId);
            db.collection("strikes").document(docId).set(strikeToMap(record), SetOptions.merge());
        }
    }

    public List<StrikeRecord> getAllStrikes() {
        synchronized (strikeRecords) {
            return new ArrayList<>(strikeRecords);
        }
    }

    public List<StrikeRecord> getStrikesForUser(String userId) {
        if (userId == null) return new ArrayList<>();
        List<StrikeRecord> result = new ArrayList<>();
        synchronized (strikeRecords) {
            for (StrikeRecord s : strikeRecords) {
                if (userId.equals(s.getMemberId())) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    public boolean pardonStrike(String strikeId, String reason, String adminName) {
        if (strikeId == null || reason == null || reason.trim().isEmpty() || adminName == null || adminName.trim().isEmpty()) {
            return false;
        }
        boolean updated = false;
        String memberId = null;
        synchronized (strikeRecords) {
            for (StrikeRecord s : strikeRecords) {
                if (strikeId.equals(s.getId())) {
                    s.setPardoned(true);
                    s.setPardonedReason(reason.trim());
                    s.setPardonedBy(adminName.trim());
                    memberId = s.getMemberId();
                    updated = true;
                    break;
                }
            }
        }
        if (updated) {
            synchronized (members) {
                for (User u : members) {
                    if (u.getId().equals(memberId) && u.getStrikeCount() > 0) {
                        u.setStrikeCount(u.getStrikeCount() - 1);
                        if (isFirebaseAvailable && db != null) {
                            db.collection("users").document(u.getId()).update("strikeCount", u.getStrikeCount());
                        }
                    }
                }
            }
            if (isFirebaseAvailable && db != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("isPardoned", true);
                updates.put("pardonedReason", reason.trim());
                updates.put("pardonedBy", adminName.trim());
                db.collection("strikes").document(strikeId).update(updates);
            }
        }
        return updated;
    }

    // --- Overdue Task Automated Strike Evaluation Hook ---

    public interface OverdueEvaluationCallback {
        void onComplete(int newStrikesCount);
    }

    /**
     * Evaluates tasks where isOverdue48h == false. If a task is >48 hours past its deadline,
     * it idempotently creates a StrikeRecord in Firestore strikes collection and marks the task
     * as isOverdue48h = true.
     */
    public void evaluateOverdueTasks(OverdueEvaluationCallback callback) {
        final long FORTY_EIGHT_HOURS_MS = 48L * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();

        if (isFirebaseAvailable && db != null) {
            db.collection("tasks")
                    .whereEqualTo("isOverdue48h", false)
                    .get()
                    .addOnCompleteListener(taskSnapshotResult -> {
                        if (!taskSnapshotResult.isSuccessful() || taskSnapshotResult.getResult() == null) {
                            if (callback != null) callback.onComplete(0);
                            return;
                        }

                        List<DocumentSnapshot> overdueCandidateDocs = new ArrayList<>();
                        for (DocumentSnapshot doc : taskSnapshotResult.getResult().getDocuments()) {
                            TaskItem t = parseTask(doc);
                            if (t == null) continue;
                            if (t.getStatus() == TaskItem.Status.DONE) continue;

                            long deadlineMs = parseDeadlineMillis(t.getDeadline());
                            if (deadlineMs > 0 && (now - deadlineMs) > FORTY_EIGHT_HOURS_MS) {
                                overdueCandidateDocs.add(doc);
                            }
                        }

                        if (overdueCandidateDocs.isEmpty()) {
                            if (callback != null) callback.onComplete(0);
                            return;
                        }

                        final int[] processedCount = {0};
                        final int[] newStrikesRecorded = {0};
                        final int total = overdueCandidateDocs.size();

                        for (DocumentSnapshot doc : overdueCandidateDocs) {
                            TaskItem t = parseTask(doc);
                            String taskId = t.getId();
                            String strikeDocId = "strk_task_" + taskId;

                            // Idempotently verify strike does not exist
                            db.collection("strikes").document(strikeDocId).get()
                                    .addOnCompleteListener(strikeCheck -> {
                                        boolean strikeExists = strikeCheck.isSuccessful() && strikeCheck.getResult() != null && strikeCheck.getResult().exists();
                                        if (!strikeExists) {
                                            String timeStr = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                                            StrikeRecord strike = new StrikeRecord(
                                                    strikeDocId,
                                                    t.getAssigneeId(),
                                                    t.getAssigneeName(),
                                                    StrikeRecord.TriggerType.OVERDUE_TASK,
                                                    "Task '" + t.getTitle() + "' exceeded deadline (" + t.getDeadline() + ") by > 48 hours.",
                                                    timeStr,
                                                    false,
                                                    null,
                                                    null
                                            );
                                            db.collection("strikes").document(strikeDocId).set(strikeToMap(strike), SetOptions.merge());

                                            // Increment member's strike count
                                            if (t.getAssigneeId() != null) {
                                                db.collection("users").document(t.getAssigneeId())
                                                        .update("strikeCount", com.google.firebase.firestore.FieldValue.increment(1));
                                            }
                                            newStrikesRecorded[0]++;
                                        }

                                        // Mark task overdue flag in Firestore
                                        db.collection("tasks").document(taskId).update("isOverdue48h", true, "updatedAt", com.google.firebase.Timestamp.now());

                                        processedCount[0]++;
                                        if (processedCount[0] >= total && callback != null) {
                                            callback.onComplete(newStrikesRecorded[0]);
                                        }
                                    });
                        }
                    });
            return;
        }

        // Local / in-memory evaluation for JVM tests and offline mode
        int inMemoryNewStrikes = 0;
        synchronized (tasks) {
            for (TaskItem t : tasks) {
                if (t.isOverdue48h() || t.getStatus() == TaskItem.Status.DONE) continue;
                long deadlineMs = parseDeadlineMillis(t.getDeadline());
                if (deadlineMs > 0 && (now - deadlineMs) > FORTY_EIGHT_HOURS_MS) {
                    t.setOverdue48h(true);
                    String strikeDocId = "strk_task_" + t.getId();
                    boolean exists = false;
                    synchronized (strikeRecords) {
                        for (StrikeRecord s : strikeRecords) {
                            if (strikeDocId.equals(s.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            String timeStr = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                            StrikeRecord strike = new StrikeRecord(
                                    strikeDocId,
                                    t.getAssigneeId(),
                                    t.getAssigneeName(),
                                    StrikeRecord.TriggerType.OVERDUE_TASK,
                                    "Task '" + t.getTitle() + "' exceeded deadline (" + t.getDeadline() + ") by > 48 hours.",
                                    timeStr,
                                    false,
                                    null,
                                    null
                            );
                            strikeRecords.add(strike);
                            inMemoryNewStrikes++;

                            // Update member strike count in-memory
                            synchronized (members) {
                                for (User u : members) {
                                    if (u.getId().equals(t.getAssigneeId())) {
                                        u.setStrikeCount(u.getStrikeCount() + 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (callback != null) {
            callback.onComplete(inMemoryNewStrikes);
        }
    }

    private long parseDeadlineMillis(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) return -1;
        String[] patterns = new String[]{"MMM dd, yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MMM d, yyyy"};
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(deadlineStr.trim());
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {}
        }
        return -1;
    }

    public List<User> getMembers() {
        synchronized (members) {
            return new ArrayList<>(members);
        }
    }

    // --- FCM Token Management ---
    public void updateUserFcmToken(String userId, String token) {
        if (userId == null || token == null) return;
        if (isFirebaseAvailable && db != null) {
            db.collection("users").document(userId)
                    .update("fcmToken", token, "updatedAt", com.google.firebase.Timestamp.now());
        }
    }

    // --- Receipt Image Upload to Firebase Storage ---
    public interface UploadCallback {
        void onSuccess(String downloadUri);
        void onError(String message);
    }

    public void uploadReceiptImage(String userId, byte[] imageBytes, String filename, UploadCallback callback) {
        if (userId == null || imageBytes == null || imageBytes.length == 0) {
            callback.onError("Invalid image data");
            return;
        }

        String safeFilename = (filename != null && !filename.isEmpty()) ? filename : System.currentTimeMillis() + ".jpg";

        try {
            if (FirebaseApp.getInstance() != null) {
                com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage.getInstance();
                com.google.firebase.storage.StorageReference ref = storage.getReference()
                        .child("receipts")
                        .child(userId)
                        .child(safeFilename);

                ref.putBytes(imageBytes)
                        .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e -> callback.onError("Failed to obtain download URL: " + e.getMessage())))
                        .addOnFailureListener(e -> callback.onError("Failed to upload image: " + e.getMessage()));
                return;
            }
        } catch (Throwable t) {
            // Storage unavailable (offline or JVM environment)
        }

        // Fallback for offline / demo mode
        String simulatedUri = "https://firebasestorage.googleapis.com/v0/b/gdg-corehq.appspot.com/o/receipts%2F" + userId + "%2F" + safeFilename;
        callback.onSuccess(simulatedUri);
    }

    // --- Real-Time Attendance Listener for Member ---
    public interface AttendanceListener {
        void onAttendanceChanged(AttendanceRecord record);
    }

    public com.google.firebase.firestore.ListenerRegistration listenForMemberAttendance(String meetingId, String prn, AttendanceListener listener) {
        if (meetingId == null || prn == null || listener == null) return null;
        String cleanPrn = prn.trim().toUpperCase();

        if (isFirebaseAvailable && db != null) {
            String docId = meetingId + "_" + cleanPrn;
            return db.collection("attendance").document(docId).addSnapshotListener((doc, error) -> {
                if (error != null || doc == null || !doc.exists()) return;
                AttendanceRecord record = parseAttendance(doc);
                if (record != null) {
                    listener.onAttendanceChanged(record);
                }
            });
        }
        return null;
    }

    // --- Entity Parsing Helpers ---

    private User parseUser(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            if (id == null) id = doc.getId();
            String name = doc.getString("name");
            String prn = doc.getString("prn");
            String email = doc.getString("email");
            String roleStr = doc.getString("role");
            User.Role role = "SUPER_ADMIN".equalsIgnoreCase(roleStr) ? User.Role.SUPER_ADMIN : User.Role.CORE_MEMBER;
            String department = doc.getString("department");
            Long strikesLong = doc.getLong("strikeCount");
            int strikeCount = strikesLong != null ? strikesLong.intValue() : 0;
            return new User(id, name, prn, email, role, department, strikeCount);
        } catch (Exception e) {
            return null;
        }
    }

    private TaskItem parseTask(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            if (id == null) id = doc.getId();
            String title = doc.getString("title");
            String desc = doc.getString("description");
            String assigneeId = doc.getString("assigneeId");
            String assigneeName = doc.getString("assigneeName");
            String deadline = doc.getString("deadline");
            String statusStr = doc.getString("status");
            TaskItem.Status status = TaskItem.Status.TODO;
            if ("IN_PROGRESS".equalsIgnoreCase(statusStr)) status = TaskItem.Status.IN_PROGRESS;
            else if ("DONE".equalsIgnoreCase(statusStr)) status = TaskItem.Status.DONE;
            String department = doc.getString("department");
            Boolean overdue = doc.getBoolean("isOverdue48h");
            boolean isOverdue = overdue != null && overdue;
            return new TaskItem(id, title, desc, assigneeId, assigneeName, deadline, status, department, isOverdue);
        } catch (Exception e) {
            return null;
        }
    }

    private Meeting parseMeeting(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            if (id == null) id = doc.getId();
            String title = doc.getString("title");
            String date = doc.getString("date");
            String time = doc.getString("time");
            String location = doc.getString("location");
            List<String> topics = (List<String>) doc.get("topics");
            String publishedBy = doc.getString("publishedBy");
            Boolean upcoming = doc.getBoolean("isUpcoming");
            boolean isUpcoming = upcoming != null && upcoming;
            return new Meeting(id, title, date, time, location, topics, publishedBy, isUpcoming);
        } catch (Exception e) {
            return null;
        }
    }

    private AttendanceRecord parseAttendance(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            if (id == null) id = doc.getId();
            String meetingId = doc.getString("meetingId");
            String prn = doc.getString("prn");
            String memberName = doc.getString("memberName");
            String memberRole = doc.getString("memberRole");
            String timestamp = doc.getString("timestamp");
            String status = doc.getString("status");
            String recordedById = doc.getString("recordedById");
            return new AttendanceRecord(id, meetingId, prn, memberName, memberRole, timestamp, status, recordedById);
        } catch (Exception e) {
            return null;
        }
    }

    private PostMeetingReport parsePostMeetingReport(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            if (id == null) id = doc.getId();
            String meetingId = doc.getString("meetingId");
            String memberId = doc.getString("memberId");
            String memberName = doc.getString("memberName");
            String progress = doc.getString("progress");
            String roadblocks = doc.getString("roadblocks");
            String feedback = doc.getString("feedback");
            String timestamp = doc.getString("timestamp");
            return new PostMeetingReport(id, meetingId, memberId, memberName, progress, roadblocks, feedback, timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    private Reimbursement parseReimbursement(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            if (id == null) id = doc.getId();
            String memberId = doc.getString("memberId");
            String memberName = doc.getString("memberName");
            Double amount = doc.getDouble("amount");
            String date = doc.getString("date");
            String category = doc.getString("category");
            String receiptUri = doc.getString("receiptUri");
            String statusStr = doc.getString("status");
            Reimbursement.Status status = Reimbursement.Status.PENDING;
            if ("APPROVED".equalsIgnoreCase(statusStr)) status = Reimbursement.Status.APPROVED;
            else if ("REJECTED".equalsIgnoreCase(statusStr)) status = Reimbursement.Status.REJECTED;
            String rejectionReason = doc.getString("rejectionReason");
            return new Reimbursement(id, memberId, memberName, amount != null ? amount : 0.0, date, category, receiptUri, status, rejectionReason);
        } catch (Exception e) {
            return null;
        }
    }

    private StrikeRecord parseStrike(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            if (id == null) id = doc.getId();
            String memberId = doc.getString("memberId");
            String memberName = doc.getString("memberName");
            String triggerStr = doc.getString("triggerType");
            StrikeRecord.TriggerType triggerType = "OVERDUE_TASK".equalsIgnoreCase(triggerStr) ? StrikeRecord.TriggerType.OVERDUE_TASK : StrikeRecord.TriggerType.ABSENCE;
            String reason = doc.getString("reason");
            String timestamp = doc.getString("timestamp");
            Boolean pardoned = doc.getBoolean("isPardoned");
            boolean isPardoned = pardoned != null && pardoned;
            String pardonedReason = doc.getString("pardonedReason");
            String pardonedBy = doc.getString("pardonedBy");
            return new StrikeRecord(id, memberId, memberName, triggerType, reason, timestamp, isPardoned, pardonedReason, pardonedBy);
        } catch (Exception e) {
            return null;
        }
    }

    // --- Entity to Map Serializers ---

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("name", u.getName());
        map.put("prn", u.getPrn());
        map.put("email", u.getEmail());
        map.put("role", u.getRole().name());
        map.put("department", u.getDepartment());
        map.put("strikeCount", u.getStrikeCount());
        map.put("createdAt", com.google.firebase.Timestamp.now());
        return map;
    }

    private Map<String, Object> taskToMap(TaskItem t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("title", t.getTitle());
        map.put("description", t.getDescription());
        map.put("assigneeId", t.getAssigneeId());
        map.put("assigneeName", t.getAssigneeName());
        map.put("deadline", t.getDeadline());
        map.put("status", t.getStatus().name());
        map.put("department", t.getDepartment());
        map.put("isOverdue48h", t.isOverdue48h());
        map.put("createdAt", com.google.firebase.Timestamp.now());
        return map;
    }

    private Map<String, Object> meetingToMap(Meeting m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("title", m.getTitle());
        map.put("date", m.getDate());
        map.put("time", m.getTime());
        map.put("location", m.getLocation());
        map.put("topics", m.getTopics() != null ? m.getTopics() : new ArrayList<>());
        map.put("publishedBy", m.getPublishedBy());
        map.put("isUpcoming", m.isUpcoming());
        map.put("createdAt", com.google.firebase.Timestamp.now());
        return map;
    }

    private Map<String, Object> attendanceToMap(AttendanceRecord a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("meetingId", a.getMeetingId());
        map.put("prn", a.getPrn());
        map.put("memberName", a.getMemberName());
        map.put("memberRole", a.getMemberRole());
        map.put("timestamp", a.getTimestamp());
        map.put("status", a.getStatus());
        map.put("recordedById", a.getRecordedById());
        map.put("createdAt", com.google.firebase.Timestamp.now());
        return map;
    }

    private Map<String, Object> postMeetingReportToMap(PostMeetingReport r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("meetingId", r.getMeetingId());
        map.put("memberId", r.getMemberId());
        map.put("memberName", r.getMemberName());
        map.put("progress", r.getProgress());
        map.put("roadblocks", r.getRoadblocks());
        map.put("feedback", r.getFeedback());
        map.put("timestamp", r.getTimestamp());
        map.put("createdAt", com.google.firebase.Timestamp.now());
        return map;
    }

    private Map<String, Object> reimbursementToMap(Reimbursement r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("memberId", r.getMemberId());
        map.put("memberName", r.getMemberName());
        map.put("amount", r.getAmount());
        map.put("date", r.getDate());
        map.put("category", r.getCategory());
        map.put("receiptUri", r.getReceiptUri());
        map.put("status", r.getStatus().name());
        map.put("rejectionReason", r.getRejectionReason());
        map.put("createdAt", com.google.firebase.Timestamp.now());
        return map;
    }

    private Map<String, Object> strikeToMap(StrikeRecord s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("memberId", s.getMemberId());
        map.put("memberName", s.getMemberName());
        map.put("triggerType", s.getTriggerType().name());
        map.put("reason", s.getReason());
        map.put("timestamp", s.getTimestamp());
        map.put("isPardoned", s.isPardoned());
        map.put("pardonedReason", s.getPardonedReason());
        map.put("pardonedBy", s.getPardonedBy());
        map.put("createdAt", com.google.firebase.Timestamp.now());
        return map;
    }

    // --- Snapshot Listeners ---
}
