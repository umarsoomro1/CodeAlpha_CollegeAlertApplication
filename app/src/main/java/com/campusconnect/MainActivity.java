package com.campusconnect;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();
    private FirebaseHelper firebaseHelper;
    private Button profileButton, addEventButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "CampusConnectPrefs";
    private static final String EVENTS_KEY = "cached_events";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(eventList, this::openEventDetails);
        recyclerView.setAdapter(eventAdapter);

        profileButton = findViewById(R.id.profile_button);
        addEventButton = findViewById(R.id.add_event_button);

        firebaseHelper = new FirebaseHelper();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        profileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        addEventButton.setOnClickListener(v -> showAddEventDialog(null));

        // Check for edit event intent
        if (getIntent().hasExtra("edit_event_id")) {
            Event editEvent = new Event(
                    getIntent().getStringExtra("edit_event_id"),
                    getIntent().getStringExtra("edit_event_title"),
                    getIntent().getStringExtra("edit_event_description"),
                    new Timestamp(
                            getIntent().getLongExtra("edit_event_date_time_seconds", 0),
                            getIntent().getIntExtra("edit_event_date_time_nanos", 0)
                    ),
                    getIntent().getStringExtra("edit_event_location"),
                    getIntent().getStringExtra("edit_event_category"),
                    new Timestamp(
                            getIntent().getLongExtra("edit_event_created_at_seconds", 0),
                            getIntent().getIntExtra("edit_event_created_at_nanos", 0)
                    )
            );
            showAddEventDialog(editEvent);
        }

        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            } else {
                loadEvents();
            }
        } else {
            loadEvents();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted");
                loadEvents();
            } else {
                Log.d("MainActivity", "Notification permission denied");
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                loadEvents(); // Load events even if permission is denied
            }
        }
    }

    private void loadEvents() {
        firebaseHelper.getUpcomingEvents((snapshot, e) -> {
            if (e != null) {
                Log.e("MainActivity", "Error loading events: " + e.getMessage());
                Toast.makeText(this, "Network error, loading cached events", Toast.LENGTH_SHORT).show();
                loadCachedEvents();
                return;
            }
            if (snapshot != null) {
                eventList.clear();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    Event event = doc.toObject(Event.class);
                    if (event != null) {
                        event.setId(doc.getId());
                        eventList.add(event);
                    }
                }
                eventAdapter.notifyDataSetChanged();
                cacheEvents();
                scheduleNotifications();
            }
        });
    }

    private void loadCachedEvents() {
        String json = sharedPreferences.getString(EVENTS_KEY, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Event>>() {}.getType();
            eventList = gson.fromJson(json, type);
            eventAdapter.notifyDataSetChanged();
        }
    }

    private void cacheEvents() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(eventList);
        editor.putString(EVENTS_KEY, json);
        editor.apply();
    }

    private void scheduleNotifications() {
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        if (!notificationsEnabled) {
            Log.d("MainActivity", "Notifications disabled in preferences");
            return;
        }

        boolean seminar = sharedPreferences.getBoolean("category_seminar", true);
        boolean exam = sharedPreferences.getBoolean("category_exam", true);
        boolean fest = sharedPreferences.getBoolean("category_fest", true);
        boolean notice = sharedPreferences.getBoolean("category_notice", true);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (Event event : eventList) {
            boolean shouldNotify = false;
            switch (event.getCategory()) {
                case "seminar": shouldNotify = seminar; break;
                case "exam": shouldNotify = exam; break;
                case "fest": shouldNotify = fest; break;
                case "notice": shouldNotify = notice; break;
            }
            if (shouldNotify && event.getDate_time().toDate().getTime() > System.currentTimeMillis()) {
                Intent intent = new Intent(this, NotificationReceiver.class);
                intent.putExtra("event_title", event.getTitle());
                intent.putExtra("event_id", event.getId());

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        event.getId().hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                long triggerTime = event.getDate_time().toDate().getTime() - 3600000; // 1 hour before
                Log.d("MainActivity", "Scheduling notification for event: " + event.getTitle() + " at " + new Date(triggerTime));
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_title", event.getTitle());
        intent.putExtra("event_description", event.getDescription());
        intent.putExtra("event_date_time_seconds", event.getDate_time().getSeconds());
        intent.putExtra("event_date_time_nanos", event.getDate_time().getNanoseconds());
        intent.putExtra("event_location", event.getLocation());
        intent.putExtra("event_category", event.getCategory());
        intent.putExtra("event_created_at_seconds", event.getCreated_at().getSeconds());
        intent.putExtra("event_created_at_nanos", event.getCreated_at().getNanoseconds());
        startActivity(intent);
    }

    private void showAddEventDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);
        builder.setView(dialogView);

        EditText titleEdit = dialogView.findViewById(R.id.title_edit);
        EditText descEdit = dialogView.findViewById(R.id.desc_edit);
        EditText locationEdit = dialogView.findViewById(R.id.location_edit);
        EditText categoryEdit = dialogView.findViewById(R.id.category_edit);
        DatePicker datePicker = dialogView.findViewById(R.id.date_picker);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
        Button saveButton = dialogView.findViewById(R.id.save_button);
        Button deleteButton = dialogView.findViewById(R.id.delete_button);

        if (event != null) {
            titleEdit.setText(event.getTitle());
            descEdit.setText(event.getDescription());
            locationEdit.setText(event.getLocation());
            categoryEdit.setText(event.getCategory());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(event.getDate_time().toDate());
            datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
            timePicker.setMinute(calendar.get(Calendar.MINUTE));
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Save button clicked");
            String title = titleEdit.getText().toString().trim();
            String description = descEdit.getText().toString().trim();
            String location = locationEdit.getText().toString().trim();
            String category = categoryEdit.getText().toString().trim();

            if (title.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate category
            if (!category.equals("seminar") && !category.equals("exam") && !category.equals("fest") && !category.equals("notice")) {
                Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                    timePicker.getHour(), timePicker.getMinute(), 0);
            Timestamp timestamp = new Timestamp(calendar.getTime());

            Event newEvent = new Event(event != null ? event.getId() : null, title, description, timestamp, location, category, Timestamp.now());

            if (event == null) {
                firebaseHelper.addEvent(newEvent, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Event added", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEvents(); // Refresh events
                    } else {
                        Log.e("MainActivity", "Add event failed: " + task.getException().getMessage());
                        Toast.makeText(this, "Add failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                firebaseHelper.updateEvent(event.getId(), newEvent, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEvents(); // Refresh events
                    } else {
                        Log.e("MainActivity", "Update event failed: " + task.getException().getMessage());
                        Toast.makeText(this, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        deleteButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Delete button clicked");
            if (event != null) {
                firebaseHelper.deleteEvent(event.getId(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEvents(); // Refresh events
                    } else {
                        Log.e("MainActivity", "Delete event failed: " + task.getException().getMessage());
                        Toast.makeText(this, "Delete failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }
}