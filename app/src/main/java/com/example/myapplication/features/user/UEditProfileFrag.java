package com.example.myapplication.features.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.core.DeviceLoginStore;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UEditProfileFrag extends Fragment {
    private static final String TAG = "UEditProfileFrag";
    private TextInputEditText inputFirstName, inputLastName, inputEmail, inputPhone;
    private TextInputLayout tilFirstName, tilEmail;
    private View progressView;
    private MaterialButton saveButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Store these for retry after reauthentication
    private String pendingEmailChange = null;
    private String pendingFirstName = null;

    /**
     * Default public constructor for the fragment.
     * Inflates {@code R.layout.fragment_u_edit_profile} as the associated view.
     * @param None
     * @return void
     */
    public UEditProfileFrag() {
        super(R.layout.fragment_u_edit_profile);
    }

    /**
     * Called immediately after the view hierarchy associated with this fragment has been created.
     *
     * @param view the view returned by {@link #onCreateView}
     * @param savedInstanceState previously saved state, if any
     * @return void
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        initViews(view);
        populateFields();
    }

    /**
     * Initializes view components and sets up button listeners.
     *
     * @param view The root view of the fragment.
     * @return void
     */
    private void initViews(View view) {
        inputFirstName = view.findViewById(R.id.edit_first_name_input);
        inputLastName = view.findViewById(R.id.edit_last_name_input);
        inputEmail = view.findViewById(R.id.edit_email_input);
        inputPhone = view.findViewById(R.id.edit_phone_input);
        tilFirstName = view.findViewById(R.id.tilEditFirstName);
        tilEmail = view.findViewById(R.id.tilEditEmail);
        progressView = view.findViewById(R.id.editProfileProgress);
        saveButton = view.findViewById(R.id.btnSaveProfile);
        MaterialButton backButton = view.findViewById(R.id.btnBackProfile);
        ImageButton goBackButton = view.findViewById(R.id.bckButton);

        saveButton.setOnClickListener(v -> attemptSave());
        View.OnClickListener backListener = v -> NavHostFragment.findNavController(this).popBackStack();
        backButton.setOnClickListener(backListener);
        goBackButton.setOnClickListener(backListener);
    }

    /**
     * Populates the input fields with the current user's profile data from Firestore.
     *
     * @param None
     * @return void
     */
    private void populateFields() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        setLoading(true);
        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    setLoading(false);
                    if (snapshot.exists()) {
                        String firstName = snapshot.getString("firstName");
                        if (TextUtils.isEmpty(firstName)) {
                            firstName = snapshot.getString("username");
                        }
                        inputFirstName.setText(firstName);
                        inputLastName.setText(snapshot.getString("lastName"));
                        inputPhone.setText(snapshot.getString("cell"));

                        String email = snapshot.getString("email");
                        if (TextUtils.isEmpty(email)) {
                            email = firebaseUser.getEmail();
                        }
                        inputEmail.setText(email);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Failed to load profile");
                });
    }

    /**
     * Validates input fields and attempts to save profile changes to Firestore and Firebase Auth.
     *
     * @param None
     * @return void
     */
    private void attemptSave() {
        tilFirstName.setError(null);
        tilEmail.setError(null);

        String firstName = textOf(inputFirstName);
        String lastName = textOf(inputLastName);
        String email = textOf(inputEmail);
        String phone = textOf(inputPhone);

        if (TextUtils.isEmpty(firstName)) {
            tilFirstName.setError(getString(R.string.edit_profile_first_name_required));
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.edit_profile_email_invalid));
            return;
        }

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        setLoading(true);
        boolean emailChanged = !Objects.equals(firebaseUser.getEmail(), email);

        // Update Firestore first (non-email fields)
        WriteBatch batch = db.batch();
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("username", firstName);
        updates.put("lastName", TextUtils.isEmpty(lastName) ? null : lastName);
        updates.put("cell", TextUtils.isEmpty(phone) ? null : phone);

        batch.set(db.collection("users").document(firebaseUser.getUid()), updates, SetOptions.merge());
        batch.commit()
                .addOnSuccessListener(v -> {
                    if (emailChanged) {
                        // Attempt email change with reauthentication support
                        attemptEmailChange(firebaseUser, email, firstName);
                    } else {
                        updateLocalSession(firstName, email);
                        setLoading(false);
                        toast("Profile updated successfully!");
                        NavHostFragment.findNavController(this).popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Update failed: " + e.getMessage());
                });
    }

    /**
     * Attempts to change the user's email, handling reauthentication if required.
     *
     * @param user The current Firebase user.
     * @param newEmail The new email address to set.
     * @param firstName The updated first name for local session update.
     * @return void
     */
    private void attemptEmailChange(FirebaseUser user, String newEmail, String firstName) {
        // Send a verification link to the new email before applying the change
        user.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Verification email sent to new address");
                    db.collection("users").document(user.getUid())
                            .update("email", newEmail)
                            .addOnSuccessListener(v3 -> {
                                updateLocalSession(firstName, newEmail);
                                pendingEmailChange = null;
                                pendingFirstName = null;
                                setLoading(false);
                                toast("Check " + newEmail + " to confirm your new email (check spam)");
                                NavHostFragment.findNavController(this).popBackStack();
                            })
                            .addOnFailureListener(e -> {
                                // Email will still update after verification; keep local state in sync
                                updateLocalSession(firstName, newEmail);
                                pendingEmailChange = null;
                                pendingFirstName = null;
                                setLoading(false);
                                toast("Verification sent to " + newEmail + " (check spam). Confirm it to finish the update.");
                                NavHostFragment.findNavController(this).popBackStack();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Email update failed", e);
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "";

                    // Check if it's a reauthentication error
                    if (e instanceof FirebaseAuthRecentLoginRequiredException ||
                            errorMsg.contains("requires-recent-login")) {
                        // Store pending change and show password dialog
                        pendingEmailChange = newEmail;
                        pendingFirstName = firstName;
                        showPasswordDialog(user);
                    } else if (errorMsg.contains("email-already-in-use")) {
                        setLoading(false);
                        tilEmail.setError("This email is already in use");
                    } else if (errorMsg.contains("invalid-email")) {
                        setLoading(false);
                        tilEmail.setError("Invalid email format");
                    } else {
                        setLoading(false);
                        toast("Failed to update email: " + errorMsg);
                    }
                });
    }

    /**
     * Displays a dialog prompting the user to enter their password for reauthentication.
     *
     * @param user The current Firebase user.
     * @return void
     */
    private void showPasswordDialog(FirebaseUser user) {
        setLoading(false);

        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_password_input, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.dialog_password_input);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.dialog_password_layout);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Confirm Password")
                .setMessage("For security, please enter your password to change your email")
                .setView(dialogView)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", (d, which) -> {
                    pendingEmailChange = null;
                    pendingFirstName = null;
                    d.dismiss();
                })
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = passwordInput.getText() != null ?
                        passwordInput.getText().toString().trim() : "";

                if (TextUtils.isEmpty(password)) {
                    passwordLayout.setError("Password required");
                    return;
                }

                passwordLayout.setError(null);
                dialog.dismiss();

                reauthenticateAndRetry(user, password);
            });
        });

        dialog.show();
    }

    /**
     * Reauthenticates the user with the provided password and retries the pending email change.
     *
     * @param user The current Firebase user.
     * @param password The password entered by the user for reauthentication.
     * @return void
     */
    private void reauthenticateAndRetry(FirebaseUser user, String password) {
        if (pendingEmailChange == null) return;

        setLoading(true);

        String currentEmail = user.getEmail();
        if (currentEmail == null) {
            setLoading(false);
            toast("Error: No email associated with account");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, password);

        user.reauthenticate(credential)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Reauthentication successful, retrying email update");
                    // Now retry the email change
                    attemptEmailChange(user, pendingEmailChange, pendingFirstName);
                    pendingEmailChange = null;
                    pendingFirstName = null;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Reauthentication failed", e);
                    setLoading(false);
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                    if (errorMsg.contains("wrong-password") || errorMsg.contains("invalid-credential")) {
                        toast("Incorrect password");
                        showPasswordDialog(user); // Try again
                    } else {
                        toast("Authentication failed: " + errorMsg);
                        pendingEmailChange = null;
                        pendingFirstName = null;
                    }
                });
    }

    /**
     * Updates the local user session and device storage with the new profile data.
     *
     * @param firstName The updated first name.
     * @param email The updated email address.
     * @return void
     */
    private void updateLocalSession(String firstName, String email) {
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            user.setUsername(firstName);
            user.setEmail(email);
            UserSession.getInstance().setCurrentUser(user);
            if (getContext() != null) {
                DeviceLoginStore.rememberUser(getContext(), user);
            }
        }
    }

    /**
     * Sets the loading state of the fragment, showing or hiding the progress indicator
     * and enabling/disabling the save button.
     *
     * @param loading true to show loading state, false to hide
     * @return void
     */
    private void setLoading(boolean loading) {
        if (progressView != null) {
            progressView.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (saveButton != null) {
            saveButton.setEnabled(!loading);
        }
    }

    /**
     * Retrieves and trims text from a TextInputEditText.
     * @param input
     * @return
     */
    private String textOf(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    /**
     * Displays a short Toast message.
     *
     * @param message The message to display.
     * @return void
     */
    private void toast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets a custom FirebaseAuth instance (for testing purposes).
     * @param auth
     * @return void
     */
    void setAuth(FirebaseAuth auth) {
        this.auth = auth;
    }

    /**
     * Sets a custom FirebaseFirestore instance (for testing purposes).
     * @param db
     * @return void
     */
    void setDb(FirebaseFirestore db) {
        this.db = db;
    }
}
