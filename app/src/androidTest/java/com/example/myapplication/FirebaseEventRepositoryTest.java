package com.example.myapplication;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Instrumentation tests for the {@link FirebaseEventRepository} class.
 *
 * This class focuses on the following tests:
 * - leaveInvitedList
 *
 */
@RunWith(AndroidJUnit4.class)
public class FirebaseEventRepositoryTest {

    private FirebaseFirestore db;
    private FirebaseEventRepository repo;

    /**
     * Initializes the test environment before each test case.
     *
     * This includes obtaining a Firestore instance and constructing a fresh
     * {@link FirebaseEventRepository}. If the Firestore emulator is used,
     * configuration can be added here as well.
     *
     */
    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        repo = new FirebaseEventRepository();
    }

    /**
     * This test verifies that leaveInvitedList correctly removes a user from the notificationLists
     * and any notifications that may have been sent to the user linked to the specified event
     *
     * The test performs the following:
     * - Creates a mock {@code notificationList} document containing the user in its {@code invited}
     *   and {@code all} arrays.
     * - Creates a notification document of type {@code "lottery_win"} that includes the user in
     *   its {@code uID} array.
     * - Calls {@code leaveInvitedList(...)} to remove the user.
     * - Asserts that the user is removed from the {@code invited} and {@code all} arrays but that
     *   other users remain unaffected.
     * - Asserts that the user is removed from the {@code uID} array of matching notification
     * documents
     *
     * @throws Exception thrown if Firestore reads or writes fail during the test
     */
    @Test
    public void leaveInvitedList_removesUserFromNotificationListAndNotifications() throws Exception {
        String eventId = "testEvent_leaveInvited";
        String userId = "user123";
        String otherUserId = "user999";

        Map<String, Object> notificationListPayload = new HashMap<>();
        notificationListPayload.put("eventId", eventId);
        notificationListPayload.put("invited", Arrays.asList(userId, otherUserId));
        notificationListPayload.put("all", Arrays.asList(userId, otherUserId));
        notificationListPayload.put("waiting", Collections.emptyList());
        notificationListPayload.put("cancelled", Collections.emptyList());

        Tasks.await(
                db.collection("notificationList")
                        .document(eventId)
                        .set(notificationListPayload)
        );

        Map<String, Object> notifPayload = new HashMap<>();
        notifPayload.put("eventId", eventId);
        notifPayload.put("type", "lottery_win");
        notifPayload.put("uID", Arrays.asList(userId, otherUserId));

        Tasks.await(
                db.collection("notifications")
                        .add(notifPayload)
        );

        CountDownLatch latch = new CountDownLatch(1);

        repo.leaveInvitedList(
                eventId,
                userId,
                v -> latch.countDown(),
                e -> {
                    e.printStackTrace();
                    fail("leaveInvitedList failed: " + e.getMessage());
                    latch.countDown();
                }
        );

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue("leaveInvitedList did not complete in time", completed);

        var notifListSnap = Tasks.await(
                db.collection("notificationList")
                        .document(eventId)
                        .get()
        );

        assertTrue("notificationList doc should exist", notifListSnap.exists());

        java.util.List<String> invited =
                (java.util.List<String>) notifListSnap.get("invited");
        java.util.List<String> all =
                (java.util.List<String>) notifListSnap.get("all");

        assertNotNull(invited);
        assertNotNull(all);

        assertFalse("invited should NOT contain userId", invited.contains(userId));
        assertTrue("invited should still contain otherUserId", invited.contains(otherUserId));

        assertFalse("all should NOT contain userId", all.contains(userId));
        assertTrue("all should still contain otherUserId", all.contains(otherUserId));

        var notifQuerySnap = Tasks.await(
                db.collection("notifications")
                        .whereEqualTo("eventId", eventId)
                        .get()
        );

        assertFalse("Expected at least one notification doc", notifQuerySnap.isEmpty());

        notifQuerySnap.getDocuments().forEach(doc -> {
            java.util.List<String> uids = (java.util.List<String>) doc.get("uID");
            assertNotNull(uids);
            assertFalse("uID array should NOT contain userId", uids.contains(userId));
            assertTrue("uID array should still contain otherUserId", uids.contains(otherUserId));
        });
    }
}