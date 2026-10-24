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
        seedInitialData();
        initFirestore();
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
            if (!snapshots.isEmpty()) {
                synchronized (members) {
                    members.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        User user = parseUser(doc);
                        if (user != null) members.add(user);
                    }
                }
            } else {
                // Seed Firestore if collection is empty
                seedUsersToFirestore();
            }
        });

        // 2. Tasks collection
        db.collection("tasks").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            if (!snapshots.isEmpty()) {
                synchronized (tasks) {
                    tasks.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        TaskItem task = parseTask(doc);
                        if (task != null) tasks.add(task);
                    }
                }
            } else {
                seedTasksToFirestore();
            }
        });

        // 3. Meetings collection
        db.collection("meetings").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            if (!snapshots.isEmpty()) {
                synchronized (meetings) {
                    meetings.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Meeting meeting = parseMeeting(doc);
                        if (meeting != null) meetings.add(meeting);
                    }
                }
            } else {
                seedMeetingsToFirestore();
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
            if (!snapshots.isEmpty()) {
                synchronized (reimbursements) {
                    reimbursements.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Reimbursement r = parseReimbursement(doc);
                        if (r != null) reimbursements.add(r);
                    }
                }
            } else {
                seedReimbursementsToFirestore();
            }
        });

        // 7. Strikes collection
        db.collection("strikes").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            if (!snapshots.isEmpty()) {
                synchronized (strikeRecords) {
                    strikeRecords.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        StrikeRecord s = parseStrike(doc);
                        if (s != null) strikeRecords.add(s);
                    }
                }
            } else {
                seedStrikesToFirestore();
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

    public List<User> getMembers() {
        synchronized (members) {
            return new ArrayList<>(members);
        }
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

    // --- Seeding Methods ---

    private void seedUsersToFirestore() {
        if (!isFirebaseAvailable || db == null) return;
        synchronized (members) {
            for (User u : members) {
                db.collection("users").document(u.getId()).set(userToMap(u), SetOptions.merge());
            }
        }
    }

    private void seedTasksToFirestore() {
        if (!isFirebaseAvailable || db == null) return;
        synchronized (tasks) {
            for (TaskItem t : tasks) {
                db.collection("tasks").document(t.getId()).set(taskToMap(t), SetOptions.merge());
            }
        }
    }

    private void seedMeetingsToFirestore() {
        if (!isFirebaseAvailable || db == null) return;
        synchronized (meetings) {
            for (Meeting m : meetings) {
                db.collection("meetings").document(m.getId()).set(meetingToMap(m), SetOptions.merge());
            }
        }
    }

    private void seedReimbursementsToFirestore() {
        if (!isFirebaseAvailable || db == null) return;
        synchronized (reimbursements) {
            for (Reimbursement r : reimbursements) {
                db.collection("reimbursements").document(r.getId()).set(reimbursementToMap(r), SetOptions.merge());
            }
        }
    }

    private void seedStrikesToFirestore() {
        if (!isFirebaseAvailable || db == null) return;
        synchronized (strikeRecords) {
            for (StrikeRecord s : strikeRecords) {
                db.collection("strikes").document(s.getId()).set(strikeToMap(s), SetOptions.merge());
            }
        }
    }

    private void seedInitialData() {
        // Sample Core Members
        members.add(new User("usr_admin_01", "Aditya Desai", "PRN2022001", "aditya.d@gdg.org", User.Role.SUPER_ADMIN, "Organizer", 0));
        members.add(new User("usr_member_01", "Siddhi Kulkarni", "PRN2024001", "siddhi.k@gdg.org", User.Role.CORE_MEMBER, "Design & Web Lead", 1));
        members.add(new User("usr_member_02", "Tanmay Patil", "PRN2024002", "tanmay.p@gdg.org", User.Role.CORE_MEMBER, "PR & Outreach", 0));
        members.add(new User("usr_member_03", "Rahul Patil", "PRN2024003", "rahul.p@gdg.org", User.Role.CORE_MEMBER, "Technical Team", 3));
        members.add(new User("usr_member_04", "Ananya Shinde", "PRN2024004", "ananya.s@gdg.org", User.Role.CORE_MEMBER, "Logistics Lead", 2));

        // Sample Tasks
        tasks.add(new TaskItem("task_01", "Finalize Cloud Community Day Poster", "Export Instagram & LinkedIn flyers in approved brand dimensions.", "usr_member_01", "Siddhi Kulkarni", "Oct 24, 2026", TaskItem.Status.IN_PROGRESS, "Design", false));
        tasks.add(new TaskItem("task_02", "Book Auditorium & Sound System", "Coordinate with college administration for Friday seminar hall.", "usr_member_04", "Ananya Shinde", "Oct 22, 2026", TaskItem.Status.TODO, "Logistics", false));
        tasks.add(new TaskItem("task_03", "Publish Speaker Announcement Post", "Draft caption and tag speaker profiles on Twitter and LinkedIn.", "usr_member_02", "Tanmay Patil", "Oct 21, 2026", TaskItem.Status.DONE, "PR", false));
        tasks.add(new TaskItem("task_04", "Deploy Hackathon Registration Form", "Complete Google Cloud backend validation and link to CoreHQ.", "usr_member_03", "Rahul Patil", "Oct 18, 2026", TaskItem.Status.TODO, "Technical", true));

        // Sample Meetings & Agendas
        meetings.add(new Meeting(
                "meet_101",
                "Weekly Chapter Sync & Hackathon Prep",
                "Friday, Oct 24, 2026",
                "05:00 PM - 06:30 PM",
                "Seminar Hall B, TKIET",
                Arrays.asList(
                        "Review registration milestone (150+ signups)",
                        "Sponsorship deck updates & budget allocation",
                        "Departmental roadblock check-in",
                        "Post-meeting report submissions"
                ),
                "Aditya Desai (Organizer)",
                true
        ));
        meetings.add(new Meeting(
                "meet_100",
                "Core Team Onboarding & Roles Alignment",
                "Friday, Oct 17, 2026",
                "04:30 PM - 06:00 PM",
                "Computer Lab 4, TKIET",
                Arrays.asList(
                        "Welcome new tech and design leads",
                        "Introduce GDG CoreHQ operations system",
                        "PRN ID card distribution for attendance"
                ),
                "Aditya Desai (Organizer)",
                false
        ));

        // Sample Reimbursements
        reimbursements.add(new Reimbursement(
                "exp_01",
                "usr_member_04",
                "Ananya Shinde",
                1250.00,
                "Oct 19, 2026",
                "Printing & Swag",
                "receipt_banners_tkiet.jpg",
                Reimbursement.Status.PENDING,
                null
        ));
        reimbursements.add(new Reimbursement(
                "exp_02",
                "usr_member_01",
                "Siddhi Kulkarni",
                480.00,
                "Oct 15, 2026",
                "Refreshments",
                "receipt_snacks_oct15.jpg",
                Reimbursement.Status.APPROVED,
                null
        ));
        reimbursements.add(new Reimbursement(
                "exp_03",
                "usr_member_03",
                "Rahul Patil",
                2100.00,
                "Oct 12, 2026",
                "Logistics",
                "receipt_cables.jpg",
                Reimbursement.Status.REJECTED,
                "Receipt image blurry and missing vendor GST tax invoice. Please re-upload legible bill."
        ));

        // Sample Strike Records
        strikeRecords.add(new StrikeRecord(
                "strk_01",
                "usr_member_03",
                "Rahul Patil",
                StrikeRecord.TriggerType.OVERDUE_TASK,
                "Task 'Deploy Hackathon Registration Form' exceeded deadline by > 48 hours without prior notice.",
                "Oct 20, 2026",
                false,
                null,
                null
        ));
        strikeRecords.add(new StrikeRecord(
                "strk_02",
                "usr_member_03",
                "Rahul Patil",
                StrikeRecord.TriggerType.ABSENCE,
                "Unexcused absence from Chapter Sync (Oct 17, 2026). No attendance scan recorded.",
                "Oct 17, 2026",
                false,
                null,
                null
        ));
        strikeRecords.add(new StrikeRecord(
                "strk_03",
                "usr_member_03",
                "Rahul Patil",
                StrikeRecord.TriggerType.ABSENCE,
                "Unexcused absence from Chapter Sync (Oct 10, 2026).",
                "Oct 10, 2026",
                false,
                null,
                null
        ));
        strikeRecords.add(new StrikeRecord(
                "strk_04",
                "usr_member_04",
                "Ananya Shinde",
                StrikeRecord.TriggerType.ABSENCE,
                "Missed meeting on Oct 10 due to exam overlap.",
                "Oct 10, 2026",
                true,
                "Pardoned by Organizer: Mid-term university exam clash verified.",
                "Aditya Desai"
        ));
    }
}
