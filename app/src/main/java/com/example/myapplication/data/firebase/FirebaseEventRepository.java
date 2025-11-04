package com.example.myapplication.data.firebase;

import android.net.Uri;

import com.example.myapplication.data.model.Event;
import com.example.myapplication.data.repo.EventRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


/**
 * This class is meant to handle any operations that will happen to the events in Firebase
 *
 * Methods in this class:
 * joinWaitlist(...) - Allows users to join the waitlist of an event
 */
public class FirebaseEventRepository implements EventRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * This method adds the specified users id into the waitlist of a given event.
     *
     * The method will update the "events" collection in the application FireStore by adding
     * the user id (uid) into an array named "waitlist". "waitlist" is an array of user ids that
     * are a part of the specified events waitlist.
     *
     * @param eventId The id of event the user wants to join
     * @param uid The id of the user themselves
     * @param successListener Callback on success
     * @param failureListener Callback on failure
     */
    public void joinWaitlist(String eventId, String uid, OnSuccessListener<Void> successListener, OnFailureListener failureListener){
        db.collection("events")
                .document(eventId)
                .update("waitlist", FieldValue.arrayUnion(uid))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }


    /**
     * This method removes the specified users id from the waitlist of a given event.
     *
     * The method will update the "events" collection in the application FireStore by removing
     * the user id (uid) from an array named "waitlist". "waitlist" is an array of user ids
     * that are a part of the specified event waitlist.
     *
     * @param eventId The id of event the user wants to leave
     * @param uid The id of the user themselves
     * @param successListener Callback on success
     * @param failureListener Callback on failure
     */
    public void leaveWaitlist(String eventId, String uid, OnSuccessListener<Void> successListener, OnFailureListener failureListener){
        db.collection("events")
                .document(eventId)
                .update("waitlist", FieldValue.arrayRemove(uid))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    @Override
    public void createEvent(Event event,
                            OnSuccessListener<Void> onSuccess,
                            OnFailureListener onFailure) {

        // create a new doc id
        String id = db.collection("events").document().getId();
        event.id = id;

        db.collection("events")
                .document(id)
                .set(event)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    @Override
    public void uploadPoster(Uri imageUri,
                             OnSuccessListener<String> onSuccess,
                             OnFailureListener onFailure) {

        String path = "posters/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> onSuccess.onSuccess(uri.toString()))
                .addOnFailureListener(onFailure);
    }
}
