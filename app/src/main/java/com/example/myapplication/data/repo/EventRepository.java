package com.example.myapplication.data.repo;

import android.content.Context;
import android.net.Uri;

import com.example.myapplication.data.model.Event;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * {@code EventRepository} defines the contract for managing event-related operations
 * within the application’s data layer.
 * <p>
 * This repository serves as an abstraction over the underlying data source (e.g. Firebase Firestore),
 * ensuring a clean separation between the app’s business logic and data storage implementation.
 * <p>
 * Implementations of this interface (such as {@code FirebaseEventRepository})
 * handle all network or database operations for creating and updating event data.
 */
public interface EventRepository {
    /**
     * Creates a new event and uploads it to the backend database.
     * <p>
     * The implementation is expected to:
     * <ul>
     *     <li>Generate a unique document ID for the event.</li>
     *     <li>Upload associated poster images or files if provided.</li>
     *     <li>Store the {@link UserEvent} object in Firestore or another data source.</li>
     * </ul>
     *
     * @param context   the current {@link Context}, used for Firebase or repository access
     * @param event     the {@link UserEvent} object containing event details
     * @param onSuccess callback triggered when the event is successfully created
     * @param onFailure callback triggered when the event creation fails
     */
    void createEvent(Context context, UserEvent event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure);

    /**
     * Updates an existing event’s data in the backend.
     * <p>
     * The implementation should locate the event document using {@code eventId}
     * and apply any modified fields from the provided {@link UserEvent} object.
     *
     * @param eventId   the unique identifier of the event to be updated
     * @param event     the updated {@link UserEvent} object with new field values
     * @param onSuccess callback triggered when the event update operation succeeds
     * @param onFailure callback triggered if the update operation fails
     */
    void updateEvent(String eventId, UserEvent event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure);
}
