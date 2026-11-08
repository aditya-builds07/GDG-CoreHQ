package com.gdgcorehq.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.databinding.ActivityReimbursementReviewBinding;
import com.gdgcorehq.app.databinding.ItemReimbursementReviewBinding;
import com.gdgcorehq.app.model.Reimbursement;
import java.util.List;

/**
 * Super Admin Reimbursement Review Activity.
 * Strictly adheres to PRD §3.5 and PROJECT_RULES R22:
 * Admins review receipt images and approve payout or reject with mandatory reason note.
 */
public class ReimbursementReviewActivity extends AppCompatActivity {

    private ActivityReimbursementReviewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.gdgcorehq.app.model.User user = com.gdgcorehq.app.data.SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            Toast.makeText(this, "Access denied: Super Admin authorization required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityReimbursementReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Floating Pill Navigation Bar
        BottomNavHelper.setupAdminNav(this, BottomNavHelper.INDEX_EXPENSES,
                binding.navHome, binding.navTasks, binding.navScanner,
                binding.navExpenses, binding.navStrikes);

        renderReimbursements();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.gdgcorehq.app.model.User user = com.gdgcorehq.app.data.SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            Toast.makeText(this, "Access denied: Super Admin authorization required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        renderReimbursements();
    }

    private void renderReimbursements() {
        binding.layoutReimbursementsContainer.removeAllViews();
        List<Reimbursement> list = MockDataRepository.getInstance().getAllReimbursements();

        if (list.isEmpty()) {
            binding.layoutReimbursementsContainer.addView(
                    com.gdgcorehq.app.util.EmptyStateHelper.createEmptyState(
                            this,
                            binding.layoutReimbursementsContainer,
                            R.drawable.ic_receipt,
                            "No Reimbursements Submitted",
                            "No expense reimbursement claims have been submitted by members yet."
                    )
            );
            return;
        }

        for (Reimbursement r : list) {
            ItemReimbursementReviewBinding itemBinding = ItemReimbursementReviewBinding.inflate(
                    LayoutInflater.from(this), binding.layoutReimbursementsContainer, false);

            itemBinding.tvMemberName.setText(r.getMemberName());
            itemBinding.tvCategoryDate.setText(r.getCategory() + " • " + r.getDate());
            itemBinding.tvAmount.setText(String.format("₹%.2f", r.getAmount()));
            itemBinding.tvReceiptFilename.setText("Receipt: " + r.getReceiptUri());

            if (r.getStatus() == Reimbursement.Status.PENDING) {
                itemBinding.tvStatusBadge.setText("Pending Review");
                itemBinding.tvStatusBadge.setBackgroundTintList(getColorStateList(R.color.color_warning_container));
                itemBinding.tvStatusBadge.setTextColor(getColor(R.color.status_warning));
                itemBinding.layoutReviewActions.setVisibility(View.VISIBLE);
                itemBinding.tvRejectionReason.setVisibility(View.GONE);

                itemBinding.btnApprove.setOnClickListener(v -> {
                    itemBinding.btnApprove.setEnabled(false);
                    itemBinding.btnReject.setEnabled(false);
                    MockDataRepository.getInstance().approveReimbursement(r.getId(), new com.gdgcorehq.app.data.FirebaseDataRepository.WriteCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ReimbursementReviewActivity.this, "Expense approved for reimbursement", Toast.LENGTH_SHORT).show();
                            renderReimbursements();
                        }

                        @Override
                        public void onError(String message) {
                            itemBinding.btnApprove.setEnabled(true);
                            itemBinding.btnReject.setEnabled(true);
                            Toast.makeText(ReimbursementReviewActivity.this, "Failed to approve expense: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                });

                itemBinding.btnReject.setOnClickListener(v -> promptRejectionReason(r));

            } else if (r.getStatus() == Reimbursement.Status.APPROVED) {
                itemBinding.tvStatusBadge.setText("Approved (Reimbursed)");
                itemBinding.tvStatusBadge.setBackgroundTintList(getColorStateList(R.color.color_success_container));
                itemBinding.tvStatusBadge.setTextColor(getColor(R.color.status_success));
                itemBinding.layoutReviewActions.setVisibility(View.GONE);
                itemBinding.tvRejectionReason.setVisibility(View.GONE);

            } else {
                itemBinding.tvStatusBadge.setText("Rejected");
                itemBinding.tvStatusBadge.setBackgroundTintList(getColorStateList(R.color.color_error_container));
                itemBinding.tvStatusBadge.setTextColor(getColor(R.color.status_error));
                itemBinding.layoutReviewActions.setVisibility(View.GONE);

                if (r.getRejectionReason() != null && !r.getRejectionReason().isEmpty()) {
                    itemBinding.tvRejectionReason.setVisibility(View.VISIBLE);
                    itemBinding.tvRejectionReason.setText("Rejection Note: " + r.getRejectionReason());
                } else {
                    itemBinding.tvRejectionReason.setVisibility(View.GONE);
                }
            }

            binding.layoutReimbursementsContainer.addView(itemBinding.getRoot());
        }
    }

    private void promptRejectionReason(Reimbursement r) {
        EditText etReason = new EditText(this);
        etReason.setHint("Mandatory rejection reason note...");
        etReason.setMinLines(2);
        etReason.setPadding(32, 24, 32, 24);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reject Reimbursement")
                .setMessage("A rejection reason is strictly required per chapter accountability rules:")
                .setView(etReason)
                .setPositiveButton("Confirm Rejection", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        android.widget.Button btnReject = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnReject.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Rejection aborted: reason is mandatory.", Toast.LENGTH_LONG).show();
                return;
            }

            btnReject.setEnabled(false);
            MockDataRepository.getInstance().rejectReimbursement(r.getId(), reason, new com.gdgcorehq.app.data.FirebaseDataRepository.WriteCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ReimbursementReviewActivity.this, "Reimbursement rejected.", Toast.LENGTH_SHORT).show();
                    renderReimbursements();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    btnReject.setEnabled(true);
                    Toast.makeText(ReimbursementReviewActivity.this, "Failed to reject reimbursement: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
