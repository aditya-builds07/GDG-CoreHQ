package com.gdgcorehq.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityProfileBinding;
import com.gdgcorehq.app.model.User;

/**
 * Member and Admin Profile Activity.
 * Strictly adheres to PRD §3.1:
 * Displays member's name, core role, current tasks summary, and strike count,
 * with sign-out action returning to LoginActivity.
 */
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSignOut.setOnClickListener(v -> showSignOutDialog());

        setupNavigation();
        displayProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayProfile();
    }

    private void setupNavigation() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && !user.isSuperAdmin()) {
            binding.bottomNavPill.setVisibility(android.view.View.VISIBLE);
            BottomNavHelper.setupMemberNav(this, BottomNavHelper.INDEX_PROFILE,
                    binding.navHome, binding.navTasks, binding.navAgenda,
                    binding.navExpenses, binding.navProfile);
        } else {
            binding.bottomNavPill.setVisibility(android.view.View.GONE);
        }
    }

    private void displayProfile() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding.tvProfileName.setText(user.getName());
        binding.tvProfileDept.setText(user.getDepartment() + " • " + user.getRole().getDisplayName());
        binding.tvProfilePrn.setText("PRN: " + user.getPrn());
        binding.tvProfileEmail.setText(user.getEmail());

        int strikes = user.getStrikeCount();
        if (strikes == 0) {
            binding.tvProfileStrikes.setText("Current Strikes: 0 / 3 (Good Standing)");
            binding.tvProfileStrikes.setTextColor(getColor(R.color.status_success));
            binding.cardStrikeInfo.setCardBackgroundColor(getColor(R.color.surface));
        } else if (strikes < 3) {
            binding.tvProfileStrikes.setText("Current Strikes: " + strikes + " / 3 (Warning)");
            binding.tvProfileStrikes.setTextColor(getColor(R.color.status_warning));
            binding.cardStrikeInfo.setCardBackgroundColor(getColor(R.color.surface));
        } else {
            binding.tvProfileStrikes.setText("Current Strikes: " + strikes + " / 3 (Action Required)");
            binding.tvProfileStrikes.setTextColor(getColor(R.color.status_error));
            binding.cardStrikeInfo.setCardBackgroundColor(getColor(R.color.color_error_container));
        }
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of GDG CoreHQ?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    SessionManager.getInstance().logout();
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
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
