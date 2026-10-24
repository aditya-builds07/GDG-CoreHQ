package com.gdgcorehq.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.databinding.ActivityAdminTasksBinding;
import com.gdgcorehq.app.databinding.ItemTaskCardBinding;
import com.gdgcorehq.app.model.TaskItem;
import com.google.android.material.tabs.TabLayout;
import java.util.List;

/**
 * Super Admin Global Task Management and Kanban Board.
 * Strictly adheres to PRD §3.3:
 * View all department tasks, filter by department, update statuses,
 * and assign new tasks with deadlines.
 */
public class AdminTasksActivity extends AppCompatActivity {

    private ActivityAdminTasksBinding binding;
    private String selectedDept = "All";
    private TaskItem.Status selectedStatus = TaskItem.Status.TODO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.gdgcorehq.app.model.User user = com.gdgcorehq.app.data.SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            Toast.makeText(this, "Access denied: Super Admin authorization required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityAdminTasksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
        renderTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderTasks();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        // Floating Pill Navigation Bar
        BottomNavHelper.setupAdminNav(this, BottomNavHelper.INDEX_TASKS,
                binding.navHome, binding.navTasks, binding.navScanner,
                binding.navExpenses, binding.navStrikes);

        // Status Tabs
        binding.tabStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) selectedStatus = TaskItem.Status.TODO;
                else if (position == 1) selectedStatus = TaskItem.Status.IN_PROGRESS;
                else selectedStatus = TaskItem.Status.DONE;
                renderTasks();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Department Filters
        binding.filterAll.setOnClickListener(v -> setDepartmentFilter("All", binding.filterAll));
        binding.filterDesign.setOnClickListener(v -> setDepartmentFilter("Design", binding.filterDesign));
        binding.filterTech.setOnClickListener(v -> setDepartmentFilter("Technical", binding.filterTech));
        binding.filterPR.setOnClickListener(v -> setDepartmentFilter("PR", binding.filterPR));
        binding.filterLogistics.setOnClickListener(v -> setDepartmentFilter("Logistics", binding.filterLogistics));
    }

    private void setDepartmentFilter(String dept, View selectedView) {
        selectedDept = dept;

        // Reset backgrounds
        binding.filterAll.setBackgroundResource(R.drawable.bg_chip_rounded);
        binding.filterAll.setTextColor(getColor(R.color.text_secondary));
        binding.filterDesign.setBackgroundResource(R.drawable.bg_chip_rounded);
        binding.filterDesign.setTextColor(getColor(R.color.text_secondary));
        binding.filterTech.setBackgroundResource(R.drawable.bg_chip_rounded);
        binding.filterTech.setTextColor(getColor(R.color.text_secondary));
        binding.filterPR.setBackgroundResource(R.drawable.bg_chip_rounded);
        binding.filterPR.setTextColor(getColor(R.color.text_secondary));
        binding.filterLogistics.setBackgroundResource(R.drawable.bg_chip_rounded);
        binding.filterLogistics.setTextColor(getColor(R.color.text_secondary));

        // Highlight selected
        selectedView.setBackgroundResource(R.drawable.bg_nav_selected);
        ((android.widget.TextView) selectedView).setTextColor(getColor(R.color.surface));

        renderTasks();
    }

    private void renderTasks() {
        binding.layoutTasksContainer.removeAllViews();
        List<TaskItem> all = MockDataRepository.getInstance().getAllTasks();

        int count = 0;
        for (TaskItem task : all) {
            // Apply Status filter
            if (task.getStatus() != selectedStatus) continue;

            // Apply Department filter
            if (!selectedDept.equals("All") && !task.getDepartment().equalsIgnoreCase(selectedDept)) {
                continue;
            }

            count++;
            ItemTaskCardBinding cardBinding = ItemTaskCardBinding.inflate(
                    LayoutInflater.from(this), binding.layoutTasksContainer, false);

            cardBinding.tvTaskTitle.setText(task.getTitle());
            cardBinding.tvTaskDesc.setText(task.getDescription());
            cardBinding.tvTaskDept.setText(task.getDepartment());
            cardBinding.tvTaskAssignee.setText("Assigned: " + task.getAssigneeName());
            cardBinding.tvTaskDeadline.setText("Due: " + task.getDeadline());

            // 48h Overdue warning
            if (task.isOverdue48h() && task.getStatus() != TaskItem.Status.DONE) {
                cardBinding.tvOverdueBadge.setVisibility(View.VISIBLE);
            } else {
                cardBinding.tvOverdueBadge.setVisibility(View.GONE);
            }

            // Quick Status Buttons
            if (task.getStatus() == TaskItem.Status.TODO) {
                cardBinding.btnMoveProgress.setVisibility(View.VISIBLE);
                cardBinding.btnMoveProgress.setText("→ In Progress");
                cardBinding.btnMoveProgress.setOnClickListener(v -> {
                    MockDataRepository.getInstance().updateTaskStatus(task.getId(), TaskItem.Status.IN_PROGRESS);
                    renderTasks();
                });
                cardBinding.btnMoveDone.setVisibility(View.GONE);
            } else if (task.getStatus() == TaskItem.Status.IN_PROGRESS) {
                cardBinding.btnMoveProgress.setVisibility(View.GONE);
                cardBinding.btnMoveDone.setVisibility(View.VISIBLE);
                cardBinding.btnMoveDone.setText("Mark Done");
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
            emptyView.setText("No tasks in '" + selectedStatus.getDisplayName() + "' for " + selectedDept);
            emptyView.setPadding(32, 64, 32, 32);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTextColor(getColor(R.color.text_secondary));
            binding.layoutTasksContainer.addView(emptyView);
        }
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);
        EditText etAssignee = dialogView.findViewById(R.id.etTaskAssignee);
        EditText etDeadline = dialogView.findViewById(R.id.etTaskDeadline);
        EditText etDept = dialogView.findViewById(R.id.etTaskDept);

        new AlertDialog.Builder(this)
                .setTitle("Delegate New Task")
                .setView(dialogView)
                .setPositiveButton("Create Task", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    String assignee = etAssignee.getText().toString().trim();
                    String deadline = etDeadline.getText().toString().trim();
                    String dept = etDept.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Task title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    TaskItem newTask = new TaskItem(
                            "task_" + System.currentTimeMillis(),
                            title,
                            desc.isEmpty() ? "No description provided." : desc,
                            "usr_member_01",
                            assignee.isEmpty() ? "Siddhi Kulkarni" : assignee,
                            deadline.isEmpty() ? "Oct 26, 2026" : deadline,
                            TaskItem.Status.TODO,
                            dept.isEmpty() ? "General" : dept,
                            false
                    );

                    MockDataRepository.getInstance().addTask(newTask);
                    Toast.makeText(this, "Task assigned to " + newTask.getAssigneeName(), Toast.LENGTH_SHORT).show();
                    renderTasks();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
