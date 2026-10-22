package com.gdgcorehq.app.model;

import java.io.Serializable;

/**
 * Represents a member of GDG CoreHQ.
 * Roles strictly adhere to PRD: SUPER_ADMIN and CORE_MEMBER.
 */
public class User implements Serializable {

    public enum Role {
        SUPER_ADMIN("Super Admin"),
        CORE_MEMBER("Core Member");

        private final String displayName;

        Role(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String id;
    private String name;
    private String prn;
    private String email;
    private Role role;
    private String department;
    private int strikeCount;

    public User() {}

    public User(String id, String name, String prn, String email, Role role, String department, int strikeCount) {
        this.id = id;
        this.name = name;
        this.prn = prn;
        this.email = email;
        this.role = role;
        this.department = department;
        this.strikeCount = strikeCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrn() { return prn; }
    public void setPrn(String prn) { this.prn = prn; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getStrikeCount() { return strikeCount; }
    public void setStrikeCount(int strikeCount) { this.strikeCount = strikeCount; }

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }
}
