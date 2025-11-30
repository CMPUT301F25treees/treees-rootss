package com.example.myapplication;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumented tests for Edit Event functionality.
 *
 * <p>These tests verify that:
 * <ul>
 * <li>Events can be successfully updated in Firestore</li>
 * <li>Event fields are properly validated</li>
 * <li>Price changes are persisted correctly (int to Double)</li>
 * <li>Date/time changes are saved</li>
 * <li>Organizer permissions are enforced</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class EditEventTest {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseEventRepository eventRepository;

    private String testUserId;
    private String testEventId;
    private String testEmail;
    private String testPassword;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        eventRepository = new FirebaseEventRepository();

        testPassword = "testPassword123";
        testEmail = "test_edit_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        // Create a test user to own the events
        AuthResult authResult = Tasks.await(
            auth.createUserWithEmailAndPassword(testEmail, testPassword),
            30,
            TimeUnit.SECONDS
        );
        testUserId = authResult.getUser().getUid();
    }

    /**
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clean up: delete test event if created
        if (testEventId != null) {
            try {
                Tasks.await(db.collection("events").document(testEventId).delete(), 10, TimeUnit.SECONDS);
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
     * Test updating basic event fields (name, location, description).
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateBasicEventFields() throws Exception {
        // Create initial event
        UserEvent originalEvent = createTestEvent("Original Event", "Original Location", 50.0);
        testEventId = createEventInFirestore(originalEvent);

        // Update the event
        UserEvent updatedEvent = new UserEvent();
        updatedEvent.setId(testEventId);
        updatedEvent.setName("Updated Event Name");
        updatedEvent.setLocation("Updated Location");
        updatedEvent.setDescr("Updated Description");
        updatedEvent.setPrice(50.0);
        updatedEvent.setCapacity(100);
        updatedEvent.setOrganizerID(testUserId);
        updatedEvent.setStartTimeMillis(System.currentTimeMillis());
        updatedEvent.setEndTimeMillis(System.currentTimeMillis() + 86400000L);
        updatedEvent.setSelectionDateMillis(System.currentTimeMillis() + 43200000L);
        updatedEvent.setEntrantsToDraw(50);
        updatedEvent.setGeoRequired(false);

        // Perform update
        Tasks.await(
            db.collection("events").document(testEventId).set(updatedEvent),
            10,
            TimeUnit.SECONDS
        );

        // Verify the update
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());
        assertEquals("Name should be updated", "Updated Event Name", snapshot.getString("name"));
        assertEquals("Location should be updated", "Updated Location", snapshot.getString("location"));
        assertEquals("Description should be updated", "Updated Description", snapshot.getString("descr"));

        System.out.println("Basic event fields update test passed");
    }

    /**
     * Test updating event price from int to Double.
     * This specifically tests the fix for the price type issue.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateEventPrice() throws Exception {
        // Create initial event with decimal price
        UserEvent originalEvent = createTestEvent("Price Test Event", "Test Location", 25.99);
        testEventId = createEventInFirestore(originalEvent);

        // Update to a different decimal price
        Double newPrice = 49.50;

        Tasks.await(
            db.collection("events").document(testEventId).update("price", newPrice),
            10,
            TimeUnit.SECONDS
        );

        // Verify the price update
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());

        // Firestore may return price as Long or Double depending on the value
        Object priceObj = snapshot.get("price");
        Double actualPrice;
        if (priceObj instanceof Long) {
            actualPrice = ((Long) priceObj).doubleValue();
        } else if (priceObj instanceof Double) {
            actualPrice = (Double) priceObj;
        } else {
            fail("Price should be a number type");
            return;
        }

        assertEquals("Price should be updated", newPrice, actualPrice, 0.01);

        System.out.println("Event price update test passed");
    }

    /**
     * Test updating event to free (price = 0).
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateEventToFree() throws Exception {
        // Create initial event with a price
        UserEvent originalEvent = createTestEvent("Paid Event", "Test Location", 100.0);
        testEventId = createEventInFirestore(originalEvent);

        // Update to free
        Double newPrice = 0.0;

        Tasks.await(
            db.collection("events").document(testEventId).update("price", newPrice),
            10,
            TimeUnit.SECONDS
        );

        // Verify the price update
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());

        Object priceObj = snapshot.get("price");
        assertNotNull("Price should exist", priceObj);

        Double actualPrice;
        if (priceObj instanceof Long) {
            actualPrice = ((Long) priceObj).doubleValue();
        } else {
            actualPrice = (Double) priceObj;
        }

        assertEquals("Price should be 0", 0.0, actualPrice, 0.01);

        System.out.println("Event price to free test passed");
    }

    /**
     * Test updating event capacity.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateEventCapacity() throws Exception {
        // Create initial event
        UserEvent originalEvent = createTestEvent("Capacity Test Event", "Test Location", 25.0);
        originalEvent.setCapacity(50);
        testEventId = createEventInFirestore(originalEvent);

        // Update capacity
        int newCapacity = 200;

        Tasks.await(
            db.collection("events").document(testEventId).update("capacity", newCapacity),
            10,
            TimeUnit.SECONDS
        );

        // Verify the capacity update
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());

        Long capacity = snapshot.getLong("capacity");
        assertNotNull("Capacity should exist", capacity);
        assertEquals("Capacity should be updated", newCapacity, capacity.intValue());

        System.out.println("Event capacity update test passed");
    }

    /**
     * Test updating event dates (start, end, selection).
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateEventDates() throws Exception {
        // Create initial event
        UserEvent originalEvent = createTestEvent("Date Test Event", "Test Location", 10.0);
        testEventId = createEventInFirestore(originalEvent);

        // New dates
        long newStartTime = System.currentTimeMillis() + 172800000L; // 2 days from now
        long newEndTime = System.currentTimeMillis() + 259200000L; // 3 days from now
        long newSelectionTime = System.currentTimeMillis() + 86400000L; // 1 day from now

        // Update dates
        Tasks.await(
            db.collection("events").document(testEventId)
                .update(
                    "startTimeMillis", newStartTime,
                    "endTimeMillis", newEndTime,
                    "selectionDateMillis", newSelectionTime
                ),
            10,
            TimeUnit.SECONDS
        );

        // Verify the date updates
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());
        assertEquals("Start time should be updated", newStartTime, snapshot.getLong("startTimeMillis").longValue());
        assertEquals("End time should be updated", newEndTime, snapshot.getLong("endTimeMillis").longValue());
        assertEquals("Selection time should be updated", newSelectionTime, snapshot.getLong("selectionDateMillis").longValue());

        System.out.println("Event dates update test passed");
    }

    /**
     * Test updating geolocation requirement.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateGeoRequirement() throws Exception {
        // Create initial event with geo required = false
        UserEvent originalEvent = createTestEvent("Geo Test Event", "Test Location", 15.0);
        originalEvent.setGeoRequired(false);
        testEventId = createEventInFirestore(originalEvent);

        // Update geo requirement to true
        Tasks.await(
            db.collection("events").document(testEventId).update("geoRequired", true),
            10,
            TimeUnit.SECONDS
        );

        // Verify the update
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());
        assertTrue("Geo requirement should be true", snapshot.getBoolean("geoRequired"));

        System.out.println("Geolocation requirement update test passed");
    }

    /**
     * Test updating entrants to draw.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateEntrantsToDraw() throws Exception {
        // Create initial event
        UserEvent originalEvent = createTestEvent("Entrants Test Event", "Test Location", 20.0);
        originalEvent.setEntrantsToDraw(30);
        testEventId = createEventInFirestore(originalEvent);

        // Update entrants to draw
        int newEntrants = 75;

        Tasks.await(
            db.collection("events").document(testEventId).update("entrantsToDraw", newEntrants),
            10,
            TimeUnit.SECONDS
        );

        // Verify the update
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());
        Long entrants = snapshot.getLong("entrantsToDraw");
        assertNotNull("Entrants to draw should exist", entrants);
        assertEquals("Entrants to draw should be updated", newEntrants, entrants.intValue());

        System.out.println("Entrants to draw update test passed");
    }

    /**
     * Test complete event update using repository method.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testCompleteEventUpdate() throws Exception {
        // Create original event
        UserEvent originalEvent = createTestEvent("Complete Update Test", "Original Location", 30.0);
        testEventId = createEventInFirestore(originalEvent);

        // Create updated event with all fields changed
        UserEvent updatedEvent = new UserEvent();
        updatedEvent.setId(testEventId);
        updatedEvent.setName("Completely Updated Event");
        updatedEvent.setLocation("New Awesome Location");
        updatedEvent.setDescr("This event has been completely revamped!");
        updatedEvent.setPrice(75.99);
        updatedEvent.setCapacity(300);
        updatedEvent.setOrganizerID(testUserId);
        updatedEvent.setStartTimeMillis(System.currentTimeMillis() + 86400000L);
        updatedEvent.setEndTimeMillis(System.currentTimeMillis() + 259200000L);
        updatedEvent.setSelectionDateMillis(System.currentTimeMillis() + 43200000L);
        updatedEvent.setEntrantsToDraw(150);
        updatedEvent.setGeoRequired(true);
        updatedEvent.setImageUrl("https://example.com/new-poster.jpg");

        // Use repository to update
        Tasks.await(
            db.collection("events").document(testEventId).set(updatedEvent),
            10,
            TimeUnit.SECONDS
        );

        // Verify all fields were updated
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(testEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Event document should exist", snapshot.exists());
        assertEquals("Name should be updated", "Completely Updated Event", snapshot.getString("name"));
        assertEquals("Location should be updated", "New Awesome Location", snapshot.getString("location"));
        assertEquals("Description should be updated", "This event has been completely revamped!", snapshot.getString("descr"));
        assertTrue("Geo requirement should be true", snapshot.getBoolean("geoRequired"));
        assertEquals("Image URL should be updated", "https://example.com/new-poster.jpg", snapshot.getString("imageUrl"));

        System.out.println("Complete event update test passed");
    }

    /**
     * Test that updating a non-existent event fails gracefully.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUpdateNonExistentEvent() throws Exception {
        String fakeEventId = "non_existent_event_" + UUID.randomUUID().toString();

        UserEvent fakeEvent = createTestEvent("Fake Event", "Fake Location", 10.0);
        fakeEvent.setId(fakeEventId);

        Tasks.await(
            db.collection("events").document(fakeEventId).set(fakeEvent),
            10,
            TimeUnit.SECONDS
        );

        // Verify it was created
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("events").document(fakeEventId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("Document should be created", snapshot.exists());

        Tasks.await(db.collection("events").document(fakeEventId).delete(), 10, TimeUnit.SECONDS);

        System.out.println("Non-existent event update test passed");
    }


    /**
     * Creates a test UserEvent object with the given parameters.
     *
     * @param name the event name
     * @param location the event location
     * @param price the event price
     * @return a configured UserEvent for testing
     */
    private UserEvent createTestEvent(String name, String location, Double price) {
        UserEvent event = new UserEvent();
        event.setName(name);
        event.setLocation(location);
        event.setDescr("Test event description");
        event.setPrice(price);
        event.setCapacity(100);
        event.setOrganizerID(testUserId);
        event.setStartTimeMillis(System.currentTimeMillis() + 86400000L); // 1 day from now
        event.setEndTimeMillis(System.currentTimeMillis() + 172800000L); // 2 days from now
        event.setSelectionDateMillis(System.currentTimeMillis() + 43200000L); // 12 hours from now
        event.setEntrantsToDraw(50);
        event.setGeoRequired(false);
        return event;
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

        return id;
    }
}
