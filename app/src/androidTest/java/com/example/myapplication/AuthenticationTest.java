package com.example.myapplication;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for Firebase Authentication (Login and Registration).
 * Note: These tests create real users in Firebase, so use a test project.
 */
@RunWith(AndroidJUnit4.class)
public class AuthenticationTest {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String testEmail;
    private String testPassword;
    private String testUserId;

    @Before
    public void setUp() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        testPassword = "testPassword123";

        // Generate unique email for each test run to avoid conflicts
        testEmail = "test_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    @After
    public void tearDown() throws Exception {
        // Clean up: delete test user if created
        if (testUserId != null) {
            try {
                // Delete user document from Firestore
                Tasks.await(db.collection("users").document(testUserId).delete(), 10, TimeUnit.SECONDS);

            } catch (Exception e) {
                // Ignore cleanup errors
                System.err.println("Cleanup error: " + e.getMessage());
            }
        }

        // Sign out
        if (auth.getCurrentUser() != null) {
            auth.signOut();
        }
    }

    /**
     * Test user registration with Firebase Authentication.
     *
     * This test:
     * 1. Creates a new user account with email and password
     * 2. Verifies the user is created in Firebase Auth
     * 3. Creates a user document in Firestore
     * 4. Verifies the document was created with correct data
     */
    @Test
    public void testRegister() throws Exception {
        String firstName = "Test";
        String lastName = "User";
        String phone = "1234567890";

        // Step 1: Create user with Firebase Auth
        AuthResult authResult = Tasks.await(
            auth.createUserWithEmailAndPassword(testEmail, testPassword),
            30,
            TimeUnit.SECONDS
        );

        assertNotNull("Auth result should not be null", authResult);
        FirebaseUser firebaseUser = authResult.getUser();
        assertNotNull("Firebase user should not be null", firebaseUser);

        testUserId = firebaseUser.getUid();
        assertNotNull("User ID should not be null", testUserId);
        assertEquals("Email should match", testEmail, firebaseUser.getEmail());

        // Step 2: Create user document in Firestore
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", testEmail);
        userMap.put("cell", phone);
        userMap.put("role", "User");

        Tasks.await(
            db.collection("users").document(testUserId).set(userMap),
            10,
            TimeUnit.SECONDS
        );

        // Step 3: Verify the document was created
        DocumentSnapshot snapshot = Tasks.await(
            db.collection("users").document(testUserId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("User document should exist", snapshot.exists());
        assertEquals("First name should match", firstName, snapshot.getString("firstName"));
        assertEquals("Last name should match", lastName, snapshot.getString("lastName"));
        assertEquals("Email should match", testEmail, snapshot.getString("email"));
        assertEquals("Phone should match", phone, snapshot.getString("cell"));
        assertEquals("Role should be User", "User", snapshot.getString("role"));

        System.out.println("Registration test passed for user: " + testEmail);
    }

    /**
     * Test user login with Firebase Authentication.
     *
     * This test:
     * 1. Creates a test user first
     * 2. Signs out
     * 3. Signs back in with the credentials
     * 4. Verifies the user data can be retrieved from Firestore
     */
    @Test
    public void testLogin() throws Exception {
        String firstName = "Login";
        String lastName = "Test";

        // Step 1: Create a test user first
        AuthResult registerResult = Tasks.await(
            auth.createUserWithEmailAndPassword(testEmail, testPassword),
            30,
            TimeUnit.SECONDS
        );

        testUserId = registerResult.getUser().getUid();

        // Create user document
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", testEmail);
        userMap.put("role", "User");

        Tasks.await(
            db.collection("users").document(testUserId).set(userMap),
            10,
            TimeUnit.SECONDS
        );

        // Step 2: Sign out
        auth.signOut();
        assertNull("User should be signed out", auth.getCurrentUser());

        // Step 3: Sign in with credentials
        AuthResult loginResult = Tasks.await(
            auth.signInWithEmailAndPassword(testEmail, testPassword),
            30,
            TimeUnit.SECONDS
        );

        assertNotNull("Login result should not be null", loginResult);
        FirebaseUser loggedInUser = loginResult.getUser();
        assertNotNull("Logged in user should not be null", loggedInUser);
        assertEquals("User ID should match", testUserId, loggedInUser.getUid());
        assertEquals("Email should match", testEmail, loggedInUser.getEmail());

        // Step 4: Verify we can retrieve user data from Firestore
        DocumentSnapshot userDoc = Tasks.await(
            db.collection("users").document(testUserId).get(),
            10,
            TimeUnit.SECONDS
        );

        assertTrue("User document should exist", userDoc.exists());
        assertEquals("First name should match", firstName, userDoc.getString("firstName"));
        assertEquals("Role should exist", "User", userDoc.getString("role"));

        System.out.println("Login test passed for user: " + testEmail);
    }

    /**
     * Test login with incorrect password.
     * Should fail with authentication error.
     */
    @Test
    public void testLoginWithWrongPassword() throws Exception {
        // Create a test user
        AuthResult registerResult = Tasks.await(
            auth.createUserWithEmailAndPassword(testEmail, testPassword),
            30,
            TimeUnit.SECONDS
        );
        testUserId = registerResult.getUser().getUid();

        // Sign out
        auth.signOut();

        // Try to sign in with wrong password
        try {
            Tasks.await(
                auth.signInWithEmailAndPassword(testEmail, "wrongPassword"),
                30,
                TimeUnit.SECONDS
            );
            fail("Login should have failed with wrong password");
        } catch (Exception e) {
            // Expected - login should fail
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String message = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            boolean isAuthError = cause instanceof FirebaseAuthException
                    || message.contains("password")
                    || message.contains("credential");

            assertTrue("Should be authentication error", isAuthError);
            System.out.println("Wrong password test passed - login correctly rejected");
        }
    }

    /**
     * Test registration with duplicate email.
     * Should fail with email already in use error.
     */
    @Test
    public void testRegisterDuplicateEmail() throws Exception {
        // Create first user
        AuthResult firstResult = Tasks.await(
            auth.createUserWithEmailAndPassword(testEmail, testPassword),
            30,
            TimeUnit.SECONDS
        );
        testUserId = firstResult.getUser().getUid();

        // Try to create another user with same email
        try {
            Tasks.await(
                auth.createUserWithEmailAndPassword(testEmail, testPassword),
                30,
                TimeUnit.SECONDS
            );
            fail("Registration should have failed with duplicate email");
        } catch (Exception e) {
            // Expected - registration should fail
            assertTrue("Should be duplicate email error",
                e.getMessage().contains("already in use") || e.getMessage().contains("email"));
            System.out.println("Duplicate email test passed - registration correctly rejected");
        }
    }
}
