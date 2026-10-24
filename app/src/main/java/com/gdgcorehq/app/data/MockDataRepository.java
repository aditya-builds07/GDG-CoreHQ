package com.gdgcorehq.app.data;

import com.gdgcorehq.app.model.AttendanceRecord;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.PostMeetingReport;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.StrikeRecord;
import com.gdgcorehq.app.model.TaskItem;
import com.gdgcorehq.app.model.User;

import java.util.List;

/**
 * Facade Repository for GDG CoreHQ operations.
 * Strictly adheres to PRD specifications and seamless repository swap constraint:
 * Preserves existing signatures to ensure ZERO changes are required across UI Activities,
 * while delegating all data queries, mutations, and real-time syncing to FirebaseDataRepository.
 */
public class MockDataRepository {

    private static MockDataRepository instance;
    private final FirebaseDataRepository backend;

    private MockDataRepository() {
        backend = FirebaseDataRepository.getInstance();
    }

    public static synchronized MockDataRepository getInstance() {
        if (instance == null) {
            instance = new MockDataRepository();
        }
        return instance;
    }

    // --- Task Methods ---
    public List<TaskItem> getAllTasks() {
        return backend.getAllTasks();
    }

    public List<TaskItem> getTasksForUser(String userId) {
        return backend.getTasksForUser(userId);
    }

    public boolean addTask(TaskItem task) {
        return backend.addTask(task);
    }

    public boolean updateTaskStatus(String taskId, TaskItem.Status newStatus) {
        return backend.updateTaskStatus(taskId, newStatus);
    }

    // --- Meeting Methods ---
    public List<Meeting> getMeetings() {
        return backend.getMeetings();
    }

    public Meeting getNextUpcomingMeeting() {
        return backend.getNextUpcomingMeeting();
    }

    public boolean addMeeting(Meeting meeting) {
        return backend.addMeeting(meeting);
    }

    // --- Post-Meeting Report Methods ---
    public boolean submitPostMeetingReport(PostMeetingReport report) {
        return backend.submitPostMeetingReport(report);
    }

    public List<PostMeetingReport> getPostMeetingReportsForMeeting(String meetingId) {
        return backend.getPostMeetingReportsForMeeting(meetingId);
    }

    public boolean hasSubmittedReport(String meetingId, String memberId) {
        return backend.hasSubmittedReport(meetingId, memberId);
    }

    // --- Reimbursement Methods ---
    public List<Reimbursement> getAllReimbursements() {
        return backend.getAllReimbursements();
    }

    public List<Reimbursement> getReimbursementsForUser(String userId) {
        return backend.getReimbursementsForUser(userId);
    }

    public boolean submitReimbursement(Reimbursement r) {
        return backend.submitReimbursement(r);
    }

    public boolean approveReimbursement(String id) {
        return backend.approveReimbursement(id);
    }

    public boolean rejectReimbursement(String id, String reason) {
        return backend.rejectReimbursement(id, reason);
    }

    // --- QR Attendance Methods ---
    public User findMemberByPrn(String prn) {
        return backend.findMemberByPrn(prn);
    }

    public User findMemberById(String userId) {
        return backend.findMemberById(userId);
    }

    public boolean isAttendanceAlreadyRecorded(String meetingId, String prn) {
        return backend.isAttendanceAlreadyRecorded(meetingId, prn);
    }

    public boolean recordAttendance(AttendanceRecord record) {
        return backend.recordAttendance(record);
    }

    // --- Strike Methods ---
    public void addStrike(StrikeRecord record) {
        backend.addStrike(record);
    }

    public List<StrikeRecord> getAllStrikes() {
        return backend.getAllStrikes();
    }

    public List<StrikeRecord> getStrikesForUser(String userId) {
        return backend.getStrikesForUser(userId);
    }

    public boolean pardonStrike(String strikeId, String reason, String adminName) {
        return backend.pardonStrike(strikeId, reason, adminName);
    }

    public List<User> getMembers() {
        return backend.getMembers();
    }
}
