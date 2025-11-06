package com.example.myapplication;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Instrumented test for event-related functionality (waitlist operations).
 *
 * For authentication tests, see AuthenticationTest.java
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.myapplication", appContext.getPackageName());
    }


    @Test
    public void testJoinWaitlistDirect() throws Exception {
        String eventId = "Uu8qd4j2Xdo1TyeOFmsy";
        String userId = "testUser123";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseEventRepository repo = new FirebaseEventRepository();

        DocumentSnapshot beforeSnap = Tasks.await(
                db.collection("events").document(eventId).get()
        );

        List<String> beforeMethod = (List<String>) beforeSnap.get("waitlist");
        int beforeSize = beforeMethod == null ? 0 : beforeMethod.size();

        final TaskCompletionSource<Void> waiter = new TaskCompletionSource<>();

        repo.joinWaitlist(
                eventId,
                userId,
                v -> waiter.setResult(null),
                e -> waiter.setException(e)
        );

        Tasks.await(waiter.getTask());

        DocumentSnapshot afterSnap = Tasks.await(
                db.collection("events").document(eventId).get()
        );

        List<String> afterMethod = (List<String>) afterSnap.get("waitlist");

        assertNotNull(afterMethod);
        assertTrue(afterMethod.contains(userId));
        assertEquals(beforeSize + (beforeMethod != null && beforeMethod.contains(userId) ? 0 : 1), afterMethod.size());
    }

    @Test
    public void testJoinWaitlistDuplicate() throws Exception {
        String eventId = "Uu8qd4j2Xdo1TyeOFmsy";
        String userId = "testUser123";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseEventRepository repo = new FirebaseEventRepository();

        TaskCompletionSource<Void> waiter2 = new TaskCompletionSource<>();
        repo.joinWaitlist(
                eventId,
                userId,
                v -> waiter2.setResult(null),
                waiter2::setException
        );
        Tasks.await(waiter2.getTask());

        DocumentSnapshot snap = Tasks.await(
                db.collection("events").document(eventId).get()
        );

        List<String> list = (List<String>) snap.get("waitlist");
        assertNotNull(list);

        long occurrences = list.stream().filter(id -> id.equals(userId)).count();

        assertEquals(1, occurrences);
    }

    @Test
    public void testLeaveWaitlist() throws Exception {
        String eventId = "Uu8qd4j2Xdo1TyeOFmsy";
        String userId = "testUser123";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseEventRepository repo = new FirebaseEventRepository();

        DocumentSnapshot beforeSnap = Tasks.await(
                db.collection("events").document(eventId).get()
        );
        List<String> beforeList = (List<String>) beforeSnap.get("waitlist");
        int beforeSize = beforeList == null ? 0 : beforeList.size();
        boolean userWasPresent = beforeList != null && beforeList.contains(userId);

        TaskCompletionSource<Void> waiter = new TaskCompletionSource<>();
        repo.leaveWaitlist(
                eventId,
                userId,
                v -> waiter.setResult(null),
                waiter::setException
        );

        Tasks.await(waiter.getTask());

        DocumentSnapshot afterSnap = Tasks.await(
                db.collection("events").document(eventId).get()
        );
        List<String> afterList = (List<String>) afterSnap.get("waitlist");

        assertNotNull(afterList);
        assertFalse(afterList.contains(userId));

        int expectedSize = beforeSize - (userWasPresent ? 1 : 0);
        assertEquals(expectedSize, afterList.size());
    }
}