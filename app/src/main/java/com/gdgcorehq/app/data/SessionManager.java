package com.gdgcorehq.app.data;

import com.gdgcorehq.app.model.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

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
    public static class ProvisionedAccount {
        public final User user;
        public final String password;

        public ProvisionedAccount(User user, String password) {
            this.user = user;
            this.password = password;
        }
    }

    private final Map<String, ProvisionedAccount> provisionedAccounts = new HashMap<>();

    private SessionManager() {
        // Production starts 100% clean, reading only from Firestore/FirebaseAuth.
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Seeds test accounts for offline / JVM unit test execution (Rule R24).
     */
    public void seedTestAccounts(Map<String, ProvisionedAccount> testAccounts) {
        synchronized (provisionedAccounts) {
            provisionedAccounts.clear();
            if (testAccounts != null) {
                provisionedAccounts.putAll(testAccounts);
            }
        }
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
        try {
            if (FirebaseApp.getInstance() != null) {
                FirebaseAuth.getInstance().signOut();
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Checks if a persistent session exists in FirebaseAuth and restores currentUser from Firestore.
     * Invoked during SplashActivity to pre-warm user session without blocking video playback.
     */
    public void restoreSession(AuthCallback callback) {
        if (currentUser != null) {
            if (callback != null) callback.onSuccess(currentUser);
            return;
        }

        try {
            if (FirebaseApp.getInstance() != null) {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseUser fbUser = auth.getCurrentUser();
                if (fbUser != null) {
                    String uid = fbUser.getUid();
                    FirebaseFirestore.getInstance().collection("users").document(uid).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                    currentUser = parseUserFromDoc(task.getResult());
                                    postAuthSuccess(currentUser);
                                    if (callback != null) callback.onSuccess(currentUser);
                                } else {
                                    String email = fbUser.getEmail();
                                    ProvisionedAccount localAccount = email != null ? provisionedAccounts.get(email.toLowerCase()) : null;
                                    if (localAccount != null) {
                                        currentUser = localAccount.user;
                                    } else {
                                        currentUser = new User(uid, fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "GDG Member", "PRN2024001", fbUser.getEmail(), User.Role.CORE_MEMBER, "General", 0);
                                    }
                                    postAuthSuccess(currentUser);
                                    if (callback != null) callback.onSuccess(currentUser);
                                }
                            });
                    return;
                }
            }
        } catch (Throwable ignored) {}

        if (callback != null) callback.onError("No active session");
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    /**
     * Authenticates with assigned username/ID/email and password.
     * Authenticates against live FirebaseAuth when available, with Firestore role fetching,
     * and preserves fallback to pre-provisioned directory for local JVM testing and offline access.
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
        ProvisionedAccount localAccount = provisionedAccounts.get(id);

        boolean isFirebaseAvailable = false;
        try {
            isFirebaseAvailable = (FirebaseApp.getInstance() != null);
        } catch (Throwable ignored) {}

        if (!isFirebaseAvailable) {
            // Local JVM / offline testing fallback
            authenticateLocal(id, password, callback);
            return;
        }

        // Resolve email for live Firebase Auth
        String emailToUse = identifier.trim();
        if (!emailToUse.contains("@")) {
            if (localAccount != null && localAccount.user != null && localAccount.user.getEmail() != null) {
                emailToUse = localAccount.user.getEmail();
            } else {
                emailToUse = id + "@gdg.org";
            }
        }

        final String finalEmail = emailToUse;
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInWithEmailAndPassword(finalEmail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            FirebaseUser fbUser = task.getResult().getUser();
                            String uid = fbUser != null ? fbUser.getUid() : null;
                            if (uid != null) {
                                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                        .addOnCompleteListener(userDocTask -> {
                                            if (userDocTask.isSuccessful() && userDocTask.getResult() != null && userDocTask.getResult().exists()) {
                                                DocumentSnapshot doc = userDocTask.getResult();
                                                currentUser = parseUserFromDoc(doc);
                                            } else if (localAccount != null) {
                                                currentUser = localAccount.user;
                                            } else {
                                                currentUser = new User(uid, fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "GDG Member", "PRN2024001", fbUser.getEmail(), User.Role.CORE_MEMBER, "General", 0);
                                            }
                                            postAuthSuccess(currentUser);
                                            callback.onSuccess(currentUser);
                                        });
                            } else {
                                postAuthSuccess(localAccount != null ? localAccount.user : null);
                                callback.onSuccess(currentUser);
                            }
                        } else {
                            // If live auth fails (e.g. unseeded auth user in Firebase console), check local provisioned directory
                            if (localAccount != null && localAccount.password.equals(password)) {
                                currentUser = localAccount.user;
                                postAuthSuccess(currentUser);
                                callback.onSuccess(currentUser);
                            } else {
                                String errorMsg = "Incorrect password or account not found.";
                                if (task.getException() != null && task.getException().getMessage() != null) {
                                    errorMsg = task.getException().getMessage();
                                }
                                callback.onError(errorMsg);
                            }
                        }
                    });
        } catch (Throwable t) {
            authenticateLocal(id, password, callback);
        }
    }

    private void authenticateLocal(String id, String password, AuthCallback callback) {
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

    private void postAuthSuccess(User user) {
        if (user == null) return;
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("meeting_agendas");
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                FirebaseDataRepository.getInstance().updateUserFcmToken(user.getId(), token);
            });
        } catch (Throwable ignored) {}
    }

    private User parseUserFromDoc(DocumentSnapshot doc) {
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
    }
}
