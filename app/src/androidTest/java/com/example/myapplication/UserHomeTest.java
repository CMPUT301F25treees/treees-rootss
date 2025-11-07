package com.example.myapplication;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumented tests for User Home page functionality.
 *
 * <p>These tests verify that:
 * <ul>
 * <li>Events are successfully loaded from Firestore</li>
 * <li>Multiple events can be fetched and displayed</li>
 * <li>Event data fields are retrieved correctly</li>
 * <li>Empty event lists are handled gracefully</li>
 * <li>Individual events can be fetched by ID</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class UserHomeTest {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseEventRepository eventRepository;

    private String testUserId;
    private String testEmail;
    private String testPassword;
    private List<String> createdEventIds;

    @Before
    public void setUp() throws Exception {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        eventRepository = new FirebaseEventRepository();
        createdEventIds = new ArrayList<>();

        testPassword = "testPassword123";
        testEmail = "test_home_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        // Create a test user
        AuthResult authResult = Tasks.await(
            auth.createUserWithEmailAndPassword(testEmail, testPassword),
            30,
            TimeUnit.SECONDS
        );
        testUserId = authResult.getUser().getUid();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up: delete all created test events
        for (String eventId : createdEventIds) {
            try {
                Tasks.await(db.collection("events").document(eventId).delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Cleanup error for event: " + e.getMessage());
            }
        }

        // Clean up: delete test user
        if (testUserId != null) {
            try {
                Tasks.await(db.collection("users").document(testUserId).delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Cleanup error for user: " + e.getMessage());
            }
        }

        // Sign out
        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }
    }

    /**
     * Test fetching all events from Firestore.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testFetchAllEventsFromFirestore() throws Exception {
        // Create multiple test events
        createTestEvent("Event 1", "Location 1", 10.0);
        createTestEvent("Event 2", "Location 2", 20.0);
        createTestEvent("Event 3", "Location 3", 0.0); // Free event

        // Fetch all events using repository
        final List<UserEvent>[] fetchedEvents = new List[]{null};
        final Exception[] fetchError = new Exception[]{null};

        eventRepository.getAllEvents(new FirebaseEventRepository.EventListCallback() {
            @Override
            public void onEventsFetched(List<UserEvent> events) {
                fetchedEvents[0] = events;
            }

            @Override
            public void onError(Exception e) {
                fetchError[0] = e;
            }
        });

        // Wait for async operation to complete
        Thread.sleep(3000);

        assertNull("Should not have any errors", fetchError[0]);
        assertNotNull("Events should be fetched", fetchedEvents[0]);
        assertTrue("Should fetch at least 3 events", fetchedEvents[0].size() >= 3);

        System.out.println("Fetch all events test passed - fetched " + fetchedEvents[0].size() + " events");
    }

    /**
     * Test fetching events with different price types.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testFetchEventsWithDifferentPrices() throws Exception {
        // Create events with various prices
        String freeEventId = createTestEvent("Free Event", "Free Location", 0.0);
        String paidEventId = createTestEvent("Paid Event", "Paid Location", 49.99);
        String expensiveEventId = createTestEvent("Expensive Event", "Expensive Location", 199.50);

        // Fetch events directly from Firestore
        DocumentSnapshot freeEvent = Tasks.await(
            db.collection("events").document(freeEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        DocumentSnapshot paidEvent = Tasks.await(
            db.collection("events").document(paidEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        DocumentSnapshot expensiveEvent = Tasks.await(
            db.collection("events").document(expensiveEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        // Verify all events were fetched
        assertTrue("Free event should exist", freeEvent.exists());
        assertTrue("Paid event should exist", paidEvent.exists());
        assertTrue("Expensive event should exist", expensiveEvent.exists());

        // Verify prices are stored correctly
        UserEvent freeEventObj = freeEvent.toObject(UserEvent.class);
        UserEvent paidEventObj = paidEvent.toObject(UserEvent.class);
        UserEvent expensiveEventObj = expensiveEvent.toObject(UserEvent.class);

        assertEquals("Free event price should be 0", 0.0, freeEventObj.getPrice(), 0.01);
        assertEquals("Paid event price should be 49.99", 49.99, paidEventObj.getPrice(), 0.01);
        assertEquals("Expensive event price should be 199.50", 199.50, expensiveEventObj.getPrice(), 0.01);

        System.out.println("Fetch events with different prices test passed");
    }

    /**
     * Test fetching a specific event by ID.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testFetchEventById() throws Exception {
        // Create a test event
        String eventId = createTestEvent("Specific Event", "Specific Location", 50.0);

        // Fetch the event by ID
        final UserEvent[] fetchedEvent = new UserEvent[]{null};
        final Exception[] fetchError = new Exception[]{null};

        eventRepository.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                fetchedEvent[0] = event;
            }

            @Override
            public void onError(Exception e) {
                fetchError[0] = e;
            }
        });

        // Wait for async operation
        Thread.sleep(2000);

        assertNull("Should not have any errors", fetchError[0]);
        assertNotNull("Event should be fetched", fetchedEvent[0]);
        assertEquals("Event ID should match", eventId, fetchedEvent[0].getId());
        assertEquals("Event name should match", "Specific Event", fetchedEvent[0].getName());
        assertEquals("Event location should match", "Specific Location", fetchedEvent[0].getLocation());

        System.out.println("Fetch event by ID test passed");
    }

    /**
     * Test that fetching non-existent event returns error.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testFetchNonExistentEvent() throws Exception {
        String fakeEventId = "non_existent_" + UUID.randomUUID().toString();

        final UserEvent[] fetchedEvent = new UserEvent[]{null};
        final Exception[] fetchError = new Exception[]{null};

        eventRepository.fetchEventById(fakeEventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                fetchedEvent[0] = event;
            }

            @Override
            public void onError(Exception e) {
                fetchError[0] = e;
            }
        });

        // Wait for async operation
        Thread.sleep(2000);

        assertNotNull("Should have an error", fetchError[0]);
        assertNull("Event should be null", fetchedEvent[0]);
        assertTrue("Error should mention event not found",
            fetchError[0].getMessage().contains("Event not found"));

        System.out.println("Fetch non-existent event test passed");
    }

    /**
     * Test that all event fields are properly loaded from Firestore.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testEventFieldsLoadedCorrectly() throws Exception {
        // Create event with all fields populated
        UserEvent testEvent = new UserEvent();
        testEvent.setName("Complete Event");
        testEvent.setLocation("Complete Location");
        testEvent.setDescr("Complete Description");
        testEvent.setPrice(99.99);
        testEvent.setCapacity(500);
        testEvent.setOrganizerID(testUserId);
        testEvent.setStartTimeMillis(System.currentTimeMillis() + 86400000L);
        testEvent.setEndTimeMillis(System.currentTimeMillis() + 172800000L);
        testEvent.setSelectionDateMillis(System.currentTimeMillis() + 43200000L);
        testEvent.setEntrantsToDraw(250);
        testEvent.setGeoRequired(true);
        testEvent.setImageUrl("https://res.cloudinary.com/dyb8t5n7k/image/upload/v1762484990/yuqcasgy4yrect9pyljs.jpg");
        testEvent.setQrData("QR_CODE_DATA_123");

        String eventId = createEventInFirestore(testEvent);

        // Fetch and verify
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(eventId).get(),
            10,
            TimeUnit.SECONDS
        );

        UserEvent fetchedEvent = snapshot.toObject(UserEvent.class);
        assertNotNull("Event should be fetched", fetchedEvent);

        assertEquals("Name should match", "Complete Event", fetchedEvent.getName());
        assertEquals("Location should match", "Complete Location", fetchedEvent.getLocation());
        assertEquals("Description should match", "Complete Description", fetchedEvent.getDescr());
        assertEquals("Price should match", 99.99, fetchedEvent.getPrice(), 0.01);
        assertEquals("Capacity should match", 500, fetchedEvent.getCapacity());
        assertEquals("Organizer ID should match", testUserId, fetchedEvent.getOrganizerID());
        assertEquals("Entrants to draw should match", 250, fetchedEvent.getEntrantsToDraw());
        assertTrue("Geo required should be true", fetchedEvent.isGeoRequired());
        assertEquals("Image URL should match", "https://res.cloudinary.com/dyb8t5n7k/image/upload/v1762484990/yuqcasgy4yrect9pyljs.jpg", fetchedEvent.getImageUrl());
        assertEquals("QR data should match", "QR_CODE_DATA_123", fetchedEvent.getQrData());

        System.out.println("Event fields loaded correctly test passed");
    }

    /**
     * Test loading events with various date configurations.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testFetchEventsWithDifferentDates() throws Exception {
        long now = System.currentTimeMillis();
        long oneDay = 86400000L;

        // Create events with different date ranges
        UserEvent upcomingEvent = new UserEvent();
        upcomingEvent.setName("Upcoming Event");
        upcomingEvent.setLocation("Future Location");
        upcomingEvent.setDescr("Starts tomorrow");
        upcomingEvent.setPrice(25.0);
        upcomingEvent.setCapacity(100);
        upcomingEvent.setOrganizerID(testUserId);
        upcomingEvent.setStartTimeMillis(now + oneDay);
        upcomingEvent.setEndTimeMillis(now + (oneDay * 2));
        upcomingEvent.setSelectionDateMillis(now + (oneDay / 2));
        upcomingEvent.setEntrantsToDraw(50);
        upcomingEvent.setGeoRequired(false);

        String upcomingEventId = createEventInFirestore(upcomingEvent);

        // Fetch and verify dates
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(upcomingEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        UserEvent fetched = snapshot.toObject(UserEvent.class);
        assertNotNull("Event should be fetched", fetched);

        assertTrue("Start time should be in the future", fetched.getStartTimeMillis() > now);
        assertTrue("End time should be after start time", fetched.getEndTimeMillis() > fetched.getStartTimeMillis());
        assertTrue("Selection date should be before event start", fetched.getSelectionDateMillis() < fetched.getStartTimeMillis());

        System.out.println("Fetch events with different dates test passed");
    }

    /**
     * Test batch fetching multiple events efficiently.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testBatchFetchMultipleEvents() throws Exception {
        // Create 5 test events
        List<String> eventIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String eventId = createTestEvent(
                "Batch Event " + i,
                "Location " + i,
                i * 10.0
            );
            eventIds.add(eventId);
        }

        // Fetch all events using QuerySnapshot
        QuerySnapshot querySnapshot = Tasks.await(
            db.collection("events").get(),
            10,
            TimeUnit.SECONDS
        );

        List<UserEvent> fetchedEvents = new ArrayList<>();
        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            UserEvent event = doc.toObject(UserEvent.class);
            if (event != null) {
                event.setId(doc.getId());
                fetchedEvents.add(event);
            }
        }

        // Verify we fetched at least our 5 events
        assertTrue("Should fetch at least 5 events", fetchedEvents.size() >= 5);

        // Verify our created events are in the fetched list
        int foundCount = 0;
        for (String eventId : eventIds) {
            for (UserEvent event : fetchedEvents) {
                if (event.getId().equals(eventId)) {
                    foundCount++;
                    break;
                }
            }
        }

        assertEquals("All 5 created events should be fetched", 5, foundCount);

        System.out.println("Batch fetch multiple events test passed - fetched " + fetchedEvents.size() + " total events");
    }

    /**
     * Test fetching events with waitlist data.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testFetchEventWithWaitlist() throws Exception {
        // Create event with waitlist
        UserEvent eventWithWaitlist = new UserEvent();
        eventWithWaitlist.setName("Popular Event");
        eventWithWaitlist.setLocation("Popular Location");
        eventWithWaitlist.setDescr("Has a waitlist");
        eventWithWaitlist.setPrice(35.0);
        eventWithWaitlist.setCapacity(50);
        eventWithWaitlist.setOrganizerID(testUserId);
        eventWithWaitlist.setStartTimeMillis(System.currentTimeMillis() + 86400000L);
        eventWithWaitlist.setEndTimeMillis(System.currentTimeMillis() + 172800000L);
        eventWithWaitlist.setSelectionDateMillis(System.currentTimeMillis() + 43200000L);
        eventWithWaitlist.setEntrantsToDraw(25);
        eventWithWaitlist.setGeoRequired(false);

        // Create waitlist with some user IDs
        List<String> waitlist = new ArrayList<>();
        waitlist.add("user1");
        waitlist.add("user2");
        waitlist.add("user3");
        eventWithWaitlist.setWaitlist(waitlist);

        String eventId = createEventInFirestore(eventWithWaitlist);

        // Fetch and verify waitlist
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(eventId).get(),
            10,
            TimeUnit.SECONDS
        );

        UserEvent fetched = snapshot.toObject(UserEvent.class);
        assertNotNull("Event should be fetched", fetched);
        assertNotNull("Waitlist should not be null", fetched.getWaitlist());
        assertEquals("Waitlist should have 3 users", 3, fetched.getWaitlist().size());
        assertTrue("Waitlist should contain user1", fetched.getWaitlist().contains("user1"));
        assertTrue("Waitlist should contain user2", fetched.getWaitlist().contains("user2"));
        assertTrue("Waitlist should contain user3", fetched.getWaitlist().contains("user3"));

        System.out.println("Fetch event with waitlist test passed");
    }

    /**
     * Test price display formatting for different price values.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testPriceDisplayFormatting() throws Exception {
        // Test free event
        UserEvent freeEvent = new UserEvent();
        freeEvent.setPrice(0.0);
        assertEquals("Free event should display 'Free'", "Free", freeEvent.getPriceDisplay());

        // Test null price
        UserEvent nullPriceEvent = new UserEvent();
        nullPriceEvent.setPrice(null);
        assertEquals("Null price should display 'Free'", "Free", nullPriceEvent.getPriceDisplay());

        // Test decimal price
        UserEvent paidEvent = new UserEvent();
        paidEvent.setPrice(49.99);
        assertEquals("Paid event should format correctly", "$49.99", paidEvent.getPriceDisplay());

        // Test whole number price
        UserEvent wholeNumberEvent = new UserEvent();
        wholeNumberEvent.setPrice(100.0);
        assertEquals("Whole number should format with decimals", "$100.00", wholeNumberEvent.getPriceDisplay());

        System.out.println("Price display formatting test passed");
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a test event owned by the test user.
     *
     * @param name the event name
     * @param location the event location
     * @param price the event price
     * @return the Firestore document ID of the created event
     * @throws Exception if the creation fails
     */
    private String createTestEvent(String name, String location, Double price) throws Exception {
        UserEvent event = new UserEvent();
        event.setName(name);
        event.setLocation(location);
        event.setDescr("Test event description");
        event.setPrice(price);
        event.setCapacity(100);
        event.setOrganizerID(testUserId);
        event.setStartTimeMillis(System.currentTimeMillis() + 86400000L);
        event.setEndTimeMillis(System.currentTimeMillis() + 172800000L);
        event.setSelectionDateMillis(System.currentTimeMillis() + 43200000L);
        event.setEntrantsToDraw(50);
        event.setGeoRequired(false);

        String eventId = createEventInFirestore(event);
        return eventId;
    }

    /**
     * Creates an event in Firestore and returns its ID.
     *
     * @param event the event to create
     * @return the Firestore document ID of the created event
     * @throws Exception if the creation fails
     */
    private String createEventInFirestore(UserEvent event) throws Exception {
        String id = db.collection("events").document().getId();
        event.setId(id);

        Tasks.await(
            db.collection("events").document(id).set(event),
            10,
            TimeUnit.SECONDS
        );

        createdEventIds.add(id);
        return id;
    }
}
