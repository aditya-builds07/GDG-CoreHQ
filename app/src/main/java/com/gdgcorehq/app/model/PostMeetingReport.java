package com.gdgcorehq.app.model;

import java.io.Serializable;

/**
 * Represents a post-meeting report submitted by a core member.
 * Strictly adheres to PRD §3.2 and §3.4:
 * Captures weekly progress, roadblocks/assistance needed, and meeting feedback.
 */
public class PostMeetingReport implements Serializable {

    private String id;
    private String meetingId;
    private String memberId;
    private String memberName;
    private String progress;
    private String roadblocks;
    private String feedback;
    private String timestamp;

    public PostMeetingReport() {}

    public PostMeetingReport(String id, String meetingId, String memberId, String memberName,
                             String progress, String roadblocks, String feedback, String timestamp) {
        this.id = id;
        this.meetingId = meetingId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.progress = progress;
        this.roadblocks = roadblocks;
        this.feedback = feedback;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }

    public String getRoadblocks() { return roadblocks; }
    public void setRoadblocks(String roadblocks) { this.roadblocks = roadblocks; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
