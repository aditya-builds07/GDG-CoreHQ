package com.gdgcorehq.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityAdminDashboardBinding;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.User;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

/**
 * Super Admin Dashboard for GDG CoreHQ.
 * Strictly adheres to PRD §2.1 and §4:
 * Full operations overview, QR attendance launch, task management,
 * agenda publishing, expense review, and strike accountability.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            android.widget.Toast.makeText(this, "Access denied: Super Admin authorization required.", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkNotificationPermission();
        displayUserData();
        setupNavigationAndActions();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMetrics();
    }

    private void displayUserData() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
            String firstName = user.getName().contains(" ") ? user.getName().split(" ")[0] : user.getName();
            binding.tvAdminGreeting.setText("Welcome, " + firstName);
        } else {
            binding.tvAdminGreeting.setText("Welcome, Admin");
        }
    }

    private void refreshMetrics() {
        MockDataRepository repo = MockDataRepository.getInstance();

        // Tasks count
        int taskCount = repo.getAllTasks().size();
        binding.tvTaskCount.setText(taskCount + " Active");

        // Next Meeting
        Meeting next = repo.getNextUpcomingMeeting();
        if (next != null) {
            binding.tvMeetingTitle.setText(next.getTitle());
            binding.tvMeetingTimeLocation.setText(next.getDate() + " • " + next.getTime() + " • " + next.getLocation());
        } else {
            binding.tvMeetingTitle.setText("No Upcoming Meeting");
            binding.tvMeetingTimeLocation.setText("No meeting scheduled yet.");
        }

        // Pending Reimbursements
        long pendingExp = repo.getAllReimbursements().stream()
                .filter(r -> r.getStatus() == com.gdgcorehq.app.model.Reimbursement.Status.PENDING)
                .count();
        binding.tvPendingReimbursements.setText(pendingExp + " Pending");

        // High strike alerts (>= 3)
        long alertStrikes = repo.getMembers().stream()
                .filter(m -> m.getStrikeCount() >= 3)
                .count();
        binding.tvStrikeAlerts.setText(alertStrikes + " Member 3+");
    }

    private void setupNavigationAndActions() {
        // Handle back press from root dashboard to exit cleanly
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        // Quick Action Cards
        binding.cardQrScanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, QrScannerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.cardTasksOverview.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminTasksActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.cardMeetingOverview.setOnClickListener(v ->
                startActivity(new Intent(this, AgendaManagementActivity.class)));

        binding.btnManageAgenda.setOnClickListener(v ->
                startActivity(new Intent(this, AgendaManagementActivity.class)));

        binding.cardReimbursementsOverview.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReimbursementReviewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.cardStrikeOverview.setOnClickListener(v -> {
            Intent intent = new Intent(this, StrikeDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.ivProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Floating Pill Navigation Bar
        BottomNavHelper.setupAdminNav(this, BottomNavHelper.INDEX_HOME,
                binding.navHome, binding.navTasks, binding.navScanner,
                binding.navExpenses, binding.navStrikes);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
