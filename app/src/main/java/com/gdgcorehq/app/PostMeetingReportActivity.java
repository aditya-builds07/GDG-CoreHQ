package com.gdgcorehq.app;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityPostMeetingReportBinding;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.PostMeetingReport;
import com.gdgcorehq.app.model.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Core Member Post-Meeting Form Activity.
 * Strictly adheres to PRD §3.2 and §3.4:
 * Members submit weekly progress, roadblocks, and meeting feedback linked to the current meeting.
 */
public class PostMeetingReportActivity extends AppCompatActivity {

    private ActivityPostMeetingReportBinding binding;
    private Meeting currentMeeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostMeetingReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentMeeting = MockDataRepository.getInstance().getNextUpcomingMeeting();
        if (currentMeeting != null) {
            binding.tvMeetingContextTitle.setText(currentMeeting.getTitle());
            binding.tvMeetingContextSchedule.setText(currentMeeting.getDate() + " • " + currentMeeting.getLocation());
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String progress = binding.etProgress.getText() != null ?
                binding.etProgress.getText().toString().trim() : "";
        String roadblocks = binding.etRoadblocks.getText() != null ?
                binding.etRoadblocks.getText().toString().trim() : "";
        String feedback = binding.etFeedback.getText() != null ?
                binding.etFeedback.getText().toString().trim() : "";

        if (progress.isEmpty()) {
            binding.tilProgress.setError("Weekly progress summary is required");
            return;
        } else {
            binding.tilProgress.setError(null);
        }

        if (roadblocks.isEmpty()) {
            binding.tilRoadblocks.setError("Please describe roadblocks or write 'None'");
            return;
        } else {
            binding.tilRoadblocks.setError(null);
        }

        User current = SessionManager.getInstance().getCurrentUser();
        String memberId = current != null ? current.getId() : "usr_member_01";
        String memberName = current != null ? current.getName() : "Siddhi Kulkarni";
        String meetingId = currentMeeting != null ? currentMeeting.getId() : "meet_101";
        String timeStr = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date());

        PostMeetingReport report = new PostMeetingReport(
                "report_" + System.currentTimeMillis(),
                meetingId,
                memberId,
                memberName,
                progress,
                roadblocks,
                feedback,
                timeStr
        );

        boolean success = MockDataRepository.getInstance().submitPostMeetingReport(report);
        if (success) {
            binding.btnSubmitReport.setEnabled(false);
            Toast.makeText(this, "✓ Post-meeting report submitted for " + (currentMeeting != null ? currentMeeting.getTitle() : "chapter sync"), Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to submit report. Please check required fields.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
