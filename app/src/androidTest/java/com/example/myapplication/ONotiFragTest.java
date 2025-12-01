package com.example.myapplication;

import static org.junit.Assert.*;

import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.myapplication.features.organizer.ONotiFrag;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for ONotiFrag:
 * - Verifies that sendCustomPush(...) creates a custom notification
 *   for the correct recipients based on notificationList.
 */
@RunWith(AndroidJUnit4.class)
public class ONotiFragTest {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String testEmail;
    private String testPassword;
    private String testUserId;

    // Track created docs for cleanup
    private final List<DocumentReference> createdUserDocs = new ArrayList<>();
    private final List<DocumentReference> createdNotificationListDocs = new ArrayList<>();
    private final List<DocumentReference> createdNotificationDocs = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        testPassword = "testPassword123";
        testEmail = "org_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        // Create & sign in organizer user
        AuthResult result = Tasks.await(
                auth.createUserWithEmailAndPassword(testEmail, testPassword),
                30,
                TimeUnit.SECONDS
        );
        assertNotNull(result);
        assertNotNull(result.getUser());

        testUserId = result.getUser().getUid();
        assertNotNull(testUserId);

        // Create a user document so preloadOrganizerName() has something to read
        DocumentReference userRef = db.collection("users").document(testUserId);
        Tasks.await(
                userRef.set(new java.util.HashMap<String, Object>() {{
                    put("firstName", "Test");
                    put("lastName", "Organizer");
                    put("email", testEmail);
                }}),
                15,
                TimeUnit.SECONDS
        );
        createdUserDocs.add(userRef);
    }

    @After
    public void tearDown() throws Exception {
        // Clean up notifications we created
        for (DocumentReference ref : createdNotificationDocs) {
            try {
                Tasks.await(ref.delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Error cleaning notifications: " + e.getMessage());
            }
        }

        // Clean up notificationList docs
        for (DocumentReference ref : createdNotificationListDocs) {
            try {
                Tasks.await(ref.delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Error cleaning notificationList: " + e.getMessage());
            }
        }

        // Clean up user docs
        for (DocumentReference ref : createdUserDocs) {
            try {
                Tasks.await(ref.delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Error cleaning users: " + e.getMessage());
            }
        }

        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }
    }

    /**
     * Helper: create a notificationList doc for a given eventId
     * with "all" audience containing some test recipients.
     */
    private List<String> createNotificationListForEvent(String eventId) throws Exception {
        List<String> recipients = Arrays.asList(
                testUserId,
                "otherUser123"
        );

        DocumentReference notifListRef = db.collection("notificationList").document();
        Tasks.await(
                notifListRef.set(new java.util.HashMap<String, Object>() {{
                    put("eventId", eventId);
                    put("all", recipients);
                    // other audience arrays can be omitted for this test
                }}),
                15,
                TimeUnit.SECONDS
        );

        createdNotificationListDocs.add(notifListRef);
        return recipients;
    }

    /**
     * Test that calling sendCustomPush(...) with Audience.ALL:
     *  - reads recipients from notificationList.all
     *  - creates a custom notification in "notifications" with correct eventId and uID list.
     */
    @Test
    public void testSendCustomPushAllAudienceCreatesNotification() throws Exception {
        assertNotNull("User must be logged in", auth.getCurrentUser());
        assertEquals(testUserId, auth.getCurrentUser().getUid());

        final String eventId = "event_" + UUID.randomUUID().toString().substring(0, 6);
        final String message = "Hello all attendees!";
        final String eventName = "Test Event for Custom Noti";

        // notificationList doc with "all" audience
        List<String> expectedRecipients = createNotificationListForEvent(eventId);

        // Launch organizer notifications fragment
        FragmentScenario<ONotiFrag> scenario =
                FragmentScenario.launchInContainer(
                        ONotiFrag.class,
                        null,
                        R.style.Theme_MyApplication // adjust to your app theme
                );

        // Let preloadOrganizerName() run a bit
        Thread.sleep(2000);

        // Use reflection to call private sendCustomPush(String, String, Audience.ALL)
        scenario.onFragment(fragment -> {
            try {
                Class<?> fragClass = fragment.getClass();

                // Set selectedEventName so sendCustomPush has a non-empty event name
                Field nameField = fragClass.getDeclaredField("selectedEventName");
                nameField.setAccessible(true);
                nameField.set(fragment, eventName);

                // Find the private enum Audience inside ONotiFrag
                Class<?> audienceClass = null;
                for (Class<?> inner : fragClass.getDeclaredClasses()) {
                    if (inner.getSimpleName().equals("Audience") && inner.isEnum()) {
                        audienceClass = inner;
                        break;
                    }
                }
                assertNotNull("Audience enum should be found by reflection", audienceClass);

                @SuppressWarnings("unchecked")
                Object audienceAll = Enum.valueOf((Class<Enum>) audienceClass, "ALL");

                // Locate private sendCustomPush(String eventId, String message, Audience audience)
                Method sendCustomPush = fragClass.getDeclaredMethod(
                        "sendCustomPush",
                        String.class,
                        String.class,
                        audienceClass
                );
                sendCustomPush.setAccessible(true);

                // Invoke sendCustomPush(...)
                sendCustomPush.invoke(fragment, eventId, message, audienceAll);

            } catch (Exception e) {
                throw new RuntimeException("Reflection call to sendCustomPush failed", e);
            }
        });

        // Wait a bit to allow Firestore write to complete
        Thread.sleep(4000);

        // Query notifications for our eventId and type = "custom"
        QuerySnapshot qs = Tasks.await(
                db.collection("notifications")
                        .whereEqualTo("eventId", eventId)
                        .whereEqualTo("type", "custom")
                        .get(),
                20,
                TimeUnit.SECONDS
        );

        assertFalse("Custom notification should be created", qs.isEmpty());

        DocumentSnapshot doc = qs.getDocuments().get(0);
        createdNotificationDocs.add(doc.getReference());

        assertEquals("EventId should match", eventId, doc.getString("eventId"));
        assertEquals("Type should be custom", "custom", doc.getString("type"));
        assertEquals("Message should match", message, doc.getString("message"));

        @SuppressWarnings("unchecked")
        List<String> actualRecipients = (List<String>) doc.get("uID");
        assertNotNull("uID list should not be null", actualRecipients);
        assertEquals("Recipients list size should match", expectedRecipients.size(), actualRecipients.size());
        assertTrue("Recipients should contain all expected IDs", actualRecipients.containsAll(expectedRecipients));
    }
}
