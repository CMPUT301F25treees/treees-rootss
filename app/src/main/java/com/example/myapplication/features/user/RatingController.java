package com.example.myapplication.features.user;

import com.example.myapplication.data.firebase.FirebaseUserRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Controller for handling rating-related logic.
 * Acts as the bridge between the View (Fragments) and the Model (Repositories/Firestore).
 */
public class RatingController {
    private final FirebaseUserRepository userRepository;
    private final FirebaseFirestore db;

    public RatingController() {
        this.userRepository = new FirebaseUserRepository();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Submits a new rating for an organizer.
     *
     * @param organizerId ID of the organizer being rated.
     * @param rating The rating value (1-5).
     * @param notificationId ID of the notification triggering this rating.
     * @param onSuccess Callback for success.
     * @param onFailure Callback for failure.
     */
    public void submitRating(String organizerId, int rating, String notificationId, Runnable onSuccess, OnFailureListener onFailure) {
        userRepository.submitOrganizerRating(organizerId, rating, notificationId, onSuccess, onFailure);
    }

    /**
     * Callback interface for receiving rating data.
     */
    public interface OnRatingFetchedListener {
        void onRatingFetched(double rating);
        void onError(Exception e);
    }

    /**
     * Fetches an organizer's rating for display.
     *
     * @param organizerId ID of the organizer.
     * @param listener Callback listener.
     */
    public void fetchOrganizerRating(String organizerId, OnRatingFetchedListener listener) {
        if (organizerId == null || organizerId.isEmpty()) {
            return;
        }
        db.collection("users").document(organizerId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Double rating = documentSnapshot.getDouble("rating");
                    if (rating == null) rating = 0.0;
                    listener.onRatingFetched(rating);
                } else {
                    // If user doesn't exist or has no rating, return 0
                    listener.onRatingFetched(0.0);
                }
            })
            .addOnFailureListener(listener::onError);
    }
}
