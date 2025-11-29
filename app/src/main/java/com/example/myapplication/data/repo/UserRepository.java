package com.example.myapplication.data.repo;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;

/**
 * Repository interface for user profile related operations.
 */
public interface UserRepository {

    /**
     * Deletes all events created by the user.
     * @param uid User ID.
     * @param onSuccess Callback for success.
     * @param onFailure Callback for failure.
     */
    void deleteEventsForUser(String uid, Runnable onSuccess, OnFailureListener onFailure);

    /**
     * Disables all events created by the user (for admin use).
     * @param uid User ID.
     * @param onSuccess Callback for success.
     * @param onFailure Callback for failure.
     */
    void disableEventsForUser(String uid, Runnable onSuccess, OnFailureListener onFailure);

    /**
     * Deletes the user's Firestore document.
     * @param uid User ID.
     * @param onSuccess Callback for success.
     * @param onFailure Callback for failure.
     */
    void deleteUserDocument(String uid, Runnable onSuccess, OnFailureListener onFailure);

    /**
     * Deletes the Firebase Auth user.
     * @param user Firebase User object.
     * @param onSuccess Callback for success.
     * @param onFailure Callback for failure.
     */
    void deleteAuthUser(FirebaseUser user, Runnable onSuccess, OnFailureListener onFailure);
}
