package com.campusconnect;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;

public class EventDetailsActivity extends AppCompatActivity {
    private TextView titleTextView, descriptionTextView, locationTextView, categoryTextView, dateTextView;
    private Button addToCalendarButton, setReminderButton, editButton;
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        titleTextView = findViewById(R.id.title);
        descriptionTextView = findViewById(R.id.description);
        locationTextView = findViewById(R.id.location);
        categoryTextView = findViewById(R.id.category);
        dateTextView = findViewById(R.id.date);
        addToCalendarButton = findViewById(R.id.add_to_calendar);
        setReminderButton = findViewById(R.id.set_reminder);
        editButton = findViewById(R.id.edit_button);

        // Reconstruct Event from Intent extras
        Intent intent = getIntent();
        event = new Event(
                intent.getStringExtra("event_id"),
                intent.getStringExtra("event_title"),
                intent.getStringExtra("event_description"),
                new Timestamp(
                        intent.getLongExtra("event_date_time_seconds", 0),
                        intent.getIntExtra("event_date_time_nanos", 0)
                ),
                intent.getStringExtra("event_location"),
                intent.getStringExtra("event_category"),
                new Timestamp(
                        intent.getLongExtra("event_created_at_seconds", 0),
                        intent.getIntExtra("event_created_at_nanos", 0)
                )
        );

        if (event.getTitle() != null) {
            titleTextView.setText(event.getTitle());
            descriptionTextView.setText(event.getDescription() != null ? event.getDescription() : "");
            locationTextView.setText(event.getLocation() != null ? event.getLocation() : "");
            categoryTextView.setText(event.getCategory());
            dateTextView.setText(event.getDate_time().toDate().toString());
        } else {
            Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
            finish();
        }

        addToCalendarButton.setOnClickListener(v -> addToCalendar());

        setReminderButton.setOnClickListener(v -> setReminder());

        editButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(this, MainActivity.class);
            editIntent.putExtra("edit_event_id", event.getId());
            editIntent.putExtra("edit_event_title", event.getTitle());
            editIntent.putExtra("edit_event_description", event.getDescription());
            editIntent.putExtra("edit_event_date_time_seconds", event.getDate_time().getSeconds());
            editIntent.putExtra("edit_event_date_time_nanos", event.getDate_time().getNanoseconds());
            editIntent.putExtra("edit_event_location", event.getLocation());
            editIntent.putExtra("edit_event_category", event.getCategory());
            editIntent.putExtra("edit_event_created_at_seconds", event.getCreated_at().getSeconds());
            editIntent.putExtra("edit_event_created_at_nanos", event.getCreated_at().getNanoseconds());
            startActivity(editIntent);
        });
    }

    @SuppressWarnings("deprecation")
    private void addToCalendar() {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, event.getTitle())
                .putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription())
                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocation())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDate_time().toDate().getTime())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getDate_time().toDate().getTime() + 3600000); // Add 1 hour for end time
        startActivity(intent);
    }

    private void setReminder() {
        Toast.makeText(this, "Reminder set for " + event.getTitle(), Toast.LENGTH_SHORT).show();
        // Handled by MainActivity's AlarmManager
    }
}