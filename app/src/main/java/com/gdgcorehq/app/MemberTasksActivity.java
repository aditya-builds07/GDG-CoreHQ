package com.gdgcorehq.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityMemberTasksBinding;
import com.gdgcorehq.app.databinding.ItemTaskCardBinding;
import com.gdgcorehq.app.model.TaskItem;
import com.gdgcorehq.app.model.User;
import com.google.android.material.tabs.TabLayout;
import java.util.List;

/**
 * Core Member Tasks Activity.
 * Strictly adheres to PRD §3.3:
 * Personal Kanban board with To-Do, In Progress, and Done columns,
 * one-tap status updates, and a Global View toggle to monitor all departments.
 */
public class MemberTasksActivity extends AppCompatActivity {

    private ActivityMemberTasksBinding binding;
    private boolean isGlobalView = false;
    private TaskItem.Status selectedStatus = TaskItem.Status.IN_PROGRESS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberTasksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
        // Default to In Progress tab (index 1) for active focus
        binding.tabStatus.selectTab(binding.tabStatus.getTabAt(1));
        renderTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderTasks();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Floating Pill Navigation Bar
        BottomNavHelper.setupMemberNav(this, BottomNavHelper.INDEX_TASKS,
                binding.navHome, binding.navTasks, binding.navAgenda,
                binding.navExpenses, binding.navProfile);

        binding.btnToggleGlobal.setOnClickListener(v -> {
            isGlobalView = !isGlobalView;
            if (isGlobalView) {
                binding.btnToggleGlobal.setText("My Tasks");
                binding.tvBoardTitle.setText("Global Task Board");
            } else {
                binding.btnToggleGlobal.setText("Global View");
                binding.tvBoardTitle.setText("My Task Kanban");
            }
            renderTasks();
        });

        binding.tabStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) selectedStatus = TaskItem.Status.TODO;
                else if (pos == 1) selectedStatus = TaskItem.Status.IN_PROGRESS;
                else selectedStatus = TaskItem.Status.DONE;
                renderTasks();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void renderTasks() {
        binding.layoutTasksContainer.removeAllViews();
        User currentUser = SessionManager.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getId() : "usr_member_01";

        List<TaskItem> tasks = isGlobalView ?
                MockDataRepository.getInstance().getAllTasks() :
                MockDataRepository.getInstance().getTasksForUser(currentUserId);

        int count = 0;
        for (TaskItem task : tasks) {
            if (task.getStatus() != selectedStatus) continue;

            count++;
            ItemTaskCardBinding cardBinding = ItemTaskCardBinding.inflate(
                    LayoutInflater.from(this), binding.layoutTasksContainer, false);

            cardBinding.tvTaskTitle.setText(task.getTitle());
            cardBinding.tvTaskDesc.setText(task.getDescription());
            cardBinding.tvTaskDept.setText(task.getDepartment());
            cardBinding.tvTaskAssignee.setText("👤 " + task.getAssigneeName());
            cardBinding.tvTaskDeadline.setText("Due: " + task.getDeadline());

            // 48h Overdue warning
            if (task.isOverdue48h() && task.getStatus() != TaskItem.Status.DONE) {
                cardBinding.tvOverdueBadge.setVisibility(View.VISIBLE);
            } else {
                cardBinding.tvOverdueBadge.setVisibility(View.GONE);
            }

            // Status Transitions for Member
            if (task.getStatus() == TaskItem.Status.TODO) {
                cardBinding.btnMoveProgress.setVisibility(View.VISIBLE);
                cardBinding.btnMoveProgress.setText("→ Start Task");
                cardBinding.btnMoveProgress.setOnClickListener(v -> {
                    MockDataRepository.getInstance().updateTaskStatus(task.getId(), TaskItem.Status.IN_PROGRESS);
                    renderTasks();
                });
                cardBinding.btnMoveDone.setVisibility(View.GONE);
            } else if (task.getStatus() == TaskItem.Status.IN_PROGRESS) {
                cardBinding.btnMoveProgress.setVisibility(View.GONE);
                cardBinding.btnMoveDone.setVisibility(View.VISIBLE);
                cardBinding.btnMoveDone.setText("✓ Mark Complete");
                cardBinding.btnMoveDone.setOnClickListener(v -> {
                    MockDataRepository.getInstance().updateTaskStatus(task.getId(), TaskItem.Status.DONE);
                    renderTasks();
                });
            } else {
                cardBinding.layoutStatusButtons.setVisibility(View.GONE);
            }

            binding.layoutTasksContainer.addView(cardBinding.getRoot());
        }

        if (count == 0) {
            android.widget.TextView emptyView = new android.widget.TextView(this);
            emptyView.setText(isGlobalView ?
                    "No chapter tasks in '" + selectedStatus.getDisplayName() + "'" :
                    "You have no personal tasks in '" + selectedStatus.getDisplayName() + "'");
            emptyView.setPadding(32, 64, 32, 32);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTextColor(getColor(R.color.text_secondary));
            binding.layoutTasksContainer.addView(emptyView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
