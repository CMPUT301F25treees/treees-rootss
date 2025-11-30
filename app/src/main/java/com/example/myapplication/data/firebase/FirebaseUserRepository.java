package com.example.myapplication.data.firebase;

import com.example.myapplication.data.repo.UserRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class FirebaseUserRepository implements UserRepository {

    private final FirebaseFirestore firestore;

    public FirebaseUserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void deleteEventsForUser(String uid, Runnable onSuccess, OnFailureListener onFailure) {
        deleteEventsByField("organizerID", uid,
                () -> deleteEventsByField("organizerId", uid, onSuccess, onFailure),
                onFailure);
    }

    private void deleteEventsByField(String fieldName, String uid, Runnable onSuccess, OnFailureListener onFailure) {
        firestore.collection("events")
                .whereEqualTo(fieldName, uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        onSuccess.run();
                        return;
                    }
                    List<Task<Void>> deletions = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        deletions.add(doc.getReference().delete());
                    }
                    Tasks.whenAllComplete(deletions)
                            .addOnSuccessListener(tasks -> onSuccess.run())
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    @Override
    public void disableEventsForUser(String uid, Runnable onSuccess, OnFailureListener onFailure) {
         firestore.collection("events").whereEqualTo("organizerID", uid).get()
                .addOnSuccessListener(q -> {
                    if (q == null || q.isEmpty()) {
                        onSuccess.run();
                        return;
                    }
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        batch.update(doc.getReference(), "disabled", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> onSuccess.run())
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    @Override
    public void deleteUserDocument(String uid, Runnable onSuccess, OnFailureListener onFailure) {
        firestore.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure);
    }

    @Override
    public void deleteAuthUser(FirebaseUser user, Runnable onSuccess, OnFailureListener onFailure) {
        user.delete()
                .addOnSuccessListener(aVoid -> onSuccess.run())
                .addOnFailureListener(onFailure);
    }

    /**
     * Updates the organizer's rating in Firestore.
     * @param organizerId The ID of the organizer to rate.
     * @param newRating The new rating value (1-5) provided by the entrant.
     * @param notificationId The ID of the notification that triggered this rating (to delete it).
     * @param onSuccess Callback for success.
     * @param onFailure Callback for failure.
     */
    public void submitOrganizerRating(String organizerId, int newRating, String notificationId, Runnable onSuccess, OnFailureListener onFailure) {
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(firestore.collection("users").document(organizerId));
            Double currentRating = snapshot.getDouble("rating");
            Long countLong = snapshot.getLong("ratingCount");
            
            if (currentRating == null) currentRating = 0.0;
            int currentCount = (countLong == null) ? 0 : countLong.intValue();
            
            double newAverage = ((currentRating * currentCount) + newRating) / (currentCount + 1);
            
            transaction.update(firestore.collection("users").document(organizerId), "rating", newAverage);
            transaction.update(firestore.collection("users").document(organizerId), "ratingCount", currentCount + 1);
            transaction.delete(firestore.collection("notifications").document(notificationId));
            
            return null;
        }).addOnSuccessListener(aVoid -> onSuccess.run())
          .addOnFailureListener(onFailure);
    }
}
