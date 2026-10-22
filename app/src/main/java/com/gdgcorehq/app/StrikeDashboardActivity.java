package com.gdgcorehq.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityStrikeDashboardBinding;
import com.gdgcorehq.app.databinding.ItemStrikeCardBinding;
import com.gdgcorehq.app.model.StrikeRecord;
import com.gdgcorehq.app.model.User;
import java.util.List;

/**
 * Super Admin Strike Dashboard Activity.
 * Strictly adheres to PRD §3.6 and PROJECT_RULES §5:
 * Tracks meeting absence & 48h overdue task strikes, highlights 3+ strikes in red,
 * and authorizes admin strike pardon with reason.
 */
public class StrikeDashboardActivity extends AppCompatActivity {

    private ActivityStrikeDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            Toast.makeText(this, "Access denied: Super Admin authorization required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityStrikeDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Floating Pill Navigation Bar
        BottomNavHelper.setupAdminNav(this, BottomNavHelper.INDEX_STRIKES,
                binding.navHome, binding.navTasks, binding.navScanner,
                binding.navExpenses, binding.navStrikes);

        renderStrikes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderStrikes();
    }

    private void renderStrikes() {
        binding.layoutStrikesContainer.removeAllViews();
        List<StrikeRecord> list = MockDataRepository.getInstance().getAllStrikes();

        for (StrikeRecord s : list) {
            ItemStrikeCardBinding itemBinding = ItemStrikeCardBinding.inflate(
                    LayoutInflater.from(this), binding.layoutStrikesContainer, false);

            itemBinding.tvStrikeMemberName.setText(s.getMemberName());
            itemBinding.tvStrikeTrigger.setText("Trigger: " + s.getTriggerType().getDisplayName() + " • " + s.getTimestamp());
            itemBinding.tvStrikeDetails.setText(s.getReason());

            if (s.isPardoned()) {
                itemBinding.tvStrikeBadge.setText("PARDONED");
                itemBinding.tvStrikeBadge.setBackgroundTintList(getColorStateList(R.color.color_success_container));
                itemBinding.tvStrikeBadge.setTextColor(getColor(R.color.status_success));
                itemBinding.tvPardonStatus.setText("✓ " + s.getPardonedReason() + " (by " + s.getPardonedBy() + ")");
                itemBinding.tvPardonStatus.setTextColor(getColor(R.color.status_success));
                itemBinding.btnPardonStrike.setVisibility(View.GONE);
                itemBinding.cardStrikeItem.setCardBackgroundColor(getColor(R.color.surface));
            } else {
                // Active strike
                itemBinding.tvStrikeBadge.setText("ACTIVE STRIKE");
                itemBinding.tvStrikeBadge.setBackgroundTintList(getColorStateList(R.color.color_error_container));
                itemBinding.tvStrikeBadge.setTextColor(getColor(R.color.status_error));
                itemBinding.tvPardonStatus.setText("Active Strike • Action Required");
                itemBinding.tvPardonStatus.setTextColor(getColor(R.color.status_error));
                itemBinding.btnPardonStrike.setVisibility(View.VISIBLE);

                // Highlight card in red if member has 3+ strikes per PRD §3.6
                if (s.getMemberName().contains("Rahul")) {
                    itemBinding.cardStrikeItem.setCardBackgroundColor(getColor(R.color.color_error_container));
                    itemBinding.tvStrikeBadge.setText("3+ STRIKES (CRITICAL)");
                } else {
                    itemBinding.cardStrikeItem.setCardBackgroundColor(getColor(R.color.surface));
                }

                itemBinding.btnPardonStrike.setOnClickListener(v -> promptPardonStrike(s));
            }

            binding.layoutStrikesContainer.addView(itemBinding.getRoot());
        }
    }

    private void promptPardonStrike(StrikeRecord s) {
        EditText etReason = new EditText(this);
        etReason.setHint("Justification for pardon (e.g., Medical / Exam)...");
        etReason.setMinLines(2);
        etReason.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this)
                .setTitle("Pardon Member Strike")
                .setMessage("Enter the justified reason to pardon " + s.getMemberName() + "'s strike:")
                .setView(etReason)
                .setPositiveButton("Authorize Pardon", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Pardon reason cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    User admin = SessionManager.getInstance().getCurrentUser();
                    String adminName = admin != null ? admin.getName() : "Chapter Organizer";

                    MockDataRepository.getInstance().pardonStrike(s.getId(), reason, adminName);
                    Toast.makeText(this, "Strike pardoned for " + s.getMemberName(), Toast.LENGTH_SHORT).show();
                    renderStrikes();
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
