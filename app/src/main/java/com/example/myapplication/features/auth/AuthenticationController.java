package com.example.myapplication.features.auth;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;

import com.example.myapplication.core.DeviceLoginStore;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthenticationController {

    private final Context context;
    private final AuthenticationCallback callback;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface AuthenticationCallback {
        void onLoginSuccess(User user);
        void onLoginFailure(String message);
        void onNoSavedUser(); // For auto-login when no user is persisted
        default void onLoading(boolean isLoading) {}
    }


    /**
     * Constructor for AuthenticationController.
     *
     * @param context  Application context
     * @param callback Callback interface for authentication events
     */
    public AuthenticationController(Context context, AuthenticationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Attempts to log in using the saved user session from DeviceLoginStore.
     */
    public void attemptAutoLogin() {
        User rememberedUser = DeviceLoginStore.getRememberedUser(context);
        if (rememberedUser == null) {
            callback.onNoSavedUser();
            return;
        }

        if (isLocalUser(rememberedUser)) {
            finalizeLogin(rememberedUser);
            return;
        }

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || firebaseUser.getUid() == null
                || !firebaseUser.getUid().equals(rememberedUser.getUid())) {
            // Session invalid or mismatch
            logout(); // Ensure clean state
            callback.onLoginFailure("Unable to restore your session. Please login.");
            return;
        }

        callback.onLoading(true);

        // Verify with Firestore
        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    callback.onLoading(false);
                    if (!task.isSuccessful()) {
                        // Fallback to saved user if network/db error
                        finalizeLogin(rememberedUser); 
                        return;
                    }

                    DocumentSnapshot d = task.getResult();
                    if (d == null) {
                        finalizeLogin(rememberedUser);
                        return;
                    }

                    User user = new User();
                    user.setUid(firebaseUser.getUid());
                    user.setEmail(firebaseUser.getEmail());

                    String role = null;
                    Object roleValue = d.get("role");
                    if (roleValue != null) {
                        role = roleValue.toString();
                    }
                    user.setRole(role != null ? role : "user");

                    if (d.contains("username")) {
                        Object username = d.get("username");
                        if (username != null) {
                            user.setUsername(username.toString());
                        }
                    }

                    finalizeLogin(user);
                });
    }

    /**
     * Attempts to log in with email and password.
     * @param email    User email
     * @param password User password
     */
    public void login(String email, String password) {
        if (tryDummyUserLogin(email, password)) {
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            callback.onLoginFailure("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            callback.onLoginFailure("Enter your password");
            return;
        }

        callback.onLoading(true);

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onLoading(false);
                callback.onLoginFailure(task.getException() != null ? task.getException().getMessage() : "Login failed");
                return;
            }
            
            FirebaseUser firebaseUser = auth.getCurrentUser();
            if (firebaseUser == null) {
                callback.onLoading(false);
                callback.onLoginFailure("Authentication failed.");
                return;
            }

            String uid = firebaseUser.getUid();
            db.collection("users").document(uid).get().addOnCompleteListener(rt -> {
                callback.onLoading(false);
                if (!rt.isSuccessful()) {
                    callback.onLoginFailure("Failed to load user role");
                    return;
                }
                DocumentSnapshot d = rt.getResult();
                String role = null;
                if (d != null) {
                    Object roleValue = d.get("role");
                    if (roleValue != null) {
                        role = roleValue.toString();
                    }
                }

                // Create User object from Firebase data
                User user = new User();
                user.setUid(uid);
                user.setEmail(firebaseUser.getEmail());
                user.setRole(role != null ? role : "user");
                if (d != null && d.contains("username")) {
                    Object username = d.get("username");
                    if (username != null) {
                        user.setUsername(username.toString());
                    }
                }

                finalizeLogin(user);
            });
        });
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        DeviceLoginStore.markLoggedOut(context);
        UserSession.getInstance().clearSession();
        auth.signOut();
    }

    /**
     * Finalizes the login process by setting the user session and persisting the user.
     *
     * @param user The logged-in user
     */
    private void finalizeLogin(User user) {
        UserSession.getInstance().setCurrentUser(user);
        DeviceLoginStore.rememberUser(context, user);
        callback.onLoginSuccess(user);
    }

    /**
     * Checks if the user is a local dummy user.
     *
     * @param user The user to check
     * @return true if the user is a local dummy user, false otherwise
     */
    private boolean isLocalUser(User user) {
        return user.getUid() != null && user.getUid().startsWith("LOCAL_");
    }

    /**
     * Attempts to log in as a dummy user for local testing.
     *
     * @param email The email provided
     * @param pass  The password provided
     * @return true if dummy login was successful, false otherwise
     */
    private boolean tryDummyUserLogin(String email, String pass) {
        final String dummyEmail = "user@example.com";
        final String dummyPassword = "password123";

        if (!dummyEmail.equalsIgnoreCase(email) || !dummyPassword.equals(pass)) {
            return false;
        }

        User dummyUser = new User();
        dummyUser.setUid("LOCAL_DUMMY_USER");
        dummyUser.setEmail(dummyEmail);
        dummyUser.setUsername("Demo User");
        dummyUser.setRole("user");
        dummyUser.setCreatedAt(System.currentTimeMillis());

        finalizeLogin(dummyUser);
        return true;
    }
}