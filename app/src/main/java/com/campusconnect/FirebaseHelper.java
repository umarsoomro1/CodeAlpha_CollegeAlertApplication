package com.campusconnect;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for Firebase operations.
 */
public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference eventsCollection;

    public FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        eventsCollection = db.collection("events");
    }

    // Authentication methods
    public void signUpWithEmail(String email, String password, Context context, OnCompleteListener<AuthResult> listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public void loginWithEmail(String email, String password, Context context, OnCompleteListener<AuthResult> listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public void signOut() {
        mAuth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Firestore methods
    public void addEvent(Event event, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("title", event.getTitle());
        eventMap.put("description", event.getDescription());
        eventMap.put("date_time", event.getDate_time());
        eventMap.put("location", event.getLocation());
        eventMap.put("category", event.getCategory());
        eventMap.put("created_at", Timestamp.now());

        eventsCollection.add(eventMap).addOnCompleteListener(listener);
    }

    public void updateEvent(String id, Event event, OnCompleteListener<Void> listener) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("title", event.getTitle());
        eventMap.put("description", event.getDescription());
        eventMap.put("date_time", event.getDate_time());
        eventMap.put("location", event.getLocation());
        eventMap.put("category", event.getCategory());
        eventMap.put("created_at", event.getCreated_at());

        eventsCollection.document(id).set(eventMap).addOnCompleteListener(listener);
    }

    public void deleteEvent(String id, OnCompleteListener<Void> listener) {
        eventsCollection.document(id).delete().addOnCompleteListener(listener);
    }

    public void getUpcomingEvents(EventListener<QuerySnapshot> listener) {
        eventsCollection
                .whereGreaterThan("date_time", Timestamp.now())
                .orderBy("date_time", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }
}