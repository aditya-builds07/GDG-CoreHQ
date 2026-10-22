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

/**
 * Core Member Dashboard for GDG CoreHQ.
 * Strictly adheres to PRD §2.2 and §4:
 * Shows member profile, strike accountability badge, next meeting agenda link,
 * post-meeting form entry, assigned tasks overview, and reimbursement shortcuts.
 */
public class MemberDashboardActivity extends AppCompatActivity {

    private ActivityMemberDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        displayMemberData();
        setupNavigationAndActions();
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
            binding.tvMeetingSchedule.setText("📅 " + next.getDate() + " • " + next.getTime() + " • " + next.getLocation());
        }

        // Tasks Summary
        List<TaskItem> myTasks = MockDataRepository.getInstance().getTasksForUser(user.getId());
        long inProgress = myTasks.stream().filter(t -> t.getStatus() == TaskItem.Status.IN_PROGRESS).count();
        long todo = myTasks.stream().filter(t -> t.getStatus() == TaskItem.Status.TODO).count();
        binding.tvMyTaskSummary.setText(inProgress + " In Progress • " + todo + " To-Do");
    }

    private void setupNavigationAndActions() {
        // Handle back press from root dashboard to exit cleanly
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
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
        binding = null;
    }
}
