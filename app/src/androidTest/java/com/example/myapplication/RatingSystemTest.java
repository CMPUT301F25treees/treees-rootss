package com.example.myapplication;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.myapplication.data.firebase.FirebaseUserRepository;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.RatingController; // Import RatingController
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for Rating System (Submission and Calculation) via MVC Controller.
 */
@RunWith(AndroidJUnit4.class)
public class RatingSystemTest {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String testOrganizerId;
    private String testNotificationId;
    private String testEventId;
    private String testEntrantId;
    private RatingController ratingController;
    private FirebaseEventRepository eventRepository;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        ratingController = new RatingController();
        eventRepository = new FirebaseEventRepository();
        
        // Generate unique IDs
        testOrganizerId = "test_organizer_" + UUID.randomUUID().toString();
        testNotificationId = "test_noti_" + UUID.randomUUID().toString();
        testEventId = "test_event_" + UUID.randomUUID().toString();
        testEntrantId = "test_entrant_" + UUID.randomUUID().toString();
    }

    @After
    public void tearDown() {
        // Clean up
        try {
            if (testOrganizerId != null) Tasks.await(db.collection("users").document(testOrganizerId).delete(), 5, TimeUnit.SECONDS);
            if (testNotificationId != null) Tasks.await(db.collection("notifications").document(testNotificationId).delete(), 5, TimeUnit.SECONDS);
            if (testEventId != null) {
                Tasks.await(db.collection("events").document(testEventId).delete(), 5, TimeUnit.SECONDS);
                Tasks.await(db.collection("notificationList").document(testEventId).delete(), 5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void testSubmitFirstRating() throws Exception {
        Map<String, Object> organizerMap = new HashMap<>();
        organizerMap.put("firstName", "Test Organizer");
        organizerMap.put("rating", 0.0);
        organizerMap.put("ratingCount", 0);

        Tasks.await(db.collection("users").document(testOrganizerId).set(organizerMap), 10, TimeUnit.SECONDS);

        Map<String, Object> notiMap = new HashMap<>();
        notiMap.put("type", "rating_request");
        Tasks.await(db.collection("notifications").document(testNotificationId).set(notiMap), 10, TimeUnit.SECONDS);

        // Submit a 5-star rating via Controller
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final boolean[] success = {false};

        ratingController.submitRating(testOrganizerId, 5, testNotificationId, 
            () -> {
                success[0] = true;
                latch.countDown();
            },
            e -> {
                latch.countDown();
            }
        );

        latch.await(10, TimeUnit.SECONDS);
        assertTrue("Rating submission should succeed", success[0]);

        // 4. Verify Firestore Update
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(testOrganizerId).get(), 10, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        
        Double rating = snapshot.getDouble("rating");
        Long count = snapshot.getLong("ratingCount");

        assertNotNull(rating);
        assertNotNull(count);
        assertEquals(5.0, rating, 0.01);
        assertEquals(1, count.intValue());
    }

    @Test
    public void testAverageRatingCalculation() throws Exception {
        // 1 rating of 5 stars.
        Map<String, Object> organizerMap = new HashMap<>();
        organizerMap.put("firstName", "Test Organizer");
        organizerMap.put("rating", 5.0);
        organizerMap.put("ratingCount", 1);

        Tasks.await(db.collection("users").document(testOrganizerId).set(organizerMap), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("notifications").document(testNotificationId).set(new HashMap<>()), 10, TimeUnit.SECONDS);

        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        
        ratingController.submitRating(testOrganizerId, 3, testNotificationId, 
            latch::countDown,
            e -> latch.countDown()
        );

        latch.await(10, TimeUnit.SECONDS);

        // 4. Verify: (5 + 3) / 2 = 4.0
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(testOrganizerId).get(), 10, TimeUnit.SECONDS);
        
        Double rating = snapshot.getDouble("rating");
        Long count = snapshot.getLong("ratingCount");

        assertEquals(4.0, rating, 0.01);
        assertEquals(2, count.intValue());
    }
    
    @Test
    public void testFetchOrganizerRating() throws Exception {
        // Create a dummy organizer with rating 4.2
        Map<String, Object> organizerMap = new HashMap<>();
        organizerMap.put("firstName", "Fetch Test");
        organizerMap.put("rating", 4.2);
        organizerMap.put("ratingCount", 10);

        Tasks.await(db.collection("users").document(testOrganizerId).set(organizerMap), 10, TimeUnit.SECONDS);
        
        //  Fetch using Controller
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final double[] fetchedRating = {-1.0};
        
        ratingController.fetchOrganizerRating(testOrganizerId, new RatingController.OnRatingFetchedListener() {
            @Override
            public void onRatingFetched(double rating) {
                fetchedRating[0] = rating;
                latch.countDown();
            }
            
            @Override
            public void onError(Exception e) {
                latch.countDown();
            }
        });
        
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(4.2, fetchedRating[0], 0.01);
    }

    @Test
    public void testSendRatingRequestNotifications() throws Exception {
        // Create Organizer
        Map<String, Object> organizerMap = new HashMap<>();
        organizerMap.put("firstName", "Big Boss");
        Tasks.await(db.collection("users").document(testOrganizerId).set(organizerMap), 10, TimeUnit.SECONDS);

        //  Create Event (Ended in the past)
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("organizerID", testOrganizerId);
        eventMap.put("title", "Super Event");
        eventMap.put("endTimeMillis", System.currentTimeMillis() - 10000); // 10 seconds ago
        Tasks.await(db.collection("events").document(testEventId).set(eventMap), 10, TimeUnit.SECONDS);

        //  Create NotificationList with entrant in 'final'
        Map<String, Object> notiListMap = new HashMap<>();
        notiListMap.put("eventId", testEventId);
        notiListMap.put("final", Arrays.asList(testEntrantId));
        Tasks.await(db.collection("notificationList").document(testEventId).set(notiListMap), 10, TimeUnit.SECONDS);

        // Send Request
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final boolean[] success = {false};

        eventRepository.sendRatingRequestNotifications(testEventId, 
            v -> {
                success[0] = true;
                latch.countDown();
            },
            e -> latch.countDown()
        );
        
        latch.await(10, TimeUnit.SECONDS);
        assertTrue("Send notification should succeed", success[0]);

        //  Verify Notification Created
        QuerySnapshot qs = Tasks.await(
            db.collection("notifications")
              .whereEqualTo("eventId", testEventId)
              .whereEqualTo("type", "rating_request")
              .whereArrayContains("uID", testEntrantId)
              .get(),
            10, TimeUnit.SECONDS
        );

        assertFalse("Should find at least one notification", qs.isEmpty());
        DocumentSnapshot doc = qs.getDocuments().get(0);
        assertEquals("Organizer Name should be in 'from'", "Big Boss", doc.getString("from"));
        assertEquals("Organizer ID should be in 'fromId'", testOrganizerId, doc.getString("fromId"));
    }

    @Test
    public void testSendRatingRequestNotifications_EventNotEnded() throws Exception {
        //  Create Organizer
        Map<String, Object> organizerMap = new HashMap<>();
        organizerMap.put("firstName", "Big Boss");
        Tasks.await(db.collection("users").document(testOrganizerId).set(organizerMap), 10, TimeUnit.SECONDS);

        //  Create Event (Ends in future)
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("organizerID", testOrganizerId);
        eventMap.put("title", "Future Event");
        eventMap.put("endTimeMillis", System.currentTimeMillis() + 100000); // In future
        Tasks.await(db.collection("events").document(testEventId).set(eventMap), 10, TimeUnit.SECONDS);

        // Create NotificationList
        Map<String, Object> notiListMap = new HashMap<>();
        notiListMap.put("eventId", testEventId);
        notiListMap.put("final", Arrays.asList(testEntrantId));
        Tasks.await(db.collection("notificationList").document(testEventId).set(notiListMap), 10, TimeUnit.SECONDS);

        // Send Request
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final boolean[] success = {false};

        eventRepository.sendRatingRequestNotifications(testEventId, 
            v -> {
                success[0] = true;
                latch.countDown();
            },
            e -> latch.countDown()
        );
        
        latch.await(10, TimeUnit.SECONDS);
        assertTrue("Method call should succeed (graceful exit)", success[0]);

        //  Verify no Notification Created
        QuerySnapshot qs = Tasks.await(
            db.collection("notifications")
              .whereEqualTo("eventId", testEventId)
              .whereEqualTo("type", "rating_request")
              .get(),
            10, TimeUnit.SECONDS
        );

        assertTrue("Should not find any notifications", qs.isEmpty());
    }
}
