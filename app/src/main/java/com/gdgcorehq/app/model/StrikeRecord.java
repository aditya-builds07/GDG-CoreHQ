package com.gdgcorehq.app.model;

import java.io.Serializable;

/**
 * Represents an accountability strike issued to a core member.
 * Triggers match PRD §3.6: Meeting Absence or Task Overdue by 48 Hours.
 * Admin can pardon/remove strike when justified.
 */
public class StrikeRecord implements Serializable {

    public enum TriggerType {
        ABSENCE("Meeting Absence"),
        OVERDUE_TASK("Task Overdue > 48h");

        private final String displayName;

        TriggerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String id;
    private String memberId;
    private String memberName;
    private TriggerType triggerType;
    private String reason;
    private String timestamp;
    private boolean isPardoned;
    private String pardonedReason;
    private String pardonedBy;

    public StrikeRecord() {}

    public StrikeRecord(String id, String memberId, String memberName, TriggerType triggerType,
                        String reason, String timestamp, boolean isPardoned,
                        String pardonedReason, String pardonedBy) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.triggerType = triggerType;
        this.reason = reason;
        this.timestamp = timestamp;
        this.isPardoned = isPardoned;
        this.pardonedReason = pardonedReason;
        this.pardonedBy = pardonedBy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isPardoned() { return isPardoned; }
    public void setPardoned(boolean pardoned) { isPardoned = pardoned; }

    public String getPardonedReason() { return pardonedReason; }
    public void setPardonedReason(String pardonedReason) { this.pardonedReason = pardonedReason; }

    public String getPardonedBy() { return pardonedBy; }
    public void setPardonedBy(String pardonedBy) { this.pardonedBy = pardonedBy; }
}
