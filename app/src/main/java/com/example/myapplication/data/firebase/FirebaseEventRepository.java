package com.example.myapplication.data.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.example.myapplication.data.model.EntrantLocation;
import com.example.myapplication.data.model.Event;
import com.example.myapplication.data.model.NotificationList;
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
     * are a part of the specified events waitlist. It also updates the notificationList document
     * for the linked event with the user id.
     *
     * @param eventId The id of event the user wants to join
     * @param uid The id of the user themselves
     * @param successListener Callback on success
     * @param failureListener Callback on failure
     */
    public void joinWaitlist(String eventId,
                             String uid,
                             @Nullable Double lat,
                             @Nullable Double lng,
                             OnSuccessListener<Void> successListener,
                             OnFailureListener failureListener) {

        db.collection("events")
                .document(eventId)
                .update("waitlist", FieldValue.arrayUnion(uid))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);

        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(x -> {
                    if (!x.isEmpty()) {
                        var doc = x.getDocuments().get(0);
                        doc.getReference().update(
                                "waiting", FieldValue.arrayUnion(uid),
                                "all", FieldValue.arrayUnion(uid)
                        );
                    }
                });

        // store location if present
        if (lat != null && lng != null) {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("uid", uid);
            payload.put("lat", lat);
            payload.put("lng", lng);
            payload.put("joinedAt", FieldValue.serverTimestamp());

            db.collection("events")
                    .document(eventId)
                    .collection("waitlistLocations")
                    .document(uid)
                    .set(payload);
        }
    }


    /**
     * This method removes the specified users id from the waitlist of a given event.
     *
     * The method will update the "events" collection in the application FireStore by removing
     * the user id (uid) from an array named "waitlist". "waitlist" is an array of user ids
     * that are a part of the specified event waitlist. Also updated the notificationList to remove
     * the user id from the waitlist array.
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

        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(x -> {
                    if (!x.isEmpty()) {
                        var doc = x.getDocuments().get(0);
                        doc.getReference().update(
                                "waiting", FieldValue.arrayRemove(uid)
                        );
                    }
                });
    }


    public interface EventListCallback {
        void onEventsFetched(List<UserEvent> events);

        void onError(Exception e);
    }

    /**
     * Gets all events that are stored in the "events" collection on Firestore.
     *
     * Each document is converted into a UserEvent object and is returned in a list
     * through the callback.
     *
     * @param callback gets the list of events or receives an error.
     */
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

    /**
     * This method gets only one event by the event id, which matches the specified events documnet
     * id in the "events" collection on Firestore.
     *
     * documents is converted into a UserEvent object and returned through the callback.
     *
     * @param eventId the eventId is a Firestore document ID of the specified event
     * @param callback receives an event or an error
     */
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

    /**
     * Creates a new event in the Firestore "events" collection as well as the QR code image.
     *
     * The following operations are performed by this method:
     * - A Firestore document Id gets generated.
     * - A QR code gets generated and saved to cloudinary database
     * - The event objects itself gets saved to Firestore
     * - A corresponding notificationList document is created in Firestore.
     *
     * @param context application context
     * @param event UserEvent object that is to be created on Firestore
     * @param onSuccess callback triggered when successful
     * @param onFailure callback triggered when uncsuccessful
     */
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
                event.setQrData(secureUrl);

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

        db.collection("notificationList")
                .document(id)
                .set(new NotificationList(id));


    }

    /**
     * This method Updated an existing event in firestore.
     *
     * Other data is ovverwritten by the newly provided UserEvent object.
     *
     * @param eventId Firestore ID of the event
     * @param event event with updated data
     * @param onSuccess callback triggered when successful
     * @param onFailure callback triggered when unsuccessful
     */
    @Override
    public void updateEvent(String eventId, UserEvent event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        event.setId(eventId); // Ensure the event has the correct ID

        db.collection("events")
                .document(eventId)
                .set(event)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * This method sends a notification to all users who have won the lottery for a specified event.
     *
     * A document gets created in the "notifications" collection containing event details, message type,
     * and the list of userIds that won.
     *
     * @param eventId Firestore ID of the event
     * @param eventName the name of the event
     * @param winnerIds list of users Ids who won
     * @param onSuccess callback triggered when notificaiton is successfully added
     * @param onFailure callback triggered on Firestore failures
     */
    public void sendLotteryWinNotifications(String eventId, String eventName, List<String> winnerIds,
                                            OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {

        var payload = new java.util.HashMap<String, Object>();
        payload.put("dateMade", com.google.firebase.Timestamp.now());
        payload.put("event", eventName);
        payload.put("eventId", eventId);
        payload.put("from", "System");
        payload.put("message", "Congratulations! You've been selected to participate in this event.");
        payload.put("type", "lottery_win");
        payload.put("status", "pending");
        payload.put("uID", winnerIds);

        db.collection("notifications")
                .add(payload)
                .addOnSuccessListener(ref -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }

    /**
     * This method sends a notification to all users who have lost the lottery for a specified event.
     *
     * A document gets created in the "notifications" collection containing event details, message type,
     * and the list of userIds that lost.
     *
     * @param eventId Firestore id of the event
     * @param eventName the name of the event
     * @param loserIds list of users Ids who lost
     * @param onSuccess callback triggered when successful
     * @param onFailure callback triggered when unsuccessful
     */
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

    /**
     * This method runs the randomized lottery among the users in the events waitlist.
     *
     * Up to {numToSelect} users get selected as winners, which then leads to both
     * winners and losers getting notified. As well as updates to the "notificationList" collections
     * to record the invited participants.
     *
     * @param eventId Firestore Id of the event
     * @param eventName the name of teh event
     * @param waitlist list of user ids in the waiting list
     * @param numToSelect number of users that will be randomly selected
     * @param onSuccess callback triggered when successful
     * @param onFailure callback triggered when unsuccessful
     */
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
                                .update(
                                        "invited", FieldValue.arrayUnion(winners.toArray()),
                                        "waiting", FieldValue.arrayRemove(winners.toArray()),
                                        "all", FieldValue.arrayUnion(winners.toArray()))
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

    /**
     * Accepts the event invitation that a user receives.
     *
     * This method is in charge of the following:
     * - Updates status to "accepted"
     * - Moves the user from the invited list ot the final list
     * - The user gets removed from the waiting list.
     *
     * @param notificationId Firestore id of the notificationList
     * @param eventId the event that the invitation is connected to
     * @param userId the user who accepts the invite
     * @param onSuccess callback triggered on a successful update
     * @param onFailure callback triggered on a failure
     */
    public void acceptInvitation(String notificationId, String eventId, String userId,
                                 OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {

        // Update notification status to "accepted"
        db.collection("notifications")
                .document(notificationId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    db.collection("notificationList")
                            .whereEqualTo("eventId", eventId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (!qs.isEmpty()) {
                                    var doc = qs.getDocuments().get(0);
                                    doc.getReference()
                                            .update(
                                                    "waiting", FieldValue.arrayRemove(userId),
                                                    "final", FieldValue.arrayUnion(userId)
                                            )
                                            .addOnSuccessListener(v -> onSuccess.onSuccess(null))
                                            .addOnFailureListener(onFailure);
                                } else {
                                    onSuccess.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);

        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(x -> {
                    if (!x.isEmpty()) {
                        var doc = x.getDocuments().get(0);
                        doc.getReference().update(
                                "invited", FieldValue.arrayRemove(userId),
                                "waiting", FieldValue.arrayRemove(userId),
                                "final", FieldValue.arrayUnion(userId)
                        );
                    }
                });
    }

    /**
     * Declines an event invitation for a user
     *
     * This method is in charge of the following:
     * - The notification status gets updated to declined
     * - The user gets moved from the invited list to the cancelled list
     * - The user is removed from the waiting list
     *
     * @param notificationId Firestore id of the notification list
     * @param eventId the event that the invitation is connected to
     * @param userId the user who declines the invite
     * @param onSuccess callback triggered on a successful update
     * @param onFailure callback triggered on a failure
     */
    public void declineInvitation(String notificationId, String eventId, String userId,
                                  OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {

        db.collection("notifications")
                .document(notificationId)
                .update("status", "declined")
                .addOnSuccessListener(aVoid -> {
                    // Move user from "invited" to "cancelled" list
                    db.collection("notificationList")
                            .whereEqualTo("eventId", eventId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (!qs.isEmpty()) {
                                    var doc = qs.getDocuments().get(0);
                                    doc.getReference()
                                            .update(
                                                    "invited", FieldValue.arrayRemove(userId),
                                                    "cancelled", FieldValue.arrayUnion(userId),
                                                    "waiting", FieldValue.arrayRemove(userId)
                                            )
                                            .addOnSuccessListener(v -> onSuccess.onSuccess(null))
                                            .addOnFailureListener(onFailure);
                                } else {
                                    onSuccess.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);


    }

    public interface WaitlistLocationCallback {
        void onLocationsFetched(java.util.List<EntrantLocation> locations);
        void onError(Exception e);
    }

    public void getWaitlistLocations(String eventId, WaitlistLocationCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitlistLocations")
                .get()
                .addOnSuccessListener(qs -> {
                    java.util.List<EntrantLocation> result = new java.util.ArrayList<>();
                    for (var doc : qs.getDocuments()) {
                        EntrantLocation loc = doc.toObject(EntrantLocation.class);
                        if (loc != null) {
                            if (loc.getUid() == null) {
                                loc.setUid(doc.getId());
                            }
                            result.add(loc);
                        }
                    }
                    callback.onLocationsFetched(result);
                })
                .addOnFailureListener(callback::onError);
    }


}
