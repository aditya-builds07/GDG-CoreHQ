package com.gdgcorehq.app.model;

import java.io.Serializable;

/**
 * Represents an operational task assigned to a core member.
 * Status strictly follows PRD §3.3: To-Do, In Progress, Done.
 */
public class TaskItem implements Serializable {

    public enum Status {
        TODO("To-Do"),
        IN_PROGRESS("In Progress"),
        DONE("Done");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String id;
    private String title;
    private String description;
    private String assigneeId;
    private String assigneeName;
    private String deadline;
    private Status status;
    private String department;
    private boolean isOverdue48h;

    public TaskItem() {}

    public TaskItem(String id, String title, String description, String assigneeId,
                    String assigneeName, String deadline, Status status,
                    String department, boolean isOverdue48h) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.deadline = deadline;
        this.status = status;
        this.department = department;
        this.isOverdue48h = isOverdue48h;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isOverdue48h() { return isOverdue48h; }
    public void setOverdue48h(boolean overdue48h) { isOverdue48h = overdue48h; }
}
