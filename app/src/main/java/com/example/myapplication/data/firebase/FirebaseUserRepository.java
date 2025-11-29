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
                    for (DocumentSnapshot doc : q) {
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
}
