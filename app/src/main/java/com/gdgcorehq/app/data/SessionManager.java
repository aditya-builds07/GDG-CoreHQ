package com.gdgcorehq.app.data;

import com.gdgcorehq.app.model.User;
import java.util.HashMap;
import java.util.Map;

/**
 * Session provider for GDG CoreHQ.
 * Strictly adheres to PROJECT_RULES.md §1 (R03, R07), PRD §3.1, and TEAM_RESPONSIBILITIES.md §2:
 * Maintains local session state and validates pre-provisioned credentials.
 * Invalid credentials never grant access. Privileged server secrets are not stored.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    // Pre-provisioned user directory (temporary until Developer B integrates Firebase Auth)
    private static class ProvisionedAccount {
        final User user;
        final String password;

        ProvisionedAccount(User user, String password) {
            this.user = user;
            this.password = password;
        }
    }

    private final Map<String, ProvisionedAccount> provisionedAccounts = new HashMap<>();

    private SessionManager() {
        seedProvisionedAccounts();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    private void seedProvisionedAccounts() {
        // 1. Super Admin
        User admin = new User(
                "usr_admin_01",
                "Aditya Desai",
                "PRN2022001",
                "aditya.d@gdg.org",
                User.Role.SUPER_ADMIN,
                "Organizer / Lead",
                0
        );
        ProvisionedAccount adminAcc = new ProvisionedAccount(admin, "admin123");
        provisionedAccounts.put("admin", adminAcc);
        provisionedAccounts.put("organizer", adminAcc);
        provisionedAccounts.put("aditya.d@gdg.org", adminAcc);
        provisionedAccounts.put("prn2022001", adminAcc);

        // 2. Core Member (Design Lead)
        User siddhi = new User(
                "usr_member_01",
                "Siddhi Kulkarni",
                "PRN2024001",
                "siddhi.k@gdg.org",
                User.Role.CORE_MEMBER,
                "Design & Web Lead",
                1
        );
        ProvisionedAccount siddhiAcc = new ProvisionedAccount(siddhi, "lead123");
        provisionedAccounts.put("lead", siddhiAcc);
        provisionedAccounts.put("siddhi", siddhiAcc);
        provisionedAccounts.put("siddhi.k@gdg.org", siddhiAcc);
        provisionedAccounts.put("prn2024001", siddhiAcc);

        // 3. Core Member (PR & Outreach)
        User tanmay = new User(
                "usr_member_02",
                "Tanmay Patil",
                "PRN2024002",
                "tanmay.p@gdg.org",
                User.Role.CORE_MEMBER,
                "PR & Outreach",
                0
        );
        ProvisionedAccount tanmayAcc = new ProvisionedAccount(tanmay, "member123");
        provisionedAccounts.put("tanmay", tanmayAcc);
        provisionedAccounts.put("tanmay.p@gdg.org", tanmayAcc);
        provisionedAccounts.put("prn2024002", tanmayAcc);

        // 4. Core Member (3 Strikes Critical Flag per PRD §3.6)
        User rahul = new User(
                "usr_member_03",
                "Rahul Patil",
                "PRN2024003",
                "rahul.p@gdg.org",
                User.Role.CORE_MEMBER,
                "Technical Team",
                3
        );
        ProvisionedAccount rahulAcc = new ProvisionedAccount(rahul, "strike123");
        provisionedAccounts.put("strike", rahulAcc);
        provisionedAccounts.put("member3", rahulAcc);
        provisionedAccounts.put("rahul", rahulAcc);
        provisionedAccounts.put("rahul.p@gdg.org", rahulAcc);
        provisionedAccounts.put("prn2024003", rahulAcc);

        // 5. Core Member (Logistics Lead)
        User ananya = new User(
                "usr_member_04",
                "Ananya Shinde",
                "PRN2024004",
                "ananya.s@gdg.org",
                User.Role.CORE_MEMBER,
                "Logistics Lead",
                2
        );
        ProvisionedAccount ananyaAcc = new ProvisionedAccount(ananya, "member123");
        provisionedAccounts.put("ananya", ananyaAcc);
        provisionedAccounts.put("ananya.s@gdg.org", ananyaAcc);
        provisionedAccounts.put("prn2024004", ananyaAcc);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    /**
     * Authenticates with assigned username/ID and password.
     * Enforces PRD §3.1: No public registration. Unprovisioned accounts are rejected.
     */
    public void authenticate(String identifier, String password, AuthCallback callback) {
        if (identifier == null || identifier.trim().isEmpty()) {
            callback.onError("Please enter your assigned PRN or Username");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            callback.onError("Please enter your password");
            return;
        }

        String id = identifier.trim().toLowerCase();

        ProvisionedAccount account = provisionedAccounts.get(id);
        if (account == null) {
            callback.onError("Account not found. Accounts are provisioned by the Chapter Organizer.");
            return;
        }

        if (!account.password.equals(password)) {
            callback.onError("Incorrect password. Please verify your credentials.");
            return;
        }

        currentUser = account.user;
        callback.onSuccess(currentUser);
    }
}
