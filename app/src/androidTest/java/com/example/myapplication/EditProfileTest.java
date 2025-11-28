package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for the edit profile flow.
 *
 * These tests mirror the logic in UEditProfileFrag by validating:
 * - Profile fields merge correctly
 */
@RunWith(AndroidJUnit4.class)
public class EditProfileTest {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private List<String> createdUserIds;
    private List<FirebaseUser> createdAuthUsers;
    private final String testPassword = "TestProfile123!";
    private static final long AUTH_TIMEOUT_SEC = 60;
    private static final long DB_TIMEOUT_SEC = 60;

    @Before
    public void setUp() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        createdUserIds = new ArrayList<>();
        createdAuthUsers = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        for (String uid : createdUserIds) {
            try {
                Tasks.await(db.collection("users").document(uid).delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Cleanup error for Firestore user " + uid + ": " + e.getMessage());
            }
        }

        for (FirebaseUser user : createdAuthUsers) {
            try {
                Tasks.await(user.delete(), 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Cleanup error for auth user " + user.getUid() + ": " + e.getMessage());
            }
        }

        createdUserIds.clear();
        createdAuthUsers.clear();

        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }
    }

    /**
     * Verifies that non-email profile updates merge correctly and optional fields are cleared.
     */
    @Test
    public void testUpdateProfileWithoutEmailChange() throws Exception {
        FirebaseUser firebaseUser = createTestUser();
        String uid = firebaseUser.getUid();

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("firstName", "UpdatedName");
        profileUpdates.put("username", "UpdatedName");
        profileUpdates.put("lastName", null);
        profileUpdates.put("cell", null);
        profileUpdates.put("email", firebaseUser.getEmail());

        await(
                db.collection("users").document(uid).set(profileUpdates, SetOptions.merge()),
                DB_TIMEOUT_SEC,
                "Firestore profile merge (no email change)"
        );

        DocumentSnapshot snapshot = await(
                db.collection("users").document(uid).get(),
                DB_TIMEOUT_SEC,
                "Fetch updated profile (no email change)"
        );

        assertTrue("User document should exist", snapshot.exists());
        assertEquals("First name should be updated", "UpdatedName", snapshot.getString("firstName"));
        assertEquals("Username should mirror first name", "UpdatedName", snapshot.getString("username"));
        assertNull("Last name should be cleared when empty", snapshot.getString("lastName"));
        assertNull("Phone should be cleared when empty", snapshot.getString("cell"));
        assertEquals("Email should remain unchanged", firebaseUser.getEmail(), snapshot.getString("email"));
    }

    private FirebaseUser createTestUser() throws Exception {
        String email = "edit_profile_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        AuthResult authResult = await(
                auth.createUserWithEmailAndPassword(email, testPassword),
                AUTH_TIMEOUT_SEC,
                "FirebaseAuth createUserWithEmailAndPassword"
        );

        FirebaseUser firebaseUser = authResult.getUser();
        assertNotNull("Firebase user should not be null", firebaseUser);

        String uid = firebaseUser.getUid();
        createdUserIds.add(uid);
        createdAuthUsers.add(firebaseUser);

        Map<String, Object> baseProfile = new HashMap<>();
        baseProfile.put("firstName", "Original");
        baseProfile.put("username", "Original");
        baseProfile.put("lastName", "User");
        baseProfile.put("email", email);
        baseProfile.put("cell", "1234567890");

        await(
                db.collection("users").document(uid).set(baseProfile),
                DB_TIMEOUT_SEC,
                "Seed base profile"
        );

        return firebaseUser;
    }

    private <T> T await(Task<T> task, long timeoutSec, String operation) throws Exception {
        try {
            return Tasks.await(task, timeoutSec, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError(operation + " timed out. Verify device/emulator has network access or point tests at Firebase emulators.", e);
        }
    }
}
