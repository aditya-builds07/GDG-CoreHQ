package com.gdgcorehq.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.databinding.ActivityMemberAgendaBinding;
import com.gdgcorehq.app.databinding.ItemMeetingCardBinding;
import com.gdgcorehq.app.model.Meeting;
import java.util.List;

/**
 * Core Member Agenda Viewing Activity.
 * Strictly adheres to PRD §3.4:
 * View upcoming and past meeting agendas with full topic breakdowns,
 * directly linked to Post-Meeting Report submission.
 */
public class MemberAgendaActivity extends AppCompatActivity {

    private ActivityMemberAgendaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMemberAgendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnOpenReport.setOnClickListener(v ->
                startActivity(new Intent(this, PostMeetingReportActivity.class)));

        // Floating Pill Navigation Bar
        BottomNavHelper.setupMemberNav(this, BottomNavHelper.INDEX_AGENDA,
                binding.navHome, binding.navTasks, binding.navAgenda,
                binding.navExpenses, binding.navProfile);

        renderMeetings();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                            "No Chapter Meetings",
                            "Meeting schedules and discussion agendas will appear here."
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
                cardBinding.tvMeetingStatus.setText("Upcoming Sync");
                cardBinding.tvMeetingStatus.setBackgroundTintList(getColorStateList(R.color.color_info_container));
                cardBinding.tvMeetingStatus.setTextColor(getColor(R.color.status_info));
            } else {
                cardBinding.tvMeetingStatus.setText("Past Sync");
                cardBinding.tvMeetingStatus.setBackgroundTintList(getColorStateList(R.color.surface_subtle));
                cardBinding.tvMeetingStatus.setTextColor(getColor(R.color.text_secondary));
            }

            StringBuilder sb = new StringBuilder();
            for (String topic : m.getTopics()) {
                sb.append("• ").append(topic).append("\n");
            }
            cardBinding.tvMeetingTopics.setText(sb.toString().trim());

            binding.layoutMeetingsContainer.addView(cardBinding.getRoot());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
