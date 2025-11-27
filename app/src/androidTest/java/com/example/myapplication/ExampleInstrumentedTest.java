package com.example.myapplication;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumented test for event-related functionality (waitlist operations).
 *
 * For authentication tests, see AuthenticationTest.java
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.myapplication", appContext.getPackageName());
    }

    /**
     * Tests joining the waitlist of an event.
     */
    @Test
    public void testJoinWaitlistDirect() throws Exception {
        String eventId = "Uu8qd4j2Xdo1TyeOFmsy"; // must exist in Firestore
        String userId = "testUser123";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseEventRepository repo = new FirebaseEventRepository();

        DocumentSnapshot beforeSnap = Tasks.await(
                db.collection("events").document(eventId).get(),
                15,
                TimeUnit.SECONDS
        );

        List<String> beforeList = (List<String>) beforeSnap.get("waitlist");
        int beforeSize = beforeList == null ? 0 : beforeList.size();
        boolean userAlreadyInList = beforeList != null && beforeList.contains(userId);

        final TaskCompletionSource<Void> waiter = new TaskCompletionSource<>();

        // joinWaitlist(eventId, userId, latitude, longitude, onSuccess, onFailure)
        repo.joinWaitlist(
                eventId,
                userId,
                0.0,        // dummy latitude for test
                0.0,        // dummy longitude for test
                v -> waiter.setResult(null),
                waiter::setException
        );

        Tasks.await(waiter.getTask(), 15, TimeUnit.SECONDS);

        DocumentSnapshot afterSnap = Tasks.await(
                db.collection("events").document(eventId).get(),
                15,
                TimeUnit.SECONDS
        );

        List<String> afterList = (List<String>) afterSnap.get("waitlist");
        assertNotNull("waitlist field should not be null after joinWaitlist", afterList);
        assertTrue("waitlist should contain the userId after joinWaitlist", afterList.contains(userId));

        int expectedSize = beforeSize + (userAlreadyInList ? 0 : 1);
        assertEquals(
                "waitlist size should increment by 1 only if user was not already present",
                expectedSize,
                afterList.size()
        );
    }

    /**
     * Tests that joining the waitlist multiple times does not create duplicates.
     */
    @Test
    public void testJoinWaitlistDuplicate() throws Exception {
        String eventId = "Uu8qd4j2Xdo1TyeOFmsy"; // must exist in Firestore
        String userId = "testUser123";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseEventRepository repo = new FirebaseEventRepository();

        TaskCompletionSource<Void> waiter1 = new TaskCompletionSource<>();
        repo.joinWaitlist(
                eventId,
                userId,
                0.0,
                0.0,
                v -> waiter1.setResult(null),
                waiter1::setException
        );
        Tasks.await(waiter1.getTask(), 15, TimeUnit.SECONDS);

        TaskCompletionSource<Void> waiter2 = new TaskCompletionSource<>();
        repo.joinWaitlist(
                eventId,
                userId,
                0.0,
                0.0,
                v -> waiter2.setResult(null),
                waiter2::setException
        );
        Tasks.await(waiter2.getTask(), 15, TimeUnit.SECONDS);

        DocumentSnapshot snap = Tasks.await(
                db.collection("events").document(eventId).get(),
                15,
                TimeUnit.SECONDS
        );

        List<String> list = (List<String>) snap.get("waitlist");
        assertNotNull("waitlist should not be null", list);

        // Count occurrences without streams (for max compatibility)
        int occurrences = 0;
        for (String id : list) {
            if (userId.equals(id)) {
                occurrences++;
            }
        }

        assertEquals("userId should appear exactly once in waitlist", 1, occurrences);
    }

    /**
     * Tests leaving the waitlist of an event.
     */
    @Test
    public void testLeaveWaitlist() throws Exception {
        String eventId = "Uu8qd4j2Xdo1TyeOFmsy"; // must exist in Firestore
        String userId = "testUser123";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseEventRepository repo = new FirebaseEventRepository();

        // Ensure user is on the waitlist first
        TaskCompletionSource<Void> joinWaiter = new TaskCompletionSource<>();
        repo.joinWaitlist(
                eventId,
                userId,
                0.0,
                0.0,
                v -> joinWaiter.setResult(null),
                joinWaiter::setException
        );
        Tasks.await(joinWaiter.getTask(), 15, TimeUnit.SECONDS);

        DocumentSnapshot beforeSnap = Tasks.await(
                db.collection("events").document(eventId).get(),
                15,
                TimeUnit.SECONDS
        );

        List<String> beforeList = (List<String>) beforeSnap.get("waitlist");
        int beforeSize = beforeList == null ? 0 : beforeList.size();
        boolean userWasPresent = beforeList != null && beforeList.contains(userId);

        TaskCompletionSource<Void> leaveWaiter = new TaskCompletionSource<>();
        repo.leaveWaitlist(
                eventId,
                userId,
                v -> leaveWaiter.setResult(null),
                leaveWaiter::setException
        );

        Tasks.await(leaveWaiter.getTask(), 15, TimeUnit.SECONDS);

        DocumentSnapshot afterSnap = Tasks.await(
                db.collection("events").document(eventId).get(),
                15,
                TimeUnit.SECONDS
        );
        List<String> afterList = (List<String>) afterSnap.get("waitlist");

        if (afterList == null) {
            // If the field was removed, the user is definitely not present
            assertFalse("waitlist should no longer contain userId", userWasPresent);
        } else {
            assertFalse("waitlist should not contain userId after leaveWaitlist", afterList.contains(userId));
            int expectedSize = beforeSize - (userWasPresent ? 1 : 0);
            assertEquals(
                    "waitlist size should decrease by 1 only if user was present",
                    expectedSize,
                    afterList.size()
            );
        }
    }
}
