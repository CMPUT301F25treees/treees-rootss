package com.example.myapplication.features.user.profile;

import android.text.TextUtils;
import android.util.Patterns;

import com.example.myapplication.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Controller class for managing user profile operations.
 */
public class ProfileController {
    private final ProfileView view;
    private final ProfileModel model;
    private final FirebaseAuth auth;

    private String pendingEmailChange = null;
    private String pendingFirstName = null;

    /**
     * Constructor with dependency injection.
     * @param view
     * @param model
     * @param auth
     */
    public ProfileController(ProfileView view, ProfileModel model, FirebaseAuth auth) {
        this.view = view;
        this.model = model;
        this.auth = auth;
    }

    /**
     * Default constructor initializing with default model and auth instances.
     * @param view
     */
    public ProfileController(ProfileView view) {
        this(view, new ProfileModel(), FirebaseAuth.getInstance());
    }

    /**
     * Loads the user profile data and updates the view.
     */
    public void loadProfile() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        view.showLoading(true);
        model.fetchUserProfile(firebaseUser.getUid(), new ProfileModel.DataCallback<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                view.showLoading(false);
                if (snapshot.exists()) {
                    String firstName = snapshot.getString("firstName");
                    if (TextUtils.isEmpty(firstName)) {
                        firstName = snapshot.getString("username");
                    }
                    String lastName = snapshot.getString("lastName");
                    String phone = snapshot.getString("cell");

                    String email = snapshot.getString("email");
                    if (TextUtils.isEmpty(email)) {
                        email = firebaseUser.getEmail();
                    }
                    
                    view.showProfileData(firstName, lastName, email, phone);
                }
            }

            @Override
            public void onFailure(Exception e) {
                view.showLoading(false);
                view.showToast("Failed to load profile");
            }
        });
    }

    /**
     * Saves the updated profile data.
     * @param firstName
     * @param lastName
     * @param email
     * @param phone
     */
    public void saveProfile(String firstName, String lastName, String email, String phone) {
        if (TextUtils.isEmpty(firstName)) {
            view.showFirstNameError("First name is required");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view.showEmailError("Invalid email address");
            return;
        }

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        view.showLoading(true);
        boolean emailChanged = !Objects.equals(firebaseUser.getEmail(), email);

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("username", firstName);
        updates.put("lastName", TextUtils.isEmpty(lastName) ? null : lastName);
        updates.put("cell", TextUtils.isEmpty(phone) ? null : phone);

        model.updateUserProfile(firebaseUser.getUid(), updates, new ProfileModel.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (emailChanged) {
                    attemptEmailChange(firebaseUser, email, firstName);
                } else {
                    // No email change, just finish
                    notifySuccess(firstName, email);
                }
            }

            @Override
            public void onFailure(Exception e) {
                view.showLoading(false);
                view.showToast("Update failed: " + e.getMessage());
            }
        });
    }

    /**
     * Attempts to change the user's email, handling re-authentication if required.
     * @param user
     * @param newEmail
     * @param firstName
     */
    private void attemptEmailChange(FirebaseUser user, String newEmail, String firstName) {
        model.updateEmail(user, newEmail, new ProfileModel.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // Email verification sent and firestore updated
                pendingEmailChange = null;
                pendingFirstName = null;
                view.showLoading(false);
                view.showToast("Check " + newEmail + " to confirm your new email (check spam)");
                // We still update local session with new email because Firestore was updated
                updateLocalSessionInView(firstName, newEmail);
                view.navigateBack();
            }

            @Override
            public void onFailure(Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                if (e instanceof FirebaseAuthRecentLoginRequiredException || errorMsg.contains("requires-recent-login")) {
                    pendingEmailChange = newEmail;
                    pendingFirstName = firstName;
                    view.showPasswordDialog();
                } else if (errorMsg.contains("email-already-in-use")) {
                    view.showLoading(false);
                    view.showEmailError("This email is already in use");
                } else if (errorMsg.contains("invalid-email")) {
                    view.showLoading(false);
                    view.showEmailError("Invalid email format");
                } else {
                    // Even if email update on Auth fails, Firestore might have been updated in original logic?
                    // Actually original logic: verifyBeforeUpdateEmail -> onSuccess -> db.update
                    // So if verify fails, Firestore IS NOT updated.
                    // BUT original logic had a catch for onFailure of verify:
                    // It didn't update firestore there.
                    
                    view.showLoading(false);
                    view.showToast("Failed to update email: " + errorMsg);
                }
            }
        });
    }

    /**
     * Called when the user has confirmed their password for re-authentication.
     * @param password
     */
    public void onPasswordConfirmed(String password) {
        if (pendingEmailChange == null) return;
        
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        view.showLoading(true);
        model.reauthenticate(user, password, new ProfileModel.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                attemptEmailChange(user, pendingEmailChange, pendingFirstName);
            }

            @Override
            public void onFailure(Exception e) {
                view.showLoading(false);
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                if (errorMsg.contains("wrong-password") || errorMsg.contains("invalid-credential")) {
                    view.showToast("Incorrect password");
                    view.showPasswordDialog(); // Try again
                } else {
                    view.showToast("Authentication failed: " + errorMsg);
                    pendingEmailChange = null;
                    pendingFirstName = null;
                }
            }
        });
    }

    /**
     * Called when the password dialog is cancelled.
     */
    public void onPasswordDialogCancelled() {
        pendingEmailChange = null;
        pendingFirstName = null;
    }

    /**
     * Notifies the view of successful profile update.
     * @param firstName
     * @param email
     */
    private void notifySuccess(String firstName, String email) {
        view.updateLocalSession(firstName, email);
        view.showLoading(false);
        view.showToast("Profile updated successfully!");
        view.navigateBack();
    }

    /**
     * Updates the local session in the view.
     * @param firstName
     * @param email
     */
    private void updateLocalSessionInView(String firstName, String email) {
        view.updateLocalSession(firstName, email);
    }
}
