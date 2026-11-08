package com.gdgcorehq.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityExpenseHistoryBinding;
import com.gdgcorehq.app.databinding.ItemReimbursementReviewBinding;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.User;
import java.util.List;

/**
 * Core Member Expense History Activity.
 * Strictly adheres to PRD §3.5:
 * View a digital history of submitted, approved, and rejected expenses,
 * including feedback and rejection reasons.
 */
public class ExpenseHistoryActivity extends AppCompatActivity {

    private ActivityExpenseHistoryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) {
            android.widget.Toast.makeText(this, "Session expired. Please log in again.", android.widget.Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityExpenseHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnNewExpense.setOnClickListener(v ->
                startActivity(new Intent(this, NewExpenseActivity.class)));

        // Floating Pill Navigation Bar
        BottomNavHelper.setupMemberNav(this, BottomNavHelper.INDEX_EXPENSES,
                binding.navHome, binding.navTasks, binding.navAgenda,
                binding.navExpenses, binding.navProfile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) {
            android.widget.Toast.makeText(this, "Session expired. Please log in again.", android.widget.Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        renderExpenses();
    }

    private void renderExpenses() {
        binding.layoutExpensesContainer.removeAllViews();
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) return;
        String memberId = current.getId();

        List<Reimbursement> list = MockDataRepository.getInstance().getReimbursementsForUser(memberId);

        if (list.isEmpty()) {
            binding.layoutExpensesContainer.addView(
                    com.gdgcorehq.app.util.EmptyStateHelper.createEmptyState(
                            this,
                            binding.layoutExpensesContainer,
                            R.drawable.ic_receipt,
                            "No Reimbursements Submitted",
                            "You have not submitted any reimbursement requests yet."
                    )
            );
            return;
        }

        for (Reimbursement r : list) {
            ItemReimbursementReviewBinding itemBinding = ItemReimbursementReviewBinding.inflate(
                    LayoutInflater.from(this), binding.layoutExpensesContainer, false);

            itemBinding.tvMemberName.setText(r.getCategory());
            itemBinding.tvCategoryDate.setText("Claimed on: " + r.getDate());
            itemBinding.tvAmount.setText(String.format("₹%.2f", r.getAmount()));
            itemBinding.tvReceiptFilename.setText("Receipt: " + r.getReceiptUri());
            itemBinding.layoutReviewActions.setVisibility(View.GONE); // Member cannot review own expenses

            if (r.getStatus() == Reimbursement.Status.PENDING) {
                itemBinding.tvStatusBadge.setText("Pending Review");
                itemBinding.tvStatusBadge.setBackgroundTintList(getColorStateList(R.color.color_warning_container));
                itemBinding.tvStatusBadge.setTextColor(getColor(R.color.status_warning));
                itemBinding.tvRejectionReason.setVisibility(View.GONE);

            } else if (r.getStatus() == Reimbursement.Status.APPROVED) {
                itemBinding.tvStatusBadge.setText("Approved (Reimbursed)");
                itemBinding.tvStatusBadge.setBackgroundTintList(getColorStateList(R.color.color_success_container));
                itemBinding.tvStatusBadge.setTextColor(getColor(R.color.status_success));
                itemBinding.tvRejectionReason.setVisibility(View.GONE);

            } else {
                itemBinding.tvStatusBadge.setText("Rejected");
                itemBinding.tvStatusBadge.setBackgroundTintList(getColorStateList(R.color.color_error_container));
                itemBinding.tvStatusBadge.setTextColor(getColor(R.color.status_error));

                if (r.getRejectionReason() != null && !r.getRejectionReason().isEmpty()) {
                    itemBinding.tvRejectionReason.setVisibility(View.VISIBLE);
                    itemBinding.tvRejectionReason.setText("Admin Reason: " + r.getRejectionReason());
                } else {
                    itemBinding.tvRejectionReason.setVisibility(View.GONE);
                }
            }

            binding.layoutExpensesContainer.addView(itemBinding.getRoot());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
