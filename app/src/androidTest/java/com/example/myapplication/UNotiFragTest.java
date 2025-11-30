package com.example.myapplication;

import static org.junit.Assert.*;

import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.myapplication.features.user.UNotiFrag;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.widget.SwitchCompat;

/**
 * Instrumented tests for user notifications and UNotiFrag.
 *
 * These tests:
 *  - Create a real Firebase Auth user
 *  - Insert notification documents in Firestore
 *  - Verify the same Firestore query used by UNotiFrag
 *  - Launch UNotiFrag and assert basic view wiring (RecyclerView, switch, adapter)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UNotiFragTest {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference notificationsRef;

    private String testEmail;
    private String testPassword;
    private String testUserId;

    @Before
    public void setUp() throws Exception {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        notificationsRef = db.collection("notifications");

        testPassword = "testPassword123";
        testEmail = "notif_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        // Create and sign in a test user
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
        if (testUserId != null) {
            try {
                QuerySnapshot snapshot = Tasks.await(
                        notificationsRef.whereArrayContains("uID", testUserId).get(),
                        15,
                        TimeUnit.SECONDS
                );
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                System.err.println("Cleanup error (notifications): " + e.getMessage());
            }
        }

        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }
    }

    /**
     * Helper method to create a personal (non-invitation) notification for the test user.
     */
    private DocumentReference createPersonalNotification(String message, Date date) throws Exception {
        String eventId = "event_" + UUID.randomUUID().toString().substring(0, 6);

        Map<String, Object> data = new HashMap<>();
        data.put("uID", Arrays.asList(testUserId));
        data.put("type", "custom");
        data.put("eventId", eventId);
        data.put("event", "Test Event");
        data.put("message", message);
        data.put("dateMade", new Timestamp(date));

        DocumentReference docRef = notificationsRef.document();
        Tasks.await(docRef.set(data), 10, TimeUnit.SECONDS);
        return docRef;
    }

    /**
     * Test that the Firestore query used by UNotiFrag returns
     * notifications created for the current user.
     */
    @Test
    public void testCreateAndFetchPersonalNotificationForUser() throws Exception {
        assertNotNull("User must be logged in", auth.getCurrentUser());
        assertEquals("User IDs should match", testUserId, auth.getCurrentUser().getUid());

        String uniqueMessage = "Test personal notification " + UUID.randomUUID().toString().substring(0, 4);

        createPersonalNotification(uniqueMessage, new Date());

        Query query = notificationsRef
                .whereArrayContains("uID", testUserId)
                .orderBy("dateMade", Query.Direction.DESCENDING);

        QuerySnapshot snapshot = Tasks.await(query.get(), 15, TimeUnit.SECONDS);

        assertFalse("There should be at least one notification for this user", snapshot.isEmpty());

        boolean found = false;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String msg = doc.getString("message");
            if (uniqueMessage.equals(msg)) {
                found = true;
                assertEquals("Notification type should be custom", "custom", doc.getString("type"));
                assertEquals("Event name should match", "Test Event", doc.getString("event"));
                break;
            }
        }

        assertTrue("The test notification should be returned by the query", found);
        System.out.println("testCreateAndFetchPersonalNotificationForUser passed for " + testEmail);
    }

    /**
     * Test that notifications are ordered by dateMade in descending order,
     * matching UNotiFrag's query behavior.
     */
    @Test
    public void testNotificationsAreOrderedByDateDescending() throws Exception {
        assertNotNull("User must be logged in", auth.getCurrentUser());

        Date olderDate = new Date(System.currentTimeMillis() - 60_000L); // 1 minute ago
        Date newerDate = new Date();

        String olderMessage = "Older notification " + UUID.randomUUID().toString().substring(0, 4);
        String newerMessage = "Newer notification " + UUID.randomUUID().toString().substring(0, 4);

        createPersonalNotification(olderMessage, olderDate);
        createPersonalNotification(newerMessage, newerDate);

        Query query = notificationsRef
                .whereArrayContains("uID", testUserId)
                .orderBy("dateMade", Query.Direction.DESCENDING);

        QuerySnapshot snapshot = Tasks.await(query.get(), 15, TimeUnit.SECONDS);
        assertFalse("There should be notifications for this user", snapshot.isEmpty());

        // The first document should be the newest by dateMade
        DocumentSnapshot firstDoc = snapshot.getDocuments().get(0);
        String firstMessage = firstDoc.getString("message");

        assertEquals(
                "Newest notification should come first in DESC order",
                newerMessage,
                firstMessage
        );

        System.out.println("testNotificationsAreOrderedByDateDescending passed for " + testEmail);
    }

    /**
     * Launches UNotiFrag in a container and verifies that:
     *  - The view is created
     *  - RecyclerView and switch are present
     *  - The RecyclerView has a non-null adapter
     *  - Personal notifications are enabled by default (switch checked)
     *
     * This mirrors the wiring done in onViewCreated().
     */
    @Test
    public void testFragmentInitializesRecyclerViewAndSwitch() {
        assertNotNull("User must be logged in before launching fragment", auth.getCurrentUser());

        // NOTE: You may need to use your app theme here
        FragmentScenario<UNotiFrag> scenario =
                FragmentScenario.launchInContainer(
                        UNotiFrag.class,
                        null,
                        R.style.Theme_MyApplication

                );

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull("Fragment view should not be null", root);

            RecyclerView recyclerView = root.findViewById(R.id.notifications_list);
            assertNotNull("RecyclerView should be present in the layout", recyclerView);
            assertNotNull("RecyclerView should have an adapter attached", recyclerView.getAdapter());

            SwitchCompat switchNotifications = root.findViewById(R.id.switch_notifications);
            assertNotNull("Notification switch should be present in the layout", switchNotifications);
            assertTrue("Personal notifications switch should be checked by default",
                    switchNotifications.isChecked());
        });

        System.out.println("testFragmentInitializesRecyclerViewAndSwitch passed for " + testEmail);
    }
}
