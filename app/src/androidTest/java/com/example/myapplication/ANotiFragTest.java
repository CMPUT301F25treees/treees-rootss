package com.example.myapplication;

import static org.junit.Assert.*;

import android.view.View;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.myapplication.features.admin.ANotiFrag;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for ANotiFrag:
 * - Verifies that the user switch is hidden in admin view.
 * - Verifies that notifications with the "deleted by the system" message
 *   are excluded from the list.
 */
@RunWith(AndroidJUnit4.class)
public class ANotiFragTest {

    private FirebaseFirestore db;
    private final List<DocumentReference> createdNotificationDocs = new ArrayList<>();

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up notifications created for the test
        for (DocumentReference ref : createdNotificationDocs) {
            try {
                Tasks.await(ref.delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Error cleaning notification doc: " + e.getMessage());
            }
        }
    }

    /**
     * Helper: create a notification document in "notifications".
     */
    private DocumentReference createNotification(String message, long dateMillis) throws Exception {
        String eventId = "event_" + UUID.randomUUID().toString().substring(0, 6);

        HashMap<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("dateMade", new Date(dateMillis));
        data.put("event", "Test Event");
        data.put("eventId", eventId);
        data.put("type", "custom");
        data.put("uID", Arrays.asList("testUser123"));

        DocumentReference ref = db.collection("notifications").document();
        Tasks.await(ref.set(data), 15, TimeUnit.SECONDS);
        createdNotificationDocs.add(ref);
        return ref;
    }

    /**
     * Verifies:
     * - The admin fragment hides the switch_notifications control.
     * - Only non-deleted notifications are shown in the RecyclerView,
     *   respecting the query:
     *     whereNotEqualTo("message", "This notification was deleted by the system.")
     */
    @Test
    public void testAdminNotificationsHideDeletedAndSwitch() throws Exception {
        long now = System.currentTimeMillis();

        // 1) Normal notification that should be shown
        createNotification("Regular admin notification", now - 1000L);

        // 2) "Deleted by system" notification that should be filtered out by the query
        createNotification("This notification was deleted by the system.", now - 2000L);

        // Launch the admin notifications fragment
        FragmentScenario<ANotiFrag> scenario =
                FragmentScenario.launchInContainer(
                        ANotiFrag.class,
                        null,
                        R.style.Theme_MyApplication // adjust if your app theme has a different name
                );

        // Give Firestore + adapter some time to load
        Thread.sleep(4000);

        scenario.onFragment(fragment -> {
            View root = fragment.getView();
            assertNotNull("Fragment view should not be null", root);

            // Check that the switch is hidden for admin
            SwitchCompat switchNotifications = root.findViewById(R.id.switch_notifications);
            assertNotNull("switch_notifications should exist in the layout", switchNotifications);
            assertEquals(
                    "switch_notifications should be GONE in admin fragment",
                    View.GONE,
                    switchNotifications.getVisibility()
            );

            // Check RecyclerView and that only the non-deleted notification is shown
            RecyclerView recyclerView = root.findViewById(R.id.notifications_list);
            assertNotNull("RecyclerView should be present", recyclerView);
            assertNotNull("RecyclerView should have an adapter", recyclerView.getAdapter());

            int itemCount = recyclerView.getAdapter().getItemCount();
            assertEquals(
                    "Admin notifications should only show non-deleted notifications",
                    1,
                    itemCount
            );
        });
    }
}
