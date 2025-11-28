package com.example.myapplication.features.user.profile;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

/**
 * Model class for user profile operations using Firebase Firestore and Authentication.
 */
public class ProfileModel {
    private final FirebaseFirestore db;

    /**
     * Constructor with dependency injection for FirebaseFirestore.
     */
    public ProfileModel(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Default constructor initializing with the default Firestore instance.
     */
    public ProfileModel() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Callback interface for asynchronous data operations.
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onFailure(Exception e);
    }

    /**
     * Fetches the user profile document from Firestore.
     *
     * @param uid      The user ID.
     * @param callback The callback to handle success or failure.
     */
    public void fetchUserProfile(String uid, DataCallback<DocumentSnapshot> callback) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onFailure);
    }

    public void updateUserProfile(String uid, Map<String, Object> updates, DataCallback<Void> callback) {
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(uid), updates, SetOptions.merge());
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateEmail(FirebaseUser user, String newEmail, DataCallback<Void> callback) {
        user.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    // After verification email sent, we update Firestore (or wait? logic in fragment was: send verify -> update firestore email)
                    // The original logic was: verifyBeforeUpdateEmail -> onSuccess -> db.update("email")
                    db.collection("users").document(user.getUid())
                            .update("email", newEmail)
                            .addOnSuccessListener(v -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void reauthenticate(FirebaseUser user, String password, DataCallback<Void> callback) {
        if (user.getEmail() == null) {
            callback.onFailure(new Exception("No email associated with account"));
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
