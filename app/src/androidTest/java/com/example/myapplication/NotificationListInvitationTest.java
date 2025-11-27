package com.example.myapplication;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for invitation state logic backed by the `notificationList` collection.
 *
 * These tests:
 *  - Create a real Firebase Auth user
 *  - Insert documents into `notificationList` and `notifications`
 *  - Reproduce the logic from UNotiFrag.checkInvitationStatus:
 *        isFinal → isInvited → isCancelled → other
 *  - Assert the computed state matches expectations for each scenario.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationListInvitationTest {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private CollectionReference notificationsRef;
    private CollectionReference notificationListRef;

    private String testEmail;
    private String testPassword;
    private String testUserId;

    // Track created docs so we can clean them up
    private final List<DocumentReference> createdNotificationDocs = new ArrayList<>();
    private final List<DocumentReference> createdNotificationListDocs = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        notificationsRef = db.collection("notifications");
        notificationListRef = db.collection("notificationList");

        testPassword = "testPassword123";
        testEmail = "inv_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        // Create / sign in test user
        AuthResult authResult = Tasks.await(
                auth.createUserWithEmailAndPassword(testEmail, testPassword),
                30,
                TimeUnit.SECONDS
        );

        assertNotNull("Auth result should not be null", authResult);
        assertNotNull("User should not be null", authResult.getUser());

        testUserId = authResult.getUser().getUid();
        assertNotNull("User ID should not be null", testUserId);
    }

    @After
    public void tearDown() throws Exception {
        // Delete test notification docs
        for (DocumentReference doc : createdNotificationDocs) {
            try {
                Tasks.await(doc.delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Cleanup error (notification): " + e.getMessage());
            }
        }

        // Delete test notificationList docs
        for (DocumentReference doc : createdNotificationListDocs) {
            try {
                Tasks.await(doc.delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Cleanup error (notificationList): " + e.getMessage());
            }
        }

        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }
    }

    /**
     * Helper: create the `notificationList` document with a given eventId and lists.
     */
    private DocumentReference createNotificationListDoc(
            String eventId,
            List<String> invited,
            List<String> finalUsers,
            List<String> cancelled
    ) throws Exception {

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        if (invited != null) {
            data.put("invited", invited);
        }
        if (finalUsers != null) {
            data.put("final", finalUsers);
        }
        if (cancelled != null) {
            data.put("cancelled", cancelled);
        }

        DocumentReference docRef = notificationListRef.document();
        Tasks.await(docRef.set(data), 10, TimeUnit.SECONDS);
        createdNotificationListDocs.add(docRef);
        return docRef;
    }

    /**
     * Helper: create a lottery_win notification in `notifications` for the user / event.
     */
    private DocumentReference createLotteryNotification(String eventId, String message) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("uID", Arrays.asList(testUserId));
        data.put("type", "lottery_win");
        data.put("eventId", eventId);
        data.put("event", "Invitation Test Event");
        data.put("message", message);
        data.put("dateMade", new Timestamp(new Date()));

        DocumentReference docRef = notificationsRef.document();
        Tasks.await(docRef.set(data), 10, TimeUnit.SECONDS);
        createdNotificationDocs.add(docRef);
        return docRef;
    }

    /**
     * Helper: compute invitation state using the same logic as UNotiFrag.checkInvitationStatus:
     *
     *  isFinal → "FINAL"
     *  else if isInvited → "INVITED"
     *  else if isCancelled → "CANCELLED"
     *  else → "OTHER"
     */
    private String computeInvitationState(DocumentSnapshot doc) {
        @SuppressWarnings("unchecked")
        List<String> invited = (List<String>) doc.get("invited");
        @SuppressWarnings("unchecked")
        List<String> finalUsers = (List<String>) doc.get("final");
        @SuppressWarnings("unchecked")
        List<String> cancelled = (List<String>) doc.get("cancelled");

        boolean isInvited = invited != null && invited.contains(testUserId);
        boolean isFinal = finalUsers != null && finalUsers.contains(testUserId);
        boolean isCancelled = cancelled != null && cancelled.contains(testUserId);

        if (isFinal) {
            return "FINAL";
        } else if (isInvited) {
            return "INVITED";
        } else if (isCancelled) {
            return "CANCELLED";
        } else {
            return "OTHER";
        }
    }

    /**
     * Scenario 1: user is in `invited` only → state should be "INVITED".
     * This should drive UNotiFrag towards showInviteOption().
     */
    @Test
    public void testInvitedState() throws Exception {
        String eventId = "event_invited_" + UUID.randomUUID().toString().substring(0, 4);

        createNotificationListDoc(
                eventId,
                Arrays.asList(testUserId), // invited
                null,                      // final
                null                       // cancelled
        );
        createLotteryNotification(eventId, "You have been invited!");

        QuerySnapshot qs = Tasks.await(
                notificationListRef.whereEqualTo("eventId", eventId).limit(1).get(),
                15,
                TimeUnit.SECONDS
        );

        assertFalse("notificationList doc should exist", qs.isEmpty());
        DocumentSnapshot doc = qs.getDocuments().get(0);

        String state = computeInvitationState(doc);
        assertEquals("State should be INVITED when user is only in invited list",
                "INVITED", state);

        System.out.println("testInvitedState passed for " + testEmail);
    }

    /**
     * Scenario 2: user is in `final` (and optionally also in invited) → state should be "FINAL".
     * This should drive UNotiFrag towards showAlreadyAcceptedOption().
     */
    @Test
    public void testFinalState() throws Exception {
        String eventId = "event_final_" + UUID.randomUUID().toString().substring(0, 4);

        // User is both invited and final; isFinal should take precedence
        createNotificationListDoc(
                eventId,
                Arrays.asList(testUserId),
                Arrays.asList(testUserId),
                null
        );
        createLotteryNotification(eventId, "You already accepted this invite!");

        QuerySnapshot qs = Tasks.await(
                notificationListRef.whereEqualTo("eventId", eventId).limit(1).get(),
                15,
                TimeUnit.SECONDS
        );

        assertFalse("notificationList doc should exist", qs.isEmpty());
        DocumentSnapshot doc = qs.getDocuments().get(0);

        String state = computeInvitationState(doc);
        assertEquals("State should be FINAL when user is in final list",
                "FINAL", state);

        System.out.println("testFinalState passed for " + testEmail);
    }

    /**
     * Scenario 3: user is in `cancelled` only → state should be "CANCELLED".
     * This should drive UNotiFrag towards showAlreadyDeclinedOption().
     */
    @Test
    public void testCancelledState() throws Exception {
        String eventId = "event_cancelled_" + UUID.randomUUID().toString().substring(0, 4);

        createNotificationListDoc(
                eventId,
                null,
                null,
                Arrays.asList(testUserId)
        );
        createLotteryNotification(eventId, "You have already declined this invite!");

        QuerySnapshot qs = Tasks.await(
                notificationListRef.whereEqualTo("eventId", eventId).limit(1).get(),
                15,
                TimeUnit.SECONDS
        );

        assertFalse("notificationList doc should exist", qs.isEmpty());
        DocumentSnapshot doc = qs.getDocuments().get(0);

        String state = computeInvitationState(doc);
        assertEquals("State should be CANCELLED when user is only in cancelled list",
                "CANCELLED", state);

        System.out.println("testCancelledState passed for " + testEmail);
    }

    /**
     * Scenario 4: user in none of the lists → state should be "OTHER".
     * This should drive UNotiFrag towards showOtherOption().
     */
    @Test
    public void testOtherState() throws Exception {
        String eventId = "event_other_" + UUID.randomUUID().toString().substring(0, 4);

        // Put some *other* user IDs in the lists, but not testUserId
        createNotificationListDoc(
                eventId,
                Arrays.asList("someone_else"),
                Arrays.asList("another_user"),
                Arrays.asList("third_user")
        );
        createLotteryNotification(eventId, "No special invite state for you.");

        QuerySnapshot qs = Tasks.await(
                notificationListRef.whereEqualTo("eventId", eventId).limit(1).get(),
                15,
                TimeUnit.SECONDS
        );

        assertFalse("notificationList doc should exist", qs.isEmpty());
        DocumentSnapshot doc = qs.getDocuments().get(0);

        String state = computeInvitationState(doc);
        assertEquals("State should be OTHER when user is not in any list",
                "OTHER", state);

        System.out.println("testOtherState passed for " + testEmail);
    }
}
