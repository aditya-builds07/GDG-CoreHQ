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
import com.gdgcorehq.app.databinding.ActivityAgendaManagementBinding;
import com.gdgcorehq.app.databinding.ItemMeetingCardBinding;
import com.gdgcorehq.app.model.Meeting;
import com.gdgcorehq.app.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Super Admin Meeting Agenda Management Activity.
 * Strictly adheres to PRD §3.4:
 * Publishes upcoming meeting agendas, displays topics, location, and schedule.
 */
public class AgendaManagementActivity extends AppCompatActivity {

    private ActivityAgendaManagementBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            Toast.makeText(this, "Access denied: Super Admin authorization required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityAgendaManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddMeeting.setOnClickListener(v -> showAddMeetingDialog());

        renderMeetings();
    }

    private void renderMeetings() {
        binding.layoutMeetingsContainer.removeAllViews();
        List<Meeting> meetings = MockDataRepository.getInstance().getMeetings();

        if (meetings.isEmpty()) {
            binding.layoutMeetingsContainer.addView(
                    com.gdgcorehq.app.util.EmptyStateHelper.createEmptyState(
                            this,
                            binding.layoutMeetingsContainer,
                            R.drawable.ic_agenda,
                            "No Meetings Scheduled",
                            "Tap '+ Agenda' above to publish the next chapter meeting agenda."
                    )
            );
            return;
        }

        for (Meeting m : meetings) {
            ItemMeetingCardBinding cardBinding = ItemMeetingCardBinding.inflate(
                    LayoutInflater.from(this), binding.layoutMeetingsContainer, false);

            cardBinding.tvMeetingTitle.setText(m.getTitle());
            cardBinding.tvMeetingDateTime.setText(m.getDate() + " • " + m.getTime());
            cardBinding.tvMeetingLocation.setText(m.getLocation());
            cardBinding.tvPublishedBy.setText("Published by: " + m.getPublishedBy());

            if (m.isUpcoming()) {
                cardBinding.tvMeetingStatus.setText("Upcoming Meeting");
                cardBinding.tvMeetingStatus.setBackgroundTintList(getColorStateList(R.color.color_info_container));
                cardBinding.tvMeetingStatus.setTextColor(getColor(R.color.status_info));
            } else {
                cardBinding.tvMeetingStatus.setText("Past Meeting");
                cardBinding.tvMeetingStatus.setBackgroundTintList(getColorStateList(R.color.surface_subtle));
                cardBinding.tvMeetingStatus.setTextColor(getColor(R.color.text_secondary));
            }

            // Build bullet points
            StringBuilder sb = new StringBuilder();
            for (String topic : m.getTopics()) {
                sb.append("• ").append(topic).append("\n");
            }
            cardBinding.tvMeetingTopics.setText(sb.toString().trim());

            binding.layoutMeetingsContainer.addView(cardBinding.getRoot());
        }
    }

    private void showAddMeetingDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_meeting, null);
        EditText etTitle = dialogView.findViewById(R.id.etMeetingTitle);
        EditText etDate = dialogView.findViewById(R.id.etMeetingDate);
        EditText etTime = dialogView.findViewById(R.id.etMeetingTime);
        EditText etLocation = dialogView.findViewById(R.id.etMeetingLocation);
        EditText etTopics = dialogView.findViewById(R.id.etMeetingTopics);

        new AlertDialog.Builder(this)
                .setTitle("Publish New Agenda")
                .setView(dialogView)
                .setPositiveButton("Publish", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    String time = etTime.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    String topicsStr = etTopics.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Meeting title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (date.isEmpty() || time.isEmpty() || location.isEmpty()) {
                        Toast.makeText(this, "Date, Time, and Location are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> topicList = new ArrayList<>();
                    if (!topicsStr.isEmpty()) {
                        topicList.addAll(Arrays.asList(topicsStr.split("\n")));
                    } else {
                        topicList.add("General Chapter Operations");
                    }

                    User current = SessionManager.getInstance().getCurrentUser();
                    String publisher = (current != null && current.getName() != null && !current.getName().isEmpty())
                            ? current.getName() : "Chapter Organizer";

                    Meeting newMeeting = new Meeting(
                            "meet_" + System.currentTimeMillis(),
                            title,
                            date,
                            time,
                            location,
                            topicList,
                            publisher,
                            true
                    );

                    MockDataRepository.getInstance().addMeeting(newMeeting);
                    Toast.makeText(this, "Agenda published successfully", Toast.LENGTH_SHORT).show();
                    renderMeetings();
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
