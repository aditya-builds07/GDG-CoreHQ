package com.gdgcorehq.app.data;

import com.gdgcorehq.app.model.AttendanceRecord;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.PostMeetingReport;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.StrikeRecord;
import com.gdgcorehq.app.model.TaskItem;
import com.gdgcorehq.app.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Repository for GDG CoreHQ operations.
 * Strictly follows PRD specifications and TEAM_RESPONSIBILITIES.md §4:
 * Exposes the repository interface for UI screens with complete business logic validation
 * while isolating Firebase for Developer B.
 */
public class MockDataRepository {

    private static MockDataRepository instance;

    private final List<TaskItem> tasks = new ArrayList<>();
    private final List<Meeting> meetings = new ArrayList<>();
    private final List<AttendanceRecord> attendanceRecords = new ArrayList<>();
    private final List<Reimbursement> reimbursements = new ArrayList<>();
    private final List<StrikeRecord> strikeRecords = new ArrayList<>();
    private final List<PostMeetingReport> postMeetingReports = new ArrayList<>();
    private final List<User> members = new ArrayList<>();

    private MockDataRepository() {
        seedInitialData();
    }

    public static synchronized MockDataRepository getInstance() {
        if (instance == null) {
            instance = new MockDataRepository();
        }
        return instance;
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

    // --- Task Methods ---
    public List<TaskItem> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<TaskItem> getTasksForUser(String userId) {
        List<TaskItem> result = new ArrayList<>();
        for (TaskItem t : tasks) {
            if (t.getAssigneeId().equals(userId)) {
                result.add(t);
            }
        }
        return result;
    }

    public boolean addTask(TaskItem task) {
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return false;
        }
        tasks.add(0, task);
        return true;
    }

    public boolean updateTaskStatus(String taskId, TaskItem.Status newStatus) {
        if (newStatus == null) return false;
        for (TaskItem t : tasks) {
            if (t.getId().equals(taskId)) {
                t.setStatus(newStatus);
                // When marked Done, task is no longer actively overdue
                if (newStatus == TaskItem.Status.DONE) {
                    t.setOverdue48h(false);
                }
                return true;
            }
        }
        return false;
    }

    // --- Meeting Methods ---
    public List<Meeting> getMeetings() {
        return new ArrayList<>(meetings);
    }

    public Meeting getNextUpcomingMeeting() {
        for (Meeting m : meetings) {
            if (m.isUpcoming()) return m;
        }
        return meetings.isEmpty() ? null : meetings.get(0);
    }

    public boolean addMeeting(Meeting meeting) {
        if (meeting == null || meeting.getTitle() == null || meeting.getTitle().trim().isEmpty()) {
            return false;
        }
        meetings.add(0, meeting);
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
        postMeetingReports.add(report);
        return true;
    }

    public List<PostMeetingReport> getPostMeetingReportsForMeeting(String meetingId) {
        List<PostMeetingReport> result = new ArrayList<>();
        for (PostMeetingReport r : postMeetingReports) {
            if (r.getMeetingId().equals(meetingId)) {
                result.add(r);
            }
        }
        return result;
    }

    public boolean hasSubmittedReport(String meetingId, String memberId) {
        for (PostMeetingReport r : postMeetingReports) {
            if (r.getMeetingId().equals(meetingId) && r.getMemberId().equals(memberId)) {
                return true;
            }
        }
        return false;
    }

    // --- Reimbursement Methods ---
    public List<Reimbursement> getAllReimbursements() {
        return new ArrayList<>(reimbursements);
    }

    public List<Reimbursement> getReimbursementsForUser(String userId) {
        List<Reimbursement> result = new ArrayList<>();
        for (Reimbursement r : reimbursements) {
            if (r.getMemberId().equals(userId)) {
                result.add(r);
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
        reimbursements.add(0, r);
        return true;
    }

    public boolean approveReimbursement(String id) {
        for (Reimbursement r : reimbursements) {
            if (r.getId().equals(id)) {
                r.setStatus(Reimbursement.Status.APPROVED);
                return true;
            }
        }
        return false;
    }

    public boolean rejectReimbursement(String id, String reason) {
        // Enforce PRD §3.5 and PROJECT_RULES R22: Rejection strictly requires a non-empty reason
        if (reason == null || reason.trim().isEmpty()) {
            return false;
        }
        for (Reimbursement r : reimbursements) {
            if (r.getId().equals(id)) {
                r.setStatus(Reimbursement.Status.REJECTED);
                r.setRejectionReason(reason.trim());
                return true;
            }
        }
        return false;
    }

    // --- QR Attendance Methods ---
    public User findMemberByPrn(String prn) {
        if (prn == null) return null;
        String cleanPrn = prn.trim().toUpperCase();
        for (User u : members) {
            if (u.getPrn().equalsIgnoreCase(cleanPrn)) {
                return u;
            }
        }
        return null;
    }

    public boolean isAttendanceAlreadyRecorded(String meetingId, String prn) {
        if (meetingId == null || prn == null) return false;
        String cleanPrn = prn.trim().toUpperCase();
        for (AttendanceRecord a : attendanceRecords) {
            if (a.getMeetingId().equals(meetingId) && a.getPrn().equalsIgnoreCase(cleanPrn)) {
                return true;
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
        attendanceRecords.add(record);
        return true;
    }

    // --- Strike Methods ---
    public void addStrike(StrikeRecord record) {
        if (record != null) {
            strikeRecords.add(record);
        }
    }

    public List<StrikeRecord> getAllStrikes() {
        return new ArrayList<>(strikeRecords);
    }

    public List<StrikeRecord> getStrikesForUser(String userId) {
        List<StrikeRecord> result = new ArrayList<>();
        for (StrikeRecord s : strikeRecords) {
            if (s.getMemberId().equals(userId)) {
                result.add(s);
            }
        }
        return result;
    }

    public boolean pardonStrike(String strikeId, String reason, String adminName) {
        if (reason == null || reason.trim().isEmpty() || adminName == null || adminName.trim().isEmpty()) {
            return false;
        }
        for (StrikeRecord s : strikeRecords) {
            if (s.getId().equals(strikeId)) {
                s.setPardoned(true);
                s.setPardonedReason(reason.trim());
                s.setPardonedBy(adminName.trim());

                // Decrement user strike count
                for (User u : members) {
                    if (u.getId().equals(s.getMemberId()) && u.getStrikeCount() > 0) {
                        u.setStrikeCount(u.getStrikeCount() - 1);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<User> getMembers() {
        return new ArrayList<>(members);
    }
}
