package com.gdgcorehq.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityQrScannerBinding;
import com.gdgcorehq.app.model.AttendanceRecord;
import com.gdgcorehq.app.model.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Super Admin QR Attendance Scanner Activity.
 * Strictly adheres to PRD §3.2 and PROJECT_RULES §5:
 * Validates PRN, prevents duplicate check-ins, prompts admin confirmation,
 * and records attendance with meeting ID, timestamp, and recorder ID.
 */
public class QrScannerActivity extends AppCompatActivity {

    private ActivityQrScannerBinding binding;
    private final String currentMeetingId = "meet_101"; // Active Chapter Sync
    private User pendingMember = null;
    private final StringBuilder sessionLog = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            Toast.makeText(this, "Access denied: Attendance scanner requires Super Admin privileges.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityQrScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Floating Pill Navigation Bar
        BottomNavHelper.setupAdminNav(this, BottomNavHelper.INDEX_SCANNER,
                binding.navHome, binding.navTasks, binding.navScanner,
                binding.navExpenses, binding.navStrikes);

        // Quick simulation triggers for UI testing
        binding.btnSimulateValid.setOnClickListener(v -> processPrnScan("PRN2024001"));
        binding.btnSimulateInvalid.setOnClickListener(v -> processPrnScan("INVALID999"));

        // Manual Lookup
        binding.btnVerifyPrn.setOnClickListener(v -> {
            String input = binding.etManualPrn.getText() != null ?
                    binding.etManualPrn.getText().toString().trim() : "";
            if (input.isEmpty()) {
                binding.tilManualPrn.setError("Enter a PRN");
            } else {
                binding.tilManualPrn.setError(null);
                processPrnScan(input);
            }
        });

        // Confirm Attendance Action
        binding.btnConfirmAttendance.setOnClickListener(v -> confirmPendingAttendance());
    }

    private void processPrnScan(String prn) {
        MockDataRepository repo = MockDataRepository.getInstance();

        // 1. Validate PRN against registered member records
        User member = repo.findMemberByPrn(prn);
        if (member == null) {
            binding.cardResult.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle("Scan Rejected")
                    .setMessage("PRN '" + prn + "' is not registered in chapter records. Please check the member's physical ID card.")
                    .setPositiveButton("Dismiss", null)
                    .show();
            return;
        }

        // 2. Check for duplicate scan for the active meeting
        if (repo.isAttendanceAlreadyRecorded(currentMeetingId, member.getPrn())) {
            binding.cardResult.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle("Duplicate Scan")
                    .setMessage(member.getName() + " (" + member.getPrn() + ") has ALREADY been marked present for this meeting.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // 3. Display member confirmation card for Super Admin review
        pendingMember = member;
        binding.tvResultName.setText(member.getName());
        binding.tvResultRolePrn.setText(member.getDepartment() + " • PRN: " + member.getPrn());
        binding.cardResult.setVisibility(View.VISIBLE);
    }

    private void confirmPendingAttendance() {
        if (pendingMember == null) return;

        User admin = SessionManager.getInstance().getCurrentUser();
        String recorderId = admin != null ? admin.getId() : "admin_usr";
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        AttendanceRecord record = new AttendanceRecord(
                "att_" + System.currentTimeMillis(),
                currentMeetingId,
                pendingMember.getPrn(),
                pendingMember.getName(),
                pendingMember.getRole().getDisplayName(),
                timeStr,
                "PRESENT",
                recorderId
        );

        boolean success = MockDataRepository.getInstance().recordAttendance(record);
        if (success) {
            Toast.makeText(this, "Attendance recorded for " + pendingMember.getName(), Toast.LENGTH_SHORT).show();

            // Append to session log
            if (sessionLog.length() == 0) {
                sessionLog.append("✓ ").append(pendingMember.getName()).append(" (").append(pendingMember.getPrn()).append(") at ").append(timeStr);
            } else {
                sessionLog.insert(0, "✓ " + pendingMember.getName() + " (" + pendingMember.getPrn() + ") at " + timeStr + "\n");
            }
            binding.tvSessionHistory.setText(sessionLog.toString());

            binding.cardResult.setVisibility(View.GONE);
            pendingMember = null;
            binding.etManualPrn.setText("");
        } else {
            Toast.makeText(this, "Failed to record attendance: duplicate scan detected.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
