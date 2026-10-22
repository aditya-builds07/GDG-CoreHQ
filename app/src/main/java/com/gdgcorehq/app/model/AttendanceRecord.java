package com.gdgcorehq.app.model;

import java.io.Serializable;

/**
 * Represents an attendance confirmation record created via QR scan.
 * Per PRD §3.2 and PROJECT_RULES §5:
 * Contains member reference, meeting ID, timestamp, status, and recorder ID.
 */
public class AttendanceRecord implements Serializable {

    private String id;
    private String meetingId;
    private String prn;
    private String memberName;
    private String memberRole;
    private String timestamp;
    private String status; // "PRESENT", "ABSENT"
    private String recordedById;

    public AttendanceRecord() {}

    public AttendanceRecord(String id, String meetingId, String prn, String memberName,
                            String memberRole, String timestamp, String status, String recordedById) {
        this.id = id;
        this.meetingId = meetingId;
        this.prn = prn;
        this.memberName = memberName;
        this.memberRole = memberRole;
        this.timestamp = timestamp;
        this.status = status;
        this.recordedById = recordedById;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }

    public String getPrn() { return prn; }
    public void setPrn(String prn) { this.prn = prn; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecordedById() { return recordedById; }
    public void setRecordedById(String recordedById) { this.recordedById = recordedById; }
}
