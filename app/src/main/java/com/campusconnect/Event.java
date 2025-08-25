package com.campusconnect;

import com.google.firebase.Timestamp;

import java.io.Serializable;

/**
 * Model class for an Event.
 */
public class Event implements Serializable {
    private String id;
    private String title;
    private String description;
    private Timestamp date_time;
    private String location;
    private String category;  // 'seminar', 'exam', 'fest', 'notice'
    private Timestamp created_at;

    // Default constructor for Firestore
    public Event() {}

    public Event(String id, String title, String description, Timestamp date_time, String location, String category, Timestamp created_at) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date_time = date_time;
        this.location = location;
        this.category = category;
        this.created_at = created_at;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Timestamp getDate_time() { return date_time; }
    public void setDate_time(Timestamp date_time) { this.date_time = date_time; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }
}