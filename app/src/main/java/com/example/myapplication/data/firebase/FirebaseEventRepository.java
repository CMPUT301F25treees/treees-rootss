package com.example.myapplication.data.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.example.myapplication.data.model.Event;
import com.example.myapplication.data.repo.EventRepository;
import com.example.myapplication.data.repo.ImageRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * This class is meant to handle any operations that will happen to the events in Firebase
 *
 * Methods in this class:
 * joinWaitlist(...) - Allows users to join the waitlist of an event
 */
public class FirebaseEventRepository implements EventRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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


    public interface EventListCallback {
        void onEventsFetched(List<UserEvent> events);
        void onError(Exception e);
    }

    public void getAllEvents(EventListCallback callback){
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserEvent> events = new ArrayList<>();
                    for(var doc : queryDocumentSnapshots) {
                        UserEvent event = doc.toObject(UserEvent.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }
                    callback.onEventsFetched(events);
                })
                .addOnFailureListener(callback::onError);
    }

    public interface SingleEventCallback {
        void onEventFetched(UserEvent event);
        void onError(Exception e);
    }

    public void fetchEventById(String eventId, SingleEventCallback callback) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserEvent event = doc.toObject(UserEvent.class);
                        if (event != null) {
                            event.setId(doc.getId());
                        }
                        callback.onEventFetched(event);
                    } else {
                        callback.onError(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void createEvent(Context context, UserEvent event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {

        // create a new doc id
        String id = db.collection("events").document().getId();
        event.setId(id);

        String qrData = id;

        Bitmap qrBitmap;

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrBitmap = barcodeEncoder.encodeBitmap(
                    id,
                    BarcodeFormat.QR_CODE,
                    600,  // width
                    600   // height
            );
        } catch (Exception e) {
            e.printStackTrace();
            onFailure.onFailure(e);
            return;
        }

        File qrFile;
        try {
            qrFile = new File(context.getCacheDir(), "qr_" + id + ".png");
            FileOutputStream fos = new FileOutputStream(qrFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            onFailure.onFailure(e);
            return;
        }

        Uri qrUri = Uri.fromFile(qrFile);

        ImageRepository imageRepository = new ImageRepository();
        imageRepository.uploadImage(qrUri, new ImageRepository.UploadCallback() {
            @Override
            public void onSuccess(String secureUrl) {
                // 6) Store QR URL on the event
                event.setQrData(secureUrl);

                // 7) Save the event (with qrImageUrl) into Firestore
                db.collection("events")
                        .document(id)
                        .set(event)
                        .addOnSuccessListener(onSuccess)
                        .addOnFailureListener(onFailure);
            }

            @Override
            public void onError(String e) {
                onFailure.onFailure(new Exception("QR upload failed: " + e));
            }
        });


    }


    @Override
    public void updateEvent(String eventId, UserEvent event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        event.setId(eventId); // Ensure the event has the correct ID

        db.collection("events")
                .document(eventId)
                .set(event)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void sendLotteryWinNotifications(String eventId, String eventName, List<String> winnerIds,
                                            OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {

        var payload = new java.util.HashMap<String, Object>();
        payload.put("dateMade", com.google.firebase.Timestamp.now());
        payload.put("event", eventName);
        payload.put("eventId", eventId);
        payload.put("from", "System");
        payload.put("message", "🎉 Congratulations! You've been selected to participate in this event.");
        payload.put("type", "lottery_win");
        payload.put("uID", winnerIds);

        db.collection("notifications")
                .add(payload)
                .addOnSuccessListener(ref -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }

    public void sendLotteryLostNotifications(String eventId, String eventName, List<String> loserIds,
                                             OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {

        if (loserIds == null || loserIds.isEmpty()) {
            onSuccess.onSuccess(null); // No losers to notify
            return;
        }

        var payload = new java.util.HashMap<String, Object>();
        payload.put("dateMade", com.google.firebase.Timestamp.now());
        payload.put("event", eventName);
        payload.put("eventId", eventId);
        payload.put("from", "System");
        payload.put("message", "Unfortunately, you were not selected for this event. Better luck next time!");
        payload.put("type", "lottery_lost");
        payload.put("uID", loserIds);

        db.collection("notifications")
                .add(payload)
                .addOnSuccessListener(ref -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }

    public void runLottery(String eventId, String eventName, List<String> waitlist, int numToSelect,
                           OnSuccessListener<Integer> onSuccess, OnFailureListener onFailure) {

        if (waitlist == null || waitlist.isEmpty()) {
            onFailure.onFailure(new Exception("Waitlist is empty"));
            return;
        }

        int actualSelection = Math.min(numToSelect, waitlist.size());
        List<String> shuffled = new ArrayList<>(waitlist);
        java.util.Collections.shuffle(shuffled);
        List<String> winners = shuffled.subList(0, actualSelection);

        // Get the losers (those not selected)
        List<String> losers = new ArrayList<>(waitlist);
        losers.removeAll(winners);

        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        var doc = qs.getDocuments().get(0);
                        doc.getReference()
                                .update("invited", FieldValue.arrayUnion(winners.toArray()))
                                .addOnSuccessListener(aVoid -> {
                                    // Notify winners
                                    sendLotteryWinNotifications(eventId, eventName, winners,
                                            v -> {
                                                // After winners are notified, notify losers
                                                sendLotteryLostNotifications(eventId, eventName, losers,
                                                        v2 -> onSuccess.onSuccess(winners.size()),
                                                        onFailure);
                                            },
                                            onFailure);
                                })
                                .addOnFailureListener(onFailure);
                    } else {
                        var payload = new java.util.HashMap<String, Object>();
                        payload.put("eventId", eventId);
                        payload.put("invited", winners);
                        payload.put("waiting", new ArrayList<>());
                        payload.put("cancelled", new ArrayList<>());
                        payload.put("all", winners);

                        db.collection("notificationList")
                                .add(payload)
                                .addOnSuccessListener(ref -> {
                                    // Notify winners
                                    sendLotteryWinNotifications(eventId, eventName, winners,
                                            v -> {
                                                // After winners are notified, notify losers
                                                sendLotteryLostNotifications(eventId, eventName, losers,
                                                        v2 -> onSuccess.onSuccess(winners.size()),
                                                        onFailure);
                                            },
                                            onFailure);
                                })
                                .addOnFailureListener(onFailure);
                    }
                })
                .addOnFailureListener(onFailure);
    }
}
