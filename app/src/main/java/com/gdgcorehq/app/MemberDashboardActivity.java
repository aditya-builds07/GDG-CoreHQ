package com.gdgcorehq.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityMemberDashboardBinding;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.TaskItem;
import com.gdgcorehq.app.model.User;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Core Member Dashboard for GDG CoreHQ.
 * Strictly adheres to PRD §2.2 and §4:
 * Shows member profile, strike accountability badge, next meeting agenda link,
 * post-meeting form entry, assigned tasks overview, and reimbursement shortcuts.
 */
public class MemberDashboardActivity extends AppCompatActivity {

    private ActivityMemberDashboardBinding binding;
    private ListenerRegistration attendanceListenerRegistration;
    private boolean hasPromptedReportForCurrentMeeting = false;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handled
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkNotificationPermission();
        displayMemberData();
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
        displayMemberData();
    }

    private void displayMemberData() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding.tvMemberGreeting.setText("Hi, " + user.getName().split(" ")[0]);
        binding.tvMemberRolePrn.setText(user.getDepartment() + " • " + user.getPrn());

        // Strike Status Indicator (PRD §3.6)
        int strikes = user.getStrikeCount();
        if (strikes == 0) {
            binding.tvStrikeTitle.setText("Accountability Status: Good");
            binding.tvStrikeSubtitle.setText("0 Strikes • Attendance & tasks strictly on schedule");
            binding.ivStrikeIcon.setBackgroundTintList(getColorStateList(R.color.color_success_container));
            binding.ivStrikeIcon.setColorFilter(getColor(R.color.status_success));
            binding.cardStrikeStatus.setCardBackgroundColor(getColor(R.color.surface));
        } else if (strikes < 3) {
            binding.tvStrikeTitle.setText("Accountability Warning (" + strikes + " Strike" + (strikes > 1 ? "s" : "") + ")");
            binding.tvStrikeSubtitle.setText("Warning notice active: keep attendance and tasks on track.");
            binding.ivStrikeIcon.setBackgroundTintList(getColorStateList(R.color.color_warning_container));
            binding.ivStrikeIcon.setColorFilter(getColor(R.color.status_warning));
            binding.cardStrikeStatus.setCardBackgroundColor(getColor(R.color.surface));
        } else {
            // 3+ Strikes Critical Highlight (PRD §3.6 / design.md §3.3)
            binding.tvStrikeTitle.setText("3+ Strikes: Review Required");
            binding.tvStrikeSubtitle.setText("Critical threshold exceeded. Please speak with Organizer.");
            binding.ivStrikeIcon.setBackgroundTintList(getColorStateList(R.color.color_error_container));
            binding.ivStrikeIcon.setColorFilter(getColor(R.color.status_error));
            binding.cardStrikeStatus.setCardBackgroundColor(getColor(R.color.color_error_container));
        }

        // Meeting Info
        Meeting next = MockDataRepository.getInstance().getNextUpcomingMeeting();
        if (next != null) {
            binding.tvMeetingTitle.setText(next.getTitle());
            binding.tvMeetingSchedule.setText(next.getDate() + " • " + next.getTime() + " • " + next.getLocation());
        }

        // Tasks Summary
        List<TaskItem> myTasks = MockDataRepository.getInstance().getTasksForUser(user.getId());
        long inProgress = myTasks.stream().filter(t -> t.getStatus() == TaskItem.Status.IN_PROGRESS).count();
        long todo = myTasks.stream().filter(t -> t.getStatus() == TaskItem.Status.TODO).count();
        binding.tvMyTaskSummary.setText(inProgress + " In Progress • " + todo + " To-Do");

        // Digital Encrypted Attendance QR (PRD §3.2)
        generateAndDisplayQr(user);

        // Attach Real-Time Attendance Listener (PRD §3.2 & §8.2)
        attachRealtimeAttendanceListener(user);
    }

    private void attachRealtimeAttendanceListener(User user) {
        if (user == null || user.getPrn() == null) return;
        Meeting nextMeeting = MockDataRepository.getInstance().getNextUpcomingMeeting();
        final String currentMeetingId = (nextMeeting != null) ? nextMeeting.getId() : "meet_101";

        if (attendanceListenerRegistration != null) {
            attendanceListenerRegistration.remove();
        }

        attendanceListenerRegistration = MockDataRepository.getInstance().listenForMemberAttendance(
                currentMeetingId,
                user.getPrn(),
                record -> {
                    if (record != null && "PRESENT".equalsIgnoreCase(record.getStatus())) {
                        boolean alreadySubmitted = MockDataRepository.getInstance().hasSubmittedReport(currentMeetingId, user.getId());
                        if (!alreadySubmitted && !hasPromptedReportForCurrentMeeting && !isFinishing()) {
                            hasPromptedReportForCurrentMeeting = true;
                            new androidx.appcompat.app.AlertDialog.Builder(MemberDashboardActivity.this)
                                    .setTitle("Attendance Verified!")
                                    .setMessage("Your attendance for " + (nextMeeting != null ? nextMeeting.getTitle() : "the meeting") + " has been recorded as PRESENT.\n\nPlease submit your weekly post-meeting accountability report.")
                                    .setPositiveButton("Submit Report Now", (dialog, which) -> {
                                        startActivity(new Intent(MemberDashboardActivity.this, PostMeetingReportActivity.class));
                                    })
                                    .setNegativeButton("Later", null)
                                    .show();
                        }
                    }
                }
        );
    }

    private void generateAndDisplayQr(User user) {
        if (user == null || user.getPrn() == null) return;
        binding.tvQrPrnLabel.setText("PRN: " + user.getPrn());
        try {
            String encryptedToken = com.gdgcorehq.app.data.QrCryptoManager.encryptPayload(user.getPrn(), user.getId());
            int qrSizePx = (int) (200 * getResources().getDisplayMetrics().density);
            android.graphics.Bitmap qrBitmap = com.gdgcorehq.app.data.QrCryptoManager.generateQrBitmap(encryptedToken, Math.max(qrSizePx, 256));
            binding.ivAttendanceQr.setImageBitmap(qrBitmap);
            binding.tvQrStatus.setText("Encrypted • Valid for 15 minutes");
            binding.tvQrStatus.setTextColor(getColor(R.color.text_secondary));
        } catch (Exception e) {
            binding.tvQrStatus.setText("Failed to generate QR: " + e.getMessage());
            binding.tvQrStatus.setTextColor(getColor(R.color.status_error));
        }
    }

    private void setupNavigationAndActions() {
        // Handle back press from root dashboard to exit cleanly
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        binding.btnRefreshQr.setOnClickListener(v -> {
            User user = SessionManager.getInstance().getCurrentUser();
            if (user != null) {
                generateAndDisplayQr(user);
                android.widget.Toast.makeText(this, "Attendance QR code refreshed.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnViewAgenda.setOnClickListener(v -> {
            Intent intent = new Intent(this, MemberAgendaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.btnPostMeetingReport.setOnClickListener(v ->
                startActivity(new Intent(this, PostMeetingReportActivity.class)));

        binding.cardMyTasks.setOnClickListener(v -> {
            Intent intent = new Intent(this, MemberTasksActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.btnNewExpense.setOnClickListener(v ->
                startActivity(new Intent(this, NewExpenseActivity.class)));

        binding.btnExpenseHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExpenseHistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Bottom Pill Navigation
        BottomNavHelper.setupMemberNav(this, BottomNavHelper.INDEX_HOME,
                binding.navHome, binding.navTasks, binding.navAgenda,
                binding.navExpenses, binding.navProfile);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (attendanceListenerRegistration != null) {
            attendanceListenerRegistration.remove();
            attendanceListenerRegistration = null;
        }
        binding = null;
    }
}
