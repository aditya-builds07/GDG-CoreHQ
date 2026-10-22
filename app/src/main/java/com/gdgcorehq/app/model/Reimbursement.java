package com.gdgcorehq.app.model;

import java.io.Serializable;

/**
 * Represents an expense reimbursement request.
 * Per PRD §3.5 and PROJECT_RULES §4 (R22):
 * Rejection requires a mandatory reason note.
 */
public class Reimbursement implements Serializable {

    public enum Status {
        PENDING("Pending Review"),
        APPROVED("Approved (Reimbursed)"),
        REJECTED("Rejected");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String id;
    private String memberId;
    private String memberName;
    private double amount;
    private String date;
    private String category;
    private String receiptUri;
    private Status status;
    private String rejectionReason;

    public Reimbursement() {}

    public Reimbursement(String id, String memberId, String memberName, double amount,
                         String date, String category, String receiptUri, Status status,
                         String rejectionReason) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.receiptUri = receiptUri;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReceiptUri() { return receiptUri; }
    public void setReceiptUri(String receiptUri) { this.receiptUri = receiptUri; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
